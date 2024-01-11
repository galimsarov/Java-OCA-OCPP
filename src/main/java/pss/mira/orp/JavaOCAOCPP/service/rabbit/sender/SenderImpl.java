package pss.mira.orp.JavaOCAOCPP.service.rabbit.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.cache.Cache;

import java.util.List;
import java.util.UUID;

import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.Get;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.config_zs;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Services.ocpp;

@Service
@Slf4j
public class SenderImpl implements Sender {
    private final Cache cache;
    private final RabbitTemplate template;
    @Value("${rabbit.exchange}")
    private String exchange;

    public SenderImpl(Cache cache, RabbitTemplate template) {
        this.cache = cache;
        this.template = template;
    }

    @Override
    public void sendRequestToQueue(String key, String action) {
        List<Object> request = List.of(ocpp.name(), UUID.randomUUID(), Get.name(), List.of(config_zs.name()));
        try {
            String message = (new ObjectMapper()).writeValueAsString(request);
            log.info("Send message in Queue witch listen " + key);
            cache.addToCache(request, config_zs.name());
            log.info("Sending message: " + message);
            template.setExchange(exchange);
            template.convertAndSend(key, message);
        } catch (JsonProcessingException ignored) {
            log.error("Error when sending a request to the database");
        }
    }
}