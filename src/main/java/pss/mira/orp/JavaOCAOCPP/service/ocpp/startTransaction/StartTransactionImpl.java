package pss.mira.orp.JavaOCAOCPP.service.ocpp.startTransaction;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.IdTagInfo;
import eu.chargetime.ocpp.model.core.StartTransactionConfirmation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.Handler;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pss.mira.orp.JavaOCAOCPP.models.enums.IdType.CONNECTOR;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getIdTagInfoMap;

@Service
@Slf4j
public class StartTransactionImpl implements StartTransaction {
    private final BootNotification bootNotification;
    private final ConnectorsInfoCache connectorsInfoCache;
    private final Handler handler;
    private final Sender sender;

    public StartTransactionImpl(BootNotification bootNotification, ConnectorsInfoCache connectorsInfoCache, Handler handler, Sender sender) {
        this.bootNotification = bootNotification;
        this.connectorsInfoCache = connectorsInfoCache;
        this.handler = handler;
        this.sender = sender;
    }

    /**
     * ["myQueue1","71f599b2-b3f0-4680-b447-ae6d6dc0cc0c","StartTransaction",{"connectorId":1,"idTag":"New"}]
     */
    @Override
    public void sendStartTransaction(List<Object> parsedMessage) {
        String consumer = parsedMessage.get(0).toString();
        String requestUuid = parsedMessage.get(1).toString();
        try {
            Map<String, Object> startTransactionMap = (Map<String, Object>) parsedMessage.get(3);
            int connectorId = Integer.parseInt(startTransactionMap.get("connectorId").toString());
            String idTag = startTransactionMap.get("idTag").toString();

            ClientCoreProfile core = handler.getCore();
            JSONClient client = bootNotification.getClient();

            // Use the feature profile to help create event
            Request request = core.createStartTransactionRequest(
                    connectorId, idTag, connectorsInfoCache.getMeterValue(connectorId, CONNECTOR), ZonedDateTime.now()
            );

            // Client returns a promise which will be filled once it receives a confirmation.
            try {
                client.send(request).whenComplete((confirmation, ex) -> {
                    log.info("Received from the central system: " + confirmation.toString());
                    handleResponse(consumer, requestUuid, confirmation);
                });
            } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                log.warn("An error occurred while sending or processing start transaction request");
            }
        } catch (Exception ignored) {
            log.error("An error occurred while receiving start transaction data from the message");
        }
    }

    private void handleResponse(String consumer, String requestUuid, Confirmation confirmation) {
        IdTagInfo idTagInfo = ((StartTransactionConfirmation) confirmation).getIdTagInfo();
        Map<String, Object> result = new HashMap<>();
        result.put("idTagInfo", getIdTagInfoMap(idTagInfo));
        result.put("transactionId", ((StartTransactionConfirmation) confirmation).getTransactionId());
        sender.sendRequestToQueue(consumer, requestUuid, "", result, "");
    }
}