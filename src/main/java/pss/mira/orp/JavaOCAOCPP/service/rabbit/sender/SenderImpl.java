package pss.mira.orp.JavaOCAOCPP.service.rabbit.sender;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.queues.Queues;
import pss.mira.orp.JavaOCAOCPP.service.cache.request.RequestCache;

import java.util.Date;
import java.util.List;

import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.formatHeartbeatDateTime;

@PropertySource({"classpath:application.properties", "classpath:revision.properties"})
@Service
@Slf4j
public class SenderImpl implements Sender {
    private final Queues queues;
    private final RequestCache requestCache;
    private final RabbitTemplate template;
    @Value("${rabbit.exchange}")
    private String exchange;
    @Value("${git.commit.id.abbrev}")
    private String serviceVersion;

    public SenderImpl(Queues queues, RequestCache requestCache, RabbitTemplate template) {
        this.queues = queues;
        this.requestCache = requestCache;
        this.template = template;
    }

    @Override
    public void sendRequestToQueue(String key, String uuid, String action, Object body, String requestType) {
        List<Object> request;
        if (action.isEmpty()) {
            request = List.of(queues.getOCPP(), uuid, body);
        } else {
            request = List.of(queues.getOCPP(), uuid, action, body);
            requestCache.addToCache(request, requestType);
        }
        String message = (new Gson()).toJson(request);
        log.info("Send message in Queue witch listen " + key);
        log.info("Sending message: " + message);
        template.setExchange(exchange);
        template.convertAndSend(key, message);
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
        String message = "OCPP-service:" + formatHeartbeatDateTime(new Date());
        log.info("Send message in Queue witch listen HeartBeat");
        log.info("Sending message: " + message);
        template.setExchange(exchange);
        template.convertAndSend("HeartBeat", message);
    }
}