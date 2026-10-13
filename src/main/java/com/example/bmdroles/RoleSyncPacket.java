package com.example.bmdroles;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: the client's current role (-1 = none) and whether easy mode is on. */
public class RoleSyncPacket {
    private final int roleId;
    private final boolean easyMode;

    public RoleSyncPacket(int roleId, boolean easyMode) {
        this.roleId = roleId;
        this.easyMode = easyMode;
    }

    public static void encode(RoleSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.roleId);
        buf.writeBoolean(msg.easyMode);
    }

    public static RoleSyncPacket decode(FriendlyByteBuf buf) {
        return new RoleSyncPacket(buf.readByte(), buf.readBoolean());
    }

    public static void handle(RoleSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientRoleState.setEasyMode(msg.easyMode);
            ClientRoleState.set(Role.byId(msg.roleId));
        });
        ctx.get().setPacketHandled(true);
    }
}
