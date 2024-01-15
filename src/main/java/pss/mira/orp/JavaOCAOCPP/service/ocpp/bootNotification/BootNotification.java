package pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification;

import eu.chargetime.ocpp.JSONClient;

import java.util.List;

public interface BootNotification {
    void sendBootNotification(List<Object> parsedMessage);

    JSONClient getClient();


}