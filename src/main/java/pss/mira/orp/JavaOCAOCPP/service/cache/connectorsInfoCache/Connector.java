package pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.TreeMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Connector {
    private int id;
    private int slaveId;
    private String type;
    private Integer transactionId;
    private Integer evRequestedVoltage;
    private Integer evRequestedCurrent;
    private Integer evRequestedPower;
    private Long fullStationConsumedEnergy;
    private String idTag;
    private int minAmperage;
    private int maxAmperage;
    private int minPower;
    private int maxPower;
    private int minVoltage;
    private int maxVoltage;
    private boolean emergencyButtonPressed;
    private String status;
    private int soc;
    private int consumenerge;
    private double power;
    private long chargetime;
    private int amperage;
    private String connectorError;
    private String chargePointVendorError;
    private int maxSessionAmperage;
    private String pastStatus;
    private int temperaturePwM;
    private String availability;
    private String versionController;
    private int currentVoltage;
    private TreeMap<String, String> mapModulInfo;
    private List<String> stationInfo;
    private TreeMap<String, Integer> mapTemperatureCmInfo;
    private boolean chargingFF;
    private String lastConnectorStatus;
    private String evccid;
    private Boolean isThreadSending;
}