package pss.mira.orp.JavaOCAOCPP.service.rabbit.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pss.mira.orp.JavaOCAOCPP.models.info.ocpp.StatusNotificationInfo;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.ChargeSessionMap;
import pss.mira.orp.JavaOCAOCPP.service.cache.configuration.ConfigurationCache;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.cache.nonStoppedTransaction.NonStoppedTransactionCache;
import pss.mira.orp.JavaOCAOCPP.service.cache.request.RequestCache;
import pss.mira.orp.JavaOCAOCPP.service.cache.reservation.ReservationCache;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.authorize.Authorize;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.dataTransfer.DataTransfer;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.core.CoreHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.reservation.ReservationHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.meterValues.MeterValues;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.startTransaction.StartTransaction;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.statusNotification.StatusNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.stopTransaction.StopTransaction;

import java.util.List;
import java.util.Map;

import static eu.chargetime.ocpp.model.core.ChargePointStatus.*;
import static eu.chargetime.ocpp.model.core.Reason.Other;
import static eu.chargetime.ocpp.model.core.Reason.Reboot;

@EnableRabbit
@Component
@Slf4j
public class ListenerImpl implements Listener {
    private boolean localStopReceived = false;

    private final Authorize authorize;
    private final BootNotification bootNotification;
    private final ChargeSessionMap chargeSessionMap;
    private final ConfigurationCache configurationCache;
    private final ConnectorsInfoCache connectorsInfoCache;
    private final DataTransfer dataTransfer;
    private final CoreHandler coreHandler;
    private final MeterValues meterValues;
    private final NonStoppedTransactionCache nonStoppedTransactionCache;
    private final RequestCache requestCache;
    private final ReservationCache reservationCache;
    private final ReservationHandler reservationHandler;
    private final StartTransaction startTransaction;
    private final StatusNotification statusNotification;
    private final StopTransaction stopTransaction;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ListenerImpl(
            Authorize authorize,
            BootNotification bootNotification,
            ChargeSessionMap chargeSessionMap,
            ConfigurationCache configurationCache,
            ConnectorsInfoCache connectorsInfoCache,
            DataTransfer dataTransfer,
            CoreHandler coreHandler,
            MeterValues meterValues,
            NonStoppedTransactionCache nonStoppedTransactionCache,
            ReservationCache reservationCache,
            RequestCache requestCache,
            ReservationHandler reservationHandler,
            StartTransaction startTransaction,
            StatusNotification statusNotification,
            StopTransaction stopTransaction
    ) {
        this.authorize = authorize;
        this.bootNotification = bootNotification;
        this.chargeSessionMap = chargeSessionMap;
        this.configurationCache = configurationCache;
        this.connectorsInfoCache = connectorsInfoCache;
        this.dataTransfer = dataTransfer;
        this.coreHandler = coreHandler;
        this.meterValues = meterValues;
        this.nonStoppedTransactionCache = nonStoppedTransactionCache;
        this.reservationCache = reservationCache;
        this.requestCache = requestCache;
        this.reservationHandler = reservationHandler;
        this.startTransaction = startTransaction;
        this.statusNotification = statusNotification;
        this.stopTransaction = stopTransaction;
    }

    @Override
    @RabbitListener(queues = "ocpp")
    public void processOCPP(String message) {
        log.info("Received from ocpp queue: " + message);
        try {
            List<Object> parsedMessage = objectMapper.readValue(message, List.class);
            if (parsedMessage.size() >= 3) {
                if (parsedMessage.size() == 3) {
                    // ответы
                    String uuid = parsedMessage.get(1).toString();
                    List<Object> cashedRequest = requestCache.getCashedRequest(uuid);
                    if (cashedRequest != null) {
                        // запрос в кэше найден
                        switch (cashedRequest.get(4).toString()) {
                            // core
                            case "auth_list" -> authorize.setAuthMap(parsedMessage);
                            case "Authorize" -> coreHandler.setAuthorizeConfirmation(parsedMessage);
                            case "BootNotification" ->
                                    bootNotification.sendBootNotification(parsedMessage, "bootNotification");
                            case "changeConfiguration" -> coreHandler.setChangeConfigurationStatus(parsedMessage);
                            case "ChangeConnectorAvailability" ->
                                    coreHandler.setConnectorAvailabilityStatus(parsedMessage);
                            case "ChangeStationAvailability" -> coreHandler.setStationAvailabilityStatus(parsedMessage);
                            case "GetConfiguration" -> configurationCache.createCache(parsedMessage);
                            case "GetConnectorsInfo" -> {
                                List<StatusNotificationInfo> possibleRequests =
                                        connectorsInfoCache.createCache(parsedMessage);
                                for (StatusNotificationInfo request : possibleRequests) {
                                    statusNotification.sendStatusNotification(request);
                                }
                            }
                            case "GetNonStoppedTransactions" ->
                                    stopTransaction.checkTransactionsAfterReboot(parsedMessage);
                            case "RemoteStartTransaction" ->
                                    coreHandler.setRemoteStartStopStatus(parsedMessage, "start");
                            case "RemoteStopTransaction" -> coreHandler.setRemoteStartStopStatus(parsedMessage, "stop");
                            case "reservation" -> reservationCache.createCache(parsedMessage);
                            case "Reset" -> coreHandler.setResetStatus(parsedMessage);
                            case "transaction1" ->
                                    stopTransaction.checkTransactionCreation(parsedMessage, cashedRequest);
                            case "UnlockConnector" -> coreHandler.setUnlockConnectorStatus(parsedMessage);
                            // reservation
                            case "ReserveNow" -> reservationHandler.setReservationResult(parsedMessage);
                            case "CancelReservation" -> reservationHandler.setCancelReservationStatus(parsedMessage);
                            // remote trigger
                            case "RemoteTriggerBootNotification" ->
                                    bootNotification.sendBootNotification(parsedMessage, "remoteTrigger");
                        }
                        requestCache.removeFromCache(uuid);
                    }
                } else {
                    // запросы
                    switch (parsedMessage.get(2).toString()) {
                        case "Authorize" -> authorize.sendAuthorize(parsedMessage);
                        case "DataTransfer" -> dataTransfer.sendDataTransfer(parsedMessage);
                        case "LocalStop" -> {
                            localStopReceived = true;
                            stopTransaction.sendLocalStop(parsedMessage);
                            localStopReceived = false;
                        }
                        case "StartTransaction" -> startTransaction.sendStartTransaction(parsedMessage);
                        // remote trigger
                        case "SendHeartbeatToCentralSystem" -> bootNotification.sendTriggerMessageHeartbeat();
                        case "SendMeterValuesToCentralSystem" ->
                                meterValues.sendTriggerMessageMeterValues(parsedMessage);
                        case "SendStatusNotificationToCentralSystem" ->
                                statusNotification.sendTriggerMessageStatusNotification(parsedMessage);
                    }
                }
            } else {
                log.error("Error when parsing a message from the ocpp queue. The length of the message must be 3 for " +
                        "the response and 4 for the request");
            }
        } catch (Exception e) {
            log.error("Error when parsing a message from the ocpp queue");
        }
    }

    @Override
    // prod, test -> connectorsInfoOcpp
    // dev -> myQueue1
    @RabbitListener(queues = "connectorsInfoOcpp")
    public void processConnectorsInfo(String message) {
        log.info("Received from connectorsInfoOcpp queue: " + message);
        if (!connectorsInfoCache.isEmpty()) {
            try {
                List<Map<String, Object>> parsedMessage = objectMapper.readValue(message, List.class);
                List<StatusNotificationInfo> possibleRequests = connectorsInfoCache.addToCache(parsedMessage);
                for (StatusNotificationInfo request : possibleRequests) {
                    statusNotification.sendStatusNotification(request);
                    tryRemoteStartAndStopPreparingTimer(request);
                    remoteStartForCharging(request);
                    localStartForCharging(request);
                    removeFromChargingAndStopRemote(request);
                }
            } catch (Exception e) {
                log.error("Error when parsing a message from the connectorsInfoOcpp queue");
            }
            chargeSessionMap.deleteNotStartedRemoteTransactions();
        }
    }

    @Override
    @RabbitListener(queues = "reservationOcpp")
    public void processReservation(String message) {
        log.info("Received from reservationOcpp queue: " + message);
        try {
            List<Map<String, Object>> parsedMessage = objectMapper.readValue(message, List.class);
            reservationCache.addToCache(parsedMessage);
        } catch (Exception e) {
            log.error("Error when parsing a message from the reservationOcpp queue");
        }
    }

    @Override
    @RabbitListener(queues = "configurationOcpp")
    public void processConfiguration(String message) {
        log.info("Received from configurationOcpp queue: " + message);
        try {
            List<Map<String, Object>> parsedMessage = objectMapper.readValue(message, List.class);
            configurationCache.addToCache(parsedMessage);
        } catch (Exception e) {
            log.error("Error when parsing a message from the configurationOcpp queue");
        }
    }

    private void removeFromChargingAndStopRemote(StatusNotificationInfo request) {
        if ((request.getStatus().equals(Finishing) || request.getStatus().equals(Faulted)) &&
                !reservationCache.reserved(request.getId())
        ) {
            if (chargeSessionMap.getChargeSessionInfo(request.getId()) != null) {
                chargeSessionMap.setFinishOrFaulted(request.getId());
            }
            if ((chargeSessionMap.getChargeSessionInfo(request.getId()) != null) &&
                    !chargeSessionMap.getChargeSessionInfo(request.getId()).isRemoteStop() &&
                    !chargeSessionMap.getChargeSessionInfo(request.getId()).isLocalStop()
            ) {
                if ((chargeSessionMap.getChargeSessionInfo(request.getId()) != null) &&
                        (chargeSessionMap.getChargeSessionInfo(request.getId()).getTransactionId() != 0)
                ) {
                    setLocalStopTimer(request.getId());
                } else {
                    chargeSessionMap.removeFromChargeSessionMap(request.getId());
                }
            }
            if ((chargeSessionMap.getChargeSessionInfo(request.getId()) != null) &&
                    chargeSessionMap.isRemoteStop(request.getId())
            ) {
                stopTransaction.sendRemoteStop(request.getId());
            }
            if ((chargeSessionMap.getChargeSessionInfo(request.getId()) != null) &&
                    chargeSessionMap.isLocalStop(request.getId())
            ) {
                meterValues.removeFromChargingConnectors(request.getId());
            }
            if ((chargeSessionMap.getChargeSessionInfo(request.getId()) == null) &&
                    request.getStatus().equals(Finishing) &&
                    nonStoppedTransactionCache.hasNonStoppedTransactionsOnConnector(request.getId())
            ) {
                stopTransaction.sendLocalStopWithReason(request.getId(), Reboot);
            }
        }
    }

    private void setLocalStopTimer(int connectorId) {
        int[] timer = new int[]{20};
        Runnable timerTask = () -> {
            while (timer[0] > 0) {
                if (localStopReceived) {
                    break;
                }
                if (timer[0] % 10 == 0) {
                    log.warn(timer[0] + " seconds for the connector to receive local stop");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("An error while waiting to receive local stop");
                }
                timer[0]--;
            }
            if (!localStopReceived) {
                log.error("The time for the connector to receive local stop has expired. The stop of the transaction " +
                        "is sent to the central system");
                stopTransaction.sendLocalStopWithReason(connectorId, Other);
            }
        };
        Thread timerThread = new Thread(timerTask);
        timerThread.start();
    }

    private void localStartForCharging(StatusNotificationInfo request) {
        if (request.getStatus().equals(Charging) && !chargeSessionMap.isRemoteStart(request.getId()) &&
                !reservationCache.reserved(request.getId())
        ) {
            startTransaction.sendStartTransaction(chargeSessionMap.getChargeSessionInfo(request.getId()));
        }
    }

    private void remoteStartForCharging(StatusNotificationInfo request) {
        if (
                request.getStatus().equals(Charging) &&
                        (chargeSessionMap.getChargeSessionInfo(request.getId()) != null) &&
                        chargeSessionMap.isRemoteStart(request.getId()) &&
                        (chargeSessionMap.getChargeSessionInfo(request.getId()).getPreparingTimer() == null) &&
                        !reservationCache.reserved(request.getId())
        ) {
            startTransaction.sendStartTransaction(chargeSessionMap.getChargeSessionInfo(request.getId()));
        }
    }

    private void tryRemoteStartAndStopPreparingTimer(StatusNotificationInfo request) {
        if (request.getStatus().equals(Preparing) &&
                (chargeSessionMap.getChargeSessionInfo(request.getId()) != null) &&
                chargeSessionMap.isRemoteStart(request.getId()) &&
                !reservationCache.reserved(request.getId())
        ) {
            chargeSessionMap.stopPreparingTimer(request.getId());
            if (chargeSessionMap.canSendStartTransaction(request.getId())) {
                startTransaction.sendStartTransaction(chargeSessionMap.getChargeSessionInfo(request.getId()));
            }
        }
    }
}