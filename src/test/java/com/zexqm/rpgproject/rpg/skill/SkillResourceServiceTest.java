package com.zexqm.rpgproject.rpg.skill;

import com.zexqm.rpgproject.mana.Mana;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillResourceServiceTest {
    @Test
    void restoresPercentageOfCasterMaximum() {
        Mana caster = manaAt(100, 20);
        var payload = new SkillDefinition.ResourcePayload(0.30, 0, 0, true);

        var result = SkillResourceService.apply(caster, null, payload, true);

        assertEquals(ResourceTransactionResult.Status.APPLIED, result.status());
        assertEquals(30, result.recovered());
        assertEquals(50, caster.getMana());
    }

    @Test
    void drainsTargetAndTransfersOnlyWhatCasterCanReceive() {
        Mana caster = manaAt(100, 95);
        Mana target = manaAt(200, 15);
        var payload = new SkillDefinition.ResourcePayload(0, 0.10, 0.50, true);

        var result = SkillResourceService.apply(caster, target, payload, true);

        assertEquals(15, result.drained());
        assertEquals(5, result.recovered());
        assertEquals(5, result.transferred());
        assertEquals(0, target.getMana());
        assertEquals(100, caster.getMana());
    }

    @Test
    void reportsUnsupportedDrainTargetWithoutInventingMobResource() {
        Mana caster = manaAt(100, 50);
        var payload = new SkillDefinition.ResourcePayload(0, 0.10, 1, true);
        assertEquals(ResourceTransactionResult.Status.UNSUPPORTED_TARGET,
                SkillResourceService.apply(caster, null, payload, true).status());
    }

    @Test
    void rejectsPercentagesOutsideNormalizedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new SkillDefinition.ResourcePayload(1.01, 0, 0, true));
    }

    @Test
    void settingManaRestartsTheRegenerationInterval() {
        Mana mana = manaAt(100, 20);
        for (int tick = 0; tick < 19; tick++) mana.tickRegen();

        mana.setMana(29);
        for (int tick = 0; tick < 19; tick++) mana.tickRegen();
        assertEquals(29, mana.getMana());

        mana.tickRegen();
        assertEquals(34, mana.getMana());
    }

    private static Mana manaAt(int maximum, int current) {
        Mana mana = new Mana();
        mana.setMaxMana(maximum);
        mana.setMana(current);
        return mana;
    }
}
