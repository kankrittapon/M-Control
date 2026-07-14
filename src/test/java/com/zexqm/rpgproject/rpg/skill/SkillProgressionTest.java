package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.RpgClass;
import com.zexqm.rpgproject.rpg.RpgPlayerData;
import com.zexqm.rpgproject.rpg.WeaponSet;
import com.zexqm.rpgproject.rpg.combat.RpgPowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SkillProgressionTest {
    private static final ResourceLocation ID = new ResourceLocation("rpg_project", "learn_test");

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void clearRegistries() {
        SkillCatalog.replaceForTests(Map.of());
        SkillRegistry.replaceForTests(Map.of());
    }

    @Test
    void metadataOnlySkillCannotBeLearned() {
        SkillCatalogEntry entry = entry(false);
        SkillCatalog.replaceForTests(Map.of(ID, entry));
        RpgPlayerData data = new RpgPlayerData();
        data.setLevel(10);
        assertEquals(SkillAvailability.METADATA_ONLY, SkillLearningService.availability(data, ID));
        assertFalse(SkillLearningService.upgrade(data, ID).success());
        assertEquals(0, data.spentSkillPoints());
    }

    @Test
    void upgradeDowngradeAndPersistenceKeepPointsConsistent() {
        SkillCatalog.replaceForTests(Map.of(ID, entry(true)));
        SkillRegistry.replaceForTests(Map.of(ID, combatDefinition()));
        RpgPlayerData data = new RpgPlayerData();
        data.setLevel(10);
        int initial = data.availableSkillPoints();
        assertTrue(SkillLearningService.upgrade(data, ID).success());
        assertEquals(1, data.skillProgress().rank(ID));
        assertEquals(initial - 2, data.availableSkillPoints());

        RpgPlayerData loaded = new RpgPlayerData();
        loaded.load(data.save());
        assertEquals(1, loaded.skillProgress().rank(ID));
        assertEquals(initial - 2, loaded.availableSkillPoints());
        assertTrue(SkillLearningService.downgrade(loaded, ID).success());
        assertEquals(initial, loaded.availableSkillPoints());
    }

    @Test
    void parserPreservesMcpIdentityAndRanks() {
        String json = """
                {"mcp_id":"wizard.fireball","name":"Fireball","description":"Test",
                 "class":"wizard","tree":"main","playable":false,
                 "unavailable_reason":"Missing hit timing","ranks":[
                   {"rank":1,"required_level":1,"sp_cost":2},
                   {"rank":2,"required_level":5,"sp_cost":3}]}
                """;
        SkillCatalogEntry parsed = SkillCatalog.parse(ID, JsonParser.parseString(json).getAsJsonObject());
        assertEquals("wizard.fireball", parsed.mcpId());
        assertEquals(2, parsed.maximumRank());
        assertFalse(parsed.playable());
    }

    @Test
    void wizardMainSnapshotContainsThirtyTwoGatedEntries() throws Exception {
        Path directory = Path.of("src/main/resources/data/rpg_project/rpg_skill_catalog");
        try (var files = Files.list(directory)) {
            List<Path> wizardFiles = files.filter(path -> path.getFileName().toString().startsWith("wizard_"))
                    .toList();
            assertEquals(32, wizardFiles.size());
            for (Path path : wizardFiles) {
                ResourceLocation id = new ResourceLocation("rpg_project",
                        path.getFileName().toString().replace(".json", ""));
                SkillCatalogEntry parsed = SkillCatalog.parse(id,
                        JsonParser.parseString(Files.readString(path)).getAsJsonObject());
                assertFalse(parsed.playable(), id.toString());
                assertEquals(id.getPath(), parsed.mcpId());
            }
        }
    }

    @Test
    void graphValidationRejectsMissingCyclesAndDuplicateMcpIds() {
        ResourceLocation firstId = new ResourceLocation("rpg_project", "first");
        ResourceLocation secondId = new ResourceLocation("rpg_project", "second");
        SkillCatalogEntry first = entry(firstId, "duplicate", secondId);
        SkillCatalogEntry second = entry(secondId, "duplicate", firstId);
        assertEquals(Set.of(firstId, secondId), SkillCatalog.validateGraph(Map.of(
                firstId, first, secondId, second)));

        ResourceLocation missingId = new ResourceLocation("rpg_project", "missing_owner");
        SkillCatalogEntry missing = entry(missingId, "missing_owner",
                new ResourceLocation("rpg_project", "does_not_exist"));
        assertEquals(Set.of(missingId), SkillCatalog.validateGraph(Map.of(missingId, missing)));
    }

    private static SkillCatalogEntry entry(boolean playable) {
        return new SkillCatalogEntry(ID, "wizard.learn_test", "Learn Test", "Test", RpgClass.WIZARD,
                SkillTree.MAIN, null, playable, playable ? "" : "Missing combat data",
                List.of(new SkillRankDefinition(1, 1, 2)), List.of(), Set.of());
    }

    private static SkillCatalogEntry entry(ResourceLocation id, String mcpId, ResourceLocation prerequisite) {
        return new SkillCatalogEntry(id, mcpId, id.getPath(), "Test", RpgClass.WIZARD,
                SkillTree.MAIN, null, false, "Test metadata",
                List.of(new SkillRankDefinition(1, 1, null)),
                List.of(new SkillRequirement(prerequisite, 1)), Set.of());
    }

    private static SkillDefinition combatDefinition() {
        return new SkillDefinition(ID, false, RpgClass.WIZARD, null, 1, SkillTargetingType.RAY,
                new SkillWeaponRequirement(WeaponSet.MAIN, true, true, false), PrimaryResourceType.MP,
                1, 0, 1, 1, 1, MovementPolicy.FULL, CancelPolicy.NEVER, 8, 0,
                List.of(new SkillDefinition.Hit(1, 1, 1, 0, RpgPowerType.MAGIC,
                        null, Set.of(), List.of())), List.of());
    }
}
