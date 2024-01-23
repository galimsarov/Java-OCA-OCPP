package pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap;

import eu.chargetime.ocpp.model.core.ChargePointStatus;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.chargeSessionInfo.ChargeSessionInfo;

public interface ChargeSessionMap {
    void addToChargeSessionMap(
            int connectorId, String idTag, boolean isRemoteStart, ChargePointStatus chargePointStatus
    );

    ChargeSessionInfo getChargeSessionInfo(int connectorId);

    Integer getConnectorId(int transactionId);

    void setTransactionId(int connectorId, int transactionId);

    void stopPreparingTimer(int connectorId);

    boolean canSendStartTransaction(int connectorId);

    void removeFromChargeSessionMap(int connectorId);

    void setStartFullStationConsumedEnergy(int connectorId, int startFullStationConsumedEnergy);

    int getStartFullStationConsumedEnergy(int connectorId);

    boolean isRemoteStart(int connectorId);

    void setRemoteStopByTransactionId(int transactionId);

    boolean isRemoteStop(int connectorId);

    void deleteNotStartedRemoteTransactions();

    boolean isNotEmpty();
}