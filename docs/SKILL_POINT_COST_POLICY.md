# BRPG Skill Point Cost Policy

Status: trial policy, 2026-07-16

Skill Point earning remains independent from character level. Level gates whether a rank may be
learned; available SP pays for that rank. SP is earned through Skill EXP from combat.

## Datapack Tiers

`data/rpg_project/rpg_world_core/skill_progression.json` defines rank costs:

| Tier | Rank 1 | Rank 2 | Rank 3 | Rank 4 | Rank 5 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Basic | 10 | 20 | 30 | 40 | 50 |
| Core | 25 | 40 | 60 | 80 | 100 |
| Advanced | 50 | 75 | 100 | 125 | 150 |
| Ultimate | 100 | 150 | 250 | 350 | 500 |

Catalog entries select a tier with `sp_tier`. A rank-level `sp_cost` overrides the tier for an
exceptional skill. A playable rank with neither a resolved tier cost nor explicit cost is rejected.

Generic MCP `cost` is not treated as SP because several Wizard records use it for MP.

## Current Phase 5 Trial

- Fireball, Basic ranks I-IV: 100 SP total.
- Fireball Explosion, Advanced ranks I-III: 225 SP total.
- Concentrated Magic Arrow, Core ranks I-III: 125 SP total.
- Multiple Magic Arrows, Advanced rank I: 50 SP total.
- Mana Absorption, Core ranks I-III: 125 SP total.
- Lightning, Basic ranks I-V: 150 SP total.
- Lightning Chain, Core ranks I-IV: 205 SP total.
- First three skills subtotal: 450 SP.
- Current seven playable skills total: 980 SP.

Client acceptance is tracked separately from catalog playability. This table is a balance checkpoint,
not a promise that every future skill must use an unmodified tier cost.
