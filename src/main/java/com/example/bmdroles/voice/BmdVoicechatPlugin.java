package com.example.bmdroles.voice;

import com.example.bmdroles.BmdRoles;
import com.example.bmdroles.Role;
import com.example.bmdroles.RoleManager;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.packets.EntitySoundPacket;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Simple Voice Chat integration. Only loaded by Simple Voice Chat itself, so the mod
 * still works (Blind visuals, Deaf game sounds) when Simple Voice Chat isn't installed.
 *
 * Normal mode:
 *   Mute - the microphone packets of muted players are dropped.
 *   Deaf - no voice packets (proximity, static/whisper, group) are delivered to deaf players.
 * Easy mode:
 *   Mute - their voice is still sent, but muffled and blurry.
 *   Deaf - they still receive voices, but muffled and blurry.
 */
@ForgeVoicechatPlugin
public class BmdVoicechatPlugin implements VoicechatPlugin {
    /** Set while we re-send a processed packet, so our own handler ignores it. */
    private static final ThreadLocal<Boolean> RESENDING = ThreadLocal.withInitial(() -> false);

    private final VoiceMuffler muffler = new VoiceMuffler();
    private volatile VoicechatServerApi serverApi;

    @Override
    public String getPluginId() {
        return BmdRoles.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, e -> serverApi = e.getVoicechat());
        registration.registerEvent(VoicechatServerStoppedEvent.class, e -> {
            serverApi = null;
            muffler.clear();
        });
        registration.registerEvent(PlayerDisconnectedEvent.class, e -> muffler.remove(e.getPlayerUuid()));

        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
        registration.registerEvent(EntitySoundPacketEvent.class, this::onEntitySound);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::onLocationalSound);
        registration.registerEvent(StaticSoundPacketEvent.class, this::onStaticSound);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        if (RoleManager.isEasyMode()) {
            return; // easy mode: muted players are processed later, per receiver
        }
        UUID sender = uuidOf(event.getSenderConnection());
        if (sender != null && RoleManager.has(sender, Role.MUTE)) {
            event.cancel();
        }
    }

    private void onEntitySound(EntitySoundPacketEvent event) {
        EntitySoundPacket packet = event.getPacket();
        handleSound(event, packet.getOpusEncodedData(), data ->
                serverApi.sendEntitySoundPacketTo(event.getReceiverConnection(),
                        packet.entitySoundPacketBuilder().opusEncodedData(data).build()));
    }

    private void onLocationalSound(LocationalSoundPacketEvent event) {
        LocationalSoundPacket packet = event.getPacket();
        handleSound(event, packet.getOpusEncodedData(), data ->
                serverApi.sendLocationalSoundPacketTo(event.getReceiverConnection(),
                        packet.locationalSoundPacketBuilder().opusEncodedData(data).build()));
    }

    private void onStaticSound(StaticSoundPacketEvent event) {
        StaticSoundPacket packet = event.getPacket();
        handleSound(event, packet.getOpusEncodedData(), data ->
                serverApi.sendStaticSoundPacketTo(event.getReceiverConnection(),
                        packet.staticSoundPacketBuilder().opusEncodedData(data).build()));
    }

    private void handleSound(SoundPacketEvent<?> event, byte[] opus, Consumer<byte[]> resend) {
        if (RESENDING.get()) {
            return;
        }
        UUID sender = uuidOf(event.getSenderConnection());
        UUID receiver = uuidOf(event.getReceiverConnection());
        boolean receiverDeaf = receiver != null && RoleManager.has(receiver, Role.DEAF);
        boolean senderMute = sender != null && RoleManager.has(sender, Role.MUTE);
        if (!receiverDeaf && !senderMute) {
            return;
        }

        if (!RoleManager.isEasyMode()) {
            if (receiverDeaf) {
                event.cancel();
            }
            return;
        }

        // ---- easy mode: muffle instead of block ----
        VoicechatServerApi api = serverApi;
        if (api == null || opus == null || opus.length == 0) {
            return; // an empty packet just marks the end of a transmission, let it through
        }
        byte[] muffled = muffler.process(api, sender, receiver, opus);
        event.cancel(); // never let the clean audio through, even if processing failed
        if (muffled == null) {
            return;
        }
        RESENDING.set(true);
        try {
            resend.accept(muffled);
        } finally {
            RESENDING.set(false);
        }
    }

    private static UUID uuidOf(VoicechatConnection connection) {
        if (connection == null || connection.getPlayer() == null) {
            return null;
        }
        return connection.getPlayer().getUuid();
    }
}
