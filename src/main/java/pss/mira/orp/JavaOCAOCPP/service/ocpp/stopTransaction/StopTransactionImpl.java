package pss.mira.orp.JavaOCAOCPP.service.ocpp.stopTransaction;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.core.Reason;
import eu.chargetime.ocpp.model.core.StopTransactionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.requests.rabbit.DBTablesChangeRequest;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.ChargeSessionMap;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.Handler;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.chargetime.ocpp.model.core.Reason.Other;
import static eu.chargetime.ocpp.model.core.Reason.Remote;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.Change;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.SaveToCache;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.transaction1;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Queues.bd;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Queues.ocppCache;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.formatStartStopTransactionDateTime;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.formatStartStopTransactionDateTimeUTC;

@Service
@Slf4j
public class StopTransactionImpl implements StopTransaction {
    private final BootNotification bootNotification;
    private final ChargeSessionMap chargeSessionMap;
    private final ConnectorsInfoCache connectorsInfoCache;
    private final Handler handler;
    private final Sender sender;

    public StopTransactionImpl(
            BootNotification bootNotification,
            ChargeSessionMap chargeSessionMap,
            ConnectorsInfoCache connectorsInfoCache,
            Handler handler,
            Sender sender
    ) {
        this.bootNotification = bootNotification;
        this.chargeSessionMap = chargeSessionMap;
        this.connectorsInfoCache = connectorsInfoCache;
        this.handler = handler;
        this.sender = sender;
    }

    /**
     * ["myQueue1","71f599b2-b3f0-4680-b447-ae6d6dc0cc0c","StopTransaction",{"transactionId":2682}]
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
                ClientCoreProfile core = handler.getCore();
                JSONClient client = bootNotification.getClient();
                // Use the feature profile to help create event
                StopTransactionRequest request = core.createStopTransactionRequest(
                        connectorsInfoCache.getFullStationConsumedEnergy(connectorId), ZonedDateTime.now(),
                        chargeSessionMap.getChargeSessionInfo(connectorId).getTransactionId()
                );
                request.setReason(getReasonFromEnum(reason));

                if (client == null) {
                    sender.sendRequestToQueue(
                            ocppCache.name(),
                            UUID.randomUUID().toString(),
                            SaveToCache.name(),
                            request,
                            "stopTransaction"
                    );
                } else {
                    // Client returns a promise which will be filled once it receives a confirmation.
                    try {
                        client.send(request).whenComplete((confirmation, ex) -> {
                            log.info("Received from the central system: " + confirmation);
                            handleResponse(consumer, requestUuid, confirmation, connectorId, reason);
                        });
                    } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                        log.warn("An error occurred while sending or processing stop transaction request");
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
        ClientCoreProfile core = handler.getCore();
        JSONClient client = bootNotification.getClient();
        // Use the feature profile to help create event
        StopTransactionRequest request = core.createStopTransactionRequest(
                connectorsInfoCache.getFullStationConsumedEnergy(connectorId), ZonedDateTime.now(),
                chargeSessionMap.getChargeSessionInfo(connectorId).getTransactionId()
        );
        request.setReason(Remote);

        if (client == null) {
            sender.sendRequestToQueue(
                    ocppCache.name(),
                    UUID.randomUUID().toString(),
                    SaveToCache.name(),
                    request,
                    "stopTransaction"
            );
        } else {
            // Client returns a promise which will be filled once it receives a confirmation.
            try {
                client.send(request).whenComplete((confirmation, ex) -> {
                    log.info("Received from the central system: " + confirmation);
                    handleResponse("", "", confirmation, connectorId, Remote.name());
                });
            } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                log.warn("An error occurred while sending or processing stop transaction request");
            }
        }
    }

    /**
     * Steve возвращал null, поэтому idTagInfo собирать не из чего. При необходимости можно предусмотреть
     */
    private void handleResponse(
            String consumer, String requestUuid, Confirmation confirmation, int connectorId, String reason
    ) {
        String consumedPower = String.valueOf(connectorsInfoCache.getFullStationConsumedEnergy(connectorId) -
                chargeSessionMap.getStartFullStationConsumedEnergy(connectorId));
        sender.sendRequestToQueue(
                bd.name(),
                UUID.randomUUID().toString(),
                Change.name(),
                new DBTablesChangeRequest(
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
        chargeSessionMap.removeFromChargeSessionMap(connectorId);
        if (!consumer.isBlank()) {
            sender.sendRequestToQueue(consumer, requestUuid, "", confirmation, "");
        }
    }
}