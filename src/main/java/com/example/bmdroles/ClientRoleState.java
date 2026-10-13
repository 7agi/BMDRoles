package com.example.bmdroles;

import org.jetbrains.annotations.Nullable;

/** The local player's role as told by the server. Contains no client-only classes on purpose. */
public final class ClientRoleState {
    private static volatile Role role;

    private ClientRoleState() {
    }

    @Nullable
    public static Role get() {
        return role;
    }

    public static void set(@Nullable Role newRole) {
        role = newRole;
    }
}
