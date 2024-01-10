package pss.mira.orp.JavaOCAOCPP.service.cache;

public interface Cache {
    void addToCache(String key, String value);

    String getFromCacheByKey(String key);
}