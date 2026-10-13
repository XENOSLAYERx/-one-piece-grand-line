package com.nerodev.grandline.ability;

/** How an ability reacts to its key. */
public enum ActivationType {
    /** Fires once on key press. Cooldown starts immediately. */
    INSTANT,
    /** Active while the key is held (drains per tick). Cooldown starts on release. */
    HOLD,
    /** Press to turn on, press again to turn off (drains per tick). Cooldown starts when turned off. */
    TOGGLE,
    /** Builds power while held, fires on release with the charge time. Cooldown starts on release. */
    CHARGE;

    public boolean isSustained() {
        return this != INSTANT;
    }
}
