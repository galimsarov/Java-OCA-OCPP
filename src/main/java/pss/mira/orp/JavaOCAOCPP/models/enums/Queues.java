package pss.mira.orp.JavaOCAOCPP.models.enums;

public enum Queues {
    // prod, test -> bd
    // dev -> myQueue2
    bd,
    connectorsInfo,
    // prod, test -> mainChargePointLogic
    // dev -> cp
    mainChargePointLogic,
    ModBus,
    ocpp,
    ocppCache
}