package pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.remoteTrigger;

import eu.chargetime.ocpp.feature.profile.ClientRemoteTriggerEventHandler;
import eu.chargetime.ocpp.feature.profile.ClientRemoteTriggerProfile;
import eu.chargetime.ocpp.model.core.MeterValuesRequest;
import eu.chargetime.ocpp.model.remotetrigger.TriggerMessageConfirmation;
import eu.chargetime.ocpp.model.remotetrigger.TriggerMessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.queues.Queues;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.chargetime.ocpp.model.remotetrigger.TriggerMessageStatus.*;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.*;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.config_zs;
import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getDBTablesGetRequest;

@Service
@Slf4j
public class RemoteTriggerHandlerImpl implements RemoteTriggerHandler {
    private final Queues queues;
    private final Sender sender;
    private boolean remoteTriggerTaskExecuting = false;
    private Map<Integer, MeterValuesRequest> cachedMeterValuesRequestsMap = new HashMap<>();

    public RemoteTriggerHandlerImpl(Queues queues, Sender sender) {
        this.queues = queues;
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
                        return getBootNotificationTriggerMessageConfirmation();
                    }
                    case DiagnosticsStatusNotification, FirmwareStatusNotification -> {
                        return new TriggerMessageConfirmation(NotImplemented);
                    }
                    case Heartbeat -> {
                        return getHeartbeatTriggerMessageConfirmation();
                    }
                    case MeterValues -> {
                        int connectorId;
                        if (request.getConnectorId() == null) {
                            connectorId = 0;
                        } else {
                            connectorId = request.getConnectorId();
                        }
                        if (meterValuesCanBeSent(connectorId)) {
                            return getMeterValuesTriggerMessageConfirmation(connectorId);
                        } else {
                            TriggerMessageConfirmation result = new TriggerMessageConfirmation(Rejected);
                            log.info("Sent to central system: " + result);
                            return result;
                        }
                    }
                    case StatusNotification -> {
                        int connectorId;
                        if (request.getConnectorId() == null) {
                            connectorId = 0;
                        } else {
                            connectorId = request.getConnectorId();
                        }
                        return getStatusNotificationTriggerMessageConfirmation(connectorId);
                    }
                }
                return null;
            }

            // TODO вывести общий метод getTriggerMessageConfirmation()
            private TriggerMessageConfirmation getStatusNotificationTriggerMessageConfirmation(int connectorId) {
                Thread sendAndHandleStatusNotificationThread = getSendAndHandleStatusNotificationThread(connectorId);
                sendAndHandleStatusNotificationThread.start();
                TriggerMessageConfirmation result = new TriggerMessageConfirmation(Accepted);
                log.info("Sent to central system: " + result);
                return result;
            }

            private Thread getSendAndHandleStatusNotificationThread(int connectorId) {
                Runnable sendAndHandleStatusNotificationTask = () -> {
                    remoteTriggerTaskExecuting = true;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error("Аn error while sending trigger message confirmation");
                    }
                    sender.sendRequestToQueue(
                            queues.getOCPP(),
                            UUID.randomUUID().toString(),
                            SendStatusNotificationToCentralSystem.name(),
                            Map.of("connectorId", connectorId),
                            SendStatusNotificationToCentralSystem.name()
                    );
                };
                return new Thread(sendAndHandleStatusNotificationTask);
            }

            private boolean meterValuesCanBeSent(int connectorId) {
                if (connectorId == 0) {
                    return !cachedMeterValuesRequestsMap.isEmpty();
                } else {
                    return cachedMeterValuesRequestsMap.get(connectorId) != null;
                }
            }

            private TriggerMessageConfirmation getMeterValuesTriggerMessageConfirmation(int connectorId) {
                Thread sendAndHandleMeterValuesThread = getSendAndHandleMeterValuesThread(connectorId);
                sendAndHandleMeterValuesThread.start();
                TriggerMessageConfirmation result = new TriggerMessageConfirmation(Accepted);
                log.info("Sent to central system: " + result);
                return result;
            }

            private Thread getSendAndHandleMeterValuesThread(int connectorId) {
                Runnable sendAndHandleMeterValuesTask = () -> {
                    remoteTriggerTaskExecuting = true;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error("Аn error while sending trigger message confirmation");
                    }
                    sender.sendRequestToQueue(
                            queues.getOCPP(),
                            UUID.randomUUID().toString(),
                            SendMeterValuesToCentralSystem.name(),
                            Map.of("connectorId", connectorId),
                            SendMeterValuesToCentralSystem.name()
                    );
                };
                return new Thread(sendAndHandleMeterValuesTask);
            }

            private TriggerMessageConfirmation getBootNotificationTriggerMessageConfirmation() {
                Thread sendAndHandleBootNotificationThread = getSendAndHandleBootNotificationThread();
                sendAndHandleBootNotificationThread.start();
                TriggerMessageConfirmation result = new TriggerMessageConfirmation(Accepted);
                log.info("Sent to central system: " + result);
                return result;
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
                            queues.getDateBase(),
                            UUID.randomUUID().toString(),
                            Get.name(),
                            getDBTablesGetRequest(List.of(config_zs.name())),
                            RemoteTriggerBootNotification.name()
                    );
                };
                return new Thread(sendAndHandleBootNotificationTask);
            }

            private TriggerMessageConfirmation getHeartbeatTriggerMessageConfirmation() {
                Thread sendAndHandleHeartbeatThread = getSendAndHandleHeartbeatThread();
                sendAndHandleHeartbeatThread.start();
                TriggerMessageConfirmation result = new TriggerMessageConfirmation(Accepted);
                log.info("Sent to central system: " + result);
                return result;
            }

            private Thread getSendAndHandleHeartbeatThread() {
                Runnable sendAndHandleHeartbeatTask = () -> {
                    remoteTriggerTaskExecuting = true;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error("Аn error while sending trigger message confirmation");
                    }
                    sender.sendRequestToQueue(
                            queues.getOCPP(),
                            UUID.randomUUID().toString(),
                            SendHeartbeatToCentralSystem.name(),
                            new Object(),
                            SendHeartbeatToCentralSystem.name()
                    );
                };
                return new Thread(sendAndHandleHeartbeatTask);
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

    @Override
    public void setCachedMeterValuesRequestsMap(Map<Integer, MeterValuesRequest> cachedMeterValuesRequestsMap) {
        this.cachedMeterValuesRequestsMap = cachedMeterValuesRequestsMap;
    }
}