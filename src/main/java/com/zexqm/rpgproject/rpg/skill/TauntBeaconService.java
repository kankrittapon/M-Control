package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.RpgProject;
import com.zexqm.rpgproject.rpg.mob.MobControlProfiles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TauntBeaconService {
    private static final Map<UUID, ActiveBeacon> ACTIVE = new HashMap<>();

    public static void summon(ServerPlayer owner, SkillDefinition.TauntBeaconPayload payload,
                              Vec3 direction) {
        clear(owner);
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-6)
            horizontal = Vec3.directionFromRotation(0, owner.getYRot());
        Vec3 position = owner.position().add(horizontal.normalize().scale(payload.placementDistance()));
        ArmorStand beacon = new ArmorStand(owner.serverLevel(), position.x, position.y, position.z);
        beacon.setNoGravity(true);
        beacon.setInvulnerable(true);
        beacon.setCustomName(Component.literal("Magic Lighthouse"));
        beacon.setCustomNameVisible(true);
        beacon.setGlowingTag(true);
        beacon.addTag("rpg_magic_lighthouse");
        owner.serverLevel().addFreshEntity(beacon);
        long expiresAt = owner.serverLevel().getGameTime() + payload.durationTicks();
        ACTIVE.put(owner.getUUID(), new ActiveBeacon(beacon, expiresAt, payload));
        RpgProject.LOGGER.info("[RPG Skill] taunt-beacon summon owner={} entity={} position={} radius={} duration={} maxTargets={}",
                owner.getScoreboardName(), beacon.getId(), position, payload.radius(), payload.durationTicks(),
                payload.maxTargets());
    }

    public static void tick(ServerPlayer owner) {
        ActiveBeacon active = ACTIVE.get(owner.getUUID());
        if (active == null) return;
        ArmorStand beacon = active.beacon;
        if (!beacon.isAlive() || beacon.level() != owner.serverLevel() || !owner.isAlive()
                || owner.serverLevel().getGameTime() >= active.expiresAt) {
            clear(owner);
            return;
        }
        if (owner.tickCount % active.payload.refreshIntervalTicks() != 0) return;
        AABB area = beacon.getBoundingBox().inflate(active.payload.radius());
        var candidates = owner.serverLevel().getEntitiesOfClass(Mob.class, area, mob ->
                mob.isAlive() && mob.canAttack(owner)
                        && active.payload.allowedProfiles().contains(MobControlProfiles.resolve(mob)));
        candidates.sort(Comparator.comparingDouble(beacon::distanceToSqr));
        int accepted = 0;
        for (Mob mob : candidates) {
            if (accepted >= active.payload.maxTargets()) break;
            if (MinecraftForge.EVENT_BUS.post(new TauntBeaconTargetEvent(owner, beacon, mob))) continue;
            mob.setTarget(beacon);
            accepted++;
        }
        RpgProject.LOGGER.info("[RPG Skill] taunt-beacon pulse owner={} entity={} candidates={} accepted={} remainingTicks={}",
                owner.getScoreboardName(), beacon.getId(), candidates.size(), accepted,
                Math.max(0, active.expiresAt - owner.serverLevel().getGameTime()));
    }

    public static void clear(ServerPlayer owner) {
        ActiveBeacon active = ACTIVE.remove(owner.getUUID());
        if (active == null) return;
        active.beacon.discard();
        RpgProject.LOGGER.info("[RPG Skill] taunt-beacon clear owner={}", owner.getScoreboardName());
    }

    private record ActiveBeacon(ArmorStand beacon, long expiresAt,
                                SkillDefinition.TauntBeaconPayload payload) {}

    private TauntBeaconService() {}
}
