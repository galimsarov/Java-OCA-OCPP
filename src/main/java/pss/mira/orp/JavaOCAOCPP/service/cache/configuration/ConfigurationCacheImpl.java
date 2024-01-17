package pss.mira.orp.JavaOCAOCPP.service.cache.configuration;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getResult;

@Service
public class ConfigurationCacheImpl implements ConfigurationCache {
    private List<Map<String, Object>> configurationList = null;

    @Override
    public void addToCache(List<Object> parsedMessage) {
        configurationList = getResult(parsedMessage);
    }

    @Override
    public Object getConfigurationValue(String key) {
        return null;
    }
}