package pss.mira.orp.JavaOCAOCPP.models.enums;

public enum Actions {
    // bd
    BootNotification,
    CancelReservation,
    Change,
    CreateTransaction,
    Delete,
    Get,
    GetConfiguration,
    GetNonStoppedTransactions,
    RemoteTriggerBootNotification,
    ReserveNow,
    UpdateTransaction,
    // mainChargePointLogic
    RemoteStartTransaction,
    RemoteStopTransaction,
    Reset,
    SetLimit,
    StopChargeSession,
    UnlockConnector,
    // ocpp, то есть, сам себе
    Authorize,
    SaveToCache,
    SendHeartbeatToCentralSystem,
    SendMeterValuesToCentralSystem,
    SendStatusNotificationToCentralSystem,
    // ModBus
    ChangeConnectorAvailability,
    ChangeStationAvailability,
    GetConnectorsInfo
}