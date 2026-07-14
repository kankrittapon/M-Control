# M-Control

Minecraft Forge 1.20.1 action-RPG control and world-core mod. The project currently focuses on
server-authoritative progression, combat calculations, camera controls, and reusable class systems.
Playable class skills, models, and animations are intentionally deferred until their catalog data is complete.

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
- Character Level and Skill Points are independent progression tracks.
- Level gates skill ranks; combat Skill EXP from defeating mobs earns Skill Points.
- Skill EXP requirements grow per earned point up to a configurable cap, supporting long-term
  progression with thousands of total Skill Points.
- Separate total, spent, available Skill Points, and partial Skill EXP are persistent.
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

- Every living entity has transient server-authoritative combat state.
- Front Guard protects a configurable 180-degree frontal arc and consumes Guard Gauge.
- Side and rear damage bypass Front Guard; Iframe cancels damage and Crowd Control.
- Super Armor blocks Crowd Control while damage still applies; Grab Immunity remains a skill contract.
- CC types include stiffness, stun, freeze, knockback, knockdown, bound, float, and grab.
- Non-grab CC uses a configurable 2.0-point budget followed by 100 ticks of immunity.
- CC applies movement/action locks, floating and knockback velocity, down/air states, and freeze immunity.
- Local input and mob navigation are suppressed while the server reports an action lock.

### Tagged RPG Damage

- `RpgCombatService` resolves explicit RPG damage contexts exactly once.
- The order is Iframe/Freeze, Accuracy/Evasion, AP or Magic scaling, Critical, stacking special
  attacks, Front Guard, Damage Reduction, health damage, then Crowd Control.
- Back, Down, Air, Speed, and Counter Attack states use data-driven multipliers.
- Vanilla and environmental damage do not receive RPG stat scaling; eligible direct attacks can
  still be stopped by Iframe or Front Guard.

### Mob Level And World Scaling

- Mobs receive a persistent level when first spawned.
- Overworld levels range from 1-60, Nether from 30-80, and End from 60-100.
- Level considers dimension, distance from world spawn, and small random variation.
- Mob level applies stable Max Health and Attack Damage modifiers.
- Saved mobs do not receive duplicate scaling modifiers when reloaded.
- EXP rewards use the mob's resulting Max Health.

### Skill Runtime And Mob Compatibility

- Server-authoritative action states: Sheathed, Drawing, Ready, Casting, Recovery, and Sheathing.
- Skill requests validate class, specialization, weapon set, MP/WP, stamina, cooldown, range, and target.
- Casting a skill while sheathed queues it through the logical Draw transition without spending early.
- Datapack skills support projectile, ray, cone, line, circle, self/ground AoE, entity-targeted,
  movement, multi-hit timing, movement policies, protection windows, CC, and Status payloads.
- MP and WP share one resource runtime while remaining class-specific; stamina stays separate.
- Mob profiles resolve as Normal, Elite, Boss, or Unstoppable. Unknown modded entities default to Normal.
- Boss and Elite compatibility is extendable through entity-type tags without Java changes.
- Burn, Slow, and Defense Down are transient Status effects with refresh, stronger-replace, or stacking rules.
- Debug-only datapack skills exercise every runtime path; production Wizard/Ninja skills are still deferred.

### Skill Catalog And Progression

- Skill metadata is separate from executable combat definitions.
- Learned ranks and spent/available Skill Points persist in player data.
- Spent Skill Points are reconciled from learned ranks after login or datapack reload, so revised
  catalog costs cannot leave progression totals out of sync.
- Learning, upgrading, downgrading, and reset validation is server-authoritative.
- Client requests use replay-protected packets and receive rank, SP, and availability state from the server.
- The Wizard Main MCP catalog contains all 32 stable IDs, names, descriptions, icon source paths,
  rank counts, and known required levels.
- All Wizard Main entries are currently metadata-only and cannot be learned or cast until SP costs,
  prerequisites, and combat definitions are approved.
- Crosshair rays use the entity bounding box exactly. Projectile and line width comes only from the
  skill definition rather than a hidden targeting allowance.

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
src/main/resources/data/rpg_project/rpg_world_core/combat.json
src/main/resources/data/rpg_project/rpg_world_core/skill_runtime.json
src/main/resources/data/rpg_project/rpg_world_core/skill_progression.json
src/main/resources/data/rpg_project/rpg_skills/debug_*.json
src/main/resources/data/rpg_project/rpg_skill_catalog/wizard_*.json
src/main/resources/data/rpg_project/tags/entity_types/control/*.json
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
/rpg addskillxp 1000
/rpg setlevel 50
/rpg protection fg 200
/rpg protection sa 200
/rpg protection iframe 100
/rpg protection grab_immune 200
/rpg debug cc @e[type=zombie,limit=1] stun
/rpg debug hit @e[type=zombie,limit=1] 20
/rpg debug cast rpg_project:debug_ray
/rpg debug cast rpg_project:debug_cone
/rpg debug cast rpg_project:debug_ground
/rpg skills list
/rpg skills list 2
/rpg skills inspect rpg_project:wizard_fireball
/rpg skills upgrade rpg_project:wizard_fireball
/rpg skills downgrade rpg_project:wizard_fireball
/rpg skills reset
```

Protection and debug mutation commands require operator permission level 2.

`/rpg addxp` changes Character EXP only. `/rpg addskillxp` changes Skill EXP only and requires
operator permission. Defeating a mob normally grants both rewards through separate formulas.

The skill list is sorted and paged eight entries at a time. `inspect` shows description, readiness
reason, required levels, and known/unknown SP costs without flooding chat.

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

## Development Roadmap

World Core, the generic Skill Runtime, and skill catalog/progression Phases 1-4 are checkpointed.
The next work proceeds in this order so combat rules remain server-authoritative before UI and
presentation are added.

### Phase 5: Wizard Combat Batch A

- Approve complete source data for the first eight Wizard Main skills; do not invent missing values.
- Add rank SP costs, prerequisites, MP cost, cooldown, cast/recovery timing, movement policy, range,
  targeting shape, hit ticks, and coefficients.
- Cover projectile/ray, cone, ground AoE, and self AoE through `RpgCombatService`.
- Use debug particles only. Gate: all eight skills can be learned, ranked, and cast end to end.

### Phase 6: Wizard Combat Batch B

- Add the next eight data-complete skills with multi-hit behavior and Burn, Slow, Freeze, and
  Defense Down.
- Verify stacking rules and Normal/Elite/Boss/Unstoppable control profiles.
- Gate: 16 of 32 Wizard Main entries are playable with regression coverage.

### Phase 7: Wizard Combat Batch C

- Add the next eight skills emphasizing FG, SA, Iframe, interruption, cancel rules, and movement
  policies.
- Verify auto-draw/recovery and keep camera orientation independent from body rotation.
- Gate: 24 of 32 Wizard Main entries are playable.

### Phase 8: Wizard Combat Batch D

- Add the remaining data-complete Wizard Main skills and keep incomplete entries metadata-only.
- Test rank scaling, cooldown, CC, Status, and interactions across the complete Main tree.
- Gate: all 32 catalog entries have an explicit playable or safely gated state.

### Phase 9: Functional Skill Tree UI

- Add a rebindable `K` control for the Wizard Main tree.
- Display rank, level/SP requirements, prerequisites, and learned/available/locked/metadata-only
  states with placeholder icons where assets are missing.
- Keep learning transactions server-authoritative. Gate: progression can be managed without commands.

### Phases 10-12: Action Bar And Quick Items

- Create a persistent 20-slot RPG action bar separate from the vanilla inventory hotbar.
- Bind slots 1-10 to `1-0` and slots 11-20 to `Alt+1-0` while combat is drawn.
- Restore the vanilla hotbar when combat is sheathed and suppress invalid input during action lock.
- Add inventory `Ctrl+RMB` quick-item binding with server validation and tag-based compatibility.
- Gate: learned skills and real inventory consumables survive reconnect and execute exactly once.

### Phases 13-15: GeckoLib And Presentation

- Follow the separate Blockbench/GeckoLib contract in [`AnimationP.md`](AnimationP.md).
- Pin a Forge 1.20.1-compatible GeckoLib version and add draw, cast, hit, recovery, cancel, and
  sheathe animation events without moving combat timing away from the server.
- Integrate Blockbench Staff, Dagger, Wizard, and weapon animations after event fallback is stable.
- Present global CC/Status on vanilla, GeckoLib, and unknown modded mobs; missing clips must fall
  back to particles/tint and movement lock without errors.
- Gate: multiplayer observes the same logical timeline even when an entity has no custom animation.

### Phase 16: Wizard Main Acceptance

- Test levels 1-100, independent Skill EXP/SP farming, learning/reset, and rank scaling.
- Test the 20-slot action bar, consumables, action-camera/cursor modes, and every mob control profile.
- Verify singleplayer and dedicated multiplayer, then make structured debug logging configurable.
- Gate: every data-complete Wizard Main skill works end to end and incomplete skills remain gated.

Succession, Awakening, Ninja, final assets, and balance work begin only after this acceptance gate.

## Not Implemented Yet

- Equipment screen for the three RPG weapon slots.
- Visible Draw/Sheathe weapon models and player animations.
- Playable Wizard or Ninja combat definitions and skill tree UI.
- Skill Point spending UI.
- Full application of SA/CC/Grab through playable skills.
- Mob level display and data-driven world-region rules.
- Final stat, EXP, stamina, weight, and damage balancing.
- Gem, Apotheosis, enchantment, and enhancement integration.

## Build And Run

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
```
