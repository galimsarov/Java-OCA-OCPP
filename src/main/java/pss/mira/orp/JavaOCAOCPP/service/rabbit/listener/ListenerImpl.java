package pss.mira.orp.JavaOCAOCPP.service.rabbit.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pss.mira.orp.JavaOCAOCPP.service.cache.Cache;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;

import java.util.List;
import java.util.Map;

import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.config_zs;

@EnableRabbit
@Component
@Slf4j
public class ListenerImpl implements Listener {
    private final Cache cache;
    private final BootNotification bootNotification;

    public ListenerImpl(Cache cache, BootNotification bootNotification) {
        this.cache = cache;
        this.bootNotification = bootNotification;
    }

    @Override
    @RabbitListener(queues = "ocpp")
    public void processAddressCS(String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Object> parsedMessage = objectMapper.readValue(message, List.class);
            if (parsedMessage.size() >= 3) {
                List<Object> cashedRequest = cache.getCashedRequest(parsedMessage.get(1).toString());
                if (cashedRequest != null) {
                    // ответы
                    if (cashedRequest.get(4).equals(config_zs.name())) {
                        List<Map<String, Object>> configZSList = (List<Map<String, java.lang.Object>>) parsedMessage.get(2);
                        bootNotification.sendBootNotification(configZSList);
                    }
                } else {
                    // запросы
                }
            } else {
                log.error("Error when parsing a message from the broker. The length of the message must be 3 for the " +
                        "response and 4 for the request");
            }
        } catch (JsonProcessingException e) {
            log.error("Error when parsing a message from the broker");
        }

//        Map<String, String> map = new Gson().fromJson(message, Map.class);
//        log.info("Received from queue {}: {}", "configZS", map);
//        for (Map.Entry<String, String> entry : map.entrySet()) {
//            cache.addToCache(entry.getKey(), entry.getValue());
//        }
    }

    @Override
    @RabbitListener(queues = "myQueue2")
    public void processMyQueue2(String message) {
        log.info("Received from queue2 : {}", message);
    }


}