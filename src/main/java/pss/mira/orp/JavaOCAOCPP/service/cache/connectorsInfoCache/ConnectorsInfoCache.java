package pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache;

import pss.mira.orp.JavaOCAOCPP.models.info.ocpp.StatusNotificationInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ConnectorsInfoCache {
    List<StatusNotificationInfo> addToCache(List<Map<String, Object>> connectorsInfo);

    List<StatusNotificationInfo> createCache(List<Object> parsedMessage);

    boolean stationIsCharging();

    boolean isEmpty();

    StatusNotificationInfo getStatusNotificationRequest(int connectorId);

    int getFullStationConsumedEnergy(int connectorId);

    double getEVRequestedCurrent(int connectorId);

    double getCurrentAmperage(int connectorId);

    int getEVRequestedPower(int connectorId);

    int getPercent(int connectorId);

    String getChargePointVendorError(int connectorId);

    boolean isCharging(int connectorId);

    String getStatus(int connectorId);

    Set<Integer> getConnectorsIds();
}