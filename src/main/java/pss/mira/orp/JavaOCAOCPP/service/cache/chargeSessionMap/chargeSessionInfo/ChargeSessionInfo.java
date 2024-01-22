package pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.chargeSessionInfo;

import lombok.Data;

@Data
public class ChargeSessionInfo {
    private int connectorId;
    private String idTag;
    private int transactionId;
    private PreparingTimer preparingTimer;
    private int startFullStationConsumedEnergy = 0;
}