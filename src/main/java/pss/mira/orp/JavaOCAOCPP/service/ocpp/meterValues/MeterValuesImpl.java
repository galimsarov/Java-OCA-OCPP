package pss.mira.orp.JavaOCAOCPP.service.ocpp.meterValues;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.SampledValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.Handler;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class MeterValuesImpl implements MeterValues {
    private final BootNotification bootNotification;
    private final Handler handler;
    private final Set<Integer> chargingConnectors = new HashSet<>();

    public MeterValuesImpl(BootNotification bootNotification, Handler handler) {
        this.bootNotification = bootNotification;
        this.handler = handler;
    }

    @Override
    public void addToChargingConnectors(int connectorId) {
        chargingConnectors.add(connectorId);
        Thread meterValuesThread = getMeterValuesThread(connectorId);
        meterValuesThread.start();
    }

    private Thread getMeterValuesThread(int connectorId) {
        Runnable runMeterValues = () -> {
            while (true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    log.error("Аn error while waiting for a meter values to be sent");
                }
                if (chargingConnectors.contains(connectorId)) {
                    sendMeterValues(connectorId);
                } else {
                    break;
                }
            }
        };
        return new Thread(runMeterValues);
    }

    private void sendMeterValues(int connectorId) {
        ClientCoreProfile core = handler.getCore();
        JSONClient client = bootNotification.getClient();
        SampledValue[] sampledValues = getSampledValues();

        // Use the feature profile to help create event
        Request request = core.createMeterValuesRequest(connectorId, ZonedDateTime.now(), sampledValues);
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

    private SampledValue[] getSampledValues() {
        return new SampledValue[0];
    }

    @Override
    public void removeFromChargingConnectors(int connectorId) {
        chargingConnectors.remove(connectorId);
    }
}