package com.zexqm.rpgproject.rpg.status;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RpgStatusInstanceTest {
    @Test
    void stackRespectsMaximumAndKeepsStrongestPotency() {
        RpgStatusInstance status = instance(40, 1.0, 2, StatusStackingPolicy.STACK);
        status.merge(instance(60, 2.0, 2, StatusStackingPolicy.STACK));
        status.merge(instance(80, 1.5, 2, StatusStackingPolicy.STACK));
        assertEquals(2, status.stacks());
        assertEquals(2.0, status.potency());
        assertEquals(80, status.remainingTicks());
    }

    @Test
    void refreshDoesNotReplacePotency() {
        RpgStatusInstance status = instance(20, 3.0, 1, StatusStackingPolicy.REFRESH);
        status.merge(instance(50, 1.0, 1, StatusStackingPolicy.REFRESH));
        assertEquals(3.0, status.potency());
        assertEquals(50, status.remainingTicks());
    }

    @Test
    void replaceStrongerRejectsWeakerValue() {
        RpgStatusInstance status = instance(20, 3.0, 1, StatusStackingPolicy.REPLACE_STRONGER);
        status.merge(instance(40, 2.0, 1, StatusStackingPolicy.REPLACE_STRONGER));
        assertEquals(3.0, status.potency());
        assertEquals(40, status.remainingTicks());
    }

    private static RpgStatusInstance instance(int duration, double potency, int maxStacks,
                                              StatusStackingPolicy policy) {
        return new RpgStatusInstance(RpgStatusType.BURN, UUID.randomUUID(), duration, 20,
                potency, maxStacks, policy);
    }
}
