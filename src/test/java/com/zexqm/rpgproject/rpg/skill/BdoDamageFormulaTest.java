package com.zexqm.rpgproject.rpg.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BdoDamageFormulaTest {
    private static final SkillRuntimeConfig.BdoDamageValues CONFIG =
            SkillRuntimeConfig.BdoDamageValues.defaults();

    @Test
    void convertsKnownWizardTooltipsWithSquareRootCompression() {
        BdoDamageCoefficient fireball = BdoDamageFormula.convert(1150, 2, CONFIG);
        BdoDamageCoefficient explosion = BdoDamageFormula.convert(2500, 2, CONFIG);

        assertEquals(Math.sqrt(1.15), fireball.perHit(), 0.000001);
        assertEquals(2.144761, fireball.total(), 0.000001);
        assertEquals(Math.sqrt(2.5), explosion.perHit(), 0.000001);
        assertEquals(3.162278, explosion.total(), 0.000001);
    }

    @Test
    void preservesSourceHitCountWithoutCompoundingPerHitScaling() {
        BdoDamageCoefficient result = BdoDamageFormula.convert(1000, 7, CONFIG);
        assertEquals(1.0, result.perHit(), 0.000001);
        assertEquals(7.0, result.total(), 0.000001);
        assertEquals(7, result.hitCount());
    }

    @Test
    void clampsOutlierCoefficients() {
        assertEquals(5.0, BdoDamageFormula.convert(1_000_000, 1, CONFIG).perHit(), 0.000001);
        assertEquals(20.0, BdoDamageFormula.convert(1_000_000, 100, CONFIG).total(), 0.000001);
    }

    @Test
    void rejectsInvalidSourceValues() {
        assertThrows(IllegalArgumentException.class, () -> BdoDamageFormula.convert(0, 1, CONFIG));
        assertThrows(IllegalArgumentException.class, () -> BdoDamageFormula.convert(1000, 0, CONFIG));
        assertThrows(IllegalArgumentException.class, () -> BdoDamageFormula.convert(1000, 101, CONFIG));
    }
}
