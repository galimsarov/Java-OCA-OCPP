package pss.mira.orp.JavaOCAOCPP.service.cache;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CacheImpl implements Cache {
    private final Map<String, String> cache = new HashMap<>();

    @Override
    public void addToCache(String key, String value) {
        cache.put(key, value);
    }

    @Override
    public String getFromCacheByKey(String key) {
        return cache.get(key);
    }
}