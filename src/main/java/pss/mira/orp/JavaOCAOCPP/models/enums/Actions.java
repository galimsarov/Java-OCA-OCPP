package pss.mira.orp.JavaOCAOCPP.models.enums;

public enum Actions {
    // bd
    BootNotification,
    CancelReservation,
    Change,
    Delete,
    Get,
    RemoteTriggerBootNotification,
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
    GetConfiguration,
    GetConnectorsInfo
}