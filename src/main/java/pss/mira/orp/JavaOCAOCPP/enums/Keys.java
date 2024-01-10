package pss.mira.orp.JavaOCAOCPP.enums;

import lombok.Getter;

@Getter
public enum Keys {
    ADDRESS_CP("adresCS"),
    CHARGE_POINT_ID("ChargePointID"),
    VENDOR("ChargePointModel"),
    CHARGE_POINT_MODEL("ChargePointModel");

    private final String key;

    Keys(String key) {
        this.key = key;
    }
}