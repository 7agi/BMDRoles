package com.example.bmdroles.voice;

import com.example.bmdroles.BmdRoles;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns clean voice into "muffled and blurry" voice (easy mode):
 * decode opus -> 3-pole low-pass (~470 Hz, "through a wall") -> short smeared echo ("blur") -> encode opus.
 *
 * Opus codecs and filters are stateful, so there is one set per (sender, receiver) pair.
 * Tune the constants below to taste.
 */
final class VoiceMuffler {
    /** Low-pass strength. Smaller = more muffled (0.06 is roughly a 470 Hz cutoff at 48 kHz). */
    private static final float LOWPASS_ALPHA = 0.06F;
    /** Compensates the volume the low-pass removes. */
    private static final float OUTPUT_GAIN = 1.6F;
    /** Echo delay in samples (1920 = 40 ms at 48 kHz). */
    private static final int ECHO_DELAY = 1920;
    private static final float ECHO_GAIN = 0.40F;
    private static final float ECHO_FEEDBACK = 0.35F;

    private static final UUID NOBODY = new UUID(0L, 0L);

    private record Key(UUID sender, UUID receiver) {
    }

    private static final class Channel {
        final OpusDecoder decoder;
        final OpusEncoder encoder;
        final float[] delay = new float[ECHO_DELAY];
        float lp1, lp2, lp3;
        int pos;

        Channel(VoicechatServerApi api) {
            this.decoder = api.createDecoder();
            this.encoder = api.createEncoder();
        }

        byte[] process(byte[] opus) {
            short[] pcm = decoder.decode(opus);
            for (int i = 0; i < pcm.length; i++) {
                lp1 += LOWPASS_ALPHA * (pcm[i] - lp1);
                lp2 += LOWPASS_ALPHA * (lp1 - lp2);
                lp3 += LOWPASS_ALPHA * (lp2 - lp3);
                float y = lp3 * OUTPUT_GAIN;

                float echo = delay[pos];
                float out = y + echo * ECHO_GAIN;
                delay[pos] = y + echo * ECHO_FEEDBACK;
                pos = (pos + 1) % ECHO_DELAY;

                pcm[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(out)));
            }
            return encoder.encode(pcm);
        }

        void close() {
            try {
                decoder.close();
                encoder.close();
            } catch (Exception ignored) {
            }
        }
    }

    private final Map<Key, Channel> channels = new ConcurrentHashMap<>();

    /** @return the muffled opus data, or null if processing failed. */
    @Nullable
    byte[] process(VoicechatServerApi api, @Nullable UUID sender, @Nullable UUID receiver, byte[] opus) {
        Channel channel = channels.computeIfAbsent(
                new Key(sender == null ? NOBODY : sender, receiver == null ? NOBODY : receiver),
                k -> new Channel(api));
        synchronized (channel) {
            try {
                return channel.process(opus);
            } catch (Exception e) {
                BmdRoles.LOGGER.warn("Failed to muffle a voice packet", e);
                return null;
            }
        }
    }

    void remove(UUID player) {
        channels.entrySet().removeIf(e -> {
            if (e.getKey().sender().equals(player) || e.getKey().receiver().equals(player)) {
                e.getValue().close();
                return true;
            }
            return false;
        });
    }

    void clear() {
        channels.values().forEach(Channel::close);
        channels.clear();
    }
}
