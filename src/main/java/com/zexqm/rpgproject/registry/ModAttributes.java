package com.zexqm.rpgproject.registry;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.*;

public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, RpgProject.MOD_ID);
    public static final RegistryObject<Attribute> ATTACK_POWER = stat("attack_power", 0, 100000);
    public static final RegistryObject<Attribute> MAGIC_POWER = stat("magic_power", 0, 100000);
    public static final RegistryObject<Attribute> DEFENSE = stat("defense", 0, 100000);
    public static final RegistryObject<Attribute> DAMAGE_REDUCTION = stat("damage_reduction", 0, 0.80);
    public static final RegistryObject<Attribute> ACCURACY = stat("accuracy", 0, 100000);
    public static final RegistryObject<Attribute> EVASION = stat("evasion", 0, 100000);
    public static final RegistryObject<Attribute> CRITICAL_CHANCE = stat("critical_chance", 0, 1);
    public static final RegistryObject<Attribute> CRITICAL_DAMAGE = stat("critical_damage", 1, 10);
    public static final RegistryObject<Attribute> CAST_SPEED = stat("cast_speed", 0.1, 10);
    public static final RegistryObject<Attribute> CC_RESISTANCE = stat("cc_resistance", 0, 1);

    private static RegistryObject<Attribute> stat(String id, double min, double max) {
        return ATTRIBUTES.register(id, () -> new RangedAttribute("attribute.rpg_project." + id, min, min, max).setSyncable(true));
    }
    private ModAttributes() {}
}
