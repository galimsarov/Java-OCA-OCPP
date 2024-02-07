package pss.mira.orp.JavaOCAOCPP.service.ocpp.startTransaction;

import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.core.IdTagInfo;
import eu.chargetime.ocpp.model.core.StartTransactionConfirmation;
import eu.chargetime.ocpp.model.core.StartTransactionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.info.rabbit.DBTablesCreateInfo;
import pss.mira.orp.JavaOCAOCPP.models.queues.Queues;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.ChargeSessionMap;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.chargeSessionInfo.ChargeSessionInfo;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.client.Client;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handlers.core.CoreHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handlers.remoteTrigger.RemoteTriggerHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.meterValues.MeterValues;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.time.ZonedDateTime;
import java.util.*;

import static eu.chargetime.ocpp.model.core.AuthorizationStatus.Accepted;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.*;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.transaction1;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.*;

@Service
@Slf4j
public class StartTransactionImpl implements StartTransaction {
    private final Client client;
    private final ChargeSessionMap chargeSessionMap;
    private final ConnectorsInfoCache connectorsInfoCache;
    private final CoreHandler coreHandler;
    private final MeterValues meterValues;
    private final Queues queues;
    private final RemoteTriggerHandler remoteTriggerHandler;
    private final Sender sender;

    public StartTransactionImpl(
            Client client,
            ChargeSessionMap chargeSessionMap,
            ConnectorsInfoCache connectorsInfoCache,
            CoreHandler coreHandler,
            MeterValues meterValues,
            Queues queues,
            RemoteTriggerHandler remoteTriggerHandler,
            Sender sender
    ) {
        this.client = client;
        this.chargeSessionMap = chargeSessionMap;
        this.connectorsInfoCache = connectorsInfoCache;
        this.coreHandler = coreHandler;
        this.meterValues = meterValues;
        this.queues = queues;
        this.remoteTriggerHandler = remoteTriggerHandler;
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

            ClientCoreProfile core = coreHandler.getCore();
            // Use the feature profile to help create event
            StartTransactionRequest request = core.createStartTransactionRequest(
                    connectorId, idTag,
                    connectorsInfoCache.getFullStationConsumedEnergy(connectorId),
                    ZonedDateTime.now()
            );
            if (client.getClient() == null) {
                request.setTimestamp(null);
                sender.sendRequestToQueue(
                        queues.getOCPPCache(),
                        UUID.randomUUID().toString(),
                        SaveToCache.name(),
                        request,
                        "startTransaction"
                );
            } else {
                log.info("Sent to central system: " + request.toString());
                // Client returns a promise which will be filled once it receives a confirmation.
                try {
                    remoteTriggerHandler.waitForRemoteTriggerTaskComplete();
                    client.getClient().send(request).whenComplete((confirmation, ex) -> {
                        log.info("Received from the central system: " + confirmation.toString());
                        handleResponse(consumer, requestUuid, confirmation, connectorId, idTag);
                    });
                } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                    log.warn("An error occurred while sending or processing start transaction request");
                }
            }
        } catch (Exception ignored) {
            log.error("An error occurred while receiving start transaction data from the message");
        }
    }

    @Override
    public void sendStartTransaction(ChargeSessionInfo chargeSessionInfo) {
        Map<String, Object> startTransactionMap = new HashMap<>();
        startTransactionMap.put("connectorId", chargeSessionInfo.getConnectorId());
        startTransactionMap.put("idTag", chargeSessionInfo.getIdTag());
        sendStartTransaction(List.of("", "", "", startTransactionMap));
    }

    private void handleResponse(
            String consumer, String requestUuid, Confirmation confirmation, int connectorId, String idTag
    ) {
        StartTransactionConfirmation startTransactionConfirmation = (StartTransactionConfirmation) confirmation;
        IdTagInfo idTagInfo = startTransactionConfirmation.getIdTagInfo();
        Map<String, Object> result = new HashMap<>();
        result.put("idTagInfo", getIdTagInfoMap(idTagInfo));
        int transactionId = startTransactionConfirmation.getTransactionId();
        if (startTransactionConfirmation.getIdTagInfo().getStatus().equals(Accepted) && (transactionId != 0)) {
            result.put("transactionId", transactionId);
            chargeSessionMap.setTransactionId(connectorId, transactionId);
        }
        if (consumer.isBlank()) {
            createOrStopTransaction(confirmation, connectorId, idTag);
        } else {
            sender.sendRequestToQueue(consumer, requestUuid, "", result, "");
        }
    }

    private void createOrStopTransaction(Confirmation confirmation, int connectorId, String idTag) {
        StartTransactionConfirmation startTransactionConfirmation = (StartTransactionConfirmation) confirmation;
        if (startTransactionConfirmation.getIdTagInfo().getStatus().equals(Accepted) &&
                startTransactionConfirmation.getTransactionId() != 0) {
            sender.sendRequestToQueue(
                    queues.getDateBase(),
                    UUID.randomUUID().toString(),
                    Change.name(),
                    new DBTablesCreateInfo(
                            transaction1.name(),
                            List.of(
                                    Map.of("key", "transaction_id", "value",
                                            startTransactionConfirmation.getTransactionId().toString()),
                                    Map.of("key", "id_tag", "value", idTag),
                                    Map.of("key", "connector_id", "value", String.valueOf(connectorId)),
                                    Map.of("key", "start_date_time", "value",
                                            formatStartStopTransactionDateTime(new Date())),
                                    Map.of("key", "start_date_timeutc", "value",
                                            formatStartStopTransactionDateTimeUTC(new Date())),
                                    Map.of("key", "start_full_stations_consumed_energy", "value",
                                            String.valueOf(connectorsInfoCache.getFullStationConsumedEnergy(connectorId))),
                                    Map.of("key", "evccid", "value", connectorsInfoCache.getEVCCID(connectorId))
                            )),
                    CreateTransaction.name()
            );
            meterValues.addToChargingConnectors(connectorId, startTransactionConfirmation.getTransactionId());
            chargeSessionMap.setStartFullStationConsumedEnergy(
                    connectorId, connectorsInfoCache.getFullStationConsumedEnergy(connectorId)
            );
        } else {
            sender.sendRequestToQueue(
                    queues.getChargePointLogic(),
                    UUID.randomUUID().toString(),
                    StopChargeSession.name(),
                    Map.of("connectorId", connectorId),
                    StopChargeSession.name()
            );
            chargeSessionMap.removeFromChargeSessionMap(connectorId);
            meterValues.removeFromChargingConnectors(connectorId);
        }
    }
}