package pss.mira.orp.JavaOCAOCPP.models.enums;

public enum Actions {

    // bd
    Change,
    Get,
    // cp
    RemoteStartTransaction,
    // ocpp, то есть, сам себе
    Authorize,
    // ModBus
    ChangeAvailability
}