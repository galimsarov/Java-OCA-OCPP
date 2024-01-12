package pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static pss.mira.orp.JavaOCAOCPP.models.enums.ConnectorStatus.Charging;

@Service
public class ConnectorsInfoCacheImpl implements ConnectorsInfoCache {
    private final List<Map<String, Object>> connectorsInfoList = new ArrayList<>();

    /**
     * Тестовый сын Джейсона для steve:
     * [{"status":"Available"},{"status":"Available"}]
     */
    @Override
    public void addToCache(List<Map<String, Object>> connectorsInfo) {
        connectorsInfoList.clear();
        connectorsInfoList.addAll(connectorsInfo);
    }

    @Override
    public boolean stationIsCharging() {
        for (Map<String, Object> map : connectorsInfoList) {
            if (map.get("status").toString().equals(Charging.name())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return connectorsInfoList.isEmpty();
    }
}