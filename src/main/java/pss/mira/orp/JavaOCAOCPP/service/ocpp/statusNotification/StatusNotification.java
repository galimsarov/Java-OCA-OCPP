package pss.mira.orp.JavaOCAOCPP.service.ocpp.statusNotification;

import pss.mira.orp.JavaOCAOCPP.models.requests.ocpp.StatusNotificationRequest;

public interface StatusNotification {
    void sendStatusNotification(StatusNotificationRequest statusNotificationRequest);
}