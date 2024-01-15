package pss.mira.orp.JavaOCAOCPP.service.ocpp.stopTransaction;

import java.util.List;

public interface StopTransaction {
    void sendStopTransaction(List<Object> parsedMessage);
}
