package pss.mira.orp.JavaOCAOCPP.service.ocpp.stopTransaction;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification.BootNotification;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.Handler;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static pss.mira.orp.JavaOCAOCPP.models.enums.IdType.TRANSACTION;

@Service
@Slf4j
public class StopTransactionImpl implements StopTransaction {
    private final BootNotification bootNotification;
    private final ConnectorsInfoCache connectorsInfoCache;
    private final Handler handler;
    private final Sender sender;

    public StopTransactionImpl(
            BootNotification bootNotification, ConnectorsInfoCache connectorsInfoCache, Handler handler, Sender sender
    ) {
        this.bootNotification = bootNotification;
        this.connectorsInfoCache = connectorsInfoCache;
        this.handler = handler;
        this.sender = sender;
    }

    /**
     * ["myQueue1","71f599b2-b3f0-4680-b447-ae6d6dc0cc0c","StopTransaction",{"transactionId":2682}]
     */
    @Override
    public void sendStopTransaction(List<Object> parsedMessage) {
        String consumer = parsedMessage.get(0).toString();
        String requestUuid = parsedMessage.get(1).toString();
        try {
            Map<String, Object> stopTransactionMap = (Map<String, Object>) parsedMessage.get(3);
            int transactionId = Integer.parseInt(stopTransactionMap.get("transactionId").toString());

            ClientCoreProfile core = handler.getCore();
            JSONClient client = bootNotification.getClient();

            if (client == null) {
                log.warn("There is no connection to the central system. " +
                        "The stop transaction message will be sent after the connection is restored");
                // TODO предусмотреть кэш для отправки сообщений после появления связи
            } else {
                // Use the feature profile to help create event
                Request request = core.createStopTransactionRequest(
                        connectorsInfoCache.getMeterValue(transactionId, TRANSACTION), ZonedDateTime.now(), transactionId
                );

                // Client returns a promise which will be filled once it receives a confirmation.
                try {
                    client.send(request).whenComplete((confirmation, ex) -> {
                        log.info("Received from the central system: " + confirmation);
                        handleResponse(consumer, requestUuid, confirmation);
                    });
                } catch (OccurenceConstraintException | UnsupportedFeatureException ignored) {
                    log.warn("An error occurred while sending or processing stop transaction request");
                }
            }
        } catch (Exception ignored) {
            log.error("An error occurred while receiving stop transaction data from the message");
        }
    }

    /**
     * Steve возвращал null, поэтому idTagInfo собирать не из чего. При необходимости можно предусмотреть
     */
    private void handleResponse(String consumer, String requestUuid, Confirmation confirmation) {
        sender.sendRequestToQueue(consumer, requestUuid, "", confirmation, "");
    }
}