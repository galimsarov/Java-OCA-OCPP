package pss.mira.orp.JavaOCAOCPP.service.ocpp.meterValues;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.core.MeterValuesRequest;
import eu.chargetime.ocpp.model.core.SampledValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.Handler;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.time.ZonedDateTime;
import java.util.*;

import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.Get;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.configuration;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.getConfigurationForMeterValues;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Services.bd;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getDBTablesGetRequest;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getResult;

@Service
@Slf4j
public class MeterValuesImpl implements MeterValues {
    private final BootNotification bootNotification;
    private final Handler handler;
    private final Sender sender;
    private final Set<Integer> chargingConnectors = new HashSet<>();
    private List<Map<String, Object>> configurationList = null;

    public MeterValuesImpl(BootNotification bootNotification, Handler handler, Sender sender) {
        this.bootNotification = bootNotification;
        this.handler = handler;
        this.sender = sender;
    }

    @Override
    public void addToChargingConnectors(int connectorId) {
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
        log.info("!!!meterValuesSampledData: " + meterValuesSampledData);
        if (meterValuesSampledData != null && meterValueSampleInterval != 0) {
            Thread meterValuesThread =
                    getMeterValuesThread(connectorId, meterValueSampleInterval, meterValuesSampledData);
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

    private Thread getMeterValuesThread(int connectorId, int meterValueSampleInterval, String meterValuesSampledData) {
        Runnable runMeterValues = () -> {
            while (true) {
                try {
                    Thread.sleep(meterValueSampleInterval * 1000L);
                } catch (InterruptedException e) {
                    log.error("Аn error while waiting for a meter values to be sent");
                }
                if (chargingConnectors.contains(connectorId)) {
                    sendMeterValues(connectorId, meterValuesSampledData);
                } else {
                    break;
                }
            }
        };
        return new Thread(runMeterValues);
    }

    private void sendMeterValues(int connectorId, String meterValuesSampledData) {
        ClientCoreProfile core = handler.getCore();
        JSONClient client = bootNotification.getClient();
        SampledValue[] sampledValues = getSampledValues(meterValuesSampledData);

        // Use the feature profile to help create event
        MeterValuesRequest request = core.createMeterValuesRequest(connectorId, ZonedDateTime.now(), sampledValues);
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

    private SampledValue[] getSampledValues(String meterValuesSampledData) {
        SampledValue sampledValue = new SampledValue();
        return new SampledValue[0];
    }

    @Override
    public void removeFromChargingConnectors(int connectorId) {
        chargingConnectors.remove(connectorId);
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