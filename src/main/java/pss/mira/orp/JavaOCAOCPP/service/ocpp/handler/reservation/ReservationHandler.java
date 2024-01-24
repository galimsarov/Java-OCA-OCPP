package pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.reservation;

import eu.chargetime.ocpp.feature.profile.ClientReservationProfile;

import java.util.List;

public interface ReservationHandler {
    ClientReservationProfile getReservation();

    void setConfigurationList(List<Object> parsedMessage);

    void setReservationStatus(List<Object> parsedMessage);
}