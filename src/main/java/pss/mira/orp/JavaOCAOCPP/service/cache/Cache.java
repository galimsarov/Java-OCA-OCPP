package pss.mira.orp.JavaOCAOCPP.service.cache;

import java.util.List;

public interface Cache {
    void addToCache(List<Object> request, String requestType);

    List<Object> getCashedRequest(String uuid);

    Object removeFromCacheByUid(String uuid);
}