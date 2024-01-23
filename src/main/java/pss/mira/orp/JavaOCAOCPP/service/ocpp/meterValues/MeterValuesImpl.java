package pss.mira.orp.JavaOCAOCPP.service.ocpp.meterValues;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.core.MeterValuesRequest;
import eu.chargetime.ocpp.model.core.SampledValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.ChargeSessionMap;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.Handler;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.time.ZonedDateTime;
import java.util.*;

import static eu.chargetime.ocpp.model.core.ValueFormat.Raw;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.Get;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.SaveToCache;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.configuration;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.getConfigurationForMeterValues;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Queues.bd;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Queues.ocppCache;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getDBTablesGetRequest;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getResult;

@Service
@Slf4j
public class MeterValuesImpl implements MeterValues {
    private final BootNotification bootNotification;
    private final ChargeSessionMap chargeSessionMap;
    private final ConnectorsInfoCache connectorsInfoCache;
    private final Handler handler;
    private final Sender sender;
    private final Set<Integer> chargingConnectors = new HashSet<>();
    private List<Map<String, Object>> configurationList = null;

    public MeterValuesImpl(
            BootNotification bootNotification,
            ChargeSessionMap chargeSessionMap,
            ConnectorsInfoCache connectorsInfoCache,
            Handler handler,
            Sender sender
    ) {
        this.bootNotification = bootNotification;
        this.chargeSessionMap = chargeSessionMap;
        this.connectorsInfoCache = connectorsInfoCache;
        this.handler = handler;
        this.sender = sender;
    }

    @Override
    public void addToChargingConnectors(int connectorId, int transactionId) {
        chargingConnectors.add(connectorId);
        // Запрашиваем актуальную конфигурацию
        log.info("Trying to receive the configuration from DB");
        sender.sendRequestToQueue(
                bd.name(),
                UUID.randomUUID().toString(),
                Get.name(),
                getDBTablesGetRequest(List.of(configuration.name())),
                getConfigurationForMeterValues.name()
        );
        String meterValuesSampledData;
        int meterValueSampleInterval;
        // Дожидаемся ответа, потому что без перечня данных и интервала нет смысла ничего запускать
        while (true) {
            if (configurationList == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Аn error while waiting for a get configuration response");
                }
            } else {
                meterValuesSampledData = getMeterValuesSampledData();
                meterValueSampleInterval = getMeterValueSampleInterval();
                configurationList = null;
                break;
            }
        }
        if (meterValuesSampledData != null && meterValueSampleInterval != 0) {
            sendMeterValues(connectorId, meterValuesSampledData, "Transaction.Begin", transactionId);
            Thread meterValuesThread =
                    getMeterValuesThread(connectorId, meterValueSampleInterval, meterValuesSampledData, transactionId);
            meterValuesThread.start();
        }
    }

    private int getMeterValueSampleInterval() {
        try {
            for (Map<String, Object> map : configurationList) {
                if (map.get("key").toString().equals("MeterValueSampleInterval")) {
                    return Integer.parseInt(map.get("value").toString());
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private String getMeterValuesSampledData() {
        try {
            for (Map<String, Object> map : configurationList) {
                if (map.get("key").toString().equals("MeterValuesSampledData")) {
                    return map.get("value").toString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Thread getMeterValuesThread(
            int connectorId, int meterValueSampleInterval, String meterValuesSampledData, int transactionId
    ) {
        Runnable runMeterValues = () -> {
            while (true) {
                try {
                    Thread.sleep(meterValueSampleInterval * 1000L);
                } catch (InterruptedException e) {
                    log.error("Аn error while waiting for a meter values to be sent");
                }
                if (chargingConnectors.contains(connectorId)) {
                    sendMeterValues(connectorId, meterValuesSampledData, "Sample.Periodic", transactionId);
                } else {
                    break;
                }
            }
        };
        return new Thread(runMeterValues);
    }

    private void sendMeterValues(int connectorId, String meterValuesSampledData, String context, int transactionId) {
        ClientCoreProfile core = handler.getCore();
        JSONClient client = bootNotification.getClient();
        SampledValue[] sampledValues = getSampledValues(meterValuesSampledData, connectorId, context);

        // Use the feature profile to help create event
        MeterValuesRequest request = core.createMeterValuesRequest(connectorId, ZonedDateTime.now(), sampledValues);
        request.setTransactionId(transactionId);
        if (client == null) {
            sender.sendRequestToQueue(
                    ocppCache.name(),
                    UUID.randomUUID().toString(),
                    SaveToCache.name(),
                    request,
                    "meterValues"
            );
        } else {
            log.info("Ready to send meter values: " + request);
            // Client returns a promise which will be filled once it receives a confirmation.
            try {
                client.send(request).whenComplete((confirmation, ex) -> {
                    log.info("Received from the central system: " + confirmation.toString());
                });
            } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                log.warn("An error occurred while sending or processing meter value request");
            }
        }
    }

    private SampledValue[] getSampledValues(String meterValuesSampledData, int connectorId, String context) {
        String[] meterValuesSampledDataArray = meterValuesSampledData.split(",");
        List<SampledValue> sampledValueList = new ArrayList<>();
        for (String meterValueType : meterValuesSampledDataArray) {
            switch (meterValueType) {
                case "Current.Import":
                    SampledValue currentImport =
                            new SampledValue(String.valueOf(connectorsInfoCache.getEVRequestedCurrent(connectorId)));
                    currentImport.setContext(context);
                    currentImport.setFormat(Raw);
                    currentImport.setMeasurand("Current.Import");
                    currentImport.setUnit("A");
                    sampledValueList.add(currentImport);
                    break;
                case "Current.Offered":
                    SampledValue currentOffered =
                            new SampledValue(String.valueOf(connectorsInfoCache.getCurrentAmperage(connectorId)));
                    currentOffered.setContext(context);
                    currentOffered.setFormat(Raw);
                    currentOffered.setMeasurand("Current.Offered");
                    currentOffered.setUnit("A");
                    sampledValueList.add(currentOffered);
                    break;
                case "Energy.Active.Import.Register":
                    SampledValue energyActiveImportRegister =
                            new SampledValue(
                                    String.valueOf(connectorsInfoCache.getFullStationConsumedEnergy(connectorId))
                            );
                    energyActiveImportRegister.setContext(context);
                    energyActiveImportRegister.setFormat(Raw);
                    energyActiveImportRegister.setMeasurand("Energy.Active.Import.Register");
                    energyActiveImportRegister.setUnit("Wh");
                    sampledValueList.add(energyActiveImportRegister);
                    break;
                case "Power.Active.Import":
                    SampledValue powerActiveImport =
                            new SampledValue(String.valueOf(connectorsInfoCache.getEVRequestedPower(connectorId)));
                    powerActiveImport.setContext(context);
                    powerActiveImport.setFormat(Raw);
                    powerActiveImport.setMeasurand("Power.Active.Import");
                    powerActiveImport.setUnit("kW");
                    sampledValueList.add(powerActiveImport);
                    break;
                case "SoC":
                    SampledValue SoC =
                            new SampledValue(String.valueOf(connectorsInfoCache.getPercent(connectorId)));
                    SoC.setContext(context);
                    SoC.setFormat(Raw);
                    SoC.setMeasurand("SoC");
                    SoC.setUnit("Percent");
                    sampledValueList.add(SoC);
                    break;
            }
        }
        SampledValue[] result = new SampledValue[sampledValueList.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = sampledValueList.get(i);
        }
        return result;
    }

    @Override
    public void removeFromChargingConnectors(int connectorId) {
        // Запрашиваем актуальную конфигурацию
        log.info("Trying to receive the configuration from DB");
        sender.sendRequestToQueue(
                bd.name(),
                UUID.randomUUID().toString(),
                Get.name(),
                getDBTablesGetRequest(List.of(configuration.name())),
                getConfigurationForMeterValues.name()
        );
        String meterValuesSampledData;
        // Дожидаемся ответа, потому что нужен перечень данных
        while (true) {
            if (configurationList == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Аn error while waiting for a get configuration response");
                }
            } else {
                meterValuesSampledData = getMeterValuesSampledData();
                configurationList = null;
                break;
            }
        }
        int transactionId = chargeSessionMap.getChargeSessionInfo(connectorId).getTransactionId();
        chargingConnectors.remove(connectorId);
        sendMeterValues(connectorId, meterValuesSampledData, "Transaction.End", transactionId);
    }

    @Override
    public void setConfigurationMap(List<Object> parsedMessage) {
        try {
            configurationList = getResult(parsedMessage);
        } catch (Exception ignored) {
            log.error("An error occurred while receiving configuration table from the message");
        }
    }
}