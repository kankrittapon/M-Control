package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.mana.Mana;

public final class SkillResourceService {
    private SkillResourceService() {}

    public static ResourceTransactionResult apply(Mana caster, Mana target,
                                                  SkillDefinition.ResourcePayload payload,
                                                  boolean allowRecovery) {
        int drained = 0;
        if (payload.targetMaxResourceDrainPercent() > 0 && target != null) {
            drained = target.drain(percentOf(target.getMaxMana(), payload.targetMaxResourceDrainPercent()));
        }

        int transferred = (int) Math.floor(drained * payload.drainTransferRatio());
        int requestedRecovery = allowRecovery
                ? percentOf(caster.getMaxMana(), payload.maxMpRecoveryPercent()) + payload.flatMpRecovery() : 0;
        int recovered = caster.restore(requestedRecovery + transferred);
        int appliedTransfer = Math.min(transferred, recovered);

        if (recovered > 0 || drained > 0)
            return new ResourceTransactionResult(ResourceTransactionResult.Status.APPLIED,
                    recovered, drained, appliedTransfer);
        if (payload.targetMaxResourceDrainPercent() > 0 && target == null)
            return new ResourceTransactionResult(ResourceTransactionResult.Status.UNSUPPORTED_TARGET, 0, 0, 0);
        return new ResourceTransactionResult(ResourceTransactionResult.Status.NO_EFFECT, 0, 0, 0);
    }

    private static int percentOf(int maximum, double percent) {
        return (int) Math.floor(maximum * percent + 1.0e-9);
    }
}
