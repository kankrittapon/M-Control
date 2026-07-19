package com.zexqm.rpgproject.rpg.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.ToDoubleFunction;

public final class SkillHitResolver {
    private static final ThreadLocal<Metrics> ACTIVE_METRICS = new ThreadLocal<>();

    public record Resolution(Set<LivingEntity> targets, int candidateCount, long elapsedNanos) {}

    public static Resolution resolveMeasured(SkillDefinition definition, SkillDefinition.Hit hit,
                                             SkillExecutionContext context) {
        Metrics metrics = new Metrics();
        ACTIVE_METRICS.set(metrics);
        long start = System.nanoTime();
        try {
            Set<LivingEntity> targets = resolve(definition, hit, context);
            return new Resolution(targets, metrics.candidates, System.nanoTime() - start);
        } finally {
            ACTIVE_METRICS.remove();
        }
    }

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
                countCandidates(1);
                if (level.getEntity(context.targetEntityId()) instanceof LivingEntity target
                        && target != context.caster() && target.isAlive()
                        && target.distanceToSqr(context.caster()) <= range * range) result.add(target);
            }
            case RAY, AIM_PROJECTILE -> {
                LivingEntity nearest = rayTarget(level, context, end, Math.max(0.0, radius));
                if (nearest != null) result.add(nearest);
            }
            case CHAIN -> collectChain(level, context, radius, hit.maxTargets(), result);
            case SELF_AOE -> collectCircle(level, context.caster(), context.caster().position(), radius, result);
            case GROUND_AOE, CIRCLE -> {
                Vec3 center = offsetCenter(context.groundPosition() == null ? end : context.groundPosition(),
                        context.direction(), hit);
                if (center.distanceToSqr(context.origin()) <= range * range)
                    collectCircle(level, context.caster(), center, radius, hit.maxTargets(), result);
            }
            case LINE -> {
                List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(context.origin(), end).inflate(radius), view::valid);
                countCandidates(candidates.size());
                for (LivingEntity target : candidates) {
                    if (intersectsLine(target.getBoundingBox(), context.origin(), end, radius)) result.add(target);
                }
            }
            case CONE -> {
                double cosine = Math.cos(Math.toRadians(45.0));
                List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                        context.caster().getBoundingBox().inflate(range), view::valid);
                countCandidates(candidates.size());
                for (LivingEntity target : candidates) {
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
        if (definition.targeting() == SkillTargetingType.CHAIN) return result;
        return limit(result, context.caster(), impactCenter(definition, hit, context), hit.maxTargets());
    }

    public static Set<LivingEntity> resolveImpact(SkillDefinition definition, SkillDefinition.Hit hit,
                                                   SkillExecutionContext context, Vec3 impact,
                                                   LivingEntity primaryTarget) {
        if (!(context.caster().level() instanceof ServerLevel level)) return Set.of();
        SkillImpactShape shape = hit.impactShape() == SkillImpactShape.AUTO
                ? SkillImpactShape.SINGLE : hit.impactShape();
        if (shape == SkillImpactShape.SINGLE) {
            return primaryTarget != null && validTarget(context.caster(), primaryTarget)
                    ? Set.of(primaryTarget) : Set.of();
        }
        Set<LivingEntity> targets = new LinkedHashSet<>();
        double radius = hit.radius() > 0 ? hit.radius() : definition.radius();
        collectCircle(level, context.caster(), impact, radius, hit.maxTargets(), targets);
        return targets;
    }

    private static void collectCircle(ServerLevel level, LivingEntity caster, Vec3 center, double radius,
                                      Set<LivingEntity> result) {
        collectCircle(level, caster, center, radius, 0, result);
    }

    private static void collectCircle(ServerLevel level, LivingEntity caster, Vec3 center, double radius,
                                      int maxTargets, Set<LivingEntity> result) {
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius),
                target -> validTarget(caster, target) && distanceToBoundsSqr(center, target.getBoundingBox())
                        <= radius * radius);
        countCandidates(candidates.size());
        result.addAll(nearest(candidates, maxTargets,
                target -> distanceToBoundsSqr(center, target.getBoundingBox())));
    }

    private static boolean validTarget(LivingEntity caster, LivingEntity target) {
        return target != caster && target.isAlive() && !caster.isAlliedTo(target);
    }

    private static Vec3 offsetCenter(Vec3 center, Vec3 direction, SkillDefinition.Hit hit) {
        Vec3 forward = new Vec3(direction.x, 0, direction.z);
        if (forward.lengthSqr() < 1.0E-6) forward = new Vec3(0, 0, 1); else forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        return center.add(forward.scale(hit.forwardOffset())).add(right.scale(hit.rightOffset()));
    }

    private static Vec3 impactCenter(SkillDefinition definition, SkillDefinition.Hit hit,
                                     SkillExecutionContext context) {
        return switch (definition.targeting()) {
            case SELF_AOE -> context.caster().position();
            case GROUND_AOE, CIRCLE -> offsetCenter(context.groundPosition() == null
                    ? context.origin().add(context.direction().scale(definition.range()))
                    : context.groundPosition(), context.direction(), hit);
            default -> context.origin();
        };
    }

    private static Set<LivingEntity> limit(Set<LivingEntity> targets, LivingEntity caster,
                                           Vec3 center, int maxTargets) {
        List<LivingEntity> valid = targets.stream().filter(target -> validTarget(caster, target)).toList();
        return new LinkedHashSet<>(nearest(valid, maxTargets,
                target -> distanceToBoundsSqr(center, target.getBoundingBox())));
    }

    static double distanceToBoundsSqr(Vec3 point, AABB bounds) {
        double x = Math.max(bounds.minX, Math.min(point.x, bounds.maxX));
        double y = Math.max(bounds.minY, Math.min(point.y, bounds.maxY));
        double z = Math.max(bounds.minZ, Math.min(point.z, bounds.maxZ));
        return point.distanceToSqr(x, y, z);
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
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> validTarget(context.caster(), entity));
        countCandidates(candidates.size());
        for (LivingEntity target : candidates) {
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

    private static void collectChain(ServerLevel level, SkillExecutionContext context,
                                     double jumpRadius, int maxTargets, Set<LivingEntity> result) {
        LivingEntity current = context.targetEntityId() != null
                && level.getEntity(context.targetEntityId()) instanceof LivingEntity living
                && validTarget(context.caster(), living) ? living : null;
        if (current == null) return;
        int limit = Math.max(1, maxTargets);
        result.add(current);
        while (result.size() < limit) {
            Vec3 anchor = current.getBoundingBox().getCenter();
            List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                            current.getBoundingBox().inflate(jumpRadius),
                            target -> validTarget(context.caster(), target) && !result.contains(target)
                                    && distanceToBoundsSqr(anchor, target.getBoundingBox())
                                    <= jumpRadius * jumpRadius);
            countCandidates(candidates.size());
            LivingEntity next = candidates.stream()
                    .min(Comparator.comparingDouble(target ->
                            distanceToBoundsSqr(anchor, target.getBoundingBox())))
                    .orElse(null);
            if (next == null) break;
            result.add(next);
            current = next;
        }
    }

    static List<Double> boundedNearest(List<Double> distances, int maxTargets) {
        return nearest(distances, maxTargets, Double::doubleValue);
    }

    private static <T> List<T> nearest(Collection<T> candidates, int maxTargets,
                                       ToDoubleFunction<T> distance) {
        int limit = maxTargets <= 0 ? candidates.size() : Math.min(maxTargets, candidates.size());
        if (limit == 0) return List.of();
        if (limit == candidates.size()) {
            List<T> all = new ArrayList<>(candidates);
            all.sort(Comparator.comparingDouble(distance));
            return all;
        }
        PriorityQueue<T> nearest = new PriorityQueue<>(limit,
                Comparator.comparingDouble(distance).reversed());
        for (T candidate : candidates) {
            if (nearest.size() < limit) {
                nearest.add(candidate);
            } else if (distance.applyAsDouble(candidate) < distance.applyAsDouble(nearest.peek())) {
                nearest.poll();
                nearest.add(candidate);
            }
        }
        List<T> ordered = new ArrayList<>(nearest);
        ordered.sort(Comparator.comparingDouble(distance));
        return ordered;
    }

    private static void countCandidates(int count) {
        Metrics metrics = ACTIVE_METRICS.get();
        if (metrics != null) metrics.candidates += count;
    }

    private static final class Metrics { private int candidates; }

    private record ServerPlayerView(SkillExecutionContext context) {
        boolean valid(LivingEntity entity) { return validTarget(context.caster(), entity); }
    }

    private SkillHitResolver() {}
}
