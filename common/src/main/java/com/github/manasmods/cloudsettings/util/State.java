package com.github.manasmods.cloudsettings.util;

public enum State {
    PENDING, TRUE, FALSE;

    public boolean asBoolean() {
        return this == State.TRUE;
    }
}
