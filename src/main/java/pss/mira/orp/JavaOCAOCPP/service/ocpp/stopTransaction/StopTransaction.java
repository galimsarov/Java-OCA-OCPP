package pss.mira.orp.JavaOCAOCPP.service.ocpp.stopTransaction;

import eu.chargetime.ocpp.model.core.Reason;

import java.util.List;

public interface StopTransaction {
    void sendLocalStop(List<Object> parsedMessage);

    void sendRemoteStop(int connectorId);

    void checkTransactionCreation(List<Object> parsedMessage, List<Object> cashedRequest);

    void sendLocalStopWithReason(int connectorId, Reason reason);

    void checkTransactionsAfterReboot(List<Object> parsedMessage);
}