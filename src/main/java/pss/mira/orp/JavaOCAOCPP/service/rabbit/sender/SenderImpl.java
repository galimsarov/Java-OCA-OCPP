package pss.mira.orp.JavaOCAOCPP.service.rabbit.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.cache.request.RequestCache;

import java.util.List;

import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.config_zs;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Services.ocpp;

@Service
@Slf4j
public class SenderImpl implements Sender {
    private final RequestCache requestCache;
    private final RabbitTemplate template;
    @Value("${rabbit.exchange}")
    private String exchange;

    public SenderImpl(RequestCache requestCache, RabbitTemplate template) {
        this.requestCache = requestCache;
        this.template = template;
    }

    @Override
    public void sendRequestToQueue(String key, String uuid, String action, Object body) {
        List<Object> request;
        if (action.isEmpty()) {
            request = List.of(ocpp.name(), uuid, body);
        } else {
            request = List.of(ocpp.name(), uuid, action, body);
        }
        try {
            String message = (new ObjectMapper()).writeValueAsString(request);
            log.info("Send message in Queue witch listen " + key);
            requestCache.addToCache(request, config_zs.name());
            log.info("Sending message: " + message);
            template.setExchange(exchange);
            template.convertAndSend(key, message);
        } catch (JsonProcessingException ignored) {
            log.error("Error when sending a request to the " + key);
        }
    }
}