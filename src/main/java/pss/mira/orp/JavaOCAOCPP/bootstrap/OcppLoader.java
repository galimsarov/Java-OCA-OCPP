package pss.mira.orp.JavaOCAOCPP.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pss.mira.orp.JavaOCAOCPP.models.enums.Actions;
import pss.mira.orp.JavaOCAOCPP.models.queues.Queues;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.List;
import java.util.UUID;

import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.*;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.*;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getDBTablesGetRequest;

@Component
@Slf4j
public class OcppLoader implements CommandLineRunner {
    private final BootNotification bootNotification;
    private final Queues queues;
    private final Sender sender;

    public OcppLoader(BootNotification bootNotification, Queues queues, Sender sender) {
        this.bootNotification = bootNotification;
        this.queues = queues;
        this.sender = sender;
    }

    @Override
    public void run(String... args) {
        sender.sendVersion();

        Thread heartbeatRabbitThread = getHeartbeatRabbitThread();
        heartbeatRabbitThread.start();

        sender.sendRequestToQueue(
                queues.getDateBase(),
                UUID.randomUUID().toString(),
                Get.name(),
                getDBTablesGetRequest(List.of(config_zs.name())),
                Actions.BootNotification.name()
        );

        Thread connectorsInfoThread = getConnectorsInfoThread();
        connectorsInfoThread.start();

        sender.sendRequestToQueue(
                queues.getDateBase(),
                UUID.randomUUID().toString(),
                Get.name(),
                getDBTablesGetRequest(List.of(reservation.name())),
                reservation.name()
        );

        sender.sendRequestToQueue(
                queues.getDateBase(),
                UUID.randomUUID().toString(),
                Get.name(),
                getDBTablesGetRequest(List.of(configuration.name())),
                GetConfiguration.name()
        );
    }

    private Thread getConnectorsInfoThread() {
        Runnable connectorsInfoTask = () -> {
            while (true) {
                if (bootNotification.getClient() == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error("Аn error while waiting for date base response");
                    }
                } else {
                    break;
                }
            }
            sender.sendRequestToQueue(
                    queues.getModBus(),
                    UUID.randomUUID().toString(),
                    GetConnectorsInfo.name(),
                    new Object(),
                    GetConnectorsInfo.name()
            );
        };
        return new Thread(connectorsInfoTask);
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