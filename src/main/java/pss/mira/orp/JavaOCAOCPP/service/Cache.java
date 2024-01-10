package pss.mira.orp.JavaOCAOCPP.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class Cache {
    private final Map<String, String> cache = new HashMap<>();

    public void addToCache(String key, String value) {
        cache.put(key, value);
    }

    public String getFromCacheByKey(String key) {
        return cache.get(key);
    }
}