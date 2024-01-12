package pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache;

import java.util.List;
import java.util.Map;

public interface ConnectorsInfoCache {
    void addToCache(List<Map<String, Object>> connectorsInfo);

    boolean stationIsCharging();

    boolean isEmpty();
}