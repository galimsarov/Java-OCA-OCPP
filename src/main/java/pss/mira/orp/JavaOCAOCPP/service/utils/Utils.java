package pss.mira.orp.JavaOCAOCPP.service.utils;

import eu.chargetime.ocpp.model.core.IdTagInfo;

import java.util.HashMap;
import java.util.Map;

public class Utils {
    public static Map<String, String> getIdTagInfoMap(IdTagInfo idTagInfo) {
        Map<String, String> idTagInfoMap = new HashMap<>();
        idTagInfoMap.put("expiryDate", idTagInfo.getExpiryDate().toString());
        idTagInfoMap.put("parentIdTag", idTagInfo.getParentIdTag());
        idTagInfoMap.put("status", idTagInfo.getStatus().toString());
        return idTagInfoMap;
    }
}