package pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;

import java.util.List;
import java.util.Map;

public interface BootNotification {
    void sendBootNotification(List<Map<String, Object>> configZSList);

    JSONClient getClient();

    ClientCoreProfile getCore();
}