package com.zexqm.rpgproject.rpg.status;

import java.util.UUID;

public final class RpgStatusInstance {
    private final RpgStatusType type;
    private UUID source;
    private int remainingTicks;
    private int intervalTicks;
    private int intervalRemaining;
    private double potency;
    private int stacks;
    private int maxStacks;
    private StatusStackingPolicy stacking;

    public RpgStatusInstance(RpgStatusType type, UUID source, int durationTicks, int intervalTicks,
                             double potency, int maxStacks, StatusStackingPolicy stacking) {
        this.type = type;
        this.source = source;
        this.remainingTicks = Math.max(1, durationTicks);
        this.intervalTicks = Math.max(1, intervalTicks);
        this.intervalRemaining = this.intervalTicks;
        this.potency = Math.max(0, potency);
        this.stacks = 1;
        this.maxStacks = Math.max(1, maxStacks);
        this.stacking = stacking;
    }

    public RpgStatusType type() { return type; }
    public UUID source() { return source; }
    public int remainingTicks() { return remainingTicks; }
    public double potency() { return potency; }
    public int stacks() { return stacks; }

    public void merge(RpgStatusInstance incoming) {
        switch (incoming.stacking) {
            case REFRESH -> remainingTicks = Math.max(remainingTicks, incoming.remainingTicks);
            case REPLACE_STRONGER -> {
                if (incoming.potency > potency) {
                    potency = incoming.potency;
                    source = incoming.source;
                    stacks = 1;
                }
                remainingTicks = Math.max(remainingTicks, incoming.remainingTicks);
            }
            case STACK -> {
                maxStacks = Math.max(maxStacks, incoming.maxStacks);
                stacks = Math.min(maxStacks, stacks + 1);
                potency = Math.max(potency, incoming.potency);
                remainingTicks = Math.max(remainingTicks, incoming.remainingTicks);
            }
        }
    }

    public boolean tickInterval() {
        remainingTicks--;
        if (--intervalRemaining <= 0) {
            intervalRemaining = intervalTicks;
            return true;
        }
        return false;
    }

    public boolean expired() { return remainingTicks <= 0; }
}
