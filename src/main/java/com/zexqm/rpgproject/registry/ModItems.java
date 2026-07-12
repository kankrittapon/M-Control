package com.zexqm.rpgproject.registry;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.zexqm.rpgproject.rpg.*;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RpgProject.MOD_ID);

    public static final RegistryObject<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RUBY = ITEMS.register("ruby", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SAPPHIRE = ITEMS.register("sapphire", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TOPAZ = ITEMS.register("topaz", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WIZARD_STAFF = weapon("wizard_staff", RpgClass.WIZARD, WeaponSlot.MAIN, WeaponType.WIZARD_STAFF);
    public static final RegistryObject<Item> WIZARD_DAGGER = weapon("wizard_dagger", RpgClass.WIZARD, WeaponSlot.SUB, WeaponType.WIZARD_DAGGER);
    public static final RegistryObject<Item> WIZARD_SPHERA = weapon("wizard_sphera", RpgClass.WIZARD, WeaponSlot.AWAKENING, WeaponType.WIZARD_SPHERA);
    public static final RegistryObject<Item> NINJA_SHORTSWORD = weapon("ninja_shortsword", RpgClass.NINJA, WeaponSlot.MAIN, WeaponType.NINJA_SHORTSWORD);
    public static final RegistryObject<Item> NINJA_SHURIKEN = weapon("ninja_shuriken", RpgClass.NINJA, WeaponSlot.SUB, WeaponType.NINJA_SHURIKEN);
    public static final RegistryObject<Item> NINJA_SURA_KATANA = weapon("ninja_sura_katana", RpgClass.NINJA, WeaponSlot.AWAKENING, WeaponType.NINJA_SURA_KATANA);

    private static RegistryObject<Item> weapon(String id, RpgClass cls, WeaponSlot slot, WeaponType type) {
        return ITEMS.register(id, () -> new RpgWeaponItem(cls, slot, type, new Item.Properties().durability(500)));
    }

    private ModItems() {
    }
}
