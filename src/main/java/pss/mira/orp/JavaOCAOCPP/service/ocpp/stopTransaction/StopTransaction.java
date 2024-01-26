package pss.mira.orp.JavaOCAOCPP.service.ocpp.stopTransaction;

import java.util.List;

public interface StopTransaction {
    void sendLocalStop(List<Object> parsedMessage);

    void sendRemoteStop(int connectorId);

    void checkTransactionCreation(List<Object> parsedMessage, List<Object> cashedRequest);
}