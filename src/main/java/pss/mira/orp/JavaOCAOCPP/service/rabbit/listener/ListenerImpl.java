package pss.mira.orp.JavaOCAOCPP.service.rabbit.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.cache.request.RequestCache;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.authorize.Authorize;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;

import java.util.List;
import java.util.Map;

import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.config_zs;

@EnableRabbit
@Component
@Slf4j
public class ListenerImpl implements Listener {
    private final Authorize authorize;
    private final BootNotification bootNotification;
    private final ConnectorsInfoCache connectorsInfoCache;
    private final RequestCache requestCache;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ListenerImpl(Authorize authorize, BootNotification bootNotification, ConnectorsInfoCache connectorsInfoCache, RequestCache requestCache) {
        this.authorize = authorize;
        this.bootNotification = bootNotification;
        this.connectorsInfoCache = connectorsInfoCache;
        this.requestCache = requestCache;
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
                            Thread waitingConnectorsInfoThread = getWaitingConnectorsInfoThread(parsedMessage);
                            waitingConnectorsInfoThread.start();
                        }
                        requestCache.removeFromCache(uuid);
                    }
                } else {
                    // запросы
                    switch (parsedMessage.get(2).toString()) {
                        case ("Authorize"):
                            authorize.sendAuthorize(parsedMessage);
                            break;
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

    private Thread getWaitingConnectorsInfoThread(List<Object> parsedMessage) {
        Runnable task = () -> {
            while (connectorsInfoCache.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Error while waiting for the connectors info");
                }
            }
            bootNotification.sendBootNotification(parsedMessage);
        };
        return new Thread(task);
    }

    @Override
    @RabbitListener(queues = "connectorsInfo")
    public void processConnectorsInfo(String message) {
        log.info("Received from connectorsInfo queue: " + message);
        try {
            List<Map<String, Object>> parsedMessage = objectMapper.readValue(message, List.class);
            connectorsInfoCache.addToCache(parsedMessage);
        } catch (JsonProcessingException e) {
            log.error("Error when parsing a message from the broker");
        }
    }
}