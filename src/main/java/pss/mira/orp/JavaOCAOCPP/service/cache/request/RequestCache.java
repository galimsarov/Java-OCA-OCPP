package pss.mira.orp.JavaOCAOCPP.service.cache.request;

import java.util.List;

public interface RequestCache {
    void addToCache(List<Object> request, String requestType);

    List<Object> getCashedRequest(String uuid);

    void removeFromCache(String uuid);

    int getConnectorId(String uid, String requestType);
}