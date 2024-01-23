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
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.Handler;
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
    private final Handler handler;
    private final MeterValues meterValues;
    private final RequestCache requestCache;
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
            Handler handler,
            MeterValues meterValues,
            RequestCache requestCache,
            StartTransaction startTransaction,
            StatusNotification statusNotification,
            StopTransaction stopTransaction
    ) {
        this.authorize = authorize;
        this.bootNotification = bootNotification;
        this.chargeSessionMap = chargeSessionMap;
        this.connectorsInfoCache = connectorsInfoCache;
        this.dataTransfer = dataTransfer;
        this.handler = handler;
        this.meterValues = meterValues;
        this.requestCache = requestCache;
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
                            case ("auth_list"):
                                authorize.setAuthMap(parsedMessage);
                                break;
                            case ("Authorize"):
                                handler.setAuthorizeConfirmation(parsedMessage);
                                break;
                            case ("config_zs"):
                                sendBootNotification(parsedMessage);
                                break;
                            case ("getConfigurationForHandler"):
                                handler.setConfigurationMap(parsedMessage);
                                break;
                            case ("getConfigurationForMeterValues"):
                                meterValues.setConfigurationMap(parsedMessage);
                                break;
                            case ("changeConfiguration"):
                                handler.setChangeConfigurationStatus(parsedMessage);
                                break;
                            case ("ChangeAvailability"):
                                // здесь была отправка Status Notification, но она обрабатывается отдельно в очереди
                                // connectorsInfo
                                handler.setAvailabilityStatus(parsedMessage);
                                break;
                            case ("RemoteStartTransaction"):
                                handler.setRemoteStartStopStatus(parsedMessage, "start");
                                break;
                            case ("RemoteStopTransaction"):
                                handler.setRemoteStartStopStatus(parsedMessage, "stop");
                                break;
                        }
                        requestCache.removeFromCache(uuid);
                    }
                } else {
                    // запросы
                    switch (parsedMessage.get(2).toString()) {
                        case ("Authorize"):
                            authorize.sendAuthorize(parsedMessage);
                            break;
                        case ("DataTransfer"):
                            dataTransfer.sendDataTransfer(parsedMessage);
                            break;
                        case ("StartTransaction"):
                            startTransaction.sendStartTransaction(parsedMessage);
                            break;
                        case ("LocalStop"):
                            stopTransaction.sendLocalStop(parsedMessage);
                            break;
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
    // prod, test -> connectorsInfo
    // dev -> myQueue1
    @RabbitListener(queues = "connectorsInfo")
    public void processConnectorsInfo(String message) {
        log.info("Received from connectorsInfo queue: " + message);
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
    }
}