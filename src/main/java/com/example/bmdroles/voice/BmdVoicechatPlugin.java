package com.example.bmdroles.voice;

import com.example.bmdroles.BmdRoles;
import com.example.bmdroles.Role;
import com.example.bmdroles.RoleManager;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;

/**
 * Simple Voice Chat integration. Only loaded by Simple Voice Chat itself, so the mod
 * still works (Blind visuals, Deaf game sounds) when Simple Voice Chat isn't installed.
 *
 * Mute: the microphone packets of muted players are dropped.
 * Deaf: no voice packets (proximity, static/whisper, group) are delivered to deaf players.
 */
@ForgeVoicechatPlugin
public class BmdVoicechatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return BmdRoles.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
        registration.registerEvent(EntitySoundPacketEvent.class, this::onSound);
        registration.registerEvent(LocationalSoundPacketEvent.class, this::onSound);
        registration.registerEvent(StaticSoundPacketEvent.class, this::onSound);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || sender.getPlayer() == null) {
            return;
        }
        if (RoleManager.has(sender.getPlayer().getUuid(), Role.MUTE)) {
            event.cancel();
        }
    }

    private void onSound(SoundPacketEvent<?> event) {
        VoicechatConnection receiver = event.getReceiverConnection();
        if (receiver == null || receiver.getPlayer() == null) {
            return;
        }
        if (RoleManager.has(receiver.getPlayer().getUuid(), Role.DEAF)) {
            event.cancel();
        }
    }
}
