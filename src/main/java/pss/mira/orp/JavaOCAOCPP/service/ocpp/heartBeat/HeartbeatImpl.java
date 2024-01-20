package pss.mira.orp.JavaOCAOCPP.service.ocpp.heartBeat;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.HeartbeatConfirmation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.pc.TimeSetter;

import java.util.Set;

@Service
@Slf4j
public class HeartbeatImpl implements Heartbeat {
    private final TimeSetter timeSetter;

    public HeartbeatImpl(TimeSetter timeSetter) {
        this.timeSetter = timeSetter;
    }

    @Override
    public void sendHeartbeat(ClientCoreProfile core, JSONClient client) {
        // Use the feature profile to help create event
        Request request = core.createHeartbeatRequest();

        if (client == null) {
            log.warn("There is no connection to the central system. The heartbeat message will not be sent");
        } else {
            log.info("Sent to central system: " + request.toString());
            // Client returns a promise which will be filled once it receives a confirmation.
            try {
                client.send(request).whenComplete((confirmation, ex) -> {
                    log.info("Received from the central system: " + confirmation.toString());
                    handleResponse(confirmation);
                });
            } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                log.warn("An error occurred while sending or processing heartbeat request");
            }
        }
    }

    private void handleResponse(Confirmation confirmation) {
        HeartbeatConfirmation heartbeatConfirmation = (HeartbeatConfirmation) confirmation;
        timeSetter.setTime(heartbeatConfirmation.getCurrentTime());

        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        log.info("Запущено потоков " + threads.size());
    }
}