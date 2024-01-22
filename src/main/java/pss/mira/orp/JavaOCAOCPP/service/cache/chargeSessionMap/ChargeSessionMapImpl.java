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
    public void addToChargeSessionMap(int connectorId, String idTag, boolean isRemoteStart) {
        ChargeSessionInfo chargeSessionInfo = new ChargeSessionInfo();
        chargeSessionInfo.setConnectorId(connectorId);
        chargeSessionInfo.setIdTag(idTag);
        if (isRemoteStart) {
            chargeSessionInfo.setPreparingTimer(new PreparingTimer());
        }
        chargeSessionInfo.setRemoteStart(isRemoteStart);
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

    @Override
    public void removeFromChargeSessionMap(int connectorId) {
        map.remove(connectorId);
    }

    @Override
    public void setStartFullStationConsumedEnergy(int connectorId, int startFullStationConsumedEnergy) {
        map.get(connectorId).setStartFullStationConsumedEnergy(startFullStationConsumedEnergy);
    }

    @Override
    public int getStartFullStationConsumedEnergy(int connectorId) {
        return map.get(connectorId).getStartFullStationConsumedEnergy();
    }

    @Override
    public boolean isRemoteStart(int connectorId) {
        return map.get(connectorId).isRemoteStart();
    }

    @Override
    public void setRemoteStopByTransactionId(int transactionId) {
        for (Map.Entry<Integer, ChargeSessionInfo> entry : map.entrySet()) {
            if (entry.getValue().getTransactionId() == transactionId) {
                entry.getValue().setRemoteStop(true);
                break;
            }
        }
    }

    @Override
    public boolean isRemoteStop(int connectorId) {
        return map.get(connectorId).isRemoteStop();
    }
}