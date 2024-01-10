package pss.mira.orp.JavaOCAOCPP.service.rabbitListener;

public interface RabbitMQListener {
    void processAddressCS(String message);

    void processMyQueue2(String message);
}