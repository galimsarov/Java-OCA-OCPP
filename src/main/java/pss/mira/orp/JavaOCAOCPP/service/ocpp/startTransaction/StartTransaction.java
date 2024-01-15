package pss.mira.orp.JavaOCAOCPP.service.ocpp.startTransaction;

import java.util.List;

public interface StartTransaction {
    void sendStartTransaction(List<Object> parsedMessage);
}
