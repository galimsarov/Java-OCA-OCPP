package pss.mira.orp.JavaOCAOCPP.service.rabbit.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pss.mira.orp.JavaOCAOCPP.models.requests.ocpp.StatusNotificationRequest;
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

import static eu.chargetime.ocpp.model.core.ChargePointStatus.Charging;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.ChangeAvailability;

@EnableRabbit
@Component
@Slf4j
public class ListenerImpl implements Listener {
    private final Authorize authorize;
    private final BootNotification bootNotification;
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
                                // чтобы не было циклических зависимостей отправку StatusNotification делаем здесь
                                int connectorId = requestCache.getConnectorId(
                                        parsedMessage.get(1).toString(), ChangeAvailability.name()
                                );
                                if (connectorId != -1) {
                                    StatusNotificationRequest statusNotificationRequest =
                                            connectorsInfoCache.getStatusNotificationRequest(connectorId);
                                    statusNotification.sendStatusNotification(statusNotificationRequest);
                                }
                                // здесь добавляем полученное значение из очереди, благодаря которому уйдёт ответ в ЦС
                                // на запрос по смене статуса
                                handler.setAvailabilityStatus(parsedMessage);
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
                        case ("StopTransaction"):
                            stopTransaction.sendStopTransaction(parsedMessage);
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
            while (connectorsInfoCache.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Error while waiting for the connectors info");
                }
            }
            bootNotification.sendBootNotification(parsedMessage);
    }

    @Override
    @RabbitListener(queues = "connectorsInfo")
    public void processConnectorsInfo(String message) {
        log.info("Received from connectorsInfo queue: " + message);
        try {
            List<Map<String, Object>> parsedMessage = objectMapper.readValue(message, List.class);
            List<StatusNotificationRequest> possibleRequests = connectorsInfoCache.addToCache(parsedMessage);
            for (StatusNotificationRequest request : possibleRequests) {
                statusNotification.sendStatusNotification(request);
                if (request.getStatus().equals(Charging)) {
                    meterValues.addToChargingConnectors(request.getId());
                } else {
                    meterValues.removeFromChargingConnectors(request.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error when parsing a message from the broker");
        }
    }
}