package pss.mira.orp.JavaOCAOCPP.service.ocpp.handler;

import eu.chargetime.ocpp.feature.profile.ClientCoreEventHandler;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.core.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.requests.rabbit.DBTablesChangeRequest;
import pss.mira.orp.JavaOCAOCPP.models.requests.rabbit.DBTablesGetRequest;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.chargetime.ocpp.model.core.ConfigurationStatus.NotSupported;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.*;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.*;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Services.ModBus;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Services.bd;

@Service
@Slf4j
public class HandlerImpl implements Handler {
    private final Sender sender;
    private AvailabilityStatus availabilityStatus = null;
    private List<Map<String, Object>> configurationList = null;
    private ConfigurationStatus changeConfigurationStatus = null;

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
                        availabilityStatus = null;
                        return result;
                    }
                }
            }

            @Override
            public GetConfigurationConfirmation handleGetConfigurationRequest(GetConfigurationRequest request) {
                log.info("Received from the central system: " + request.toString());
                sender.sendRequestToQueue(
                        bd.name(),
                        UUID.randomUUID().toString(),
                        Get.name(),
                        new DBTablesGetRequest(List.of(configuration.name())),
                        getConfiguration.name()
                );
                while (true) {
                    if (configurationList == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Аn error while waiting for a get configuration response");
                        }
                    } else {
                        GetConfigurationConfirmation result =
                                getGetConfigurationConfirmation(request, configurationList);
                        log.info("Send to the central system: " + result);
                        configurationList = null;
                        return result;
                    }
                }
            }

            @Override
            public ChangeConfigurationConfirmation handleChangeConfigurationRequest(
                    ChangeConfigurationRequest request
            ) {
                log.info("Received from the central system: " + request.toString());
                sender.sendRequestToQueue(
                        bd.name(),
                        UUID.randomUUID().toString(),
                        Change.name(),
                        new DBTablesChangeRequest(
                                configuration.name(),
                                "key:" + request.getKey(),
                                List.of(Map.of("key", "value", "value", request.getValue()))),
                        changeConfiguration.name()
                );
                while (true) {
                    if (changeConfigurationStatus == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Аn error while waiting for a change configuration response");
                        }
                    } else {
                        ChangeConfigurationConfirmation result =
                                new ChangeConfigurationConfirmation(changeConfigurationStatus);
                        log.info("Send to the central system: " + result);
                        changeConfigurationStatus = null;
                        return result;
                    }
                }
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

    private GetConfigurationConfirmation getGetConfigurationConfirmation(
            GetConfigurationRequest request, List<Map<String, Object>> configurationList
    ) {
        KeyValueType[] keyValueTypeArray = new KeyValueType[request.getKey().length];
        for (int i = 0; i < keyValueTypeArray.length; i++) {
            String key = request.getKey()[i];
            for (Map<String, Object> map : configurationList) {
                String mapKey = map.get("key").toString();
                if (key.equals(mapKey)) {
                    boolean readonly = Boolean.parseBoolean(map.get("readonly").toString());
                    String value = map.get("value").toString();
                    KeyValueType keyValueType = new KeyValueType(key, readonly);
                    keyValueType.setValue(value);
                    keyValueTypeArray[i] = keyValueType;
                    break;
                }
            }
        }
        GetConfigurationConfirmation result = new GetConfigurationConfirmation();
        result.setConfigurationKey(keyValueTypeArray);
        return result;
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

    @Override
    public void setConfigurationMap(List<Object> parsedMessage) {
        Map<String, Map<String, List<Map<String, Object>>>> map =
                (Map<String, Map<String, List<Map<String, Object>>>>) parsedMessage.get(2);
        configurationList = map.get("tables").get(configuration.name());
    }

    @Override
    public void setChangeConfigurationStatus(List<Object> parsedMessage) {
        Map<String, String> map = (Map<String, String>) parsedMessage.get(2);
        for (ConfigurationStatus configurationStatus : ConfigurationStatus.values()) {
            if (configurationStatus.name().equals(map.get("result"))) {
                changeConfigurationStatus = configurationStatus;
                return;
            }
        }
        changeConfigurationStatus = NotSupported;
    }
}