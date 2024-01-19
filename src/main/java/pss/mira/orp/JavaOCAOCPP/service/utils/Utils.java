package pss.mira.orp.JavaOCAOCPP.service.utils;

import eu.chargetime.ocpp.model.core.IdTagInfo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Utils {
    public static Map<String, String> getIdTagInfoMap(IdTagInfo idTagInfo) {
        Map<String, String> idTagInfoMap = new HashMap<>();
        idTagInfoMap.put("expiryDate", idTagInfo.getExpiryDate().toString());
        idTagInfoMap.put("parentIdTag", idTagInfo.getParentIdTag());
        idTagInfoMap.put("status", idTagInfo.getStatus().toString());
        return idTagInfoMap;
    }

    public static Map<String, List<Map<String, String>>> getDBTablesGetRequest(List<String> tableNames) {
        List<Map<String, String>> tables = new ArrayList<>();
        for (String tableName : tableNames) {
            Map<String, String> tableMap = new HashMap<>();
            tableMap.put("nameTable", tableName);
            tables.add(tableMap);
        }
        return Map.of("tables", tables);
    }

    public static List<Map<String, Object>> getResult(List<Object> parsedMessage) {
        Map<String, List<Map<String, Object>>> tables = (Map<String, List<Map<String, Object>>>) parsedMessage.get(2);
        return (List<Map<String, Object>>) tables.get("tables").get(0).get("result");
    }

    public static String format(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return dateFormat.format(date);
    }

    public static ZonedDateTime getZoneDateTimeFromAuth(String timeFromDB) {
        if (timeFromDB == null) {
            return null;
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            LocalDateTime dateTime = LocalDateTime.parse(timeFromDB, formatter);
            return ZonedDateTime.of(dateTime, ZoneId.of("UTC"));
        }
    }
}