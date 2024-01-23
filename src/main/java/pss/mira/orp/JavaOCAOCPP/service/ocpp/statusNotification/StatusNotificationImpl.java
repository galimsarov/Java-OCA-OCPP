package pss.mira.orp.JavaOCAOCPP.service.ocpp.statusNotification;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.requests.ocpp.StatusNotificationRequest;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.Handler;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.UUID;

import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.SaveToCache;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Queues.ocppCache;

@Service
@Slf4j
public class StatusNotificationImpl implements StatusNotification {
    private final BootNotification bootNotification;
    private final Handler handler;
    private final Sender sender;

    public StatusNotificationImpl(BootNotification bootNotification, Handler handler, Sender sender) {
        this.bootNotification = bootNotification;
        this.handler = handler;
        this.sender = sender;
    }

    @Override
    public void sendStatusNotification(StatusNotificationRequest statusNotificationRequest) {
        ClientCoreProfile core = handler.getCore();
        JSONClient client = bootNotification.getClient();
        // Use the feature profile to help create event
        Request request = core.createStatusNotificationRequest(
                statusNotificationRequest.getId(),
                statusNotificationRequest.getErrorCode(),
                statusNotificationRequest.getStatus()
        );
        if (client == null) {
            sender.sendRequestToQueue(
                    ocppCache.name(),
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