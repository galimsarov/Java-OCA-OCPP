package pss.mira.orp.JavaOCAOCPP.service.ocpp.meterValues;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.core.MeterValuesRequest;
import eu.chargetime.ocpp.model.core.SampledValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.queues.Queues;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.ChargeSessionMap;
import pss.mira.orp.JavaOCAOCPP.service.cache.configuration.ConfigurationCache;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.core.CoreHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.remoteTrigger.RemoteTriggerHandler;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.time.ZonedDateTime;
import java.util.*;

import static eu.chargetime.ocpp.model.core.ValueFormat.Raw;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.SaveToCache;

@Service
@Slf4j
public class MeterValuesImpl implements MeterValues {
    private final BootNotification bootNotification;
    private final ConfigurationCache configurationCache;
    private final ChargeSessionMap chargeSessionMap;
    private final ConnectorsInfoCache connectorsInfoCache;
    private final CoreHandler coreHandler;
    private final RemoteTriggerHandler remoteTriggerHandler;
    private final Queues queues;
    private final Sender sender;
    private final Set<Integer> chargingConnectors = new HashSet<>();
    private MeterValuesRequest cachedMeterValuesRequest = null;

    public MeterValuesImpl(
            BootNotification bootNotification,
            ConfigurationCache configurationCache,
            ChargeSessionMap chargeSessionMap,
            ConnectorsInfoCache connectorsInfoCache,
            CoreHandler coreHandler,
            RemoteTriggerHandler remoteTriggerHandler,
            Queues queues,
            Sender sender
    ) {
        this.bootNotification = bootNotification;
        this.configurationCache = configurationCache;
        this.chargeSessionMap = chargeSessionMap;
        this.connectorsInfoCache = connectorsInfoCache;
        this.coreHandler = coreHandler;
        this.remoteTriggerHandler = remoteTriggerHandler;
        this.queues = queues;
        this.sender = sender;
    }

    @Override
    public void addToChargingConnectors(int connectorId, int transactionId) {
        chargingConnectors.add(connectorId);
        String meterValuesSampledData = configurationCache.getMeterValuesSampledData();
        int meterValueSampleInterval = configurationCache.getMeterValueSampleInterval();
        if (meterValuesSampledData != null && meterValueSampleInterval != 0) {
            sendMeterValues(connectorId, meterValuesSampledData, "Transaction.Begin", transactionId);
            Thread meterValuesThread =
                    getMeterValuesThread(connectorId, meterValueSampleInterval, meterValuesSampledData, transactionId);
            meterValuesThread.start();
        }
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
        ClientCoreProfile core = coreHandler.getCore();
        JSONClient client = bootNotification.getClient();
        SampledValue[] sampledValues = getSampledValues(meterValuesSampledData, connectorId, context);
        // Use the feature profile to help create event
        MeterValuesRequest request = core.createMeterValuesRequest(connectorId, ZonedDateTime.now(), sampledValues);
        request.setTransactionId(transactionId);
        if (client == null) {
            sender.sendRequestToQueue(
                    queues.getOCPPCache(),
                    UUID.randomUUID().toString(),
                    SaveToCache.name(),
                    request,
                    "meterValues"
            );
        } else {
            log.info("Ready to send meter values: " + request);
            // Client returns a promise which will be filled once it receives a confirmation.
            try {
                client.send(request).whenComplete((confirmation, ex) ->
                        log.info("Received from the central system: " + confirmation.toString()));
            } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                log.warn("An error occurred while sending or processing meter value request");
            }
        }
        cachedMeterValuesRequest = request;
        remoteTriggerHandler.meterValuesCanBeSent();
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
        String meterValuesSampledData = configurationCache.getMeterValuesSampledData();
        int transactionId = chargeSessionMap.getChargeSessionInfo(connectorId).getTransactionId();
        chargeSessionMap.removeFromChargeSessionMap(connectorId);
        chargingConnectors.remove(connectorId);
        sendMeterValues(connectorId, meterValuesSampledData, "Transaction.End", transactionId);
    }
}