package pss.mira.orp.JavaOCAOCPP.models.requests.ocpp;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BootNotificationRequest {
    private String addressCP;
    private String chargePointID;
    private String model;

    public BootNotificationRequest(List<Object> parsedMessage) {
        Map<String, Map<String, List<Map<String, Object>>>> tablesMap =
                (Map<String, Map<String, List<Map<String, Object>>>>) parsedMessage.get(2);
        for (Map<String, Object> map : tablesMap.get("tables").get("config_zs")) {
            String key = map.get("key").toString();
            switch (key) {
                case ("adresCS"):
                    addressCP = map.get("value").toString();
                    break;
                case ("ChargePointID"):
                    chargePointID = map.get("value").toString();
                    break;
                case ("ChargePointModel"):
                    model = map.get("value").toString();
            }
            if (addressCP != null && chargePointID != null && model != null) {
                break;
            }
        }
    }
}