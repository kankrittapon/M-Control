package com.zexqm.rpgproject.rpg.skill;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

final class SkillDebugVisualizer {
    private static final String METEOR_SHOWER = "wizard_meteor_shower";
    private static final String FRIGID_FOG = "wizard_frigid_fog";
    private static final String BLIZZARD = "wizard_blizzard";
    private static final String LIGHTNING_STORM = "wizard_lightning_storm";
    private static final String RESIDUAL_LIGHTNING = "wizard_residual_lightning";

    static void show(SkillDefinition skill, SkillDefinition.Hit hit, SkillExecutionContext context,
                     Set<LivingEntity> targets) {
        boolean meteorPlaceholder = skill.id().getPath().equals(METEOR_SHOWER);
        boolean frigidFogPlaceholder = skill.id().getPath().equals(FRIGID_FOG);
        boolean blizzardPlaceholder = skill.id().getPath().equals(BLIZZARD);
        boolean lightningStormPlaceholder = skill.id().getPath().equals(LIGHTNING_STORM);
        boolean residualLightningPlaceholder = skill.id().getPath().equals(RESIDUAL_LIGHTNING);
        if ((!skill.debugOnly() && !meteorPlaceholder && !frigidFogPlaceholder && !blizzardPlaceholder
                && !lightningStormPlaceholder && !residualLightningPlaceholder)
                || !(context.caster().level() instanceof ServerLevel level)) return;
        Vec3 origin = context.origin();
        Vec3 end = origin.add(context.direction().scale(skill.range()));
        if (residualLightningPlaceholder) {
            Vec3 impact = context.groundPosition() == null ? end : context.groundPosition();
            lightningStormImpact(level, impact, hitRadius(skill, hit));
        } else if (lightningStormPlaceholder) {
            lightningStormImpact(level, context.caster().position(), hitRadius(skill, hit));
        } else if (blizzardPlaceholder) {
            blizzardImpact(level, context.caster().position(), hitRadius(skill, hit));
        } else if (frigidFogPlaceholder) {
            frigidFogImpact(level, context.caster().position(), hitRadius(skill, hit));
        } else if (meteorPlaceholder) {
            Vec3 impact = groundImpact(skill, hit, context, end);
            if (hit.timingTick() >= skill.castTicks() - 3)
                meteorImpact(level, impact, hitRadius(skill, hit));
            else
                meteorOpeningImpact(level, impact, hitRadius(skill, hit));
        } else {
        switch (skill.targeting()) {
            case RAY, AIM_PROJECTILE, LINE -> line(level, origin, end, 0.5);
            case CONE -> cone(level, origin, context.direction(), skill.range());
            case SELF_AOE -> circle(level, context.caster().position(), hitRadius(skill, hit));
            case GROUND_AOE, CIRCLE -> circle(level,
                    context.groundPosition() == null ? end : context.groundPosition(), hitRadius(skill, hit));
            case ENTITY_TARGETED -> line(level, origin,
                    targets.isEmpty() ? end : targets.iterator().next().getEyePosition(), 0.4);
            case CHAIN -> {
                Vec3 previous = origin;
                for (LivingEntity target : targets) {
                    Vec3 next = target.getEyePosition();
                    line(level, previous, next, 0.25);
                    previous = next;
                }
            }
            case MOVEMENT -> line(level, context.caster().position(),
                    context.caster().position().add(context.direction().scale(skill.range())), 0.2);
        }
        }
        for (LivingEntity target : targets) {
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(), 14, target.getBbWidth() * 0.4, target.getBbHeight() * 0.25,
                    target.getBbWidth() * 0.4, 0.1);
        }
        context.caster().displayClientMessage(Component.literal("[Debug Skill] " + skill.id().getPath()
                + " hit " + targets.size() + " target(s)"), true);
    }

    private static Vec3 groundImpact(SkillDefinition skill, SkillDefinition.Hit hit,
                                     SkillExecutionContext context, Vec3 fallback) {
        Vec3 center = context.groundPosition() == null ? fallback : context.groundPosition();
        Vec3 forward = new Vec3(context.direction().x, 0, context.direction().z);
        if (forward.lengthSqr() < 1.0E-6) forward = new Vec3(0, 0, 1); else forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        return center.add(forward.scale(hit.forwardOffset())).add(right.scale(hit.rightOffset()));
    }

    private static void meteorImpact(ServerLevel level, Vec3 impact, double radius) {
        Vec3 sky = impact.add(0, 14, 0);
        Vec3 delta = impact.subtract(sky);
        int steps = 28;
        for (int i = 0; i <= steps; i++) {
            Vec3 point = sky.add(delta.scale(i / (double) steps));
            level.sendParticles(i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME,
                    point.x, point.y, point.z, 1, 0.08, 0.08, 0.08, 0.01);
        }
        circle(level, impact, radius);
        level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 0.35, impact.z,
                6, radius * 0.2, 0.15, radius * 0.2, 0.02);
        level.sendParticles(ParticleTypes.FLAME, impact.x, impact.y + 0.3, impact.z,
                36, radius * 0.45, 0.2, radius * 0.45, 0.04);
    }

    private static void meteorOpeningImpact(ServerLevel level, Vec3 impact, double radius) {
        circle(level, impact, radius);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, impact.x, impact.y + 0.35, impact.z,
                28, radius * 0.4, 0.15, radius * 0.4, 0.08);
        level.sendParticles(ParticleTypes.WITCH, impact.x, impact.y + 0.5, impact.z,
                18, radius * 0.25, 0.35, radius * 0.25, 0.04);
    }

    private static void frigidFogImpact(ServerLevel level, Vec3 center, double radius) {
        circle(level, center, radius);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.7, center.z,
                48, radius * 0.6, 0.6, radius * 0.6, 0.02);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.25, center.z,
                28, radius * 0.55, 0.15, radius * 0.55, 0.01);
    }

    private static void blizzardImpact(ServerLevel level, Vec3 center, double radius) {
        circle(level, center, radius);
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1.5, center.z,
                90, radius * 0.75, 1.5, radius * 0.75, 0.08);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.5, center.z,
                45, radius * 0.7, 0.4, radius * 0.7, 0.03);
    }

    private static void lightningStormImpact(ServerLevel level, Vec3 center, double radius) {
        circle(level, center, radius);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.8, center.z,
                64, radius * 0.7, 0.8, radius * 0.7, 0.18);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 1.0, center.z,
                2, radius * 0.25, 0.4, radius * 0.25, 0.0);
    }

    private static void line(ServerLevel level, Vec3 start, Vec3 end, double spacing) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(1, (int) Math.ceil(delta.length() / spacing));
        for (int i = 0; i <= steps; i++) particle(level, start.add(delta.scale(i / (double) steps)));
    }

    private static void circle(ServerLevel level, Vec3 center, double radius) {
        if (radius <= 0) return;
        int points = Math.max(16, (int) Math.ceil(radius * 12));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            particle(level, center.add(Math.cos(angle) * radius, 0.15, Math.sin(angle) * radius));
        }
        level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.2, center.z,
                12, radius * 0.35, 0.05, radius * 0.35, 0.02);
    }

    private static void cone(ServerLevel level, Vec3 origin, Vec3 direction, double range) {
        Vec3 flat = new Vec3(direction.x, 0, direction.z);
        if (flat.lengthSqr() < 1.0E-6) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        line(level, origin, origin.add(rotate(flat, -45).scale(range)), 0.5);
        line(level, origin, origin.add(flat.scale(range)), 0.5);
        line(level, origin, origin.add(rotate(flat, 45).scale(range)), 0.5);
        Vec3 center = origin.add(flat.scale(range));
        circle(level, center, Math.sin(Math.toRadians(45)) * range);
    }

    private static Vec3 rotate(Vec3 value, double degrees) {
        double angle = Math.toRadians(degrees);
        return new Vec3(value.x * Math.cos(angle) - value.z * Math.sin(angle), 0,
                value.x * Math.sin(angle) + value.z * Math.cos(angle));
    }

    private static double hitRadius(SkillDefinition skill, SkillDefinition.Hit hit) {
        return hit.radius() > 0 ? hit.radius() : skill.radius();
    }

    private static void particle(ServerLevel level, Vec3 position) {
        level.sendParticles(ParticleTypes.END_ROD, position.x, position.y, position.z,
                1, 0, 0, 0, 0);
    }

    private SkillDebugVisualizer() {}
}
