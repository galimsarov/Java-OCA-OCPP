package pss.mira.orp.JavaOCAOCPP.service.ocpp.authorize;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.AuthorizationStatus;
import eu.chargetime.ocpp.model.core.AuthorizeConfirmation;
import eu.chargetime.ocpp.model.core.IdTagInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.Handler;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.chargetime.ocpp.model.core.AuthorizationStatus.Invalid;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.Get;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.auth_list;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Services.bd;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.*;

@Service
@Slf4j
public class AuthorizeImpl implements Authorize {
    private List<Map<String, Object>> authList = null;
    private final BootNotification bootNotification;
    private final Handler handler;
    private final Sender sender;

    public AuthorizeImpl(BootNotification bootNotification, Handler handler, Sender sender) {
        this.bootNotification = bootNotification;
        this.handler = handler;
        this.sender = sender;
    }

    /**
     * Отправляет запрос на авторизацию в ЦС
     * @param parsedMessage запрос от сервиса в формате:
     * ["myQueue1","71f599b2-b3f0-4680-b447-ae6d6dc0cc0c","Authorize",{"idTag":"New"}]
     * Формат ответа от steve:
     * AuthorizeConfirmation{idTagInfo=IdTagInfo{expiryDate="2024-01-10T11:12:02.925Z", parentIdTag=null, status=Accepted}, isValid=true}
     */
    @Override
    public void sendAuthorize(List<Object> parsedMessage) {
        String consumer = parsedMessage.get(0).toString();
        String requestUuid = parsedMessage.get(1).toString();
        Map<String, String> idTagMap;
        try {
            idTagMap = (Map<String, String>) parsedMessage.get(3);
            String idTag = idTagMap.get("idTag");

            ClientCoreProfile core = handler.getCore();
            JSONClient client = bootNotification.getClient();

            if (client == null) {
                log.warn("There is no connection to the central system. Auth list is used for authorization");
                checkAuthWithDB(idTag, consumer, requestUuid);
            } else {
                // Use the feature profile to help create event
                Request request = core.createAuthorizeRequest(idTag);
                // Client returns a promise which will be filled once it receives a confirmation.
                try {
                    client.send(request).whenComplete((confirmation, ex) -> {
                        log.info("Received from the central system: " + confirmation.toString());
                        handleResponse(consumer, requestUuid, confirmation);
                    });
                } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                    log.warn("An error occurred while sending or processing authorize request");
                }
            }
        } catch (Exception ignored) {
            log.error("An error occurred while receiving idTag from the message");
        }
    }

    @Override
    public void setAuthMap(List<Object> parsedMessage) {
        try {
            authList = getResult(parsedMessage);
        } catch (Exception ignored) {
            log.error("An error occurred while receiving auth table from the message");
        }
    }

    private void checkAuthWithDB(String idTag, String consumer, String requestUuid) {
        sender.sendRequestToQueue(
                bd.name(),
                UUID.randomUUID().toString(),
                Get.name(),
                getDBTablesGetRequest(List.of(auth_list.name())),
                auth_list.name()
        );
        while (true) {
            if (authList == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Аn error while waiting for a get auth_list response");
                }
            } else {
                AuthorizeConfirmation confirmation = getAuthorizeConfirmation(idTag);
                authList = null;
                handleResponse(consumer, requestUuid, confirmation);
            }
        }
    }

    private AuthorizeConfirmation getAuthorizeConfirmation(String idTag) {
        // AuthorizeConfirmation{idTagInfo=IdTagInfo{expiryDate="2024-01-18T12:35:20.476Z", parentIdTag=New, status=Accepted}, isValid=true}
        Map<String, Object> authMap = new HashMap<>();
        for (Map<String, Object> map : authList) {
            String bdIdTag = map.get("id_tag").toString();
            if (bdIdTag.equals(idTag)) {
                authMap = map;
                break;
            }
        }
        IdTagInfo idTagInfo = new IdTagInfo(Invalid);
        if (authMap.isEmpty()) {
            idTagInfo = new IdTagInfo(Invalid);
        } else {
            boolean enumContainsIdTag = false;
            for (AuthorizationStatus enumStatus : AuthorizationStatus.values()) {
                if (enumStatus.name().equals(authMap.get("status").toString())) {
                    idTagInfo = new IdTagInfo(enumStatus);
                    enumContainsIdTag = true;
                    idTagInfo.setExpiryDate(getZoneDateTimeFromAuth(authMap.get("expiry_date").toString()));
                    idTagInfo.setParentIdTag(authMap.get("parent_id_tag").toString());
                    break;
                }
            }
            if (!enumContainsIdTag) {
                idTagInfo = new IdTagInfo(Invalid);
            }
        }
        return new AuthorizeConfirmation(idTagInfo);
    }

    private void handleResponse(String consumer, String requestUuid, Confirmation confirmation) {
        IdTagInfo idTagInfo = ((AuthorizeConfirmation) confirmation).getIdTagInfo();
        Map<String, Map<String, String>> result = new HashMap<>();
        result.put("idTagInfo", getIdTagInfoMap(idTagInfo));
        sender.sendRequestToQueue(consumer, requestUuid, "", result, "");
    }
}