package pss.mira.orp.JavaOCAOCPP.service.ocpp.statusNotification;

import pss.mira.orp.JavaOCAOCPP.models.info.ocpp.StatusNotificationInfo;

public interface StatusNotification {
    void sendStatusNotification(StatusNotificationInfo statusNotificationInfo);
}