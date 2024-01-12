package pss.mira.orp.JavaOCAOCPP.service.ocpp.heartBeat;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;

public interface Heartbeat {
    void sendHeartbeat(ClientCoreProfile core, JSONClient client);
}
