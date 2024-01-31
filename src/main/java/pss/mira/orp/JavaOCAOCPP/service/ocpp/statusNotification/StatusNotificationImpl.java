package pss.mira.orp.JavaOCAOCPP.service.ocpp.statusNotification;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.info.ocpp.StatusNotificationInfo;
import pss.mira.orp.JavaOCAOCPP.models.queues.Queues;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.core.CoreHandler;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.UUID;

import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.SaveToCache;

@Service
@Slf4j
public class StatusNotificationImpl implements StatusNotification {
    private final BootNotification bootNotification;
    private final CoreHandler coreHandler;
    private final Queues queues;
    private final Sender sender;

    public StatusNotificationImpl(
            BootNotification bootNotification, CoreHandler coreHandler, Queues queues, Sender sender
    ) {
        this.bootNotification = bootNotification;
        this.coreHandler = coreHandler;
        this.queues = queues;
        this.sender = sender;
    }

    @Override
    public void sendStatusNotification(StatusNotificationInfo statusNotificationInfo) {
        ClientCoreProfile core = coreHandler.getCore();
        JSONClient client = bootNotification.getClient();
        // Use the feature profile to help create event
        Request request = core.createStatusNotificationRequest(
                statusNotificationInfo.getId(),
                statusNotificationInfo.getErrorCode(),
                statusNotificationInfo.getStatus()
        );
        if (client == null) {
            sender.sendRequestToQueue(
                    queues.getOCPPCache(),
                    UUID.randomUUID().toString(),
                    SaveToCache.name(),
                    request,
                    "statusNotification"
            );
        } else {
            log.info("Sent to central system: " + request.toString());
            // Client returns a promise which will be filled once it receives a confirmation.
            try {
                client.send(request).whenComplete((confirmation, ex) -> {
                    log.info("Received from the central system: " + confirmation.toString());
                });
            } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                log.warn("An error occurred while sending or processing status notification request");
            }
        }
    }
}