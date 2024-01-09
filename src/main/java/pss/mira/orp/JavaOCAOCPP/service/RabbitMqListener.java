package pss.mira.orp.JavaOCAOCPP.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@EnableRabbit
@Component
@Slf4j
public class RabbitMqListener {
    // Прослушка сообщений по определенной очереди
    @RabbitListener(queues = "myQueue1")
    public void processMyQueue1(String message) {
        log.info("Received from queue1 : {}", message);
    }

    @RabbitListener(queues = "myQueue2")
    public void processMyQueue2(String message) {
        log.info("Received from queue2 : {}", message);
    }
}