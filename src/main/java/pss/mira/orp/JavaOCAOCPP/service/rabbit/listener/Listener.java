package pss.mira.orp.JavaOCAOCPP.service.rabbit.listener;

public interface Listener {
    void processAddressCS(String message);

    void processMyQueue2(String message);
}