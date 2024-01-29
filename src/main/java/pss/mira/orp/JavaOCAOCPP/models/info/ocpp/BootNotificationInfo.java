package pss.mira.orp.JavaOCAOCPP.models.info.ocpp;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

import static pss.mira.orp.JavaOCAOCPP.service.utils.Utils.getResult;

@Getter
@Setter
public class BootNotificationInfo {
    private String addressCP;
    private String chargePointID;
    private String model;
//    private String version;

    public BootNotificationInfo(List<Object> parsedMessage) {
        List<Map<String, Object>> result = getResult(parsedMessage);
        for (Map<String, Object> map : result) {
            String key = map.get("key").toString();
            switch (key) {
                case "adresCS" -> addressCP = map.get("value").toString();
                case "ChargePointID" -> chargePointID = map.get("value").toString();
                case "ChargePointModel" -> model = map.get("value").toString();
//                case "version" -> version = map.get("value").toString();
            }
            if (addressCP != null && chargePointID != null && model != null
//                    && version != null
            ) {
                break;
            }
        }
    }
}