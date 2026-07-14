package com.zexqm.rpgproject.world;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RpgProject.MOD_ID)
public final class MobLevelSystem {
    public static final String LEVEL_TAG = "rpg_project.mob_level";
    private static final UUID HEALTH_ID = UUID.fromString("2fc394ab-5672-4e4c-b03a-742c58ab3901");
    private static final UUID DAMAGE_ID = UUID.fromString("2777fa43-23cc-4ac2-9a8e-e452fbbd113e");

    @SubscribeEvent
    public static void entityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
        if (mob.getPersistentData().contains(LEVEL_TAG)) return;
        int mobLevel = calculateLevel(level, mob);
        mob.getPersistentData().putInt(LEVEL_TAG, mobLevel);
        apply(mob, mobLevel);
    }

    private static int calculateLevel(ServerLevel level, Mob mob) {
        ResourceKey<Level> dimension = level.dimension();
        int base = dimension == Level.NETHER ? 30 : dimension == Level.END ? 60 : 1;
        int cap = dimension == Level.OVERWORLD ? 60 : dimension == Level.NETHER ? 80 : 100;
        double distance = Math.sqrt(mob.blockPosition().distSqr(level.getSharedSpawnPos()));
        int result = base + (int) (distance / 256.0) + mob.getRandom().nextInt(5) - 2;
        return Math.max(1, Math.min(cap, result));
    }

    private static void apply(Mob mob, int level) {
        addModifier(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, "RPG mob level health", (level - 1) * 0.045);
        addModifier(mob.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, "RPG mob level damage", (level - 1) * 0.025);
        mob.setHealth(mob.getMaxHealth());
    }

    private static void addModifier(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null || attribute.getModifier(id) != null) return;
        attribute.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_BASE));
    }
    public static int level(Entity entity) {
        return Math.max(1, entity.getPersistentData().getInt(LEVEL_TAG));
    }
    private MobLevelSystem() {}
}
