package pss.mira.orp.JavaOCAOCPP.service.ocpp.stopTransaction;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.core.Reason;
import eu.chargetime.ocpp.model.core.StopTransactionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.info.rabbit.DBTablesChangeInfo;
import pss.mira.orp.JavaOCAOCPP.models.info.rabbit.DBTablesCreateInfo;
import pss.mira.orp.JavaOCAOCPP.models.queues.Queues;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.ChargeSessionMap;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.core.CoreHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.remoteTrigger.RemoteTriggerHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.meterValues.MeterValues;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.chargetime.ocpp.model.core.Reason.*;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.*;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.transaction1;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.formatStartStopTransactionDateTime;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.formatStartStopTransactionDateTimeUTC;

@Service
@Slf4j
public class StopTransactionImpl implements StopTransaction {
    private final BootNotification bootNotification;
    private final ChargeSessionMap chargeSessionMap;
    private final ConnectorsInfoCache connectorsInfoCache;
    private final CoreHandler coreHandler;
    private final MeterValues meterValues;
    private final Queues queues;
    private final RemoteTriggerHandler remoteTriggerHandler;
    private final Sender sender;

    public StopTransactionImpl(
            BootNotification bootNotification,
            ChargeSessionMap chargeSessionMap,
            ConnectorsInfoCache connectorsInfoCache,
            CoreHandler coreHandler,
            MeterValues meterValues,
            Queues queues,
            RemoteTriggerHandler remoteTriggerHandler,
            Sender sender
    ) {
        this.bootNotification = bootNotification;
        this.chargeSessionMap = chargeSessionMap;
        this.connectorsInfoCache = connectorsInfoCache;
        this.coreHandler = coreHandler;
        this.meterValues = meterValues;
        this.queues = queues;
        this.remoteTriggerHandler = remoteTriggerHandler;
        this.sender = sender;
    }

    /**
     * ["cp","5e6b7e60-e5fc-411c-9d4f-26b520587973","LocalStop",{"connectorId":1,"reason":"local"}]
     */
    @Override
    public void sendLocalStop(List<Object> parsedMessage) {
        Runnable finishingTask = () -> {
            String consumer = parsedMessage.get(0).toString();
            String requestUuid = parsedMessage.get(1).toString();
            try {
                Map<String, Object> stopTransactionMap = (Map<String, Object>) parsedMessage.get(3);
                int connectorId = Integer.parseInt(stopTransactionMap.get("connectorId").toString());
                String reason = stopTransactionMap.get("reason").toString();
                while (true) {
                    if (connectorsInfoCache.isCharging(connectorId)) {
                        Thread.sleep(1000);
                    } else {
                        break;
                    }
                }
                if (chargeSessionMap.getChargeSessionInfo(connectorId) != null) {
                    ClientCoreProfile core = coreHandler.getCore();
                    JSONClient client = bootNotification.getClient();
                    // Use the feature profile to help create event
                    StopTransactionRequest request = core.createStopTransactionRequest(
                            connectorsInfoCache.getFullStationConsumedEnergy(connectorId), ZonedDateTime.now(),
                            chargeSessionMap.getChargeSessionInfo(connectorId).getTransactionId()
                    );
                    request.setReason(getReasonFromEnum(reason));

                    if (client == null) {
                        sender.sendRequestToQueue(
                                queues.getOCPPCache(),
                                UUID.randomUUID().toString(),
                                SaveToCache.name(),
                                request,
                                "stopTransaction"
                        );
                    } else {
                        // Client returns a promise which will be filled once it receives a confirmation.
                        try {
                            remoteTriggerHandler.waitForRemoteTriggerTaskComplete();
                            log.info("Sent to central system: " + request);
                            client.send(request).whenComplete((confirmation, ex) -> {
                                log.info("Received from the central system: " + confirmation);
                                handleResponse(consumer, requestUuid, connectorId, reason);
                            });
                        } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                            log.warn("An error occurred while sending or processing stop transaction request");
                        }
                    }
                }
            } catch (Exception ignored) {
                log.error("An error occurred while receiving stop transaction data from the message");
            }
        };
        Thread finishingThread = new Thread(finishingTask);
        finishingThread.start();

    }

    private Reason getReasonFromEnum(String reason) {
        for (Reason enumReason : Reason.values()) {
            if (enumReason.name().equals(reason)) {
                return enumReason;
            }
        }
        return Other;
    }

    @Override
    public void sendRemoteStop(int connectorId) {
        ClientCoreProfile core = coreHandler.getCore();
        JSONClient client = bootNotification.getClient();
        // Use the feature profile to help create event
        StopTransactionRequest request = core.createStopTransactionRequest(
                connectorsInfoCache.getFullStationConsumedEnergy(connectorId), ZonedDateTime.now(),
                chargeSessionMap.getChargeSessionInfo(connectorId).getTransactionId()
        );
        request.setReason(Remote);

        if (client == null) {
            sender.sendRequestToQueue(
                    queues.getOCPPCache(),
                    UUID.randomUUID().toString(),
                    SaveToCache.name(),
                    request,
                    "stopTransaction"
            );
        } else {
            // Client returns a promise which will be filled once it receives a confirmation.
            try {
                remoteTriggerHandler.waitForRemoteTriggerTaskComplete();
                log.info("Sent to central system: " + request);
                client.send(request).whenComplete((confirmation, ex) -> {
                    log.info("Received from the central system: " + confirmation);
                    handleResponse("", "", connectorId, Remote.name());
                });
            } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                log.warn("An error occurred while sending or processing stop transaction request");
            }
        }
    }

    @Override
    public void checkTransactionCreation(List<Object> parsedMessage, List<Object> cashedRequest) {
        Map<String, String> map = (Map<String, String>) parsedMessage.get(2);
        if (!map.get("result").equals("Accepted")) {
            DBTablesCreateInfo createRequest = (DBTablesCreateInfo) cashedRequest.get(3);
            List<Map<String, String>> values = createRequest.getValues();
            int connectorId = values.stream()
                    .filter(transactionParam -> transactionParam.get("key").equals("connector_id")).
                    findFirst().map(transactionParam -> Integer.parseInt(transactionParam.get("value")))
                    .orElse(0);
            if ((connectorId != 0) && (chargeSessionMap.getChargeSessionInfo(connectorId) != null)) {
                ClientCoreProfile core = coreHandler.getCore();
                JSONClient client = bootNotification.getClient();
                // Use the feature profile to help create event
                StopTransactionRequest request = core.createStopTransactionRequest(
                        connectorsInfoCache.getFullStationConsumedEnergy(connectorId), ZonedDateTime.now(),
                        chargeSessionMap.getChargeSessionInfo(connectorId).getTransactionId()
                );
                request.setReason(DeAuthorized);

                if (client == null) {
                    sender.sendRequestToQueue(
                            queues.getOCPPCache(),
                            UUID.randomUUID().toString(),
                            SaveToCache.name(),
                            request,
                            "stopTransaction"
                    );
                } else {
                    // Client returns a promise which will be filled once it receives a confirmation.
                    try {
                        remoteTriggerHandler.waitForRemoteTriggerTaskComplete();
                        log.info("Sent to central system: " + request);
                        client.send(request).whenComplete((confirmation, ex) -> {
                            log.info("Received from the central system: " + confirmation);
                            sender.sendRequestToQueue(
                                    queues.getChargePointLogic(),
                                    UUID.randomUUID().toString(),
                                    StopChargeSession.name(),
                                    Map.of("connectorId", connectorId),
                                    StopChargeSession.name()
                            );
                        });
                    } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                        log.warn("An error occurred while sending or processing stop transaction request");
                    }
                }
                chargeSessionMap.removeFromChargeSessionMap(connectorId);
                meterValues.removeFromChargingConnectors(connectorId);
            }
        }
        String startTransactionStatus = null;
    }

    /**
     * Steve возвращал null, поэтому idTagInfo собирать не из чего. При необходимости можно предусмотреть
     */
    private void handleResponse(
            String consumer, String requestUuid, int connectorId, String reason
    ) {
        String consumedPower = String.valueOf(connectorsInfoCache.getFullStationConsumedEnergy(connectorId) -
                chargeSessionMap.getStartFullStationConsumedEnergy(connectorId));
        sender.sendRequestToQueue(
                queues.getDateBase(),
                UUID.randomUUID().toString(),
                Change.name(),
                new DBTablesChangeInfo(
                        transaction1.name(),
                        "transaction_id:" +
                                chargeSessionMap.getChargeSessionInfo(connectorId).getTransactionId(),
                        List.of(
                                Map.of("key", "consumed_power", "value", consumedPower),
                                Map.of("key", "full_station_consumed_energy", "value",
                                        String.valueOf(connectorsInfoCache.getFullStationConsumedEnergy(connectorId))),
                                Map.of("key", "reson_stop", "value", reason),
                                Map.of("key", "stop_date_time", "value",
                                        formatStartStopTransactionDateTime(new Date())),
                                Map.of("key", "stop_date_timeutc", "value",
                                        formatStartStopTransactionDateTimeUTC(new Date())),
                                Map.of("key", "stop_error", "value", "NoError"),
                                Map.of("key", "stop_percent", "value",
                                        String.valueOf(connectorsInfoCache.getPercent(connectorId))),
                                Map.of("key", "stop_vendor_error", "value", "NoErrorVendor")
                        )),
                transaction1.name()
        );
        if (!consumer.isBlank()) {
            sender.sendRequestToQueue(consumer, requestUuid, "", Map.of("status", "Accepted"), "");
        }
    }
}