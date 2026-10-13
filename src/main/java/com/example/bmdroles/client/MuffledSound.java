package com.example.bmdroles.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Wraps any sound and makes it quieter and lower pitched (easy mode "deaf").
 * Everything else is forwarded to the original sound instance.
 */
public class MuffledSound implements SoundInstance {
    /** Tune to taste: lower volume = quieter, lower pitch = duller / more blurry. */
    private static final float VOLUME_FACTOR = 0.35F;
    private static final float PITCH_FACTOR = 0.8F;

    protected final SoundInstance delegate;

    protected MuffledSound(SoundInstance delegate) {
        this.delegate = delegate;
    }

    public static SoundInstance wrap(SoundInstance sound) {
        if (sound instanceof TickableSoundInstance tickable) {
            return new Tickable(tickable);
        }
        return new MuffledSound(sound);
    }

    @Override
    public ResourceLocation getLocation() {
        return delegate.getLocation();
    }

    @Nullable
    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        return delegate.resolve(manager);
    }

    @Override
    public net.minecraft.client.resources.sounds.Sound getSound() {
        return delegate.getSound();
    }

    @Override
    public SoundSource getSource() {
        return delegate.getSource();
    }

    @Override
    public boolean isLooping() {
        return delegate.isLooping();
    }

    @Override
    public boolean isRelative() {
        return delegate.isRelative();
    }

    @Override
    public int getDelay() {
        return delegate.getDelay();
    }

    @Override
    public float getVolume() {
        return delegate.getVolume() * VOLUME_FACTOR;
    }

    @Override
    public float getPitch() {
        return Mth.clamp(delegate.getPitch() * PITCH_FACTOR, 0.5F, 2.0F);
    }

    @Override
    public double getX() {
        return delegate.getX();
    }

    @Override
    public double getY() {
        return delegate.getY();
    }

    @Override
    public double getZ() {
        return delegate.getZ();
    }

    @Override
    public Attenuation getAttenuation() {
        return delegate.getAttenuation();
    }

    /** Variant for sounds that update themselves every tick (minecarts, elytra, ...). */
    private static final class Tickable extends MuffledSound implements TickableSoundInstance {
        private final TickableSoundInstance tickable;

        private Tickable(TickableSoundInstance tickable) {
            super(tickable);
            this.tickable = tickable;
        }

        @Override
        public boolean isStopped() {
            return tickable.isStopped();
        }

        @Override
        public void tick() {
            tickable.tick();
        }
    }
}
