package com.example.bmdroles.client;

import com.example.bmdroles.BmdRoles;
import com.example.bmdroles.ClientRoleState;
import com.example.bmdroles.Role;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

/** Client-only effects: Blind visuals and Deaf game sounds (+ a few debug commands). */
@Mod.EventBusSubscriber(modid = BmdRoles.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    /** Normal mode: grayscale pixel grid + short view distance. */
    private static final ResourceLocation BLIND_EFFECT =
            new ResourceLocation(BmdRoles.MOD_ID, "shaders/post/blind.json");
    /** Easy mode: just blurred vision. */
    private static final ResourceLocation BLIND_EASY_EFFECT =
            new ResourceLocation(BmdRoles.MOD_ID, "shaders/post/blind_easy.json");

    /** Normal mode fog (in blocks): clear up to FOG_NEAR, fully fogged at FOG_FAR. */
    private static final float FOG_NEAR = 2.0F;
    private static final float FOG_FAR = 24.0F;
    /** Fog / sky color for blind players (mid-dark gray, NOT black). */
    private static final float FOG_GRAY = 0.30F;

    // debug switches, see /bmdclient
    private static volatile boolean debugFog = true;
    private static volatile boolean debugShader = true;

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
        boolean blind = role == Role.BLIND && mc.level != null && debugShader;
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
                    BmdRoles.LOGGER.warn("Blind post effect {} FAILED to load, check the log above for the shader error", wanted);
                    retryCooldown = 200; // don't spam
                } else {
                    BmdRoles.LOGGER.info("Blind post effect {} loaded", wanted);
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
        if (!debugFog || ClientRoleState.get() != Role.BLIND || ClientRoleState.isEasyMode()
                || event.getType() != FogType.NONE) {
            return; // keep vanilla water/lava/powder snow fog
        }
        event.setNearPlaneDistance(FOG_NEAR);
        event.setFarPlaneDistance(FOG_FAR);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true); // required for the values above to be used
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!debugFog || ClientRoleState.get() != Role.BLIND || ClientRoleState.isEasyMode()) {
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

    // ------------------------------------------------------------------
    // Debug commands (client side, work for everybody):
    //   /bmdclient status
    //   /bmdclient fog on|off      blind fog + fog color
    //   /bmdclient shader on|off   blind post-processing shader
    //   /bmdclient vanilla desaturate|blur|off   test a vanilla post effect
    // ------------------------------------------------------------------
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bmdclient")
                .then(Commands.literal("status").executes(ClientEvents::status))
                .then(Commands.literal("vanilla")
                        .then(Commands.literal("desaturate").executes(ctx -> loadVanilla(ctx, "desaturate")))
                        .then(Commands.literal("blur").executes(ctx -> loadVanilla(ctx, "blur")))
                        .then(Commands.literal("off").executes(ClientEvents::unloadVanilla)))
                .then(Commands.literal("fog")
                        .then(Commands.literal("on").executes(ctx -> {
                            debugFog = true;
                            return say(ctx, "Blind fog: ON");
                        }))
                        .then(Commands.literal("off").executes(ctx -> {
                            debugFog = false;
                            return say(ctx, "Blind fog: OFF");
                        })))
                .then(Commands.literal("shader")
                        .then(Commands.literal("on").executes(ctx -> {
                            debugShader = true;
                            return say(ctx, "Blind shader: ON");
                        }))
                        .then(Commands.literal("off").executes(ctx -> {
                            debugShader = false;
                            return say(ctx, "Blind shader: OFF");
                        }))));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        Minecraft mc = Minecraft.getInstance();
        StringBuilder renderMods = new StringBuilder();
        for (String id : new String[]{"oculus", "iris", "embeddium", "rubidium", "sodium", "canvas", "optifine"}) {
            if (ModList.get().isLoaded(id)) {
                renderMods.append(id).append(' ');
            }
        }
        return say(ctx, "role=" + ClientRoleState.get()
                + " easy=" + ClientRoleState.isEasyMode()
                + " shaderLoaded=" + (mc.gameRenderer.currentEffect() != null)
                + " activeEffect=" + activeEffect
                + " fog=" + debugFog + " shader=" + debugShader
                + " renderMods=[" + renderMods.toString().trim() + "]");
    }

    /** Loads one of Minecraft's own post effects, to check whether post-processing works at all on this setup. */
    private static int loadVanilla(CommandContext<CommandSourceStack> ctx, String name) {
        GameRenderer renderer = Minecraft.getInstance().gameRenderer;
        renderer.loadEffect(new ResourceLocation("minecraft", "shaders/post/" + name + ".json"));
        return say(ctx, "Vanilla effect '" + name + "' loaded: " + (renderer.currentEffect() != null));
    }

    private static int unloadVanilla(CommandContext<CommandSourceStack> ctx) {
        Minecraft.getInstance().gameRenderer.shutdownEffect();
        return say(ctx, "Vanilla effect off");
    }

    private static int say(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
