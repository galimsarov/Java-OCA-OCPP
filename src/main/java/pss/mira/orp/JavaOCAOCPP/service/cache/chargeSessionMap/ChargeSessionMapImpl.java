package pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap;

import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.chargeSessionInfo.ChargeSessionInfo;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.chargeSessionInfo.PreparingTimer;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChargeSessionMapImpl implements ChargeSessionMap {
    private final Map<Integer, ChargeSessionInfo> map = new HashMap<>();

    @Override
    public void addToChargeSessionMap(int connectorId, String idTag) {
        ChargeSessionInfo chargeSessionInfo = new ChargeSessionInfo();
        chargeSessionInfo.setConnectorId(connectorId);
        chargeSessionInfo.setIdTag(idTag);
        chargeSessionInfo.setPreparingTimer(new PreparingTimer());
        map.put(connectorId, chargeSessionInfo);
    }

    @Override
    public ChargeSessionInfo getChargeSessionInfo(int connectorId) {
        return map.get(connectorId);
    }

    @Override
    public Integer getConnectorId(int transactionId) {
        for (Map.Entry<Integer, ChargeSessionInfo> entry : map.entrySet()) {
            if (entry.getValue().getTransactionId() == transactionId) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public void setTransactionId(int connectorId, int transactionId) {
        ChargeSessionInfo chargeSessionInfo = map.get(connectorId);
        if (transactionId != 0) {
            chargeSessionInfo.setTransactionId(transactionId);
        }
    }

    @Override
    public void stopPreparingTimer(int connectorId) {
        PreparingTimer preparingTimer = map.get(connectorId).getPreparingTimer();
        preparingTimer.setStopReceived(true);
    }

    @Override
    public boolean canSendStartTransaction(int connectorId) {
        return map.get(connectorId).getPreparingTimer().getTimer() > 0;
    }
}