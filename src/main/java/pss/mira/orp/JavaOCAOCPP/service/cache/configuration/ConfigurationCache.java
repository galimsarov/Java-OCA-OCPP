package pss.mira.orp.JavaOCAOCPP.service.cache.configuration;

import eu.chargetime.ocpp.model.core.GetConfigurationConfirmation;
import eu.chargetime.ocpp.model.core.GetConfigurationRequest;

import java.util.List;
import java.util.Map;

public interface ConfigurationCache {
    void createCache(List<Object> parsedMessage);

    GetConfigurationConfirmation getGetConfigurationConfirmation(GetConfigurationRequest request);

    boolean getAuthorizeRemoteTxRequests();

    int getMeterValueSampleInterval();

    String getMeterValuesSampledData();

    boolean reserveConnectorsZeroSupported();

    boolean reservationSupported();

    void addToCache(List<Map<String, Object>> configurations);

    int getConnectionTimeOut();
}