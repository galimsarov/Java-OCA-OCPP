package pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache;

import pss.mira.orp.JavaOCAOCPP.models.requests.ocpp.StatusNotificationRequest;

import java.util.List;
import java.util.Map;

public interface ConnectorsInfoCache {
    List<StatusNotificationRequest> addToCache(List<Map<String, Object>> connectorsInfo);

    boolean stationIsCharging();

    boolean isEmpty();

    StatusNotificationRequest getStatusNotificationRequest(int connectorId);

    int getMeterValue(int connectorId);
}