package pss.mira.orp.JavaOCAOCPP.service.rabbit.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.cache.request.RequestCache;

import java.util.Date;
import java.util.List;

import static pss.mira.orp.JavaOCAOCPP.models.enums.Services.ocpp;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.format;

@Service
@Slf4j
public class SenderImpl implements Sender {
    private final RequestCache requestCache;
    private final RabbitTemplate template;
    @Value("${rabbit.exchange}")
    private String exchange;
    @Value("${rabbit.service.version}")
    private String serviceVersion;

    public SenderImpl(RequestCache requestCache, RabbitTemplate template) {
        this.requestCache = requestCache;
        this.template = template;
    }

    @Override
    public void sendRequestToQueue(String key, String uuid, String action, Object body, String requestType) {
        List<Object> request;
        if (action.isEmpty()) {
            request = List.of(ocpp.name(), uuid, body);
        } else {
            request = List.of(ocpp.name(), uuid, action, body);
            requestCache.addToCache(request, requestType);
        }
        try {
            String message = (new ObjectMapper()).writeValueAsString(request);
            log.info("Send message in Queue witch listen " + key);
            log.info("Sending message: " + message);
            template.setExchange(exchange);
            template.convertAndSend(key, message);
        } catch (JsonProcessingException ignored) {
            log.error("Error when sending a request to the " + key);
        }
    }

    @Override
    public void sendVersion() {
        String message = serviceVersion;
        log.info("Send message in Queue witch listen version");
        log.info("Sending message: " + message);
        template.setExchange(exchange);
        template.convertAndSend("version", message);
    }

    @Override
    public void sendRabbitHeartbeat() {
        String message = "OCPP-service:" + format(new Date());
        log.info("Send message in Queue witch listen HeartBeat");
        log.info("Sending message: " + message);
        template.setExchange(exchange);
        template.convertAndSend("HeartBeat", message);
    }
}