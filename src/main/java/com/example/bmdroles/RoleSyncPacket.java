package com.example.bmdroles;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: tells a client which role it currently has (-1 = none). */
public class RoleSyncPacket {
    private final int roleId;

    public RoleSyncPacket(int roleId) {
        this.roleId = roleId;
    }

    public static void encode(RoleSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.roleId);
    }

    public static RoleSyncPacket decode(FriendlyByteBuf buf) {
        return new RoleSyncPacket(buf.readByte());
    }

    public static void handle(RoleSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientRoleState.set(Role.byId(msg.roleId)));
        ctx.get().setPacketHandled(true);
    }
}
