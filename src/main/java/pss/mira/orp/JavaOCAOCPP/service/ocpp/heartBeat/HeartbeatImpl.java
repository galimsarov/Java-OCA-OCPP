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
import pss.mira.orp.JavaOCAOCPP.service.ocpp.Utils;

@Service
@Slf4j
public class HeartbeatImpl implements Heartbeat {
    private final Utils utils;

    public HeartbeatImpl(Utils utils) {
        this.utils = utils;
    }

    @Override
    public void sendHeartbeat(ClientCoreProfile core, JSONClient client) {
        // Use the feature profile to help create event
        Request request = core.createHeartbeatRequest();

        // Client returns a promise which will be filled once it receives a confirmation.
        try {
            client.send(request).whenComplete((confirmation, ex) -> {
                log.info(confirmation.toString());
                handleResponse(confirmation);
            });
        } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
            log.warn("An error occurred while sending or processing heartbeat request");
        }
    }

    private void handleResponse(Confirmation confirmation) {
        HeartbeatConfirmation heartbeatConfirmation = (HeartbeatConfirmation) confirmation;
        Thread endOfChargingThread = utils.getEndOfChargingThread(heartbeatConfirmation.getCurrentTime());
        endOfChargingThread.start();
    }
}