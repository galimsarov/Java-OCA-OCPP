package pss.mira.orp.JavaOCAOCPP.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.authorize.Authorize;

@RestController
@Slf4j
public class SampleController {
    private final RabbitTemplate template;
    private final Authorize auth;
    String kind = "info";

    public SampleController(RabbitTemplate template, Authorize auth) {
        this.template = template;
        this.auth = auth;
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

    @PostMapping("/auth/")
    public ResponseEntity<String> auth(@RequestBody String idTag) {
        log.info("Send auth to the central system");
        auth.sendAuthorize(idTag);
        return ResponseEntity.ok("Success emit to queue");
    }
}