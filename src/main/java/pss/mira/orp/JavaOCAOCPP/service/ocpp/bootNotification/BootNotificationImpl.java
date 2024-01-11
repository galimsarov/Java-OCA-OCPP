package pss.mira.orp.JavaOCAOCPP.service.ocpp.bootNotification;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreEventHandler;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BootNotificationImpl implements BootNotification {
    private JSONClient client;

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
     * ["bd","ec95019b-dd6d-4fbb-8df9-c340d48343b7",[{"id":"","key":"adresCS","value":"ws://10.10.0.255:8080/steve/websocket/CentralSystemService","type":"","name":"","secureLevel":""},{"id":"","key":"ChargePointID","value":"22F555555","type":"","name":"","secureLevel":""},{"id":"","key":"ChargePointModel","value":"CPmodel","type":"","name":"","secureLevel":""},{"id":"","key":"ChargePointVendor","value":"CPvendor","type":"","name":"","secureLevel":""}]]
     */
    @Override
    public void sendBootNotification(List<Map<String, Object>> configZSList) {
        String addressCP = null, chargePointID = null, vendor = null, model = null;

        for (Map<String, Object> map : configZSList) {
            String key = map.get("key").toString();
            switch (key) {
                case ("adresCS"):
                    addressCP = map.get("value").toString();
                    break;
                case ("ChargePointID"):
                    chargePointID = map.get("value").toString();
                    break;
                case ("ChargePointVendor"):
                    vendor = map.get("value").toString();
                    break;
                case ("ChargePointModel"):
                    model = map.get("value").toString();
            }
        }

        if (addressCP != null && chargePointID != null && vendor != null && model != null) {
            log.info("OCPP is ready to connect with the central system and send the boot notification");

            ClientCoreProfile core = getCore();
            JSONClient jsonClient = new JSONClient(core, chargePointID);
            jsonClient.connect(addressCP, null);
            client = jsonClient;

            // Use the feature profile to help create event
            Request request = core.createBootNotificationRequest(vendor, model);

            // Client returns a promise which will be filled once it receives a confirmation.
            try {
                client.send(request).whenComplete((s, ex) -> System.out.println(s));
            } catch (OccurenceConstraintException | UnsupportedFeatureException e) {
                log.error("Аn error occurred while trying to send a boot notification");
            }
        } else {
            log.error("OCPP did not receive one of the parameters (adresCS, ChargePointID, ChargePointVendor, " +
                    "ChargePointModel) and cannot establish a connection the central system");
        }
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

                System.out.println(request);
                // ... handle event

                return new ChangeAvailabilityConfirmation(AvailabilityStatus.Accepted);
            }

            @Override
            public GetConfigurationConfirmation handleGetConfigurationRequest(GetConfigurationRequest request) {

                System.out.println(request);
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public ChangeConfigurationConfirmation handleChangeConfigurationRequest(ChangeConfigurationRequest request) {

                System.out.println(request);
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public ClearCacheConfirmation handleClearCacheRequest(ClearCacheRequest request) {

                System.out.println(request);
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public DataTransferConfirmation handleDataTransferRequest(DataTransferRequest request) {

                System.out.println(request);
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public RemoteStartTransactionConfirmation handleRemoteStartTransactionRequest(RemoteStartTransactionRequest request) {

                System.out.println(request);
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public RemoteStopTransactionConfirmation handleRemoteStopTransactionRequest(RemoteStopTransactionRequest request) {

                System.out.println(request);
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public ResetConfirmation handleResetRequest(ResetRequest request) {

                System.out.println(request);
                // ... handle event

                return null; // returning null means unsupported feature
            }

            @Override
            public UnlockConnectorConfirmation handleUnlockConnectorRequest(UnlockConnectorRequest request) {

                System.out.println(request);
                // ... handle event

                return null; // returning null means unsupported feature
            }
        });
    }
}
