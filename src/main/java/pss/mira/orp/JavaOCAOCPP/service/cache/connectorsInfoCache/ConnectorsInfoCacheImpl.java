package pss.mira.orp.JavaOCAOCPP.service.cache.connectorsInfoCache;

import eu.chargetime.ocpp.model.core.ChargePointErrorCode;
import eu.chargetime.ocpp.model.core.ChargePointStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pss.mira.orp.JavaOCAOCPP.models.info.ocpp.StatusNotificationInfo;
import pss.mira.orp.JavaOCAOCPP.service.cache.reservation.ReservationCache;

import java.util.*;

import static pss.mira.orp.JavaOCAOCPP.models.enums.ConnectorStatus.Charging;

@Service
@Slf4j
public class ConnectorsInfoCacheImpl implements ConnectorsInfoCache {
    private final Map<Integer, Map<String, Object>> connectorsMap = new HashMap<>();
    private final ReservationCache reservationCache;

    public ConnectorsInfoCacheImpl(ReservationCache reservationCache) {
        this.reservationCache = reservationCache;
    }

    /**
     * ["ModBus","92c0ac6b-38cc-4612-b985-8a00154f1129",{"connectors":[{"id":1,"slaveId":1,"status":"Available","error":"NoError","chargePointVendorError":"NoErrorVendor","errorCode":0,"unavailableTime":0,"lastChargePointVendorError":"NoErrorVendor","timer":0,"startTimer":0,"timeoutButtonStartStop":0,"excessEnergy":0,"connected":false,"ev_requested_voltage":0.0,"ev_requested_current":16.0,"ev_requested_power":0,"lastTime":"Jan 25, 2024, 7:11:28 AM","lastConnectorStatus":"Available","startFlag":false,"limitCalc":0.0,"voltageFromPhases":220.0,"lastAmperage":0.0,"lastPower":0.0,"maxDynamicPower":0.0,"maxDynamicAmperage":0.0,"CounterDynamic":0,"rfidStartLocal":false,"rfidStart":false,"buttonStart":false,"connectorInsertedTime":0,"flagConnectorInsertedTimeSend":false,"startStopButtonPressed":false,"remoteStart":false,"startFromButton":false,"sentPreparing":false,"errorMiraMeter":false,"reserved":false,"spamCount":0,"spamBlock":false,"timeLastStatusSending":"Jan 25, 2024, 7:11:28 AM","fullStationExternConsumedEnergy":0.0,"statusPrecharge":"NONE","emergencyButtonPressed":false,"openDoor":false,"mode3StopButtonPressed":false,"connectedToEV":false,"consumedEnergy":0,"percent":0,"power":0.0,"currentAmperage":16.0,"currentVoltage":0,"remainingTime":0,"type":"Mode3_type2","minAmperage":0.0,"maxAmperage":0.0,"minPower":0.0,"maxPower":0.0,"minVoltage":0,"maxVoltage":0,"statusPWM":"WAITING","wellPWM":53,"maxSessionAmperage":0.0,"maxSessionPower":0.0,"temperaturePwM":-50,"fullStationConsumedEnergy":0,"CCSControllerVersion":0,"mapModulInfo":{},"stationInfo":[],"mapTemperatureCmInfo":{},"CCSControllerConfig":0,"CCSMatchingDeviceVersion":0}]}]
     * [{"events":{"listeners":[]},"id":1,"slaveId":5,"status":"Available","error":"NoError","chargePointVendorError":"NoErrorVendor","errorCode":0,"UnavailableTime":0,"lastChargePointVendorError":"NoErrorVendor","timer":0,"startTimer":0,"timeoutButtonStartStop":0,"excessEnergy":0,"connected":false,"ev_requested_voltage":0.0,"ev_requested_current":0.0,"ev_requested_power":0,"lastTime":"Jan 18, 2024, 5:39:34 AM","lastConnectorStatus":"Available","startFlag":false,"limitCalc":0.0,"voltageFromPhases":220.0,"lastAmperage":0.0,"lastPower":0.0,"MaxDynamicPower":0.0,"MaxDynamicAmperage":0.0,"CounterDynamic":0,"rfidStartLocal":false,"rfidStart":false,"buttonStart":false,"ConnectorInsertedTime":0,"FalgConnectorInsertedTimeSend":false,"EVCCID":"","StartStopButtonPressed":false,"remoteStart":false,"StartFromButton":false,"sentPreparing":false,"errorMiraMeter":false,"reserved":false,"spamCount":0,"spamBlock":false,"timeLastStatusSending":"Jan 18, 2024, 5:39:34 AM","fullStationExternConsumedEnergy":0.0,"statusPrecharge":"NONE","emergencyButtonPressed":false,"openDoor":false,"mode3StopButtonPressed":false,"connectedToEV":false,"consumedEnergy":0,"percent":0,"chargingTime":0,"power":0.0,"currentAmperage":0.0,"currentVoltage":0,"remainingTime":0,"type":"GBT","minAmperage":0.0,"maxAmperage":155.0,"minPower":0.0,"maxPower":150.0,"minVoltage":0,"maxVoltage":0,"levelPWM":0,"statusPWM":"WAITING","wellPWM":0,"maxSessionAmperage":0.0,"maxSessionPower":0.0,"temperaturePwM":0,"availability":"Operative","fullStationConsumedEnergy":100,"CCSControllerVersion":0,"mapModulInfo":{"Модуль 1 ":["NO_ERROR"],"Модуль 2 ":["NO_ERROR"],"Модуль 3 ":["NO_ERROR"],"Модуль 4 ":["NO_ERROR"],"Модуль 5 ":["NO_ERROR"],"Модуль 6 ":["NO_ERROR"]},"stationInfo":[],"mapTemperatureCmInfo":{"Модуль 1":0,"Модуль 2":0,"Модуль 3":0,"Модуль 4":0,"Модуль 5":0,"Модуль 6":0},"CCSControllerConfig":0,"CCSMatchingDeviceVersion":0,"fullStationConsumedEnergy":100}]
     */
    @Override
    public List<StatusNotificationInfo> addToCache(List<Map<String, Object>> connectorsInfo) {
        while (true) {
            if (reservationCache.filled()) {
                break;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("An error occurred while creating reservation cache");
                }
            }
        }
        List<StatusNotificationInfo> result = new ArrayList<>();
        if (!connectorsMap.isEmpty()) {
            for (Map<String, Object> map : connectorsInfo) {
                int id = Integer.parseInt(map.get("id").toString());
                String newError = map.get("error").toString();
                String newStatus = fixStatus(map.get("status").toString(), id);

                Map<String, Object> mapFromCache = connectorsMap.get(id);
                if (mapFromCache != null) {
                    String oldError = mapFromCache.get("error").toString();
                    String oldStatus = fixStatus(mapFromCache.get("status").toString(), id);

                    if (!newError.equals(oldError) || !newStatus.equals(oldStatus)) {
                        addToResult(newError, newStatus, result, id);
                    }
                }
            }
        } else {
            for (Map<String, Object> map : connectorsInfo) {
                int id = Integer.parseInt(map.get("id").toString());
                String newError = map.get("error").toString();
                String newStatus = fixStatus(map.get("status").toString(), id);
                addToResult(newError, newStatus, result, id);
            }
        }
        for (Map<String, Object> map : connectorsInfo) {
            int connectorId = Integer.parseInt(map.get("id").toString());
            connectorsMap.put(connectorId, map);
        }
        return result;
    }

    private String fixStatus(String status, int connectorId) {
        boolean reserved = reservationCache.reserved(connectorId);
        if (reserved) {
            if (status.equals("Unavailable") || status.equals("Faulted")) {
                return status;
            } else {
                return "Reserved";
            }
        } else {
            return status;
        }
    }

    @Override
    public List<StatusNotificationInfo> createCache(List<Object> parsedMessage) {
        Map<String, List<Map<String, Object>>> map = (Map<String, List<Map<String, Object>>>) parsedMessage.get(2);
        return addToCache(map.get("connectors"));
    }

    private void addToResult(String newError, String newStatus, List<StatusNotificationInfo> result, int id) {
        ChargePointErrorCode parsedCode = getErrorCode(newError);
        ChargePointStatus parsedStatus = getChargePointStatus(newStatus);
        if ((parsedCode != null) && (parsedStatus != null)) {
            result.add(new StatusNotificationInfo(id, parsedCode, parsedStatus));
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
    public StatusNotificationInfo getStatusNotificationRequest(int connectorId) {
        Map<String, Object> connectorMap = connectorsMap.get(connectorId);
        return new StatusNotificationInfo(
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

    @Override
    public String getStatus(int connectorId) {
        return connectorsMap.get(connectorId).get("status").toString();
    }

    @Override
    public Set<Integer> getConnectorsIds() {
        return connectorsMap.keySet();
    }

    @Override
    public String getEVCCID(int connectorId) {
        try {
            return connectorsMap.get(connectorId).get("EVCCID").toString();
        } catch (Exception ignored) {
            return "";
        }
    }
}