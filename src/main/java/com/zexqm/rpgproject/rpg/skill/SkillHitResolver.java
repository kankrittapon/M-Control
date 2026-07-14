package com.zexqm.rpgproject.rpg.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Set;

public final class SkillHitResolver {
    public static Set<LivingEntity> resolve(SkillDefinition definition, SkillDefinition.Hit hit,
                                            SkillExecutionContext context) {
        ServerPlayerView view = new ServerPlayerView(context);
        if (!(context.caster().level() instanceof ServerLevel level)) return Set.of();
        double range = definition.range();
        double radius = hit.radius() > 0 ? hit.radius() : definition.radius();
        Vec3 end = context.origin().add(context.direction().scale(range));
        Set<LivingEntity> result = new LinkedHashSet<>();

        switch (definition.targeting()) {
            case ENTITY_TARGETED -> {
                if (context.targetEntityId() == null) return Set.of();
                if (level.getEntity(context.targetEntityId()) instanceof LivingEntity target
                        && target != context.caster() && target.isAlive()
                        && target.distanceToSqr(context.caster()) <= range * range) result.add(target);
            }
            case RAY, AIM_PROJECTILE -> {
                LivingEntity nearest = rayTarget(level, context, end, Math.max(0.0, radius));
                if (nearest != null) result.add(nearest);
            }
            case SELF_AOE -> collectCircle(level, context.caster(), context.caster().position(), radius, result);
            case GROUND_AOE, CIRCLE -> {
                Vec3 center = context.groundPosition() == null ? end : context.groundPosition();
                if (center.distanceToSqr(context.origin()) <= range * range)
                    collectCircle(level, context.caster(), center, radius, result);
            }
            case LINE -> {
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(context.origin(), end).inflate(radius), view::valid)) {
                    if (intersectsLine(target.getBoundingBox(), context.origin(), end, radius)) result.add(target);
                }
            }
            case CONE -> {
                double cosine = Math.cos(Math.toRadians(45.0));
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                        context.caster().getBoundingBox().inflate(range), view::valid)) {
                    Vec3 delta = target.position().subtract(context.origin());
                    if (delta.lengthSqr() <= range * range && delta.normalize().dot(context.direction()) >= cosine)
                        result.add(target);
                }
            }
            case MOVEMENT -> {
                context.caster().setDeltaMovement(context.direction().x * range,
                        context.caster().getDeltaMovement().y, context.direction().z * range);
                context.caster().hurtMarked = true;
            }
        }
        return result;
    }

    private static void collectCircle(ServerLevel level, LivingEntity caster, Vec3 center, double radius,
                                      Set<LivingEntity> result) {
        result.addAll(level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius),
                target -> target != caster && target.isAlive() && target.distanceToSqr(center) <= radius * radius));
    }

    static boolean intersectsLine(AABB targetBounds, Vec3 start, Vec3 end, double radius) {
        AABB hitbox = radius > 0.0 ? targetBounds.inflate(radius) : targetBounds;
        return hitbox.contains(start) || hitbox.clip(start, end).isPresent();
    }

    private static LivingEntity rayTarget(ServerLevel level, SkillExecutionContext context,
                                          Vec3 end, double radius) {
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        AABB search = new AABB(context.origin(), end).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != context.caster() && entity.isAlive())) {
            AABB hitbox = target.getBoundingBox().inflate(radius);
            var intersection = hitbox.clip(context.origin(), end);
            if (intersection.isEmpty() && !hitbox.contains(context.origin())) continue;
            Vec3 point = intersection.orElse(context.origin());
            double distance = point.distanceToSqr(context.origin());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = target;
            }
        }
        return nearest;
    }

    private record ServerPlayerView(SkillExecutionContext context) {
        boolean valid(LivingEntity entity) { return entity != context.caster() && entity.isAlive(); }
    }

    private SkillHitResolver() {}
}
