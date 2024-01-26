package pss.mira.orp.JavaOCAOCPP.service.rabbit.listener;

public interface Listener {
    void processOCPP(String message);

    void processConnectorsInfo(String message);

    void processReservation(String message);
}