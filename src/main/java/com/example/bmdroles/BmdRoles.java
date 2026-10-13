package com.example.bmdroles;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(BmdRoles.MOD_ID)
public class BmdRoles {
    public static final String MOD_ID = "bmdroles";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BmdRoles() {
        NetworkHandler.init();
    }
}
