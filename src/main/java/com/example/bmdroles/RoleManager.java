package com.example.bmdroles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side access to the role data. Safe to call from other threads (voice chat). */
public final class RoleManager {
    private static volatile RoleData data;

    private RoleManager() {
    }

    public static void load(MinecraftServer server) {
        data = server.overworld().getDataStorage().computeIfAbsent(RoleData::load, RoleData::new, RoleData.NAME);
    }

    public static void unload() {
        data = null;
    }

    @Nullable
    public static Role get(UUID id) {
        RoleData d = data;
        return d == null ? null : d.roles.get(id);
    }

    public static boolean has(UUID id, Role role) {
        return get(id) == role;
    }

    public static boolean isEasyMode() {
        RoleData d = data;
        return d != null && d.easyMode;
    }

    public static Map<UUID, Role> snapshot() {
        RoleData d = data;
        return d == null ? new HashMap<>() : new HashMap<>(d.roles);
    }

    /** Turn easy mode (muffled / blurred instead of fully blocked) on or off for everybody. */
    public static void setEasyMode(MinecraftServer server, boolean enabled) {
        RoleData d = data;
        if (d == null) {
            return;
        }
        d.easyMode = enabled;
        d.setDirty();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            NetworkHandler.sync(player);
            if (get(player.getUUID()) != null) {
                player.displayClientMessage(Component.literal("Easy mode is now " + (enabled ? "ON" : "OFF") + ".")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GOLD), true);
            }
        }
    }

    /** Set (or with null, remove) the role of a player. */
    public static void assign(ServerPlayer player, @Nullable Role role) {
        RoleData d = data;
        if (d == null) {
            return;
        }
        if (role == null) {
            d.roles.remove(player.getUUID());
        } else {
            d.roles.put(player.getUUID(), role);
        }
        d.setDirty();
        refresh(player, true);
    }

    /**
     * Shift every assigned role one step: Blind -> Deaf -> Mute -> Blind.
     * Nobody is picked randomly, so the distribution of roles stays exactly the same and
     * everybody walks through all three roles over three rotations.
     */
    public static int rotateAll(MinecraftServer server) {
        RoleData d = data;
        if (d == null) {
            return 0;
        }
        int count = 0;
        for (UUID id : new ArrayList<>(d.roles.keySet())) {
            Role old = d.roles.get(id);
            if (old != null) {
                d.roles.put(id, old.next());
                count++;
            }
        }
        d.setDirty();
        for (UUID id : d.roles.keySet()) {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online != null) {
                refresh(online, true);
            }
        }
        return count;
    }

    public static int clearAll(MinecraftServer server) {
        RoleData d = data;
        if (d == null) {
            return 0;
        }
        ArrayList<UUID> ids = new ArrayList<>(d.roles.keySet());
        d.roles.clear();
        d.setDirty();
        for (UUID id : ids) {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online != null) {
                refresh(online, true);
            }
        }
        return ids.size();
    }

    /** Sync the role to the player's client and optionally show it on the action bar. */
    public static void refresh(ServerPlayer player, boolean announce) {
        NetworkHandler.sync(player);
        if (announce) {
            Role role = get(player.getUUID());
            Component msg = role == null
                    ? Component.literal("You are no longer Blind, Deaf or Mute.").withStyle(ChatFormatting.GREEN)
                    : Component.literal("You are now " + role.displayName() + ".").withStyle(role.color());
            player.displayClientMessage(msg, true);
        }
    }
}
