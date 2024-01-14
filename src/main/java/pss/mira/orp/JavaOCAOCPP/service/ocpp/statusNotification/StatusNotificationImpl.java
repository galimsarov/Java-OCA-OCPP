package pss.mira.orp.JavaOCAOCPP.service.ocpp.statusNotification;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.requests.ocpp.StatusNotificationRequest;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;

@Service
@Slf4j
public class StatusNotificationImpl implements StatusNotification {
    private final BootNotification bootNotification;

    public StatusNotificationImpl(BootNotification bootNotification) {
        this.bootNotification = bootNotification;
    }

    @Override
    public void sendStatusNotification(StatusNotificationRequest statusNotificationRequest) {
        ClientCoreProfile core;
        JSONClient client;

        while (true) {
            core = bootNotification.getCore();
            client = bootNotification.getClient();
            if (core != null && client != null) {
                break;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("An error occurred while waiting the connect for sending the status notification");
                }
            }
        }

        // Use the feature profile to help create event
        Request request = core.createStatusNotificationRequest(
                statusNotificationRequest.getId(),
                statusNotificationRequest.getErrorCode(),
                statusNotificationRequest.getStatus()
        );

        // Client returns a promise which will be filled once it receives a confirmation.
        try {
            client.send(request).whenComplete((confirmation, ex) -> {
                log.info(confirmation.toString());
                handleRespond(confirmation);
            });
        } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
            log.warn("An error occurred while sending or processing status notification request");
        }
    }

    private void handleRespond(Confirmation confirmation) {

    }
}