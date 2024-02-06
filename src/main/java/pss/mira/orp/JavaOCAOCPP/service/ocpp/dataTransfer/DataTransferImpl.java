package pss.mira.orp.JavaOCAOCPP.service.ocpp.dataTransfer;

import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.core.DataTransferConfirmation;
import eu.chargetime.ocpp.model.core.DataTransferRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.client.Client;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handlers.core.CoreHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handlers.remoteTrigger.RemoteTriggerHandler;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DataTransferImpl implements DataTransfer {
    private final Client client;
    private final CoreHandler coreHandler;
    private final RemoteTriggerHandler remoteTriggerHandler;
    private final Sender sender;

    public DataTransferImpl(
            Client client,
            CoreHandler coreHandler,
            RemoteTriggerHandler remoteTriggerHandler,
            Sender sender
    ) {
        this.client = client;
        this.coreHandler = coreHandler;
        this.remoteTriggerHandler = remoteTriggerHandler;
        this.sender = sender;
    }

    /**
     * ["myQueue1","71f599b2-b3f0-4680-b447-ae6d6dc0cc0c","DataTransfer",{"vendorId":"vendorId"}]
     */
    @Override
    public void sendDataTransfer(List<Object> parsedMessage) {
        String consumer = parsedMessage.get(0).toString();
        String requestUuid = parsedMessage.get(1).toString();
        Map<String, Object> dataTransferMap;
        try {
            dataTransferMap = (Map<String, Object>) parsedMessage.get(3);
            // required
            String vendorId = dataTransferMap.get("vendorId").toString();
            // optional
            String messageId = null, data = null;
            try {
                messageId = dataTransferMap.get("messageId").toString();
            } catch (NullPointerException ignored) {

            }
            try {
                data = dataTransferMap.get("data").toString();
            } catch (NullPointerException ignored) {

            }

            ClientCoreProfile core = coreHandler.getCore();

            if (client.getClient() == null) {
                log.warn("There is no connection to the central system. " +
                        "The data transfer message will be sent after the connection is restored");
                // TODO предусмотреть кэш для отправки сообщений после появления связи
            } else {
                // Use the feature profile to help create event
                DataTransferRequest request = core.createDataTransferRequest(vendorId);
                if (messageId != null) {
                    request.setMessageId(messageId);
                }
                if (data != null) {
                    request.setData(data);
                }

                // Client returns a promise which will be filled once it receives a confirmation.
                try {
                    remoteTriggerHandler.waitForRemoteTriggerTaskComplete();
                    client.getClient().send(request).whenComplete((confirmation, ex) -> {
                        log.info("Received from the central system: " + confirmation.toString());
                        handleResponse(consumer, requestUuid, confirmation);
                    });
                } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                    log.warn("An error occurred while sending or processing authorize request");
                }
            }
        } catch (Exception ignored) {
            log.error("An error occurred while receiving vendorId from the message");
        }
    }

    private void handleResponse(String consumer, String requestUuid, Confirmation confirmation) {
        DataTransferConfirmation dataTransferConfirmation = (DataTransferConfirmation) confirmation;
        Map<String, Object> result = new HashMap<>();
        result.put("status", dataTransferConfirmation.getStatus());
        result.put("data", dataTransferConfirmation.getData());
        sender.sendRequestToQueue(consumer, requestUuid, "", result, "");
    }
}