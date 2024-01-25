package pss.mira.orp.JavaOCAOCPP.service.cache.reservation;

import java.util.List;

public interface ReservationCache {
    void addToCache(List<Object> parsedMessage);

    boolean filled();

    boolean reserved(int connectorId);
}