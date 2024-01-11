package pss.mira.orp.JavaOCAOCPP.service.ocpp.statusNotification;

import eu.chargetime.ocpp.model.core.ChargePointErrorCode;
import eu.chargetime.ocpp.model.core.ChargePointStatus;

public interface StatusNotification {
    void sendStatusNotification(int connectorId, ChargePointErrorCode errorCode, ChargePointStatus status);
}