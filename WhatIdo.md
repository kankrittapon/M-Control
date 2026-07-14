# What We Did

Updated: 2026-07-14

## Foundations

- Built server-authoritative World Core combat for Player and Mob: AP/Magic scaling, Accuracy,
  Critical, Evasion, DR, FG, SA, Iframe, CC budget/immunity, Smash, special attacks, and Status.
- Added NORMAL, ELITE, BOSS, and UNSTOPPABLE control profiles with datapack compatibility for
  Vanilla and modded LivingEntity types.
- Added level 1-100, persistent EXP, independent Skill EXP/SP, class/spec, equipment, stamina,
  training stats, learned ranks, and persistent cooldowns.
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

## Verification

- Latest `gradlew test build`: passed.
- Latest `gradlew runGameTestServer`: all 12 required tests passed.
- Runtime loaded 19 definitions across 12 stable skill IDs.

Current continuation point: `TASK.md`.
