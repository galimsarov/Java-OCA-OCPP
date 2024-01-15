package pss.mira.orp.JavaOCAOCPP.service.rabbit.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pss.mira.orp.JavaOCAOCPP.models.enums.Actions;
import pss.mira.orp.JavaOCAOCPP.models.requests.ocpp.StatusNotificationRequest;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.cache.request.RequestCache;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.authorize.Authorize;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.Handler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.statusNotification.StatusNotification;

import java.util.List;
import java.util.Map;

import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.ChangeAvailability;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.config_zs;

@EnableRabbit
@Component
@Slf4j
public class ListenerImpl implements Listener {
    private final Authorize authorize;
    private final BootNotification bootNotification;
    private final ConnectorsInfoCache connectorsInfoCache;
    private final Handler handler;
    private final RequestCache requestCache;
    private final StatusNotification statusNotification;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ListenerImpl(
            Authorize authorize,
            BootNotification bootNotification,
            ConnectorsInfoCache connectorsInfoCache,
            Handler handler,
            RequestCache requestCache,
            StatusNotification statusNotification
    ) {
        this.authorize = authorize;
        this.bootNotification = bootNotification;
        this.connectorsInfoCache = connectorsInfoCache;
        this.handler = handler;
        this.requestCache = requestCache;
        this.statusNotification = statusNotification;
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
                        if (cashedRequest.get(4).equals(config_zs.name())) {
                            sendBootNotification(parsedMessage);
                        } else if (cashedRequest.get(4).equals(ChangeAvailability.name())) {
                            // чтобы не было циклических зависимостей отправку StatusNotification делаем здесь
                            int connectorId = requestCache.getConnectorId(parsedMessage.get(1).toString(), ChangeAvailability.name());
                            if (connectorId != -1) {
                                StatusNotificationRequest statusNotificationRequest =
                                        connectorsInfoCache.getStatusNotificationRequest(connectorId);
                                statusNotification.sendStatusNotification(statusNotificationRequest);
                            }
                            // здесь добавляем полученное значение из очереди, благодаря которому уйдёт ответ в ЦС на
                            // запрос по смене статуса
                            handler.setAvailabilityStatus(parsedMessage);
                        }
                        requestCache.removeFromCache(uuid);
                    }
                } else {
                    // запросы
                    if (parsedMessage.get(2).toString().equals(Actions.Authorize.name())) {
                        authorize.sendAuthorize(parsedMessage);
                    }
                }
            } else {
                log.error("Error when parsing a message from the broker. The length of the message must be 3 for the " +
                        "response and 4 for the request");
            }
        } catch (JsonProcessingException e) {
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
            }

        } catch (JsonProcessingException e) {
            log.error("Error when parsing a message from the broker");
        }
    }
}