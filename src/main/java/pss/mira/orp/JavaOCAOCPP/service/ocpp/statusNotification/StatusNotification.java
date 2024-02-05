package pss.mira.orp.JavaOCAOCPP.service.ocpp.statusNotification;

import pss.mira.orp.JavaOCAOCPP.models.info.ocpp.StatusNotificationInfo;

import java.util.List;

public interface StatusNotification {
    void sendStatusNotification(StatusNotificationInfo statusNotificationInfo);

    void sendTriggerMessageStatusNotification(List<Object> parsedMessage);
}