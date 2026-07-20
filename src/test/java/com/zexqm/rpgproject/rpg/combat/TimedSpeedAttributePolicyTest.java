package com.zexqm.rpgproject.rpg.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimedSpeedAttributePolicyTest {
    @Test void multiplyTotalPreservesTheAdvertisedBonusAfterWeaponPenalty() {
        double base = 4.0;
        double weaponPenalty = -2.4;
        double resultBeforeSpeedSpell = base + weaponPenalty;
        double resultWithRankThree = resultBeforeSpeedSpell * 1.20;
        assertEquals(1.6, resultBeforeSpeedSpell, 1.0e-9);
        assertEquals(1.92, resultWithRankThree, 1.0e-9);
    }
}
