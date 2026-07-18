package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillPointCostPolicyTest {
    @Test
    void catalogTierResolvesRankCostsAndExplicitCostOverridesPolicy() {
        String json = """
                {"mcp_id":"test","name":"Test","description":"Test","class":"wizard",
                 "tree":"main","sp_tier":"core","playable":true,"ranks":[
                   {"rank":1,"required_level":1},
                   {"rank":2,"required_level":10,"sp_cost":99}]}
                """;
        SkillCatalogEntry entry = SkillCatalog.parse(new ResourceLocation("rpg_project", "test"),
                JsonParser.parseString(json).getAsJsonObject());
        assertEquals(25, entry.rank(1).skillPointCost());
        assertEquals(99, entry.rank(2).skillPointCost());
    }

    @Test
    void defaultPolicyHasAllTiersAndRejectsRanksOutsideConfiguredTable() {
        SkillProgressionConfig.Values values = SkillProgressionConfig.values();
        assertEquals(10, values.skillPointCost(SkillCostTier.BASIC, 1));
        assertEquals(60, values.skillPointCost(SkillCostTier.CORE, 3));
        assertEquals(100, values.skillPointCost(SkillCostTier.ADVANCED, 3));
        assertEquals(250, values.skillPointCost(SkillCostTier.ULTIMATE, 3));
        assertNull(values.skillPointCost(SkillCostTier.BASIC, 99));
    }
}
