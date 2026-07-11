package com.zexqm.rpgproject.registry;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RpgProject.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rpg_project.main"))
                    .icon(() -> ModItems.MANA_CRYSTAL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MANA_CRYSTAL.get());
                        output.accept(ModItems.RUBY.get());
                        output.accept(ModItems.SAPPHIRE.get());
                        output.accept(ModItems.TOPAZ.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
