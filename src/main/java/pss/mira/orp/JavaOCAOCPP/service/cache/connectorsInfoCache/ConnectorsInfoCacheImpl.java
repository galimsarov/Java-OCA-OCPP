package pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache;

import eu.chargetime.ocpp.model.core.ChargePointErrorCode;
import eu.chargetime.ocpp.model.core.ChargePointStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.requests.ocpp.StatusNotificationRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pss.mira.orp.JavaOCAOCPP.models.enums.ConnectorStatus.Charging;

@Service
@Slf4j
public class ConnectorsInfoCacheImpl implements ConnectorsInfoCache {
    private final Map<Integer, Map<String, Object>> connectorsMap = new HashMap<>();

    /**
     * Тестовый сын Джейсона для steve:
     * [{"id":1,"errorCode":"NoError","status":"Available"},{"id":2,"errorCode":"NoError","status":"Available"},{"id":3,"errorCode":"NoError","status":"Available"}]
     */
    @Override
    public List<StatusNotificationRequest> addToCache(List<Map<String, Object>> connectorsInfo) {
        List<StatusNotificationRequest> result = new ArrayList<>();
        if (!connectorsMap.isEmpty()) {
            for (Map<String, Object> map : connectorsInfo) {
                int id = Integer.parseInt(map.get("id").toString());
                String newErrorCode = map.get("errorCode").toString();
                String newStatus = map.get("status").toString();

                Map<String, Object> mapFromCache = connectorsMap.get(id);
                String oldErrorCode = mapFromCache.get("errorCode").toString();
                String oldStatus = mapFromCache.get("status").toString();

                if (!newErrorCode.equals(oldErrorCode) || !newStatus.equals(oldStatus)) {
                    addToResult(newErrorCode, newStatus, result, id);
                }
            }
            connectorsMap.clear();
        } else {
            for (Map<String, Object> map : connectorsInfo) {
                int id = Integer.parseInt(map.get("id").toString());
                String newErrorCode = map.get("errorCode").toString();
                String newStatus = map.get("status").toString();
                addToResult(newErrorCode, newStatus, result, id);
            }
        }
        for (Map<String, Object> map : connectorsInfo) {
            int connectorId = Integer.parseInt(map.get("id").toString());
            connectorsMap.put(connectorId, map);
        }
        return result;
    }

    private void addToResult(String newErrorCode, String newStatus, List<StatusNotificationRequest> result, int id) {
        ChargePointErrorCode parsedCode = getErrorCode(newErrorCode);
        ChargePointStatus parsedStatus = getChargePointStatus(newStatus);
        if ((parsedCode != null) && (parsedStatus != null)) {
            result.add(new StatusNotificationRequest(id, parsedCode, parsedStatus));
        } else {
            log.error("The status or error of connector " + id +
                    " does not correspond to the values of the corresponding enum");
        }
    }

    private ChargePointStatus getChargePointStatus(String newStatus) {
        for (ChargePointStatus status : ChargePointStatus.values()) {
            if (status.name().equals(newStatus)) {
                return status;
            }
        }
        return null;
    }

    private ChargePointErrorCode getErrorCode(String newErrorCode) {
        for (ChargePointErrorCode errorCode : ChargePointErrorCode.values()) {
            if (errorCode.name().equals(newErrorCode)) {
                return errorCode;
            }
        }
        return null;
    }

    @Override
    public boolean stationIsCharging() {
        for (Map.Entry<Integer, Map<String, Object>> entry : connectorsMap.entrySet()) {
            Map<String, Object> map = entry.getValue();
            if (map.get("status").toString().equals(Charging.name())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return connectorsMap.isEmpty();
    }
}