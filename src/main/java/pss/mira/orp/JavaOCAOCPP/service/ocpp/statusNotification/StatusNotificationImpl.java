package pss.mira.orp.JavaOCAOCPP.service.ocpp.statusNotification;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.info.ocpp.StatusNotificationInfo;
import pss.mira.orp.JavaOCAOCPP.models.queues.Queues;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.client.Client;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handlers.core.CoreHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handlers.remoteTrigger.RemoteTriggerHandler;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.SaveToCache;

@Service
@Slf4j
public class StatusNotificationImpl implements StatusNotification {
    private final Client client;
    private final CoreHandler coreHandler;
    private final RemoteTriggerHandler remoteTriggerHandler;
    private final Queues queues;
    private final Sender sender;
    private final Map<Integer, Request> cachedStatusNotificationRequestsMap = new HashMap<>();

    public StatusNotificationImpl(
            Client client,
            CoreHandler coreHandler,
            RemoteTriggerHandler remoteTriggerHandler,
            Queues queues,
            Sender sender
    ) {
        this.client = client;
        this.coreHandler = coreHandler;
        this.remoteTriggerHandler = remoteTriggerHandler;
        this.queues = queues;
        this.sender = sender;
    }

    @Override
    public void sendStatusNotification(StatusNotificationInfo statusNotificationInfo) {
        ClientCoreProfile core = coreHandler.getCore();
        // Use the feature profile to help create event
        Request request = core.createStatusNotificationRequest(
                statusNotificationInfo.getId(),
                statusNotificationInfo.getErrorCode(),
                statusNotificationInfo.getStatus()
        );
        if (client.getClient() == null) {
            sender.sendRequestToQueue(
                    queues.getOCPPCache(),
                    UUID.randomUUID().toString(),
                    SaveToCache.name(),
                    request,
                    "statusNotification"
            );
        } else {
            sendToCentralSystem(request, client.getClient());
        }
        cachedStatusNotificationRequestsMap.put(statusNotificationInfo.getId(), request);
    }

    @Override
    public void sendTriggerMessageStatusNotification(List<Object> parsedMessage) {
        Map<String, Integer> map = (Map<String, Integer>) parsedMessage.get(3);
        Integer connectorId = map.get("connectorId");
        if (connectorId != null) {
            if (connectorId == 0) {
                for (Request request : cachedStatusNotificationRequestsMap.values()) {
                    sendToCentralSystem(request, client.getClient());
                }
            } else {
                Request request = cachedStatusNotificationRequestsMap.get(connectorId);
                sendToCentralSystem(request, client.getClient());
            }
        }
        remoteTriggerHandler.setRemoteTriggerTaskFinished();
    }

    private void sendToCentralSystem(Request request, JSONClient client) {
        log.info("Sent to central system: " + request.toString());
        // Client returns a promise which will be filled once it receives a confirmation.
        try {
            client.send(request).whenComplete((confirmation, ex) ->
                    log.info("Received from the central system: " + confirmation.toString()));
        } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
            log.warn("An error occurred while sending or processing meter value request");
        }
    }
}