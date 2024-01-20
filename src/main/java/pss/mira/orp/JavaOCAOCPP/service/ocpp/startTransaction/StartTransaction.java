package pss.mira.orp.JavaOCAOCPP.service.ocpp.startTransaction;

import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.chargeSessionInfo.ChargeSessionInfo;

import java.util.List;

public interface StartTransaction {
    void sendStartTransaction(List<Object> parsedMessage);

    void sendStartTransaction(ChargeSessionInfo chargeSessionInfo);
}
