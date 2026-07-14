package com.zexqm.rpgproject.rpg.skill;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

final class SkillDebugVisualizer {
    static void show(SkillDefinition skill, SkillDefinition.Hit hit, SkillExecutionContext context,
                     Set<LivingEntity> targets) {
        if (!skill.debugOnly() || !(context.caster().level() instanceof ServerLevel level)) return;
        Vec3 origin = context.origin();
        Vec3 end = origin.add(context.direction().scale(skill.range()));
        switch (skill.targeting()) {
            case RAY, AIM_PROJECTILE, LINE -> line(level, origin, end, 0.5);
            case CONE -> cone(level, origin, context.direction(), skill.range());
            case SELF_AOE -> circle(level, context.caster().position(), hitRadius(skill, hit));
            case GROUND_AOE, CIRCLE -> circle(level,
                    context.groundPosition() == null ? end : context.groundPosition(), hitRadius(skill, hit));
            case ENTITY_TARGETED -> line(level, origin,
                    targets.isEmpty() ? end : targets.iterator().next().getEyePosition(), 0.4);
            case MOVEMENT -> line(level, context.caster().position(),
                    context.caster().position().add(context.direction().scale(skill.range())), 0.2);
        }
        for (LivingEntity target : targets) {
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(), 14, target.getBbWidth() * 0.4, target.getBbHeight() * 0.25,
                    target.getBbWidth() * 0.4, 0.1);
        }
        context.caster().displayClientMessage(Component.literal("[Debug Skill] " + skill.id().getPath()
                + " hit " + targets.size() + " target(s)"), true);
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
