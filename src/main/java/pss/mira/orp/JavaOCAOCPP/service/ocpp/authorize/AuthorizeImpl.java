package pss.mira.orp.JavaOCAOCPP.service.ocpp.authorize;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.AuthorizeConfirmation;
import eu.chargetime.ocpp.model.core.IdTagInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AuthorizeImpl implements Authorize {
    private final BootNotification bootNotification;
    private final Sender sender;

    public AuthorizeImpl(BootNotification bootNotification, Sender sender) {
        this.bootNotification = bootNotification;
        this.sender = sender;
    }

    /**
     * Отправляет запрос на авторизацию в ЦС
     *
     * @param parsedMessage запрос от сервиса в формате:
     *                      ["myQueue1","71f599b2-b3f0-4680-b447-ae6d6dc0cc0c","Authorize",{"idTag":"New"}]
     * Формат ответа от steve:
     *                      AuthorizeConfirmation{idTagInfo=IdTagInfo{expiryDate="2024-01-10T11:12:02.925Z", parentIdTag=null, status=Accepted}, isValid=true}
     */
    @Override
    public void sendAuthorize(List<Object> parsedMessage) {
        String consumer = parsedMessage.get(0).toString();
        String requestUuid = parsedMessage.get(1).toString();
        Map<String, String> idTagMap = (Map<String, String>) parsedMessage.get(3);
        String idTag = idTagMap.get("idTag");

        ClientCoreProfile core = bootNotification.getCore();
        JSONClient client = bootNotification.getClient();

        // Use the feature profile to help create event
        Request request = core.createAuthorizeRequest(idTag);

        // Client returns a promise which will be filled once it receives a confirmation.
        try {
            client.send(request).whenComplete((confirmation, ex) -> {
                log.info(confirmation.toString());
                handleResponse(consumer, requestUuid, confirmation);
            });
        } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
            log.warn("An error occurred while sending or processing authorize request");
        }
    }

    private void handleResponse(String consumer, String requestUuid, Confirmation confirmation) {
        IdTagInfo idTagInfo = ((AuthorizeConfirmation) confirmation).getIdTagInfo();
        Map<String, String> result = new HashMap<>();
        result.put("expiryDate", idTagInfo.getExpiryDate().toString());
        result.put("parentIdTag", idTagInfo.getParentIdTag());
        result.put("status", idTagInfo.getStatus().toString());
        sender.sendRequestToQueue(consumer, requestUuid, "", result);
    }
}