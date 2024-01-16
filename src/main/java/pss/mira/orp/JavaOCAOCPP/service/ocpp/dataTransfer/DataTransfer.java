package pss.mira.orp.JavaOCAOCPP.service.ocpp.dataTransfer;

import java.util.List;

public interface DataTransfer {
    void sendDataTransfer(List<Object> parsedMessage);
}
