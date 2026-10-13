package com.example.bmdroles.client;

import com.example.bmdroles.BmdRoles;
import com.example.bmdroles.ClientRoleState;
import com.example.bmdroles.Role;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only effects: Blind visuals and Deaf game-sound silencing. */
@Mod.EventBusSubscriber(modid = BmdRoles.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final ResourceLocation BLIND_EFFECT =
            new ResourceLocation(BmdRoles.MOD_ID, "shaders/post/blind.json");

    /** How far (in blocks) a blind player can make anything out. */
    private static final float BLIND_FOG_FAR = 14.0F;
    private static final float FOG_GRAY = 0.10F;

    private static PostChain blindChain;
    private static boolean wasDeaf;
    private static int retryCooldown;

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Role role = ClientRoleState.get();
        GameRenderer renderer = mc.gameRenderer;

        // ---- Blind: grayscale / pixel-grid post effect ----
        boolean blind = role == Role.BLIND && mc.level != null;
        if (blind) {
            if (retryCooldown > 0) {
                retryCooldown--;
            }
            if (renderer.currentEffect() == null && retryCooldown == 0) {
                renderer.loadEffect(BLIND_EFFECT);
                blindChain = renderer.currentEffect();
                if (blindChain == null) {
                    retryCooldown = 200; // shader failed to load, don't spam
                }
            }
        } else if (blindChain != null) {
            if (renderer.currentEffect() == blindChain) {
                renderer.shutdownEffect();
            }
            blindChain = null;
            retryCooldown = 0;
        }

        // ---- Deaf: cut everything that is already playing ----
        boolean deaf = role == Role.DEAF;
        if (deaf && !wasDeaf) {
            mc.getSoundManager().stop();
        }
        wasDeaf = deaf;
    }

    /** Blind players only see what is close to them. */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (ClientRoleState.get() != Role.BLIND || event.getType() != FogType.NONE) {
            return; // keep vanilla water/lava/powder snow fog
        }
        event.setNearPlaneDistance(0.0F);
        event.setFarPlaneDistance(BLIND_FOG_FAR);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true); // required for the values above to be used
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (ClientRoleState.get() != Role.BLIND) {
            return;
        }
        event.setRed(FOG_GRAY);
        event.setGreen(FOG_GRAY);
        event.setBlue(FOG_GRAY);
    }

    /** Deaf players hear no Minecraft sounds at all (music, ambience, blocks, UI, ...). */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (ClientRoleState.get() == Role.DEAF) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientRoleState.set(null);
    }
}
