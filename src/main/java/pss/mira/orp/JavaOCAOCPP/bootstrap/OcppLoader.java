package pss.mira.orp.JavaOCAOCPP.bootstrap;

import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.feature.profile.ClientCoreEventHandler;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pss.mira.orp.JavaOCAOCPP.service.cache.Cache;

import static pss.mira.orp.JavaOCAOCPP.enums.Keys.*;

@Component
@Slf4j
public class OcppLoader implements CommandLineRunner {
    private final Cache cache;

    public OcppLoader(Cache cache) {
        this.cache = cache;
    }

    @Getter
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
     */
    @Override
    public void run(String... args) throws Exception {
        String addressCP, chargePointID, vendor, model;

        while (true) {
            addressCP = cache.getFromCacheByKey(ADDRESS_CP.getKey());
            chargePointID = cache.getFromCacheByKey(CHARGE_POINT_ID.getKey());
            vendor = cache.getFromCacheByKey(VENDOR.getKey());
            model = cache.getFromCacheByKey(CHARGE_POINT_MODEL.getKey());
            if (addressCP != null && chargePointID != null && vendor != null && model != null) {
                log.info("OCPP is ready to connect with the central system and send the boot notification");
                break;
            }
        }

        ClientCoreProfile core = getCore();
        JSONClient jsonClient = new JSONClient(core, chargePointID);
        jsonClient.connect(addressCP, null);
        client = jsonClient;

        // Use the feature profile to help create event
        Request request = core.createBootNotificationRequest(vendor, model);

        // Client returns a promise which will be filled once it receives a confirmation.
        client.send(request).whenComplete((s, ex) -> System.out.println(s));
    }

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