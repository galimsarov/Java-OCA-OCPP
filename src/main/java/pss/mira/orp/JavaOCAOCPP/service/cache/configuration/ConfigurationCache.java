package pss.mira.orp.JavaOCAOCPP.service.cache.configuration;

import java.util.List;

public interface ConfigurationCache {
    void addToCache(List<Object> parsedMessage);

    Object getConfigurationValue(String key);
}