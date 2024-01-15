package pss.mira.orp.JavaOCAOCPP.service.cache.request;

import eu.chargetime.ocpp.model.core.ChangeAvailabilityRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RequestCacheImpl implements RequestCache {
    private final Map<String, List<Object>> cache = new HashMap<>();

    @Override
    public void addToCache(List<Object> request, String requestType) {
        List<Object> extendedRequest = new ArrayList<>(request);
        extendedRequest.add(requestType);
        cache.put(extendedRequest.get(1).toString(), extendedRequest);
    }

    @Override
    public List<Object> getCashedRequest(String uuid) {
        return cache.get(uuid);
    }

    @Override
    public void removeFromCache(String uuid) {
        cache.remove(uuid);
    }

    @Override
    public int getConnectorId(String uid, String requestType) {
        for (Object possibleRequest : cache.get(uid)) {
            try {
                ChangeAvailabilityRequest request = (ChangeAvailabilityRequest) possibleRequest;
                return request.getConnectorId();
            } catch (ClassCastException ignored) {

            }
        }
        return -1;
    }
}