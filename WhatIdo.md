# What We Did

Updated: 2026-07-19

## Foundations

- Built server-authoritative World Core combat for Player and Mob: AP/Magic scaling, Accuracy,
  Critical, Evasion, DR, FG, SA, Iframe, CC budget/immunity, Smash, special attacks, and Status.
- Added NORMAL, ELITE, BOSS, and UNSTOPPABLE control profiles with datapack compatibility for
  Vanilla and modded LivingEntity types.
- Added level 1-100, persistent EXP, independent Skill EXP/SP, class/spec, equipment, stamina,
  training stats, learned ranks, and persistent cooldowns.
- Added datapack-backed Basic/Core/Advanced/Ultimate SP tiers with per-rank overrides.
- Added data-driven skill catalog/runtime, targeting shapes, multi-hit timelines, resources,
  protection windows, projectile collision, impact AoE, skill links, and structured debug logs.

## Controls And Camera

- Third-person shoulder camera remains anchored to the player with smoothing, collision, and zoom.
- Idle mouse movement orbits the camera without rotating the player.
- Manual WASD is camera-relative and supports eight directions. While moving, body yaw follows the
  actual reticle ray; camera yaw remains independent.
- Hold Ctrl for cursor commands. Ctrl+LMB selects/clears targets; Ctrl+RMB moves to ground/entity.
- Ctrl+LMB camera drag was removed. Selected targets remain UI/context state, not skill lock-on.
- Click-to-move and auto-attack rotate the body without forcing camera yaw.
- Skill facing is data-driven: none, aim on cast, target on cast, or track aim until release.

## Phase 5 Skill Work

- Converted sourced BDO percentages through a configurable BRPG coefficient formula.
- Implemented Fireball ranks I-IV and Fireball Explosion ranks I-III.
- Fireball is a quick aim projectile; Explosion consumes its stored impact anchor for AoE.
- Implemented Concentrated Magic Arrow ranks I-III from audited values and added per-hit hit/critical
  chance modifiers to the damage context.
- Frozen the eight-skill Wizard Main Batch A roster without inventing missing combat data.

## Phase 6 Skill Work

- Expanded Wizard Main to 20 playable entries and 12 metadata-only entries.
- Added generic heal-only targeting for Healing Aura I-V and three-pulse support AoE for Healing
  Lighthouse I-IV, including separate caster/ally HP values and MP recovery.
- Added a standalone Resurrection lifecycle skeleton and kept the skill unplayable until a real
  dead/downed-player flow exists.
- Implemented Magical Shield I-IV through a generic defensive payload: damage-to-MP conversion,
  insufficient-MP fallback, timed CC Resistance, vanilla/RPG damage integration, and debug logs.
- Implemented Protected Area I-V with generic timed damage reduction for self and nearby allies.
- Completed the Vanilla Combat Bridge through Phase F: shared impact classification and protection
  resolution, derived Perfect Guard, RPG-combat enchant policy, compatibility events, deterministic
  impact probes, and live Zombie/Zoglin, Skeleton, and Creeper acceptance.
- Updated the explicit playable skill budget to 3257 SP.

## Verification

- Latest `gradlew test`: 79 tests passed.
- Latest `gradlew build`: passed.
- Latest `gradlew runGameTestServer`: all 18 required tests passed.
- Runtime loaded 90 definitions across 29 stable skill IDs and 32 catalog entries
  (`20 playable`, `12 metadata-only`).

Current continuation point: `TASK.md`.
