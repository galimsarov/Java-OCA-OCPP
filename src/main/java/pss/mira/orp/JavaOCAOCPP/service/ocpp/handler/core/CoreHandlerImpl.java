package pss.mira.orp.JavaOCAOCPP.service.ocpp.handler.core;

import eu.chargetime.ocpp.feature.profile.ClientCoreEventHandler;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.core.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.info.rabbit.DBTablesChangeInfo;
import pss.mira.orp.JavaOCAOCPP.models.info.rabbit.DBTablesDeleteInfo;
import pss.mira.orp.JavaOCAOCPP.models.queues.Queues;
import pss.mira.orp.JavaOCAOCPP.service.cache.chargeSessionMap.ChargeSessionMap;
import pss.mira.orp.JavaOCAOCPP.service.cache.configuration.ConfigurationCache;
import pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache.ConnectorsInfoCache;
import pss.mira.orp.JavaOCAOCPP.service.cache.reservation.ReservationCache;
import pss.mira.orp.JavaOCAOCPP.service.rabbit.sender.Sender;

import java.io.IOException;
import java.util.*;

import static eu.chargetime.ocpp.model.core.AuthorizationStatus.Accepted;
import static eu.chargetime.ocpp.model.core.AuthorizationStatus.Invalid;
import static eu.chargetime.ocpp.model.core.ConfigurationStatus.NotSupported;
import static eu.chargetime.ocpp.model.core.RemoteStartStopStatus.Rejected;
import static eu.chargetime.ocpp.model.core.ResetType.Soft;
import static pss.mira.orp.JavaOCAOCPP.models.enums.Actions.*;
import static pss.mira.orp.JavaOCAOCPP.models.enums.DBKeys.*;

@Service
@Slf4j
public class CoreHandlerImpl implements CoreHandler {
    private final ConfigurationCache configurationCache;
    private final ConnectorsInfoCache connectorsInfoCache;
    private final ChargeSessionMap chargeSessionMap;
    private final Queues queues;
    private final ReservationCache reservationCache;
    private final Sender sender;
    private AvailabilityStatus connectorAvailabilityStatus = null;
    private ConfigurationStatus changeConfigurationStatus = null;
    private AuthorizeConfirmation authorizeConfirmation = null;
    private RemoteStartStopStatus remoteStartStatus = null;
    private RemoteStartStopStatus remoteStopStatus = null;
    private ResetStatus resetStatus = null;
    private UnlockStatus unlockConnectorStatus = null;
    private final Set<String> sentUUIDs = new HashSet<>();
    private final Map<String, AvailabilityStatus> receivedUUIDs = new HashMap<>();

    public CoreHandlerImpl(
            ConfigurationCache configurationCache,
            ConnectorsInfoCache connectorsInfoCache,
            ChargeSessionMap chargeSessionMap,
            Queues queues,
            ReservationCache reservationCache,
            Sender sender
    ) {
        this.configurationCache = configurationCache;
        this.connectorsInfoCache = connectorsInfoCache;
        this.chargeSessionMap = chargeSessionMap;
        this.queues = queues;
        this.reservationCache = reservationCache;
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
                if (request.getConnectorId() == 0) {
                    return changeStationAvailability(request);
                } else {
                    sender.sendRequestToQueue(
                            queues.getModBus(),
                            UUID.randomUUID().toString(),
                            ChangeConnectorAvailability.name(),
                            request,
                            ChangeConnectorAvailability.name()
                    );
                    while (true) {
                        if (connectorAvailabilityStatus == null) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                log.error("Аn error while waiting for a change availability response");
                            }
                        } else {
                            ChangeAvailabilityConfirmation result =
                                    new ChangeAvailabilityConfirmation(connectorAvailabilityStatus);
                            log.info("Send to the central system: " + result);
                            connectorAvailabilityStatus = null;
                            return result;
                        }
                    }
                }
            }

            private ChangeAvailabilityConfirmation changeStationAvailability(ChangeAvailabilityRequest request) {
                for (Integer connectorId : connectorsInfoCache.getConnectorsIds()) {
                    ChangeAvailabilityRequest connectorRequest =
                            new ChangeAvailabilityRequest(connectorId, request.getType());
                    UUID uuid = UUID.randomUUID();
                    sender.sendRequestToQueue(
                            queues.getModBus(),
                            uuid.toString(),
                            ChangeStationAvailability.name(),
                            connectorRequest,
                            ChangeStationAvailability.name()
                    );
                    sentUUIDs.add(uuid.toString());
                }
                while (true) {
                    if (sentUUIDs.size() != receivedUUIDs.size()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Аn error while waiting for a change availability response");
                        }
                    } else {
                        AvailabilityStatus stationAvailabilityStatus = getStationAvailabilityStatus(receivedUUIDs);
                        ChangeAvailabilityConfirmation result =
                                new ChangeAvailabilityConfirmation(stationAvailabilityStatus);
                        log.info("Send to the central system: " + result);
                        sentUUIDs.clear();
                        receivedUUIDs.clear();
                        return result;
                    }
                }
            }

            private AvailabilityStatus getStationAvailabilityStatus(Map<String, AvailabilityStatus> receivedUUIDs) {
                if (receivedUUIDs.containsValue(AvailabilityStatus.Rejected)) {
                    return AvailabilityStatus.Rejected;
                } else if (receivedUUIDs.containsValue(AvailabilityStatus.Scheduled)) {
                    return AvailabilityStatus.Scheduled;
                }
                return AvailabilityStatus.Accepted;
            }

            @Override
            public GetConfigurationConfirmation handleGetConfigurationRequest(GetConfigurationRequest request) {
                log.info("Received from the central system: " + request.toString());
                GetConfigurationConfirmation result = configurationCache.getGetConfigurationConfirmation(request);
                log.info("Send to the central system: " + result);
                return result;
            }

            @Override
            public ChangeConfigurationConfirmation handleChangeConfigurationRequest(
                    ChangeConfigurationRequest request
            ) {
                log.info("Received from the central system: " + request.toString());
                sender.sendRequestToQueue(
                        queues.getDateBase(),
                        UUID.randomUUID().toString(),
                        Change.name(),
                        new DBTablesChangeInfo(
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
                log.info("Received from the central system: " + request.toString());
                return null; // returning null means unsupported feature
            }

            @Override
            public DataTransferConfirmation handleDataTransferRequest(DataTransferRequest request) {
                log.info("Received from the central system: " + request.toString());
                return null; // returning null means unsupported feature
            }

            /**
             * Ответ от mainChargePointLogic для ocpp
             * ["mainChargePointLogic","c29baee7-dfed-4160-9edc-1548693a0cdf",{"status":"Accepted"}]
             */
            @Override
            public RemoteStartTransactionConfirmation handleRemoteStartTransactionRequest(
                    RemoteStartTransactionRequest request
            ) {
                log.info("Received from the central system: " + request.toString());
                RemoteStartTransactionConfirmation reservationReject = getReservationReject(request);
                if (reservationReject != null) {
                    log.info("Sent to central system: " + reservationReject);
                    return reservationReject;
                }
                if (configurationCache.getAuthorizeRemoteTxRequests()) {
                    RemoteStartTransactionConfirmation notAuthorizedReject = getNotAuthorizedReject(request);
                    if (notAuthorizedReject != null) {
                        log.info("Sent to central system: " + notAuthorizedReject);
                        return notAuthorizedReject;
                    }
                }
                authorizeConfirmation = null;
                interactWithChargePointLogic(request);
                RemoteStartTransactionConfirmation result =
                        new RemoteStartTransactionConfirmation(remoteStartStatus);
                if (remoteStartStatus.equals(RemoteStartStopStatus.Accepted)) {
                    chargeSessionMap.addToChargeSessionMap(
                            request.getConnectorId(),
                            request.getIdTag(),
                            true,
                            connectorsInfoCache.getStatusNotificationRequest(request.getConnectorId()).getStatus(),
                            new int[]{configurationCache.getConnectionTimeOut()}
                    );
                }
                remoteStartStatus = null;
                log.info("Sent to central system: " + result);
                return result;
            }

            private RemoteStartTransactionConfirmation getReservationReject(RemoteStartTransactionRequest request) {
                Integer reservationId = reservationCache.getReservationId(request.getConnectorId(), request.getIdTag());
                if (reservationId != null) {
                    sender.sendRequestToQueue(
                            queues.getDateBase(),
                            UUID.randomUUID().toString(),
                            Delete.name(),
                            new DBTablesDeleteInfo(
                                    reservation.name(),
                                    "reservation_id",
                                    reservationId),
                            CancelReservation.name()
                    );
                    reservationCache.remove(reservationId);
                }
                if (reservationCache.reserved(request.getConnectorId())) {
                    log.warn("The central system tried to start a transaction on the reserved connector");
                    return new RemoteStartTransactionConfirmation(Rejected);
                }
                return null;
            }

            private void interactWithChargePointLogic(RemoteStartTransactionRequest request) {
                Map<String, Integer> map = new HashMap<>();
                map.put("connectorId", request.getConnectorId());
                sender.sendRequestToQueue(
                        queues.getChargePointLogic(),
                        UUID.randomUUID().toString(),
                        RemoteStartTransaction.name(),
                        map,
                        RemoteStartTransaction.name()
                );
                while (true) {
                    if (remoteStartStatus == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Аn error while waiting for remote start transaction response");
                        }
                    } else {
                        break;
                    }
                }
            }

            private RemoteStartTransactionConfirmation getNotAuthorizedReject(RemoteStartTransactionRequest request) {
                // AuthorizeRemoteTxRequests в таблице configuration -> true
                log.info("IdTag is sent for authorization to the central system");
                sender.sendRequestToQueue(
                        queues.getOCPP(),
                        UUID.randomUUID().toString(),
                        Authorize.name(),
                        Map.of("idTag", request.getIdTag()),
                        Authorize.name()
                );
                while (true) {
                    if (authorizeConfirmation == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Аn error while waiting for a authorize response from the central " +
                                    "system");
                        }
                    } else {
                        break;
                    }
                }
                if (!authorizeConfirmation.getIdTagInfo().getStatus().equals(Accepted)) {
                    authorizeConfirmation = null;
                    log.warn("The central system tried to start a transaction with an unauthorized idTag");
                    return new RemoteStartTransactionConfirmation(Rejected);
                }
                return null;
            }

            @Override
            public RemoteStopTransactionConfirmation handleRemoteStopTransactionRequest(
                    RemoteStopTransactionRequest request
            ) {
                log.info("Received from the central system: " + request.toString());
                sender.sendRequestToQueue(
                        queues.getChargePointLogic(),
                        UUID.randomUUID().toString(),
                        RemoteStopTransaction.name(),
                        request,
                        RemoteStopTransaction.name()
                );
                while (true) {
                    if (remoteStopStatus == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Аn error while waiting for remote stop transaction response");
                        }
                    } else {
                        break;
                    }
                }
                RemoteStopTransactionConfirmation result = new RemoteStopTransactionConfirmation(remoteStopStatus);
                if (remoteStopStatus.equals(RemoteStartStopStatus.Accepted)) {
                    chargeSessionMap.setRemoteStopByTransactionId(request.getTransactionId());
                }
                remoteStopStatus = null;
                log.info("Sent to central system: " + result);
                return result;
            }

            @Override
            public ResetConfirmation handleResetRequest(ResetRequest request) {
                log.info("Received from the central system: " + request.toString());
                sender.sendRequestToQueue(
                        queues.getChargePointLogic(),
                        UUID.randomUUID().toString(),
                        Reset.name(),
                        request,
                        Reset.name()
                );
                while (true) {
                    if (resetStatus == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Аn error while waiting for reset response");
                        }
                    } else {
                        break;
                    }
                }
                ResetConfirmation result = new ResetConfirmation(resetStatus);
                if (request.getType().equals(Soft) && resetStatus.equals(ResetStatus.Accepted)) {
                    log.warn("The application will restart at the end of all charging sessions");
                    restartService();
                }
                resetStatus = null;
                log.info("Sent to central system: " + result);
                return result;
            }

            private void restartService() {
                Runnable restartTask = () -> {
                    while (true) {
                        if (chargeSessionMap.isNotEmpty()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                log.error("Аn error while waiting for a restart");
                            }
                        } else {
                            break;
                        }
                    }
                    if (SystemUtils.IS_OS_WINDOWS) {
                        log.error("Application restart doesn't support in windows");
                        return;
                    }
                    int restartTries = 10;
                    for (int i = 0; i < restartTries; i++) {
                        log.info("Try to restart #{}", i);
                        try {
                            log.info("Restarting...");
                            Runtime.getRuntime().exec("sudo systemctl restart ocpp-service.service");
                            return; // По идее уже вырубится приложение, но на всякий return
                        } catch (IOException e) {
                            log.error("Аn error while trying to restart");
                        }
                    }
                    log.warn("System wasn't restarted in {} tries!", restartTries);
                };
                Thread restartThread = new Thread(restartTask);
                restartThread.start();
            }

            @Override
            public UnlockConnectorConfirmation handleUnlockConnectorRequest(UnlockConnectorRequest request) {
                log.info("Received from the central system: " + request.toString());
                sender.sendRequestToQueue(
                        queues.getChargePointLogic(),
                        UUID.randomUUID().toString(),
                        UnlockConnector.name(),
                        request,
                        UnlockConnector.name()
                );
                while (true) {
                    if (unlockConnectorStatus == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Аn error while waiting for remote start transaction response");
                        }
                    } else {
                        break;
                    }
                }
                UnlockConnectorConfirmation result = new UnlockConnectorConfirmation(unlockConnectorStatus);
                unlockConnectorStatus = null;
                log.info("Sent to central system: " + result);
                return result;
            }
        });
    }

    @Override
    public void setConnectorAvailabilityStatus(List<Object> parsedMessage) {
        try {
            Map<String, String> map = (Map<String, String>) parsedMessage.get(2);
            for (AvailabilityStatus statusFromEnum : AvailabilityStatus.values()) {
                if (statusFromEnum.name().equals(map.get("ChangeAvailability"))) {
                    connectorAvailabilityStatus = statusFromEnum;
                    break;
                }
            }
        } catch (Exception ignored) {
            log.error("An error occurred while receiving availabilityStatus from the message");
            connectorAvailabilityStatus = AvailabilityStatus.Rejected;
        }
    }

    @Override
    public void setChangeConfigurationStatus(List<Object> parsedMessage) {
        try {
            Map<String, String> map = (Map<String, String>) parsedMessage.get(2);
            for (ConfigurationStatus configurationStatus : ConfigurationStatus.values()) {
                if (configurationStatus.name().equals(map.get("result"))) {
                    changeConfigurationStatus = configurationStatus;
                    return;
                }
            }
        } catch (Exception ignored) {
            log.error("An error occurred while receiving configuration change status from the message");
            changeConfigurationStatus = NotSupported;
        }
    }

    @Override
    public void setAuthorizeConfirmation(List<Object> parsedMessage) {
        try {
            Map<String, String> map = (Map<String, String>) parsedMessage.get(2);
            String status = map.get("status");
            for (AuthorizationStatus statusFromEnum : AuthorizationStatus.values()) {
                if (statusFromEnum.name().equals(status)) {
                    IdTagInfo idTagInfo = new IdTagInfo(Accepted);
                    authorizeConfirmation = new AuthorizeConfirmation(idTagInfo);
                    return;
                }
            }
            IdTagInfo idTagInfo = new IdTagInfo(Invalid);
            authorizeConfirmation = new AuthorizeConfirmation(idTagInfo);
        } catch (Exception ignored) {
            IdTagInfo idTagInfo = new IdTagInfo(Invalid);
            authorizeConfirmation = new AuthorizeConfirmation(idTagInfo);
        }
    }

    @Override
    public void setRemoteStartStopStatus(List<Object> parsedMessage, String type) {
        try {
            Map<String, String> remoteStartTransactionMap = (Map<String, String>) parsedMessage.get(2);
            if (remoteStartTransactionMap.get("status").equals(Accepted.toString())) {
                if (type.equals("start")) {
                    remoteStartStatus = RemoteStartStopStatus.Accepted;
                } else {
                    remoteStopStatus = RemoteStartStopStatus.Accepted;
                }
            } else {
                if (type.equals("start")) {
                    remoteStartStatus = RemoteStartStopStatus.Rejected;
                } else {
                    remoteStopStatus = RemoteStartStopStatus.Rejected;
                }
            }
        } catch (Exception ignored) {
            if (type.equals("start")) {
                remoteStartStatus = RemoteStartStopStatus.Rejected;
            } else {
                remoteStopStatus = RemoteStartStopStatus.Rejected;
            }
        }
    }

    @Override
    public void setResetStatus(List<Object> parsedMessage) {
        Map<String, String> resetMap = (Map<String, String>) parsedMessage.get(2);
        if (resetMap.get("status").equals(ResetStatus.Accepted.name())) {
            resetStatus = ResetStatus.Accepted;
        } else {
            resetStatus = ResetStatus.Rejected;
        }
    }

    @Override
    public void setUnlockConnectorStatus(List<Object> parsedMessage) {
        Map<String, String> unlockConnectorStatusMap = (Map<String, String>) parsedMessage.get(2);
        for (UnlockStatus status : UnlockStatus.values()) {
            if (unlockConnectorStatusMap.get("status").equals(status.name())) {
                unlockConnectorStatus = status;
                return;
            }
        }
        unlockConnectorStatus = UnlockStatus.NotSupported;
    }

    @Override
    public void setStationAvailabilityStatus(List<Object> parsedMessage) {
        try {
            Map<String, String> map = (Map<String, String>) parsedMessage.get(2);
            String uuid = parsedMessage.get(1).toString();
            for (AvailabilityStatus statusFromEnum : AvailabilityStatus.values()) {
                if (statusFromEnum.name().equals(map.get("ChangeAvailability"))) {
                    receivedUUIDs.put(uuid, statusFromEnum);
                    break;
                }
            }
        } catch (Exception ignored) {
            log.error("An error occurred while receiving availabilityStatus from the message");
            receivedUUIDs.put("", AvailabilityStatus.Rejected);
        }
    }
}