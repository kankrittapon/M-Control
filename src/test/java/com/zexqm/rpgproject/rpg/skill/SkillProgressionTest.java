package com.zexqm.rpgproject.rpg.skill;

import com.google.gson.JsonParser;
import com.zexqm.rpgproject.rpg.RpgClass;
import com.zexqm.rpgproject.rpg.RpgPlayerData;
import com.zexqm.rpgproject.rpg.WeaponSet;
import com.zexqm.rpgproject.rpg.Specialization;
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
    void nextRankRequiresItsOwnProductionCombatDefinition() {
        SkillCatalogEntry twoRanks = new SkillCatalogEntry(ID, "wizard.learn_test", "Learn Test", "Test",
                RpgClass.WIZARD, SkillTree.MAIN, null, true, "",
                List.of(new SkillRankDefinition(1, 1, 1), new SkillRankDefinition(2, 1, 1)), List.of(), Set.of());
        SkillCatalog.replaceForTests(Map.of(ID, twoRanks));
        SkillRegistry.replaceForTests(Map.of(ID, combatDefinition()));
        RpgPlayerData data = new RpgPlayerData();
        data.addSkillExperience(202);
        assertTrue(SkillLearningService.upgrade(data, ID).success());
        assertEquals(SkillAvailability.METADATA_ONLY, SkillLearningService.availability(data, ID));
    }

    @Test
    void acceptanceForceUpgradeBypassesOnlyPrerequisiteGate() {
        ResourceLocation prerequisite = new ResourceLocation("rpg_project", "unfinished_prerequisite");
        SkillCatalogEntry gated = new SkillCatalogEntry(ID, "wizard.learn_test", "Learn Test", "Test",
                RpgClass.WIZARD, SkillTree.MAIN, null, true, "",
                List.of(new SkillRankDefinition(1, 1, 2)),
                List.of(new SkillRequirement(prerequisite, 1)), Set.of());
        SkillCatalog.replaceForTests(Map.of(ID, gated));
        SkillRegistry.replaceForTests(Map.of(ID, combatDefinition()));
        RpgPlayerData data = new RpgPlayerData();
        data.addSkillExperience(202);

        assertEquals(SkillAvailability.PREREQUISITE_REQUIRED,
                SkillLearningService.availability(data, ID));
        assertTrue(SkillLearningService.forceUpgradeForAcceptance(data, ID).success());
        assertEquals(1, data.skillProgress().rank(ID));
        assertEquals(2, data.spentSkillPoints());
    }

    @Test
    void upgradeDowngradeAndPersistenceKeepPointsConsistent() {
        SkillCatalog.replaceForTests(Map.of(ID, entry(true)));
        SkillRegistry.replaceForTests(Map.of(ID, combatDefinition()));
        RpgPlayerData data = new RpgPlayerData();
        data.setLevel(10);
        assertEquals(2, data.addSkillExperience(202));
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
    void learnedRanksSurviveCloneClassSpecAndRepeatedPersistence() {
        SkillCatalog.replaceForTests(Map.of(ID, entry(true)));
        SkillRegistry.replaceForTests(Map.of(ID, combatDefinition()));
        RpgPlayerData original = new RpgPlayerData();
        original.setLevel(10);
        assertEquals(2, original.addSkillExperience(202));
        assertTrue(SkillLearningService.upgrade(original, ID).success());

        RpgPlayerData deathClone = new RpgPlayerData();
        deathClone.copyFrom(original);
        deathClone.setClass(RpgClass.NINJA);
        deathClone.setSpecialization(Specialization.AWAKENING);
        assertEquals(1, deathClone.skillProgress().rank(ID));
        assertEquals(SkillAvailability.WRONG_CLASS, SkillLearningService.availability(deathClone, ID));

        deathClone.setClass(RpgClass.WIZARD);
        RpgPlayerData dimensionRoundTrip = new RpgPlayerData();
        dimensionRoundTrip.load(deathClone.save());
        assertEquals(1, dimensionRoundTrip.skillProgress().rank(ID));
        assertEquals(original.spentSkillPoints(), dimensionRoundTrip.spentSkillPoints());
        assertEquals(original.availableSkillPoints(), dimensionRoundTrip.availableSkillPoints());
    }

    @Test
    void characterLevelAndSkillPointProgressionAreIndependent() {
        RpgPlayerData data = new RpgPlayerData();
        data.setLevel(50);
        assertEquals(0, data.availableSkillPoints());
        assertEquals(0, data.addSkillExperience(99));
        assertEquals(0, data.availableSkillPoints());
        assertEquals(1, data.addSkillExperience(1));
        assertEquals(1, data.availableSkillPoints());

        data.setLevel(1);
        assertEquals(1, data.availableSkillPoints());
        assertEquals(18, data.addExperience(100_000));
        assertEquals(1, data.availableSkillPoints());
        assertEquals(19, data.level());

        assertEquals(1, data.addSkillExperience(102));
        assertEquals(2, data.availableSkillPoints());

        SkillCatalog.replaceForTests(Map.of(ID, entry(true)));
        SkillRegistry.replaceForTests(Map.of(ID, combatDefinition()));
        assertTrue(SkillLearningService.upgrade(data, ID).success());
        data.setLevel(1);
        assertEquals(1, data.level());
        assertEquals(1, data.skillProgress().rank(ID));
        assertEquals(0, data.availableSkillPoints());
    }

    @Test
    void spentPointsReconcileFromCurrentCatalogCosts() {
        SkillCatalog.replaceForTests(Map.of(ID, entry(true)));
        SkillRegistry.replaceForTests(Map.of(ID, combatDefinition()));
        RpgPlayerData data = new RpgPlayerData();
        data.setLevel(10);
        assertEquals(2, data.addSkillExperience(202));
        assertTrue(SkillLearningService.upgrade(data, ID).success());
        assertEquals(2, data.spentSkillPoints());

        SkillCatalogEntry repriced = new SkillCatalogEntry(ID, "wizard.learn_test", "Learn Test", "Test",
                RpgClass.WIZARD, SkillTree.MAIN, null, true, "",
                List.of(new SkillRankDefinition(1, 1, 5)), List.of(), Set.of());
        SkillCatalog.replaceForTests(Map.of(ID, repriced));

        assertTrue(SkillLearningService.reconcileSpentSkillPoints(data));
        assertEquals(5, data.spentSkillPoints());
        assertEquals(0, data.availableSkillPoints());
    }

    @Test
    void incompleteCatalogDoesNotErasePersistedSpending() {
        SkillCatalog.replaceForTests(Map.of(ID, entry(true)));
        SkillRegistry.replaceForTests(Map.of(ID, combatDefinition()));
        RpgPlayerData data = new RpgPlayerData();
        data.setLevel(10);
        assertEquals(2, data.addSkillExperience(202));
        assertTrue(SkillLearningService.upgrade(data, ID).success());

        SkillCatalog.replaceForTests(Map.of());
        assertFalse(SkillLearningService.reconcileSpentSkillPoints(data));
        assertEquals(2, data.spentSkillPoints());
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
    void wizardMainSnapshotContainsFireballVerticalSliceAndThirtyTwoEntries() throws Exception {
        Path directory = Path.of("src/main/resources/data/rpg_project/rpg_skill_catalog");
        try (var files = Files.list(directory)) {
            List<Path> wizardFiles = files.filter(path -> path.getFileName().toString().startsWith("wizard_"))
                    .toList();
            assertEquals(32, wizardFiles.size());
            int playable = 0;
            for (Path path : wizardFiles) {
                ResourceLocation id = new ResourceLocation("rpg_project",
                        path.getFileName().toString().replace(".json", ""));
                SkillCatalogEntry parsed = SkillCatalog.parse(id,
                        JsonParser.parseString(Files.readString(path)).getAsJsonObject());
                if (parsed.playable()) playable++;
                assertEquals(id.getPath(), parsed.mcpId());
                if (id.getPath().equals("wizard_fireball")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(10, 20, 30, 40), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                } else if (id.getPath().equals("wizard_fireball_explosion")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(50, 75, 100), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                    assertEquals(new ResourceLocation("rpg_project", "wizard_fireball"),
                            parsed.prerequisites().get(0).skillId());
                } else if (id.getPath().equals("wizard_concentrated_magic_arrow")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(25, 40, 60), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                } else if (id.getPath().equals("wizard_multiple_magic_arrows")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(50), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                    assertEquals(new ResourceLocation("rpg_project", "wizard_magic_arrow"),
                            parsed.prerequisites().get(0).skillId());
                    assertEquals(5, parsed.prerequisites().get(0).minimumRank());
                } else if (id.getPath().equals("wizard_mana_absorption")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(25, 40, 60), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                } else if (id.getPath().equals("wizard_lightning")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(10, 20, 30, 40, 50), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                } else if (id.getPath().equals("wizard_lightning_chain")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(25, 40, 60, 80), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                } else if (id.getPath().equals("wizard_meteor_shower")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(100, 150, 250), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                } else if (id.getPath().equals("wizard_freeze")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(10, 20, 30, 40, 50), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                } else if (id.getPath().equals("wizard_frigid_fog")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(25, 40, 60, 80), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                    assertEquals(new ResourceLocation("rpg_project", "wizard_freeze"),
                            parsed.prerequisites().get(0).skillId());
                    assertEquals(1, parsed.prerequisites().get(0).minimumRank());
                } else if (id.getPath().equals("wizard_blizzard")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(50, 75, 100, 125), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                    assertEquals(new ResourceLocation("rpg_project", "wizard_freeze"),
                            parsed.prerequisites().get(0).skillId());
                } else if (id.getPath().equals("wizard_lightning_storm")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(25, 40, 60), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                    assertEquals(new ResourceLocation("rpg_project", "wizard_lightning_chain"),
                            parsed.prerequisites().get(0).skillId());
                } else if (id.getPath().equals("wizard_residual_lightning")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(25, 40, 60, 80), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                    assertEquals(new ResourceLocation("rpg_project", "wizard_lightning"),
                            parsed.prerequisites().get(0).skillId());
                } else if (id.getPath().equals("wizard_earthquake")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(50, 75, 100, 125), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                    assertTrue(parsed.prerequisites().isEmpty());
                } else if (id.getPath().equals("wizard_earth_s_response")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(10, 20, 30), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                    assertTrue(parsed.prerequisites().isEmpty());
                } else if (id.getPath().equals("wizard_staff_attack")) {
                    assertTrue(parsed.playable());
                    assertEquals(List.of(0, 5, 7, 10, 12, 14, 19, 23, 24, 25), parsed.ranks().stream()
                            .map(SkillRankDefinition::skillPointCost).toList());
                    assertTrue(parsed.prerequisites().isEmpty());
                } else {
                    assertFalse(parsed.playable(), id.toString());
                }
            }
            assertEquals(16, playable);
        }
    }

    @Test
    void playableSkillPointBudgetIncludesPhaseSixFreeze() throws Exception {
        assertEquals(100, totalSkillPointCost("wizard_fireball"));
        assertEquals(225, totalSkillPointCost("wizard_fireball_explosion"));
        assertEquals(125, totalSkillPointCost("wizard_concentrated_magic_arrow"));
        assertEquals(50, totalSkillPointCost("wizard_multiple_magic_arrows"));
        assertEquals(125, totalSkillPointCost("wizard_mana_absorption"));
        assertEquals(150, totalSkillPointCost("wizard_lightning"));
        assertEquals(205, totalSkillPointCost("wizard_lightning_chain"));
        assertEquals(500, totalSkillPointCost("wizard_meteor_shower"));
        assertEquals(150, totalSkillPointCost("wizard_freeze"));
        assertEquals(205, totalSkillPointCost("wizard_frigid_fog"));
        assertEquals(350, totalSkillPointCost("wizard_blizzard"));
        assertEquals(125, totalSkillPointCost("wizard_lightning_storm"));
        assertEquals(205, totalSkillPointCost("wizard_residual_lightning"));
        assertEquals(350, totalSkillPointCost("wizard_earthquake"));
        assertEquals(60, totalSkillPointCost("wizard_earth_s_response"));
        assertEquals(139, totalSkillPointCost("wizard_staff_attack"));
        assertEquals(3064, totalSkillPointCost("wizard_fireball")
                + totalSkillPointCost("wizard_fireball_explosion")
                + totalSkillPointCost("wizard_concentrated_magic_arrow")
                + totalSkillPointCost("wizard_multiple_magic_arrows")
                + totalSkillPointCost("wizard_mana_absorption")
                + totalSkillPointCost("wizard_lightning")
                + totalSkillPointCost("wizard_lightning_chain")
                + totalSkillPointCost("wizard_meteor_shower")
                + totalSkillPointCost("wizard_freeze")
                + totalSkillPointCost("wizard_frigid_fog")
                + totalSkillPointCost("wizard_blizzard")
                + totalSkillPointCost("wizard_lightning_storm")
                + totalSkillPointCost("wizard_residual_lightning")
                + totalSkillPointCost("wizard_earthquake")
                + totalSkillPointCost("wizard_earth_s_response")
                + totalSkillPointCost("wizard_staff_attack"));
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

    private static int totalSkillPointCost(String path) throws Exception {
        Path file = Path.of("src/main/resources/data/rpg_project/rpg_skill_catalog", path + ".json");
        SkillCatalogEntry entry = SkillCatalog.parse(new ResourceLocation("rpg_project", path),
                JsonParser.parseString(Files.readString(file)).getAsJsonObject());
        return entry.ranks().stream().mapToInt(SkillRankDefinition::skillPointCost).sum();
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
                        null, Set.of(), List.of(), List.of())), List.of());
    }
}
