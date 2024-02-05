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
    SendHeartbeatToCentralSystem,
    SendMeterValuesToCentralSystem,
    SendStatusNotificationToCentralSystem,
    // ModBus
    ChangeConnectorAvailability,
    ChangeStationAvailability,
    GetConfiguration,
    GetConnectorsInfo
}