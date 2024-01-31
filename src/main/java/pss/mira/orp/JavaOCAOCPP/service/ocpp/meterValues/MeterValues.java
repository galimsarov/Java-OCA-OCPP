package pss.mira.orp.JavaOCAOCPP.service.ocpp.meterValues;

public interface MeterValues {
    void addToChargingConnectors(int connectorId, int transactionId);

    void removeFromChargingConnectors(int connectorId);

//    void setConfigurationMap(List<Object> parsedMessage);
}