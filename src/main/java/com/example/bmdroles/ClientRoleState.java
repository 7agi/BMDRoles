package com.example.bmdroles;

import org.jetbrains.annotations.Nullable;

/** The local player's role (and the server's easy mode flag). Contains no client-only classes on purpose. */
public final class ClientRoleState {
    private static volatile Role role;
    private static volatile boolean easyMode;

    private ClientRoleState() {
    }

    @Nullable
    public static Role get() {
        return role;
    }

    public static void set(@Nullable Role newRole) {
        role = newRole;
    }

    public static boolean isEasyMode() {
        return easyMode;
    }

    public static void setEasyMode(boolean enabled) {
        easyMode = enabled;
    }
}
