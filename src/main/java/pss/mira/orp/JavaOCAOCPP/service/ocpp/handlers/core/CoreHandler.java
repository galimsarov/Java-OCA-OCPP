package pss.mira.orp.JavaOCAOCPP.service.ocpp.handlers.core;

import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;

import java.util.List;

public interface CoreHandler {
    ClientCoreProfile getCore();

    void setConnectorAvailabilityStatus(List<Object> parsedMessage);

    void setChangeConfigurationStatus(List<Object> parsedMessage);

    void setAuthorizeConfirmation(List<Object> parsedMessage);

    void setRemoteStartStopStatus(List<Object> parsedMessage, String type);

    void setResetStatus(List<Object> parsedMessage);

    void setUnlockConnectorStatus(List<Object> parsedMessage);

    void setStationAvailabilityStatus(List<Object> parsedMessage);

    void setLimitStatus(List<Object> parsedMessage);
}