package pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreEventHandler;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.Utils;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.heartBeat.Heartbeat;

import java.util.List;
import java.util.Map;

import static eu.chargetime.ocpp.model.core.RegistrationStatus.Accepted;

@Service
@Slf4j
public class BootNotificationImpl implements BootNotification {
    private final Heartbeat heartbeat;
    private final Utils utils;

    private JSONClient client;
    @Value("${vendor.name}")
    private String vendorName;

    public BootNotificationImpl(Heartbeat heartbeat, Utils utils) {
        this.heartbeat = heartbeat;
        this.utils = utils;
    }

    /**
     * Подключается к центральной системе:
     * addressCP - адрес ЦС,
     * chargePointID - id зарядной станции.
     * Отправляет запрос bootNotification:
     * vendor - информация о производителе,
     * model - информация о модели станции.
     * Формат ответа от steve:
     * BootNotificationConfirmation{currentTime="2024-01-10T10:09:17.743Z", interval=60, status=Accepted, isValid=true}
     * Тестовый сын Джейсона для steve:
     * ["bd","235e1217-d1c0-4984-8b81-f430015ab983",[{"id":"","key":"adresCS","value":"ws://10.10.0.255:8080/steve/websocket/CentralSystemService","type":"","name":"","secureLevel":""},{"id":"","key":"ChargePointID","value":"22F555555","type":"","name":"","secureLevel":""},{"id":"","key":"ChargePointModel","value":"CPmodel","type":"","name":"","secureLevel":""}]]
     */
    @Override
    public void sendBootNotification(List<Object> parsedMessage) {
        Map<String, Map<String, String>> tablesMap = (Map<String, Map<String, String>>) parsedMessage.get(2);
        String fixedString = (tablesMap.get("tables")).get("config_zs")
                .replace("[*[", "[[")
                .replace("\\\"", "\"");

        try {
            List<Map<String, Object>> configZSList = (new ObjectMapper()).readValue(fixedString, List.class);
            if (configZSList != null) {
                String addressCP = null, chargePointID = null, model = null;

                for (Map<String, Object> map : configZSList) {
                    String key = map.get("key").toString();
                    switch (key) {
                        case ("adresCS"):
                            addressCP = map.get("value").toString();
                            break;
                        case ("ChargePointID"):
                            chargePointID = map.get("value").toString();
                            break;
                        case ("ChargePointModel"):
                            model = map.get("value").toString();
                    }
                }

                if (addressCP != null && chargePointID != null && model != null) {
                    log.info("OCPP is ready to connect with the central system and send the boot notification");

                    if (addressCP.endsWith("/")) {
                        addressCP = addressCP.substring(0, addressCP.length() - 1);
                    }

                    ClientCoreProfile core = getCore();
                    JSONClient jsonClient = new JSONClient(core, chargePointID);
                    jsonClient.connect(addressCP, null);
                    client = jsonClient;

                    // Use the feature profile to help create event
                    Request request = core.createBootNotificationRequest(vendorName, model);

                    // Client returns a promise which will be filled once it receives a confirmation.
                    try {
                        client.send(request).whenComplete((confirmation, ex) -> {
                            log.info(confirmation.toString());
                            handleResponse(confirmation);
                        });
                    } catch (OccurenceConstraintException | UnsupportedFeatureException e) {
                        log.error("Аn error occurred while trying to send a boot notification");
                    }
                } else {
                    log.error("OCPP did not receive one of the parameters (adresCS, ChargePointID, ChargePointVendor, " +
                            "ChargePointModel) and cannot establish a connection the central system");
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error when parsing config_zs table");
        }
    }

    private void handleResponse(Confirmation confirmation) {
        BootNotificationConfirmation bootNotificationConfirmation = (BootNotificationConfirmation) confirmation;
        if (bootNotificationConfirmation.getStatus().equals(Accepted)) {
            Thread endOfChargingThread = utils.getEndOfChargingThread(bootNotificationConfirmation.getCurrentTime());
            endOfChargingThread.start();

            Thread heartBeatThread = getHeartbeatThread(bootNotificationConfirmation);
            heartBeatThread.start();
        }
    }

    private Thread getHeartbeatThread(BootNotificationConfirmation bootNotificationConfirmation) {
        Runnable runHeartbeat = () -> {
            while (true) {
                try {
                    Thread.sleep(bootNotificationConfirmation.getInterval() * 1000);
                } catch (InterruptedException e) {
                    log.error("Аn error while waiting for a heartbeat to be sent");
                }
                heartbeat.sendHeartbeat(getCore(), getClient());
            }
        };
        return new Thread(runHeartbeat);
    }

    @Override
    public JSONClient getClient() {
        return client;
    }

    @Override
    public ClientCoreProfile getCore() {
        return new ClientCoreProfile(new ClientCoreEventHandler() {
            @Override
            public ChangeAvailabilityConfirmation handleChangeAvailabilityRequest(ChangeAvailabilityRequest request) {

                log.info(request.toString());
                // ... handle event

                return new ChangeAvailabilityConfirmation(AvailabilityStatus.Accepted);
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
}
