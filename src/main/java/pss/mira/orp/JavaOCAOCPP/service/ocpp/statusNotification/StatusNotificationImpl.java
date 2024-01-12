package pss.mira.orp.JavaOCAOCPP.service.ocpp.statusNotification;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.ChargePointErrorCode;
import eu.chargetime.ocpp.model.core.ChargePointStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;

@Service
@Slf4j
public class StatusNotificationImpl implements StatusNotification {
    private final BootNotification bootNotification;

    public StatusNotificationImpl(BootNotification bootNotification) {
        this.bootNotification = bootNotification;
    }


    @Override
    public void sendStatusNotification(int connectorId, ChargePointErrorCode errorCode, ChargePointStatus status) {
        ClientCoreProfile core = bootNotification.getCore();
        JSONClient client = bootNotification.getClient();

        // Use the feature profile to help create event
        Request request = core.createStatusNotificationRequest(connectorId, errorCode, status);

        // Client returns a promise which will be filled once it receives a confirmation.
        try {
            client.send(request).whenComplete((s, ex) -> log.info(s.toString()));
        } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
            log.warn("An error occurred while sending or processing status notification request");
        }
    }
}