package pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification;

import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.core.BootNotificationConfirmation;
import eu.chargetime.ocpp.model.core.BootNotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.info.ocpp.BootNotificationInfo;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.client.Client;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handlers.core.CoreHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handlers.remoteTrigger.RemoteTriggerHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.heartbeat.Heartbeat;
import pss.mira.orp.JavaOCAOCPP.service.pc.TimeSetter;

import static eu.chargetime.ocpp.model.core.RegistrationStatus.Accepted;

@Service
@Slf4j
public class BootNotificationImpl implements BootNotification {
    private final Client client;
    private final CoreHandler coreHandler;
    private final RemoteTriggerHandler remoteTriggerHandler;
    private final Heartbeat heartbeat;
    private final TimeSetter timeSetter;
    private Thread heartbeatThread = null;
    @Value("${vendor.name}")
    private String vendorName;

    public BootNotificationImpl(
            Client client,
            CoreHandler coreHandler,
            RemoteTriggerHandler remoteTriggerHandler,
            Heartbeat heartbeat,
            TimeSetter timeSetter
    ) {
        this.client = client;
        this.coreHandler = coreHandler;
        this.remoteTriggerHandler = remoteTriggerHandler;
        this.heartbeat = heartbeat;
        this.timeSetter = timeSetter;
    }

    @Override
    public void sendBootNotification(String source) {
        Runnable clientTask = () -> {
            while (true) {
                if (client.getClient() == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error("Аn error while waiting for connection");
                    }
                } else {
                    break;
                }
            }
            // Use the feature profile to help create event
            BootNotificationRequest request = getBootNotificationRequest(
                    coreHandler.getCore(), client.getBootNotificationInfo()
            );
            log.info("Sent to central system: " + request);
            // Client returns a promise which will be filled once it receives a confirmation.
            try {
                client.getClient().send(request).whenComplete((confirmation, ex) -> {
                    log.info("Received from the central system: " + confirmation.toString());
                    handleResponse(confirmation, source);
                });
            } catch (OccurenceConstraintException | UnsupportedFeatureException e) {
                log.error("Аn error occurred while trying to send a boot notification");
            }
        };
        Thread clientThread = new Thread(clientTask);
        clientThread.start();
    }

    private BootNotificationRequest getBootNotificationRequest(
            ClientCoreProfile core, BootNotificationInfo bootNotificationInfo
    ) {
        BootNotificationRequest request =
                core.createBootNotificationRequest(vendorName, bootNotificationInfo.getModel());
        request.setChargePointSerialNumber(bootNotificationInfo.getChargePointID());
//            request.setFirmwareVersion(bootNotificationInfo.getVersion());
        request.setIccid(bootNotificationInfo.getChargePointID());
        request.setImsi(bootNotificationInfo.getChargePointID());
        return request;
    }

    @Override
    public void handleResponse(Confirmation confirmation, String source) {
        BootNotificationConfirmation bootNotificationConfirmation = (BootNotificationConfirmation) confirmation;
        // TODO Сделать другие варианты, кроме Accepted
        if (bootNotificationConfirmation.getStatus().equals(Accepted)) {
            if (source.equals("remoteTrigger")) {
                heartbeatThread.interrupt();
            }
            timeSetter.setTime(bootNotificationConfirmation.getCurrentTime());

            Thread thread = heartbeat.getHeartbeatThread(bootNotificationConfirmation, source);
            thread.start();
            heartbeatThread = thread;

            if (source.equals("remoteTrigger")) {
                remoteTriggerHandler.setRemoteTriggerTaskFinished();
            }
        }
    }
}