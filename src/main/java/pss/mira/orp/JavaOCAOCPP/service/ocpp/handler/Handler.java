package pss.mira.orp.JavaOCAOCPP.service.ocpp.handler;

import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;

import java.util.List;

public interface Handler {
    ClientCoreProfile getCore();

    void setAvailabilityStatus(List<Object> parsedMessage);

    void setConfigurationMap(List<Object> parsedMessage);
}