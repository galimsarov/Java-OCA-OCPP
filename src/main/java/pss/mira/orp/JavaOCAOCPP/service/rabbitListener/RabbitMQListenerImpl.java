package pss.mira.orp.JavaOCAOCPP.service.rabbitListener;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pss.mira.orp.JavaOCAOCPP.service.cache.Cache;

import java.util.Map;

@EnableRabbit
@Component
@Slf4j
public class RabbitMQListenerImpl implements RabbitMQListener {
    private final Cache cache;

    public RabbitMQListenerImpl(Cache cache) {
        this.cache = cache;
    }

    @Override
    @RabbitListener(queues = "myQueue1")
    public void processAddressCS(String message) {
        Map<String, String> map = new Gson().fromJson(message, Map.class);
        log.info("Received from queue {}: {}", "configZS", map);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            cache.addToCache(entry.getKey(), entry.getValue());
        }
    }

    @Override
    @RabbitListener(queues = "myQueue2")
    public void processMyQueue2(String message) {
        log.info("Received from queue2 : {}", message);
    }
}