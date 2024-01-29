package pss.mira.orp.JavaOCAOCPP.models.info.ocpp;

import eu.chargetime.ocpp.model.core.ChargePointErrorCode;
import eu.chargetime.ocpp.model.core.ChargePointStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class StatusNotificationInfo {
    private int id;
    private ChargePointErrorCode errorCode;
    private ChargePointStatus status;
}