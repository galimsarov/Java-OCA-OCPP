package pss.mira.orp.JavaOCAOCPP.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class SampleController {
    private final RabbitTemplate template;
    String kind = "info";

    public SampleController(RabbitTemplate template) {
        this.template = template;
    }

    // Отправка сообщений в рэббит
    @PostMapping("/emit/")
    public ResponseEntity<String> emit(@RequestBody String message) {
        log.info("Send message in Queue witch listen " + kind);
        log.info("Sending message: " + message);
        // Указание типа обменника (биржи)
        template.setExchange("direct-exchange");
        template.convertAndSend(kind, message);
        return ResponseEntity.ok("Success emit to queue");
    }
}