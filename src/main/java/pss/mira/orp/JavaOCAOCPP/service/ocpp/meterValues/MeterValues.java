package pss.mira.orp.JavaOCAOCPP.service.ocpp.meterValues;

public interface MeterValues {
    void addToChargingConnectors(int connectorId);

    void removeFromChargingConnectors(int connectorId);
}