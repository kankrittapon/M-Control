package com.zexqm.rpgproject.rpg.skill;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class SkillLinkState {
    private final Map<ResourceLocation, Long> expirations = new HashMap<>();
    private final Map<ResourceLocation, Vec3> anchors = new HashMap<>();

    public void grant(ResourceLocation link, long expirationGameTime) {
        if (link == null || expirationGameTime < 0) throw new IllegalArgumentException("Invalid skill link grant");
        expirations.merge(link, expirationGameTime, Math::max);
    }

    public boolean has(ResourceLocation link, long gameTime) {
        if (link == null) return true;
        long expiration = expirations.getOrDefault(link, 0L);
        if (expiration <= gameTime) {
            expirations.remove(link);
            anchors.remove(link);
            return false;
        }
        return true;
    }

    public boolean consume(ResourceLocation link, long gameTime) {
        if (!has(link, gameTime)) return false;
        expirations.remove(link);
        anchors.remove(link);
        return true;
    }

    public void setAnchor(ResourceLocation link, Vec3 anchor) {
        if (link == null || anchor == null) return;
        if (expirations.containsKey(link)) anchors.put(link, anchor);
    }

    public Vec3 anchor(ResourceLocation link, long gameTime) {
        return has(link, gameTime) ? anchors.get(link) : null;
    }

    public long remaining(ResourceLocation link, long gameTime) {
        return has(link, gameTime) ? expirations.get(link) - gameTime : 0L;
    }

    public void clear() { expirations.clear(); anchors.clear(); }
}
