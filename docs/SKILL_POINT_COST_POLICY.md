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
- Current twenty-seven playable skills total: 3598 SP, including Meteor Shower's Ultimate-tier 500 SP,
  Freeze's Basic-tier 150 SP, Frigid Fog's Core-tier 205 SP, Blizzard's Advanced-tier 350 SP,
  Lightning Storm's Core-tier 125 SP, and Residual Lightning's Core-tier 205 SP.
- Earthquake uses the Advanced tier for 350 SP across ranks I-IV.
- Earth's Response uses the Basic tier for 60 SP across ranks I-III.
- Healing Aura uses audited explicit costs `3/7/12/14/16` for 52 SP across ranks I-V.
- Healing Lighthouse uses audited explicit costs `5/8/14/18` for 45 SP across ranks I-IV.
- Magical Shield uses audited explicit costs `3/5/8/12` for 28 SP across ranks I-IV.
- Protected Area uses audited explicit costs `5/9/13/18/23` for 68 SP across ranks I-V.
- Spellbound Heart uses audited explicit costs `7/10/12/15/18` for 62 SP across ranks I-V.
- Speed Spell uses audited explicit costs `5/9/14` for 28 SP across ranks I-III.
- Sage's Memory uses its audited single-rank cost of 10 SP.
- Dagger Stab uses audited explicit costs `0/7/12/14/16` for 49 SP across ranks I-V.
- Magic Lighthouse uses the Core tier for 125 SP across ranks I-III.

Client acceptance is tracked separately from catalog playability. This table is a balance checkpoint,
not a promise that every future skill must use an unmodified tier cost.
