package pss.mira.orp.JavaOCAOCPP.service.cache.nonStoppedTransaction;

import java.util.List;
import java.util.Map;

public interface NonStoppedTransactionCache {
    void addToCache(Map<String, Object> transaction);

    boolean hasNonStoppedTransactionsOnConnector(int connectorId);

    Map<String, Object> removeTransaction(int connectorId);

    List<Map<String, Object>> getTransactionsByConnectorId(int connectorId);
}