package pss.mira.orp.JavaOCAOCPP.service.cache.reservation;

import java.util.List;
import java.util.Map;

public interface ReservationCache {
    void createCache(List<Object> parsedMessage);

    void addToCache(List<Map<String, Object>> reservations);

    boolean filled();

    boolean reserved(int connectorId);

    Integer getReservationId(Integer connectorId, String idTag);

    void remove(Integer reservationId);
}