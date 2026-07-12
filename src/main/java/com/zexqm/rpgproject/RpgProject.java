package com.zexqm.rpgproject;

import com.mojang.logging.LogUtils;
import com.zexqm.rpgproject.network.RpgNetwork;
import com.zexqm.rpgproject.registry.ModCreativeTabs;
import com.zexqm.rpgproject.registry.ModItems;
import com.zexqm.rpgproject.registry.ModAttributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RpgProject.MOD_ID)
public class RpgProject {
    public static final String MOD_ID = "rpg_project";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpgProject() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModAttributes.ATTRIBUTES.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
        RpgNetwork.register();
    }
}
