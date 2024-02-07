package pss.mira.orp.JavaOCAOCPP.service.ocpp.client;

import eu.chargetime.ocpp.ClientEvents;
import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.info.ocpp.BootNotificationInfo;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handlers.core.CoreHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handlers.remoteTrigger.RemoteTriggerHandler;
import pss.mira.orp.JavaOCAOCPP.service.ocpp.handlers.reservation.ReservationHandler;

import java.util.List;

@Service
@Slf4j
public class ClientImpl implements Client {
    private final CoreHandler coreHandler;
    private final RemoteTriggerHandler remoteTriggerHandler;
    private final ReservationHandler reservationHandler;
    private BootNotificationInfo bootNotificationInfo;
    private JSONClient jsonClient;
    private boolean isConnected;

    public ClientImpl(
            CoreHandler coreHandler, RemoteTriggerHandler remoteTriggerHandler, ReservationHandler reservationHandler
    ) {
        this.coreHandler = coreHandler;
        this.remoteTriggerHandler = remoteTriggerHandler;
        this.reservationHandler = reservationHandler;
    }

    @Override
    public void createClient(List<Object> parsedMessage) {
        bootNotificationInfo = new BootNotificationInfo(parsedMessage);

        if (bootNotificationInfo.getAddressCP() != null &&
                bootNotificationInfo.getChargePointID() != null &&
                bootNotificationInfo.getModel() != null) {
            log.info("OCPP is ready to connect with the central system and send the boot notification");

            if (bootNotificationInfo.getAddressCP().endsWith("/")) {
                bootNotificationInfo.setAddressCP(
                        bootNotificationInfo.getAddressCP().substring(
                                0, bootNotificationInfo.getAddressCP().length() - 1
                        )
                );
            }

            ClientCoreProfile core = coreHandler.getCore();
            jsonClient = new JSONClient(core, bootNotificationInfo.getChargePointID());
            jsonClient.addFeatureProfile(reservationHandler.getReservation());
            jsonClient.addFeatureProfile(remoteTriggerHandler.getRemoteTrigger());

            connect();
        } else {
            log.error("OCPP did not receive one of the parameters (adresCS, ChargePointID, ChargePointVendor, " +
                    "ChargePointModel) and cannot establish a connection the central system");
        }
    }

    private void connect() {
        jsonClient.connect(bootNotificationInfo.getAddressCP(), new ClientEvents() {
            @Override
            public void connectionOpened() {
                isConnected = true;
                log.info("Connection to the central system is established");
            }

            @Override
            public void connectionClosed() {
                // TODO Need for test
                Runnable connectionTask = () -> {
                    isConnected = false;
                    log.warn("The connection to the central system has not been established. " +
                            "Another try will be made");
                    try {
                        Thread.sleep(20_000);
                    } catch (InterruptedException e) {
                        log.error("Аn error while waiting for connection");
                    }
                    connect();
                };
                Thread connectionThread = new Thread(connectionTask);
                connectionThread.start();
            }
        });
    }

    @Override
    public BootNotificationInfo getBootNotificationInfo() {
        return bootNotificationInfo;
    }

    @Override
    public JSONClient getClient() {
        return jsonClient;
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }
}