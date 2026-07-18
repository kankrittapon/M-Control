# Phase 5 Multiple Magic Arrows Initial Tuning

## Audited Source Contract

- Stable ID: `rpg_project:wizard_multiple_magic_arrows`
- Preceding skill: Magic Arrow V
- Required level: 49
- MP: 100
- Cooldown: 11 seconds
- Damage: 1581% x2
- Critical Hit Rate: +100%
- Maximum targets: 10
- Usable during cooldown with reduced damage
- Source: BDO Codex skill 855, checked 2026-07-18; MCP audit status `verified`

The generic source `cost=21` is not treated as BRPG SP. BRPG assigns the single rank to the
Advanced tier for an initial cost of 50 SP.

## BRPG Provisional Delivery

- Two aim projectiles released on ticks 3 and 4 of a 5-tick cast.
- Five-tick recovery, walk movement, and aim tracking through the final release.
- Range 24 blocks, speed 1.5 blocks/tick, impact radius 1.5 blocks, maximum 10 targets.
- BDO conversion produces coefficient `sqrt(1.581) = 1.2574` per hit.
- Cooldown recast deals 35% damage and disables Critical, Special Attack, CC, Smash, Status,
  resource payloads, and protection windows.

Range, projectile speed, cast/release/recovery ticks, turn speed, impact radius, and the 35% recast
multiplier are BRPG balance values rather than claims about BDO world units or animation frames.
