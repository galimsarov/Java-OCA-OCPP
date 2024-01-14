package pss.mira.orp.JavaOCAOCPP.models.requests.ocpp;

import eu.chargetime.ocpp.model.core.ChargePointErrorCode;
import eu.chargetime.ocpp.model.core.ChargePointStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class StatusNotificationRequest {
    private int id;
    private ChargePointErrorCode errorCode;
    private ChargePointStatus status;
}