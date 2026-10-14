package com.nerodev.grandline.weapon;

/** The broad family of a weapon; fighting styles check it ("needs a sword in hand"). */
public enum WeaponKind {
    SWORD("Sword"),
    POLEARM("Polearm"),
    CLUB("Club"),
    GUN("Gun");

    private final String label;

    WeaponKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
