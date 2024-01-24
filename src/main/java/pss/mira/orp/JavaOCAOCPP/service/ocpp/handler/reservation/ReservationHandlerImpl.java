package pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.reservation;

import eu.chargetime.ocpp.feature.profile.ClientReservationEventHandler;
import eu.chargetime.ocpp.feature.profile.ClientReservationProfile;
import eu.chargetime.ocpp.model.reservation.CancelReservationConfirmation;
import eu.chargetime.ocpp.model.reservation.CancelReservationRequest;
import eu.chargetime.ocpp.model.reservation.ReserveNowConfirmation;
import eu.chargetime.ocpp.model.reservation.ReserveNowRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.requests.rabbit.DBTablesChangeRequest;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.chargetime.ocpp.model.reservation.ReservationStatus.*;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.Change;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.Get;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.*;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Queues.bd;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.*;

@Service
@Slf4j
public class ReservationHandlerImpl implements ReservationHandler {
    private final ConnectorsInfoCache connectorsInfoCache;
    private final Sender sender;
    private List<Map<String, Object>> configurationList = null;
    private String reservationStatus = null;

    public ReservationHandlerImpl(ConnectorsInfoCache connectorsInfoCache, Sender sender) {
        this.connectorsInfoCache = connectorsInfoCache;
        this.sender = sender;
    }

    @Override
    public ClientReservationProfile getReservation() {
        return new ClientReservationProfile(new ClientReservationEventHandler() {
            @Override
            public ReserveNowConfirmation handleReserveNowRequest(ReserveNowRequest request) {
                log.info("Received from the central system: " + request.toString());
                sender.sendRequestToQueue(
                        bd.name(),
                        UUID.randomUUID().toString(),
                        Get.name(),
                        getDBTablesGetRequest(List.of(configuration.name())),
                        getConfigurationForReservationHandler.name()
                );
                while (true) {
                    if (configurationList == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Аn error while receiving configuration table");
                        }
                    } else {
                        break;
                    }
                }
                ReserveNowConfirmation result = new ReserveNowConfirmation(Unavailable);
                if (reservationSupported()) {
                    String connectorStatus = connectorsInfoCache.getStatus(request.getConnectorId());
                    if (connectorStatus.equals("Charging") ||
                            connectorStatus.equals("Preparing") ||
                            connectorStatus.equals("Finishing")) {
                        result.setStatus(Occupied);
                    } else if (!connectorStatus.equals("Unavailable")) {
                        if ((request.getConnectorId() != 0) || reserveConnectorsZeroSupported()) {
                            sender.sendRequestToQueue(
                                    bd.name(),
                                    UUID.randomUUID().toString(),
                                    Change.name(),
                                    new DBTablesChangeRequest(
                                            reservation.name(),
                                            "reservation_id:" + request.getReservationId(),
                                            getReservationValues(request)),
                                    reservation.name()
                            );
                        }
                        while (true) {
                            if (reservationStatus == null) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    log.error("An error while waiting the reservation status");
                                }
                            } else {
                                break;
                            }
                        }
                        if (reservationStatus.equals("Accepted")) {
                            result.setStatus(Accepted);
                        } else {
                            result.setStatus(Rejected);
                        }
                        reservationStatus = null;
                    }
                }
                configurationList = null;
                log.info("Send to the central system: " + result);
                return result;
            }

            private List<Map<String, String>> getReservationValues(ReserveNowRequest request) {
                if (request.getParentIdTag() == null) {
                    return List.of(
                            Map.of("key", "connector_id", "value", request.getConnectorId().toString()),
                            Map.of("key", "expiry_date", "value",
                                    getStringDateForReservation(request.getExpiryDate())),
                            Map.of("key", "id_tag", "value", request.getIdTag())
                    );
                } else {
                    return List.of(
                            Map.of("key", "connector_id", "value", request.getConnectorId().toString()),
                            Map.of("key", "expiry_date", "value",
                                    getStringDateForReservation(request.getExpiryDate())),
                            Map.of("key", "id_tag", "value", request.getIdTag()),
                            Map.of("key", "parent_id_tag", "value", request.getParentIdTag())
                    );
                }
            }

            private boolean reserveConnectorsZeroSupported() {
                for (Map<String, Object> map : configurationList) {
                    String key = map.get("key").toString();
                    if (key.equals("ReserveConnectorZeroSupported")) {
                        return Boolean.parseBoolean(map.get("value").toString());
                    }
                    break;
                }
                return false;
            }

            private boolean reservationSupported() {
                for (Map<String, Object> map : configurationList) {
                    String key = map.get("key").toString();
                    if (key.equals("SupportedFeatureProfiles")) {
                        String value = map.get("value").toString();
                        String[] profiles = value.split(",");
                        for (String profile : profiles) {
                            if (profile.equals("Reservation")) {
                                return true;
                            }
                        }
                        return false;
                    }
                }
                return false;
            }


            @Override
            public CancelReservationConfirmation handleCancelReservationRequest(CancelReservationRequest request) {
                return null;
            }
        });
    }

    @Override
    public void setConfigurationList(List<Object> parsedMessage) {
        try {
            configurationList = getResult(parsedMessage);
        } catch (Exception ignored) {
            log.error("An error occurred while receiving configuration table from the message");
        }
    }

    @Override
    public void setReservationStatus(List<Object> parsedMessage) {
        Map<String, String> map = (Map<String, String>) parsedMessage.get(2);
        reservationStatus = map.get("result");
    }
}
