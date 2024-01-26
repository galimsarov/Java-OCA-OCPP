package pss.mira.orp.JavaOCAOCPP.models.enums;

public enum Actions {
    // bd
    CancelReservation,
    Change,
    Delete,
    Get,
    ReserveNow,
    // mainChargePointLogic
    RemoteStartTransaction,
    RemoteStopTransaction,
    Reset,
    StopChargeSession,
    UnlockConnector,
    // ocpp, то есть, сам себе
    Authorize,
    SaveToCache,
    // ModBus
    ChangeAvailability,
    GetConnectorsInfo
}