package pss.mira.orp.JavaOCAOCPP.service.cache.nonStoppedTransaction;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NonStoppedTransactionCacheImpl implements NonStoppedTransactionCache {
    private final List<Map<String, Object>> transactions = new ArrayList<>();

    @Override
    public void addToCache(Map<String, Object> transaction) {
        transactions.add(transaction);
    }

    @Override
    public boolean hasNonStoppedTransactionsOnConnector(int connectorId) {
        for (Map<String, Object> transaction : transactions) {
            int transactionConnectorId = Integer.parseInt(transaction.get("connector_id").toString());
            if (connectorId == transactionConnectorId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Map<String, Object>> getTransactionsByConnectorId(int connectorId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Map<String, Object>> newTransactions = new ArrayList<>();
        for (Map<String, Object> transaction : transactions) {
            if (Integer.parseInt(transaction.get("connector_id").toString()) == connectorId) {
                result.add(transaction);
            } else {
                newTransactions.add(transaction);
            }
        }
        transactions.clear();
        transactions.addAll(newTransactions);
        return result;
    }
}