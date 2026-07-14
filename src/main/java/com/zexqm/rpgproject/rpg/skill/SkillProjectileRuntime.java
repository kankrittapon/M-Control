package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.RpgProject;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class SkillProjectileRuntime {
    private static final List<Projectile> ACTIVE = new ArrayList<>();

    static void spawn(SkillDefinition skill, SkillDefinition.Hit hit, SkillExecutionContext context,
                      boolean cooldownRecast, long gameTime) {
        ACTIVE.add(new Projectile(skill, hit, context, cooldownRecast, gameTime,
                context.origin(), context.direction().normalize(), skill.range()));
        RpgProject.LOGGER.info("[RPG Skill] projectile-spawn player={} skill={} origin={} direction={} speed={} range={}",
                context.caster().getScoreboardName(), skill.id(), context.origin(), context.direction(),
                skill.projectileSpeed(), skill.range());
    }

    static boolean tick(ServerPlayer owner) {
        boolean active = false;
        Iterator<Projectile> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            if (projectile.context.caster() != owner) continue;
            active = true;
            if (!owner.isAlive() || projectile.remaining <= 0) {
                iterator.remove();
                continue;
            }
            ServerLevel level = owner.serverLevel();
            double step = Math.min(projectile.skill.projectileSpeed(), projectile.remaining);
            Vec3 desiredEnd = projectile.position.add(projectile.direction.scale(step));
            var blockHit = level.clip(new ClipContext(projectile.position, desiredEnd,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            Vec3 segmentEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : desiredEnd;
            LivingEntity target = nearestTarget(level, owner, projectile.position, segmentEnd);
            if (target != null) {
                Vec3 impact = intersectionPoint(target, projectile.position, segmentEnd);
                SkillRuntime.recordLinkAnchor(projectile.skill, owner, impact);
                var targets = SkillHitResolver.resolveImpact(projectile.skill, projectile.hit,
                        projectile.context, impact, target);
                SkillRuntime.executeResolvedHit(projectile.skill, projectile.hit, projectile.context,
                        projectile.cooldownRecast, projectile.gameTime, targets);
                RpgProject.LOGGER.info("[RPG Skill] projectile-hit player={} skill={} target={}#{} position={} areaTargets={}",
                        owner.getScoreboardName(), projectile.skill.id(), target.getType().toShortString(),
                        target.getId(), impact, targets.size());
                iterator.remove();
                continue;
            }
            level.sendParticles(ParticleTypes.FLAME, segmentEnd.x, segmentEnd.y, segmentEnd.z,
                    2, 0.04, 0.04, 0.04, 0.0);
            projectile.remaining -= projectile.position.distanceTo(segmentEnd);
            projectile.position = segmentEnd;
            if (blockHit.getType() == HitResult.Type.BLOCK || projectile.remaining <= 0) {
                SkillRuntime.recordLinkAnchor(projectile.skill, owner, segmentEnd);
                if (blockHit.getType() == HitResult.Type.BLOCK) {
                    var targets = SkillHitResolver.resolveImpact(projectile.skill, projectile.hit,
                            projectile.context, segmentEnd, null);
                    if (!targets.isEmpty()) SkillRuntime.executeResolvedHit(projectile.skill, projectile.hit,
                            projectile.context, projectile.cooldownRecast, projectile.gameTime, targets);
                }
                RpgProject.LOGGER.info("[RPG Skill] projectile-end player={} skill={} reason={} position={}",
                        owner.getScoreboardName(), projectile.skill.id(),
                        blockHit.getType() == HitResult.Type.BLOCK ? "BLOCK" : "RANGE", segmentEnd);
                iterator.remove();
            }
        }
        return active;
    }

    static void clear(ServerPlayer owner) {
        ACTIVE.removeIf(projectile -> projectile.context.caster() == owner);
    }

    private static LivingEntity nearestTarget(ServerLevel level, ServerPlayer owner, Vec3 start, Vec3 end) {
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        AABB search = new AABB(start, end).inflate(1.0);
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != owner && entity.isAlive() && !owner.isAlliedTo(entity))) {
            var intersection = candidate.getBoundingBox().clip(start, end);
            if (intersection.isEmpty() && !candidate.getBoundingBox().contains(start)) continue;
            double distance = intersection.orElse(start).distanceToSqr(start);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static Vec3 intersectionPoint(LivingEntity target, Vec3 start, Vec3 end) {
        return target.getBoundingBox().clip(start, end).orElse(start);
    }

    private static final class Projectile {
        private final SkillDefinition skill;
        private final SkillDefinition.Hit hit;
        private final SkillExecutionContext context;
        private final boolean cooldownRecast;
        private final long gameTime;
        private Vec3 position;
        private final Vec3 direction;
        private double remaining;

        private Projectile(SkillDefinition skill, SkillDefinition.Hit hit, SkillExecutionContext context,
                           boolean cooldownRecast, long gameTime, Vec3 position, Vec3 direction,
                           double remaining) {
            this.skill = skill;
            this.hit = hit;
            this.context = context;
            this.cooldownRecast = cooldownRecast;
            this.gameTime = gameTime;
            this.position = position;
            this.direction = direction;
            this.remaining = remaining;
        }
    }

    private SkillProjectileRuntime() {}
}
