package pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;

import java.util.List;

public interface BootNotification {
    void sendBootNotification(List<Object> parsedMessage, String source);

    JSONClient getClient();

    Request getBootNotificationRequest();

    void handleResponse(Confirmation confirmation, String source);
}