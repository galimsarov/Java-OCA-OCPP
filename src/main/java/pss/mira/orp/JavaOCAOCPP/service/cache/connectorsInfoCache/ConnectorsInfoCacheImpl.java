package pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache;

import eu.chargetime.ocpp.model.core.ChargePointErrorCode;
import eu.chargetime.ocpp.model.core.ChargePointStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.requests.ocpp.StatusNotificationRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pss.mira.orp.JavaOCAOCPP.models.enums.ConnectorStatus.Charging;

@Service
@Slf4j
public class ConnectorsInfoCacheImpl implements ConnectorsInfoCache {
    private final Map<Integer, Map<String, Object>> connectorsMap = new HashMap<>();

    /**
     * [{"events":{"listeners":[]},"id":1,"slaveId":5,"status":"Available","error":"NoError","chargePointVendorError":"NoErrorVendor","errorCode":0,"UnavailableTime":0,"lastChargePointVendorError":"NoErrorVendor","timer":0,"startTimer":0,"timeoutButtonStartStop":0,"excessEnergy":0,"connected":false,"ev_requested_voltage":0.0,"ev_requested_current":0.0,"ev_requested_power":0,"lastTime":"Jan 18, 2024, 5:39:34 AM","lastConnectorStatus":"Available","startFlag":false,"limitCalc":0.0,"voltageFromPhases":220.0,"lastAmperage":0.0,"lastPower":0.0,"MaxDynamicPower":0.0,"MaxDynamicAmperage":0.0,"CounterDynamic":0,"rfidStartLocal":false,"rfidStart":false,"buttonStart":false,"ConnectorInsertedTime":0,"FalgConnectorInsertedTimeSend":false,"EVCCID":"","StartStopButtonPressed":false,"remoteStart":false,"StartFromButton":false,"sentPreparing":false,"errorMiraMeter":false,"reserved":false,"spamCount":0,"spamBlock":false,"timeLastStatusSending":"Jan 18, 2024, 5:39:34 AM","fullStationExternConsumedEnergy":0.0,"statusPrecharge":"NONE","emergencyButtonPressed":false,"openDoor":false,"mode3StopButtonPressed":false,"connectedToEV":false,"consumedEnergy":0,"percent":0,"chargingTime":0,"power":0.0,"currentAmperage":0.0,"currentVoltage":0,"remainingTime":0,"type":"GBT","minAmperage":0.0,"maxAmperage":155.0,"minPower":0.0,"maxPower":150.0,"minVoltage":0,"maxVoltage":0,"levelPWM":0,"statusPWM":"WAITING","wellPWM":0,"maxSessionAmperage":0.0,"maxSessionPower":0.0,"temperaturePwM":0,"availability":"Operative","fullStationConsumedEnergy":100,"CCSControllerVersion":0,"mapModulInfo":{"Модуль 1 ":["NO_ERROR"],"Модуль 2 ":["NO_ERROR"],"Модуль 3 ":["NO_ERROR"],"Модуль 4 ":["NO_ERROR"],"Модуль 5 ":["NO_ERROR"],"Модуль 6 ":["NO_ERROR"]},"stationInfo":[],"mapTemperatureCmInfo":{"Модуль 1":0,"Модуль 2":0,"Модуль 3":0,"Модуль 4":0,"Модуль 5":0,"Модуль 6":0},"CCSControllerConfig":0,"CCSMatchingDeviceVersion":0,"fullStationConsumedEnergy":100}]
     */
    @Override
    public List<StatusNotificationRequest> addToCache(List<Map<String, Object>> connectorsInfo) {
        List<StatusNotificationRequest> result = new ArrayList<>();
        if (!connectorsMap.isEmpty()) {
            for (Map<String, Object> map : connectorsInfo) {
                int id = Integer.parseInt(map.get("id").toString());
                String newError = map.get("error").toString();
                String newStatus = map.get("status").toString();

                Map<String, Object> mapFromCache = connectorsMap.get(id);
                String oldError = mapFromCache.get("error").toString();
                String oldStatus = mapFromCache.get("status").toString();

                if (!newError.equals(oldError) || !newStatus.equals(oldStatus)) {
                    addToResult(newError, newStatus, result, id);
                }
            }
            connectorsMap.clear();
        } else {
            for (Map<String, Object> map : connectorsInfo) {
                int id = Integer.parseInt(map.get("id").toString());
                String newError = map.get("error").toString();
                String newStatus = map.get("status").toString();
                addToResult(newError, newStatus, result, id);
            }
        }
        for (Map<String, Object> map : connectorsInfo) {
            int connectorId = Integer.parseInt(map.get("id").toString());
            connectorsMap.put(connectorId, map);
        }
        return result;
    }

    private void addToResult(String newError, String newStatus, List<StatusNotificationRequest> result, int id) {
        ChargePointErrorCode parsedCode = getErrorCode(newError);
        ChargePointStatus parsedStatus = getChargePointStatus(newStatus);
        if ((parsedCode != null) && (parsedStatus != null)) {
            result.add(new StatusNotificationRequest(id, parsedCode, parsedStatus));
        } else {
            log.error("The status or error of connector " + id +
                    " does not correspond to the values of the corresponding enum");
        }
    }

    private ChargePointStatus getChargePointStatus(String newStatus) {
        for (ChargePointStatus status : ChargePointStatus.values()) {
            if (status.name().equals(newStatus)) {
                return status;
            }
        }
        return null;
    }

    private ChargePointErrorCode getErrorCode(String newErrorCode) {
        for (ChargePointErrorCode errorCode : ChargePointErrorCode.values()) {
            if (errorCode.name().equals(newErrorCode)) {
                return errorCode;
            }
        }
        return null;
    }

    @Override
    public boolean stationIsCharging() {
        for (Map.Entry<Integer, Map<String, Object>> entry : connectorsMap.entrySet()) {
            Map<String, Object> map = entry.getValue();
            if (map.get("status").toString().equals(Charging.name())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return connectorsMap.isEmpty();
    }

    @Override
    public StatusNotificationRequest getStatusNotificationRequest(int connectorId) {
        Map<String, Object> connectorMap = connectorsMap.get(connectorId);
        return new StatusNotificationRequest(
                connectorId,
                getErrorCode(connectorMap.get("errorCode").toString()),
                getChargePointStatus(connectorMap.get("status").toString())
        );
    }

    /**
     * Исходим из того, что значение счётчика будет: "fullStationConsumedEnergy":100
     * Для MeterValues "Energy.Active.Import.Register" берём "fullStationConsumedEnergy"
     */
    @Override
    public int getFullStationConsumedEnergy(int connectorId) {
        return Integer.parseInt(connectorsMap.get(connectorId).get("fullStationConsumedEnergy").toString());
    }

    /**
     * Для MeterValues "Current.Import" берём "ev_requested_current"
     */
    @Override
    public double getEVRequestedCurrent(int connectorId) {
        return Double.parseDouble(connectorsMap.get(connectorId).get("ev_requested_current").toString());
    }

    /**
     * Для MeterValues "Current.Offered" берём "currentAmperage"
     */
    @Override
    public double getCurrentAmperage(int connectorId) {
        return Double.parseDouble(connectorsMap.get(connectorId).get("currentAmperage").toString());
    }

    /**
     * Для MeterValues "Power.Active.Import" берём "ev_requested_power"
     */
    @Override
    public int getEVRequestedPower(int connectorId) {
        return Integer.parseInt(connectorsMap.get(connectorId).get("ev_requested_power").toString());
    }

    /**
     * Для MeterValues "SoC" берём "percent"
     */
    @Override
    public int getPercent(int connectorId) {
        return Integer.parseInt(connectorsMap.get(connectorId).get("percent").toString());
    }

    @Override
    public String getChargePointVendorError(int connectorId) {
        return connectorsMap.get(connectorId).get("chargePointVendorError").toString();
    }

    @Override
    public boolean isCharging(int connectorId) {
        return connectorsMap.get(connectorId).get("status").toString().equals("Charging");
    }
}