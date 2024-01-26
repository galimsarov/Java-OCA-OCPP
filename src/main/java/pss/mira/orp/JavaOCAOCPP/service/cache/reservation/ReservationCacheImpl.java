package pss.mira.orp.JavaOCAOCPP.service.cache.reservation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getResult;

@Service
@Slf4j
public class ReservationCacheImpl implements ReservationCache {
    private final Map<Integer, Map<String, Object>> reservationsMap = new HashMap<>();
    private boolean filled = false;

    @Override
    public void createCache(List<Object> parsedMessage) {
        try {
            List<Map<String, Object>> reservations = getResult(parsedMessage);
            for (Map<String, Object> reservation : reservations) {
                int reservationId = Integer.parseInt(reservation.get("reservation_id").toString());
                reservationsMap.put(reservationId, reservation);
            }
            filled = true;
            logReservationsMapSize();
        } catch (Exception ignored) {
            log.error("An error occurred while receiving reservation table from the message");
        }
    }

    @Override
    public void addToCache(List<Map<String, Object>> reservations) {
        reservationsMap.clear();
        for (Map<String, Object> reservation : reservations) {
            int reservationId = Integer.parseInt(reservation.get("reservation_id").toString());
            reservationsMap.put(reservationId, reservation);
        }
        logReservationsMapSize();
    }

    private void logReservationsMapSize() {
        if (reservationsMap.isEmpty()) {
            log.info("There are no entries in the reservation map");
        } else if (reservationsMap.size() == 1) {
            log.info("1 entry in the reservation map");
        } else {
            log.info("There are " + reservationsMap.size() + " entries in the reservation map");
        }
    }

    @Override
    public boolean filled() {
        return filled;
    }

    @Override
    public boolean reserved(int connectorId) {
        for (Map.Entry<Integer, Map<String, Object>> entry : reservationsMap.entrySet()) {
            int mapConnectorId = Integer.parseInt(entry.getValue().get("connector_id").toString());
            if (connectorId == mapConnectorId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Integer getReservationId(Integer connectorId, String idTag) {
        for (Map.Entry<Integer, Map<String, Object>> entry : reservationsMap.entrySet()) {
            int mapConnectorId = Integer.parseInt(entry.getValue().get("connector_id").toString());
            String mapIdTag = entry.getValue().get("id_tag").toString();
            if ((connectorId == mapConnectorId) && mapIdTag.equals(idTag)) {
                return Integer.parseInt(entry.getValue().get("reservation_id").toString());
            }
        }
        return null;
    }

    @Override
    public void remove(Integer reservationId) {
        reservationsMap.remove(reservationId);
    }
}