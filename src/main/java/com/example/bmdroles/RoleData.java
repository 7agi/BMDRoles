package com.example.bmdroles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Persistent storage of who has which role + the easy mode flag (saved in the world's data folder). */
public class RoleData extends SavedData {
    public static final String NAME = "bmdroles_roles";

    // Concurrent: the Simple Voice Chat server reads this from its own threads.
    final Map<UUID, Role> roles = new ConcurrentHashMap<>();
    volatile boolean easyMode = false;

    public static RoleData load(CompoundTag tag) {
        RoleData data = new RoleData();
        data.easyMode = tag.getBoolean("easyMode");
        ListTag list = tag.getList("roles", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Role role = Role.byName(entry.getString("role"));
            if (entry.hasUUID("id") && role != null) {
                data.roles.put(entry.getUUID("id"), role);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("easyMode", easyMode);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Role> e : roles.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", e.getKey());
            entry.putString("role", e.getValue().id());
            list.add(entry);
        }
        tag.put("roles", list);
        return tag;
    }
}
