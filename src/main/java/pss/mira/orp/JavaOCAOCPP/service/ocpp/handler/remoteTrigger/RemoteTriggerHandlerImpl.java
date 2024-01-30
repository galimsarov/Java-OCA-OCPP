package pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.remoteTrigger;

import eu.chargetime.ocpp.feature.profile.ClientRemoteTriggerEventHandler;
import eu.chargetime.ocpp.feature.profile.ClientRemoteTriggerProfile;
import eu.chargetime.ocpp.model.remotetrigger.TriggerMessageConfirmation;
import eu.chargetime.ocpp.model.remotetrigger.TriggerMessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.List;
import java.util.UUID;

import static eu.chargetime.ocpp.model.remotetrigger.TriggerMessageStatus.Accepted;
import static eu.chargetime.ocpp.model.remotetrigger.TriggerMessageStatus.NotImplemented;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.Get;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.RemoteTriggerBootNotification;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.config_zs;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Queues.bd;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getDBTablesGetRequest;

@Service
@Slf4j
public class RemoteTriggerHandlerImpl implements RemoteTriggerHandler {
    private final Sender sender;
    private boolean remoteTriggerTaskExecuting = false;

    public RemoteTriggerHandlerImpl(Sender sender) {
        this.sender = sender;
    }

    @Override
    public ClientRemoteTriggerProfile getRemoteTrigger() {
        return new ClientRemoteTriggerProfile(new ClientRemoteTriggerEventHandler() {
            @Override
            public TriggerMessageConfirmation handleTriggerMessageRequest(TriggerMessageRequest request) {
                log.info("Received from the central system: " + request.toString());
                switch (request.getRequestedMessage()) {
                    case BootNotification -> {
                        Thread sendAndHandleBootNotificationThread = getSendAndHandleBootNotificationThread();
                        sendAndHandleBootNotificationThread.start();
                        TriggerMessageConfirmation result = new TriggerMessageConfirmation(Accepted);
                        log.info("Sent to central system: " + result);
                        return result;
                    }
                    case DiagnosticsStatusNotification, FirmwareStatusNotification -> {
                        return new TriggerMessageConfirmation(NotImplemented);
                    }
                    case Heartbeat -> {

                    }
                    case MeterValues, StatusNotification -> {
                    }
                }
                return null;
            }

            private Thread getSendAndHandleBootNotificationThread() {
                Runnable sendAndHandleBootNotificationTask = () -> {
                    remoteTriggerTaskExecuting = true;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error("Аn error while sending trigger message confirmation");
                    }
                    sender.sendRequestToQueue(
                            bd.name(),
                            UUID.randomUUID().toString(),
                            Get.name(),
                            getDBTablesGetRequest(List.of(config_zs.name())),
                            RemoteTriggerBootNotification.name()
                    );
                };
                return new Thread(sendAndHandleBootNotificationTask);
            }
        });
    }

    @Override
    public void setRemoteTriggerTaskFinished() {
        remoteTriggerTaskExecuting = false;
    }

    @Override
    public void waitForRemoteTriggerTaskComplete() {
        while (true) {
            if (remoteTriggerTaskExecuting) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Аn error while executing remote trigger task");
                }
            } else {
                break;
            }
        }
    }
}