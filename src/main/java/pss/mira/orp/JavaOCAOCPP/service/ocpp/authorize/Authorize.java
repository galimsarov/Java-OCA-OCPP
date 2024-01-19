package pss.mira.orp.JavaOCAOCPP.service.ocpp.authorize;

import java.util.List;

public interface Authorize {
    void sendAuthorize(List<Object> parsedMessage);

    void setAuthMap(List<Object> parsedMessage);
}
