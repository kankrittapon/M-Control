# M-Control

Minecraft Forge 1.20.1 action-RPG control and world-core mod. The project currently focuses on
server-authoritative progression, combat calculations, camera controls, and reusable class systems.
Class-specific skills, models, and animations are intentionally deferred until the world core is stable.

## Requirements

- Minecraft 1.20.1
- Minecraft Forge 47.x
- Java 17

## Working Systems

### RPG Controls

- Independent third-person camera anchored to the player.
- Smooth orbit, shoulder offset, collision-safe camera distance, and scroll zoom.
- Action camera and hold-`Ctrl` cursor mode.
- `Ctrl + LMB` drag rotates the camera without rotating the player.
- Left-click selects an entity for UI/context; selection is not a hard skill lock.
- Double left-click on an entity starts auto-attack and chase.
- Right-click ground issues click-to-move; right-click entity issues move-to-target.
- WASD and jump cancel automatic movement.
- Water and lava preserve vanilla movement.
- Third-person RPG mode blocks vanilla block breaking and placement.
- First-person mode preserves vanilla Minecraft interactions.

### Class And Equipment Core

- Classes: `WIZARD` and `NINJA`.
- Specializations: `SUCCESSION` and `AWAKENING`.
- Logical Main, Sub, and Awakening weapon slots persisted in player data.
- Wizard weapon types: Staff, Dagger, and Sphera.
- Ninja weapon types: Shortsword, Shuriken, and Sura Katana.
- Server validates weapon class and slot.
- `Tab` toggles logical Draw/Sheathe state when required weapons are equipped.
- Draw/Sheathe currently has no model or animation.

### Player Progression

- Character levels 1-100.
- Persistent EXP with overflow across multiple level-ups.
- One Skill Point per level gained.
- Separate total, spent, and available Skill Point values.
- EXP rewards from killing living entities.
- Player data persists through save/load, death, respawn, and dimension changes.
- Wizard and Ninja have different health, mana, offense, defense, accuracy, and evasion growth.

### Global Attributes And Damage

- Data-driven class base stats and per-level growth.
- Custom synced attributes for Attack Power, Magic Power, Defense, Damage Reduction,
  Accuracy, Evasion, Critical Chance, Critical Damage, Cast Speed, and CC Resistance.
- Max Health, Max Mana, and Movement Speed bridge into the player runtime.
- Server damage order: Accuracy/Evasion, Attack Power, Critical, then Damage Reduction.
- Hit chance is clamped to 10-95%; Damage Reduction is capped at 80%.
- Current formulas are tuning defaults, not final balance.

### Training, Stamina, And Weight

- Breath, Strength, and Health training levels range from 1-50.
- Breath increases maximum stamina and stamina regeneration.
- Sprint drains stamina and stops when stamina is exhausted.
- Breath gains training EXP from sprint distance.
- Strength increases carrying capacity and gains EXP while moving above 70% load.
- Inventory, armor, offhand items, and RPG weapons contribute weight.
- Weight above 70% reduces movement speed; weight at or above 100% disables sprint.
- Health training gains EXP from recovered health and increases Max Health.
- A stamina bar appears in RPG third-person while stamina is not full.

### Protection And Crowd Control Core

- Front Guard protects a 180-degree frontal arc and consumes Guard Gauge.
- Side and rear damage bypass Front Guard.
- Iframe cancels eligible incoming damage.
- Super Armor, Grab Immunity, and shared CC resolution contracts are available to future skills.
- CC types include stiffness, stun, freeze, knockback, knockdown, bound, float, and grab.
- Protection states are server-authoritative and tick-based.

### Mob Level And World Scaling

- Mobs receive a persistent level when first spawned.
- Overworld levels range from 1-60, Nether from 30-80, and End from 60-100.
- Level considers dimension, distance from world spawn, and small random variation.
- Mob level applies stable Max Health and Attack Damage modifiers.
- Saved mobs do not receive duplicate scaling modifiers when reloaded.
- EXP rewards use the mob's resulting Max Health.

### Generic Skill Contract

The project contains only reusable skill definitions, not playable class skills. The contract supports
targeting type, class and specialization requirements, weapon requirements, mana/stamina costs,
cooldown, cast/recovery time, range, radius, and damage coefficient.

## Controls

| Input | Action |
| --- | --- |
| `V` | Toggle first-person and RPG third-person view |
| Hold `Left Ctrl` | Enter cursor command mode |
| `Ctrl + LMB drag` | Orbit the third-person camera |
| `Ctrl + LMB entity` | Select entity; double-click starts auto-attack |
| `Ctrl + RMB ground/entity` | Move to ground destination or follow entity |
| Mouse wheel | Adjust RPG third-person camera distance |
| `Tab` | Toggle logical weapon Draw/Sheathe state |

`Tab` shares the vanilla player-list key until input ownership is finalized.

## Data Configuration

Class growth profiles are loaded from:

```text
src/main/resources/data/rpg_project/rpg_classes/wizard.json
src/main/resources/data/rpg_project/rpg_classes/ninja.json
```

Run `/reload` after changing a profile. Invalid or missing profiles fall back to Java defaults.

## Development Commands

```mcfunction
/rpg class wizard
/rpg class ninja
/rpg specialization succession
/rpg specialization awakening
/rpg equip
/rpg status
/rpg addxp 1000
/rpg setlevel 50
/rpg protection fg 200
/rpg protection sa 200
/rpg protection iframe 100
/rpg protection grab_immune 200
```

`/rpg equip` equips the RPG weapon currently held in the main hand into its logical slot.

Example Wizard test setup:

```mcfunction
/rpg class wizard
/give @s rpg_project:wizard_staff 1
/rpg equip
/give @s rpg_project:wizard_dagger 1
/rpg equip
/rpg status
```

## Not Implemented Yet

- Equipment screen for the three RPG weapon slots.
- Visible Draw/Sheathe weapon models and player animations.
- Playable Wizard or Ninja skills and skill trees.
- Skill Point spending UI.
- Full application of SA/CC/Grab through playable skills.
- Mob level display and data-driven world-region rules.
- Final stat, EXP, stamina, weight, and damage balancing.
- Gem, Apotheosis, enchantment, and enhancement integration.

## Build And Run

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```
