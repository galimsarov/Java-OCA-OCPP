package pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification;

import eu.chargetime.ocpp.model.Confirmation;

public interface BootNotification {
    void sendBootNotification(String source);
    void handleResponse(Confirmation confirmation, String source);
}