package pss.mira.orp.JavaOCAOCPP.service.rabbit.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pss.mira.orp.JavaOCAOCPP.models.info.ocpp.StatusNotificationInfo;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.ChargeSessionMap;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
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

@EnableRabbit
@Component
@Slf4j
public class ListenerImpl implements Listener {
    private final Authorize authorize;
    private final BootNotification bootNotification;
    private final ChargeSessionMap chargeSessionMap;
    private final ConnectorsInfoCache connectorsInfoCache;
    private final DataTransfer dataTransfer;
    private final CoreHandler coreHandler;
    private final MeterValues meterValues;
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
            ConnectorsInfoCache connectorsInfoCache,
            DataTransfer dataTransfer,
            CoreHandler coreHandler,
            MeterValues meterValues,
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
        this.connectorsInfoCache = connectorsInfoCache;
        this.dataTransfer = dataTransfer;
        this.coreHandler = coreHandler;
        this.meterValues = meterValues;
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
                            case "ChangeAvailability" -> coreHandler.setAvailabilityStatus(parsedMessage);
                            case "getConfigurationForCoreHandler" -> coreHandler.setConfigurationList(parsedMessage);
                            case "getConfigurationForMeterValues" -> meterValues.setConfigurationMap(parsedMessage);
                            case "GetConnectorsInfo" -> {
                                List<StatusNotificationInfo> possibleRequests =
                                        connectorsInfoCache.createCache(parsedMessage);
                                for (StatusNotificationInfo request : possibleRequests) {
                                    statusNotification.sendStatusNotification(request);
                                }
                            }
                            case "RemoteStartTransaction" ->
                                    coreHandler.setRemoteStartStopStatus(parsedMessage, "start");
                            case "RemoteStopTransaction" -> coreHandler.setRemoteStartStopStatus(parsedMessage, "stop");
                            case "RemoteTriggerBootNotification" ->
                                    bootNotification.sendBootNotification(parsedMessage, "remoteTrigger");
                            case "reservation" -> reservationCache.createCache(parsedMessage);
                            case "Reset" -> coreHandler.setResetStatus(parsedMessage);
                            case "transaction1" ->
                                    stopTransaction.checkTransactionCreation(parsedMessage, cashedRequest);
                            case "UnlockConnector" -> coreHandler.setUnlockConnectorStatus(parsedMessage);
                            // reservation
                            case "getConfigurationForReservationHandler" ->
                                    reservationHandler.setConfigurationList(parsedMessage);
                            case "ReserveNow" -> reservationHandler.setReservationResult(parsedMessage);
                            case "CancelReservation" -> reservationHandler.setCancelReservationStatus(parsedMessage);
                        }
                        requestCache.removeFromCache(uuid);
                    }
                } else {
                    // запросы
                    switch (parsedMessage.get(2).toString()) {
                        case "Authorize" -> authorize.sendAuthorize(parsedMessage);
                        case "DataTransfer" -> dataTransfer.sendDataTransfer(parsedMessage);
                        case "LocalStop" -> stopTransaction.sendLocalStop(parsedMessage);
                        case "StartTransaction" -> startTransaction.sendStartTransaction(parsedMessage);
                    }
                }
            } else {
                log.error("Error when parsing a message from the broker. The length of the message must be 3 for the " +
                        "response and 4 for the request");
            }
        } catch (Exception e) {
            log.error("Error when parsing a message from the broker");
        }
    }

    private void sendBootNotification(List<Object> parsedMessage) {
        Thread sendBootNotificationThread = new Thread(
                () -> bootNotification.sendBootNotification(parsedMessage, "bootNotification")
        );
        sendBootNotificationThread.start();
    }

    @Override
    // prod, test -> connectorsInfoOcpp
    // dev -> myQueue1
    @RabbitListener(queues = "myQueue1")
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
                log.error("Error when parsing a message from the broker");
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
            log.error("Error when parsing a message from the broker");
        }
    }

    private void removeFromChargingAndStopRemote(StatusNotificationInfo request) {
        if (request.getStatus().equals(Finishing) && !reservationCache.reserved(request.getId())) {
            if (chargeSessionMap.isRemoteStop(request.getId())) {
                stopTransaction.sendRemoteStop(request.getId());
            }
            meterValues.removeFromChargingConnectors(request.getId());
        }
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
                        chargeSessionMap.isRemoteStart(request.getId()) &&
                        (chargeSessionMap.getChargeSessionInfo(request.getId()).getPreparingTimer() == null) &&
                        !reservationCache.reserved(request.getId())
        ) {
            startTransaction.sendStartTransaction(chargeSessionMap.getChargeSessionInfo(request.getId()));
        }
    }

    private void tryRemoteStartAndStopPreparingTimer(StatusNotificationInfo request) {
        if (request.getStatus().equals(Preparing) && chargeSessionMap.isNotEmpty() &&
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