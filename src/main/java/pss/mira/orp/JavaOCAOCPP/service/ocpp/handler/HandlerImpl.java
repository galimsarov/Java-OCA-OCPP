package pss.mira.orp.JavaOCAOCPP.service.ocpp.handler;

import eu.chargetime.ocpp.feature.profile.ClientCoreEventHandler;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.core.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.ChangeAvailability;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Services.ModBus;

@Service
@Slf4j
public class HandlerImpl implements Handler {
    private final Sender sender;
    private AvailabilityStatus availabilityStatus = null;

    public HandlerImpl(Sender sender) {
        this.sender = sender;
    }

    @Override
    public ClientCoreProfile getCore() {
        return new ClientCoreProfile(new ClientCoreEventHandler() {
            /**
             * Обработка запроса от ЦС на смену доступности. Отправляем в ModBus, слушаем ocpp, ответ кидаем в поле
             * availabilityStatus.
             * Тестовый сын Джейсона для steve:
             * ["ModBus","8a800b6f-b832-467e-8fda-818e94e6392d",{"ChangeAvailability":"Accepted"}]
             */
            @Override
            public ChangeAvailabilityConfirmation handleChangeAvailabilityRequest(ChangeAvailabilityRequest request) {
                log.info("Received from the central system: " + request.toString());
                sender.sendRequestToQueue(
                        ModBus.name(),
                        UUID.randomUUID().toString(),
                        ChangeAvailability.name(),
                        request,
                        ChangeAvailability.name()
                );
                while (true) {
                    if (availabilityStatus == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Аn error while waiting for a change availability response");
                        }
                    } else {
                        ChangeAvailabilityConfirmation result = new ChangeAvailabilityConfirmation(availabilityStatus);
                        log.info("Send to the central system: " + result);
                        return result;
                    }
                }
            }

            @Override
            public GetConfigurationConfirmation handleGetConfigurationRequest(GetConfigurationRequest request) {

                log.info(request.toString());
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public ChangeConfigurationConfirmation handleChangeConfigurationRequest(ChangeConfigurationRequest request) {

                log.info(request.toString());
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public ClearCacheConfirmation handleClearCacheRequest(ClearCacheRequest request) {

                log.info(request.toString());
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public DataTransferConfirmation handleDataTransferRequest(DataTransferRequest request) {

                log.info(request.toString());
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public RemoteStartTransactionConfirmation handleRemoteStartTransactionRequest(RemoteStartTransactionRequest request) {

                log.info(request.toString());
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public RemoteStopTransactionConfirmation handleRemoteStopTransactionRequest(RemoteStopTransactionRequest request) {

                log.info(request.toString());
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public ResetConfirmation handleResetRequest(ResetRequest request) {

                log.info(request.toString());
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public UnlockConnectorConfirmation handleUnlockConnectorRequest(UnlockConnectorRequest request) {

                log.info(request.toString());
                // ... handle event

                return null; // returning null means unsupported feature
            }
        });
    }

    @Override
    public void setAvailabilityStatus(List<Object> parsedMessage) {
        Map<String, String> map = (Map<String, String>) parsedMessage.get(2);
        for (AvailabilityStatus statusFromEnum : AvailabilityStatus.values()) {
            if (statusFromEnum.name().equals(map.get("ChangeAvailability"))) {
                availabilityStatus = statusFromEnum;
                break;
            }
        }
    }
}