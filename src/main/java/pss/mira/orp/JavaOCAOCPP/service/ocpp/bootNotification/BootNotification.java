package pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;

import java.util.List;

public interface BootNotification {
    void sendBootNotification(List<Object> parsedMessage);

    JSONClient getClient();

    ClientCoreProfile getCore();
}