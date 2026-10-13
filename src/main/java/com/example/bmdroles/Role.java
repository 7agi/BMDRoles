package com.example.bmdroles;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum Role {
    BLIND(ChatFormatting.GRAY),
    DEAF(ChatFormatting.YELLOW),
    MUTE(ChatFormatting.RED);

    private final ChatFormatting color;

    Role(ChatFormatting color) {
        this.color = color;
    }

    /** Lower-case id used in commands and NBT. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        String id = id();
        return Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }

    public ChatFormatting color() {
        return color;
    }

    /** Blind -> Deaf -> Mute -> Blind. */
    public Role next() {
        Role[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    @Nullable
    public static Role byId(int id) {
        Role[] all = values();
        return id >= 0 && id < all.length ? all[id] : null;
    }

    @Nullable
    public static Role byName(String name) {
        for (Role role : values()) {
            if (role.id().equalsIgnoreCase(name)) {
                return role;
            }
        }
        return null;
    }
}
