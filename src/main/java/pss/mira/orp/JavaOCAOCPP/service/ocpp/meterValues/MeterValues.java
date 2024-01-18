package pss.mira.orp.JavaOCAOCPP.service.ocpp.meterValues;

import java.util.List;

public interface MeterValues {
    void addToChargingConnectors(int connectorId);

    void removeFromChargingConnectors(int connectorId);

    void setConfigurationMap(List<Object> parsedMessage);
}