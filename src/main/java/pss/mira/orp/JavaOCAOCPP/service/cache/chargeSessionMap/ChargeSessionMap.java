package pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap;

import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.chargeSessionInfo.ChargeSessionInfo;

public interface ChargeSessionMap {
    void addToChargeSessionMap(int connectorId, String idTag);

    ChargeSessionInfo getChargeSessionInfo(int connectorId);

    Integer getConnectorId(int transactionId);

    void setTransactionId(int connectorId, int transactionId);

    void stopPreparingTimer(int connectorId);

    boolean canSendStartTransaction(int connectorId);
}