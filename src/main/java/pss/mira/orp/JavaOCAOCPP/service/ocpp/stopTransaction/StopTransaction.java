package pss.mira.orp.JavaOCAOCPP.service.ocpp.stopTransaction;

import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.chargeSessionInfo.ChargeSessionInfo;

import java.util.List;

public interface StopTransaction {
    void sendStopTransaction(List<Object> parsedMessage);

    void sendStopTransaction(ChargeSessionInfo chargeSessionInfo);
}
