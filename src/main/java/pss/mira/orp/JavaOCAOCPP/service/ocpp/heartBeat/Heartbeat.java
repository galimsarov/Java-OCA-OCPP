package pss.mira.orp.JavaOCAOCPP.service.ocpp.heartBeat;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.core.BootNotificationConfirmation;

public interface Heartbeat {
    void sendHeartbeat(ClientCoreProfile core, JSONClient client);

    void sendTriggerMessageHeartbeat();

    Thread getHeartbeatThread(BootNotificationConfirmation bootNotificationConfirmation, String source);
}