package pss.mira.orp.JavaOCAOCPP.service.rabbit.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pss.mira.orp.JavaOCAOCPP.models.requests.ocpp.StatusNotificationRequest;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.ChargeSessionMap;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.cache.request.RequestCache;
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
                            case "config_zs" -> sendBootNotification(parsedMessage);
                            case "getConfigurationForCoreHandler" -> coreHandler.setConfigurationList(parsedMessage);
                            case "getConfigurationForMeterValues" -> meterValues.setConfigurationMap(parsedMessage);
                            case "changeConfiguration" -> coreHandler.setChangeConfigurationStatus(parsedMessage);
                            case "ChangeAvailability" -> coreHandler.setAvailabilityStatus(parsedMessage);
                            case "RemoteStartTransaction" ->
                                    coreHandler.setRemoteStartStopStatus(parsedMessage, "start");
                            case "RemoteStopTransaction" -> coreHandler.setRemoteStartStopStatus(parsedMessage, "stop");
                            case "GetConnectorsInfo" -> {
                                List<StatusNotificationRequest> possibleRequests =
                                        connectorsInfoCache.createCache(parsedMessage);
                                for (StatusNotificationRequest request : possibleRequests) {
                                    statusNotification.sendStatusNotification(request);
                                }
                            }
                            case "Reset" -> coreHandler.setResetStatus(parsedMessage);
                            case "UnlockConnector" -> coreHandler.setUnlockConnectorStatus(parsedMessage);
                            // reservation
                            case "getConfigurationForReservationHandler" ->
                                    reservationHandler.setConfigurationList(parsedMessage);
                            case "reservation" -> reservationHandler.setReservationStatus(parsedMessage);
                        }
                        requestCache.removeFromCache(uuid);
                    }
                } else {
                    // запросы
                    switch (parsedMessage.get(2).toString()) {
                        case "Authorize" -> authorize.sendAuthorize(parsedMessage);
                        case "DataTransfer" -> dataTransfer.sendDataTransfer(parsedMessage);
                        case "StartTransaction" -> startTransaction.sendStartTransaction(parsedMessage);
                        case "LocalStop" -> stopTransaction.sendLocalStop(parsedMessage);
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
        Thread sendBootNotificationThread = new Thread(() -> bootNotification.sendBootNotification(parsedMessage));
        sendBootNotificationThread.start();
    }

    @Override
    // prod, test -> connectorsInfoOcpp
    // dev -> myQueue1
    @RabbitListener(queues = "connectorsInfoOcpp")
    public void processConnectorsInfo(String message) {
        log.info("Received from connectorsInfoOcpp queue: " + message);
        try {
            List<Map<String, Object>> parsedMessage = objectMapper.readValue(message, List.class);
            List<StatusNotificationRequest> possibleRequests = connectorsInfoCache.addToCache(parsedMessage);
            for (StatusNotificationRequest request : possibleRequests) {
                statusNotification.sendStatusNotification(request);
                if (request.getStatus().equals(Preparing) && chargeSessionMap.isRemoteStart(request.getId())) {
                    chargeSessionMap.stopPreparingTimer(request.getId());
                    if (chargeSessionMap.canSendStartTransaction(request.getId())) {
                        startTransaction.sendStartTransaction(chargeSessionMap.getChargeSessionInfo(request.getId()));
                    }
                }
                if (request.getStatus().equals(Charging) && chargeSessionMap.isRemoteStart(request.getId()) &&
                        (chargeSessionMap.getChargeSessionInfo(request.getId()).getPreparingTimer() == null)) {
                    startTransaction.sendStartTransaction(chargeSessionMap.getChargeSessionInfo(request.getId()));
                }
                if (request.getStatus().equals(Charging) && !chargeSessionMap.isRemoteStart(request.getId())) {
                    startTransaction.sendStartTransaction(chargeSessionMap.getChargeSessionInfo(request.getId()));
                }
                if (request.getStatus().equals(Finishing)) {
                    meterValues.removeFromChargingConnectors(request.getId());
                    if (chargeSessionMap.isRemoteStop(request.getId())) {
                        stopTransaction.sendRemoteStop(request.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error when parsing a message from the broker");
        }
        chargeSessionMap.deleteNotStartedRemoteTransactions();
    }
}