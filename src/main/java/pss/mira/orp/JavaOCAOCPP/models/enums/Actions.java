package pss.mira.orp.JavaOCAOCPP.models.enums;

public enum Actions {
    // bd
    Change,
    Get,
    // mainChargePointLogic
    RemoteStartTransaction,
    RemoteStopTransaction,
    Reset,
    UnlockConnector,
    // ocpp, то есть, сам себе
    Authorize,
    SaveToCache,
    // ModBus
    ChangeAvailability,
    GetConnectorsInfo
}