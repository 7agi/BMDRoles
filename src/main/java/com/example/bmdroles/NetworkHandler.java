package com.example.bmdroles;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class NetworkHandler {
    // Bumped to 2 in v1.1 (the sync packet now carries the easy mode flag).
    // Server and clients must both run 1.1+.
    private static final String PROTOCOL = "2";

    // "acceptMissingOr": clients / servers WITHOUT the mod can still connect.
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BmdRoles.MOD_ID, "main"),
            () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL),
            NetworkRegistry.acceptMissingOr(PROTOCOL)
    );

    private NetworkHandler() {
    }

    public static void init() {
        CHANNEL.registerMessage(0, RoleSyncPacket.class,
                RoleSyncPacket::encode, RoleSyncPacket::decode, RoleSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sync(ServerPlayer player) {
        if (!CHANNEL.isRemotePresent(player.connection.connection)) {
            return; // vanilla client, nothing to sync
        }
        Role role = RoleManager.get(player.getUUID());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new RoleSyncPacket(role == null ? -1 : role.ordinal(), RoleManager.isEasyMode()));
    }
}
