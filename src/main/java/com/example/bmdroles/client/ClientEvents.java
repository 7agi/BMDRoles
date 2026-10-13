package com.example.bmdroles.client;

import com.example.bmdroles.BmdRoles;
import com.example.bmdroles.ClientRoleState;
import com.example.bmdroles.Role;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only effects: Blind visuals and Deaf game sounds. */
@Mod.EventBusSubscriber(modid = BmdRoles.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    /** Normal mode: grayscale pixel grid + short view distance. */
    private static final ResourceLocation BLIND_EFFECT =
            new ResourceLocation(BmdRoles.MOD_ID, "shaders/post/blind.json");
    /** Easy mode: just blurred vision. */
    private static final ResourceLocation BLIND_EASY_EFFECT =
            new ResourceLocation(BmdRoles.MOD_ID, "shaders/post/blind_easy.json");

    /** How far (in blocks) a blind player can make anything out (normal mode). */
    private static final float BLIND_FOG_FAR = 14.0F;
    private static final float FOG_GRAY = 0.10F;

    private static PostChain blindChain;
    private static ResourceLocation activeEffect;
    private static boolean wasDeaf;
    private static boolean wasEasy;
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
        boolean easy = ClientRoleState.isEasyMode();
        GameRenderer renderer = mc.gameRenderer;

        // ---- Blind: post effect (grid in normal mode, blur in easy mode) ----
        boolean blind = role == Role.BLIND && mc.level != null;
        if (blind) {
            ResourceLocation wanted = easy ? BLIND_EASY_EFFECT : BLIND_EFFECT;
            if (activeEffect != null && !activeEffect.equals(wanted)) {
                dropBlindEffect(renderer); // easy mode was toggled
            }
            if (retryCooldown > 0) {
                retryCooldown--;
            }
            if (renderer.currentEffect() == null && retryCooldown == 0) {
                renderer.loadEffect(wanted);
                activeEffect = wanted;
                blindChain = renderer.currentEffect();
                if (blindChain == null) {
                    retryCooldown = 200; // shader failed to load, don't spam
                }
            }
        } else if (activeEffect != null) {
            dropBlindEffect(renderer);
        }

        // ---- Deaf: cut everything that is already playing so new sounds go through our filter ----
        boolean deaf = role == Role.DEAF;
        if (deaf && (!wasDeaf || easy != wasEasy)) {
            mc.getSoundManager().stop();
        }
        wasDeaf = deaf;
        wasEasy = easy;
    }

    private static void dropBlindEffect(GameRenderer renderer) {
        if (blindChain != null && renderer.currentEffect() == blindChain) {
            renderer.shutdownEffect();
        }
        blindChain = null;
        activeEffect = null;
        retryCooldown = 0;
    }

    /** Normal mode only: blind players only see what is close to them. */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (ClientRoleState.get() != Role.BLIND || ClientRoleState.isEasyMode()
                || event.getType() != FogType.NONE) {
            return; // keep vanilla water/lava/powder snow fog
        }
        event.setNearPlaneDistance(0.0F);
        event.setFarPlaneDistance(BLIND_FOG_FAR);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true); // required for the values above to be used
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (ClientRoleState.get() != Role.BLIND || ClientRoleState.isEasyMode()) {
            return;
        }
        event.setRed(FOG_GRAY);
        event.setGreen(FOG_GRAY);
        event.setBlue(FOG_GRAY);
    }

    /**
     * Deaf players: normal mode hears no Minecraft sounds at all. Easy mode hears everything
     * quieter and duller; music and jukebox records stay silent.
     */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (ClientRoleState.get() != Role.DEAF) {
            return;
        }
        SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }
        if (!ClientRoleState.isEasyMode()
                || sound.getSource() == SoundSource.MUSIC
                || sound.getSource() == SoundSource.RECORDS) {
            event.setSound(null);
        } else if (!(sound instanceof MuffledSound)) {
            event.setSound(MuffledSound.wrap(sound));
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientRoleState.set(null);
        ClientRoleState.setEasyMode(false);
    }
}
