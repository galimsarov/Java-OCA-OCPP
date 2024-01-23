package pss.mira.orp.JavaOCAOCPP.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.List;
import java.util.UUID;

import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.Get;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.config_zs;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Queues.bd;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getDBTablesGetRequest;

@Component
@Slf4j
public class OcppLoader implements CommandLineRunner {
    private final Sender sender;

    public OcppLoader(Sender sender) {
        this.sender = sender;
    }

    @Override
    public void run(String... args) {
        sender.sendVersion();

        Thread heartbeatRabbitThread = getHeartbeatRabbitThread();
        heartbeatRabbitThread.start();

        sender.sendRequestToQueue(
                bd.name(),
                UUID.randomUUID().toString(),
                Get.name(),
                getDBTablesGetRequest(List.of(config_zs.name())),
                config_zs.name()
        );
    }

    private Thread getHeartbeatRabbitThread() {
        Runnable heartbeatRabbitTask = () -> {
            while (true) {
                sender.sendRabbitHeartbeat();
                try {
                    Thread.sleep(30_000);
                } catch (InterruptedException e) {
                    log.error("Аn error while waiting for a rabbit heartbeat to be sent");
                }
            }
        };
        return new Thread(heartbeatRabbitTask);
    }
}