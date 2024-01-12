package pss.mira.orp.JavaOCAOCPP.service.rabbit.sender;

public interface Sender {
    void sendRequestToQueue(String key, String uuid, String action, Object body);
}