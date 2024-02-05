package pss.mira.orp.JavaOCAOCPP.service.cache.configuration;

import eu.chargetime.ocpp.model.core.GetConfigurationConfirmation;
import eu.chargetime.ocpp.model.core.GetConfigurationRequest;
import eu.chargetime.ocpp.model.core.KeyValueType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getResult;

@Service
@Slf4j
public class ConfigurationCacheImpl implements ConfigurationCache {
    private final Map<String, Map<String, Object>> configurationMap = new HashMap<>();

    @Override
    public void createCache(List<Object> parsedMessage) {
        try {
            List<Map<String, Object>> configurationList = getResult(parsedMessage);
            for (Map<String, Object> map : configurationList) {
                String key = map.get("key").toString();
                configurationMap.put(key, map);
            }
        } catch (Exception ignored) {
            log.error("An error occurred while receiving configuration table from the message");
        }
    }

    @Override
    public GetConfigurationConfirmation getGetConfigurationConfirmation(GetConfigurationRequest request) {
        KeyValueType[] keyValueTypeArray = new KeyValueType[request.getKey().length];
        for (int i = 0; i < keyValueTypeArray.length; i++) {
            String key = request.getKey()[i];
            Map<String, Object> map = configurationMap.get(key);
            boolean readonly = Boolean.parseBoolean(map.get("readonly").toString());
            String value = map.get("value").toString();
            KeyValueType keyValueType = new KeyValueType(key, readonly);
            keyValueType.setValue(value);
            keyValueTypeArray[i] = keyValueType;
        }
        GetConfigurationConfirmation result = new GetConfigurationConfirmation();
        result.setConfigurationKey(keyValueTypeArray);
        return result;
    }

    @Override
    public boolean getAuthorizeRemoteTxRequests() {
        try {
            return Boolean.parseBoolean(configurationMap.get("AuthorizeRemoteTxRequests").get("value").toString());
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public int getMeterValueSampleInterval() {
        try {
            return Integer.parseInt(configurationMap.get("MeterValueSampleInterval").get("value").toString());
        } catch (Exception ignored) {
            return -1;
        }
    }

    @Override
    public String getMeterValuesSampledData() {
        try {
            return configurationMap.get("MeterValuesSampledData").get("value").toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public boolean reserveConnectorsZeroSupported() {
        try {
            return Boolean.parseBoolean(configurationMap.get("ReserveConnectorZeroSupported").get("value").toString());
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public boolean reservationSupported() {
        try {
            String value = configurationMap.get("SupportedFeatureProfiles").get("value").toString();
            String[] profiles = value.split(",");
            for (String profile : profiles) {
                if (profile.equals("Reservation")) {
                    return true;
                }
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void addToCache(List<Map<String, Object>> configurations) {
        for (Map<String, Object> map : configurations) {
            String key = map.get("key").toString();
            configurationMap.put(key, map);
        }
    }

    @Override
    public int getConnectionTimeOut() {
        try {
            return Integer.parseInt(configurationMap.get("ConnectionTimeOut").get("value").toString());
        } catch (Exception ignored) {
            return 40;
        }
    }
}