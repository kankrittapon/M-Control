# Current Tasks

Checkpoint date: 2026-07-14

## Resume Here

Phase 5 Wizard Main Batch A is active. World Core and the generic Skill Runtime are treated as
stable foundations; change them only for a reproduced bug or a required production-skill contract.

Run the unified acceptance checklist in `docs/TEST_CHECKLIST.md` before implementing the next skill.

## Completed This Week

- Fireball and Fireball Explosion production definitions, projectile collision, impact anchor, AoE,
  link consume/expiry, icons, facing, and regression tests.
- BDO percentage to BRPG coefficient conversion.
- Per-hit AoE shape/radius/target cap, MP recovery/drain, cooldown-recast, Smash, and transition contracts.
- Camera-relative eight-direction movement, idle free camera, reticle-facing movement, and skill-facing policies.
- Concentrated Magic Arrow definitions for ranks I-III and per-hit hit/critical chance bonuses.
- Batch A roster frozen at eight stable skill IDs.
- `gradlew test build` passed and all 12 required GameTests passed.

## Next Tasks

- [x] Define the datapack-backed BRPG Skill Point tier policy. Generic MCP `cost` is never mapped
  to SP; Wizard records commonly use that field for MP.
- [x] Approve SP costs for Concentrated Magic Arrow I-III and enable the production catalog entry.
- [ ] Run its operator acceptance path; normal learning remains gated by unfinished Magic Arrow I.
- [ ] Run Concentrated Magic Arrow client acceptance and adjust provisional range/timing only from play feel.
- [ ] Finish Fireball rejection-path acceptance: insufficient MP, cooldown, missing/expired link, relog.
- [ ] Audit and implement Multiple Magic Arrows, including cooldown-recast damage.
- [ ] Audit and implement Mana Absorption, resolving the 7s versus 13s source conflict.
- [ ] Implement Lightning, Lightning Chain, and Meteor Shower after their damage/targeting contracts are frozen.
- [ ] Complete Batch A profile tests for Normal, Elite, Boss, and Unstoppable.
- [ ] Commit and push the Phase 5 checkpoint after acceptance.

## Do Not Guess

- SP costs, prerequisites, ambiguous MCP fields, cooldown-recast multipliers, and missing skill damage.
- BDO distance or animation frames when only tooltip combat values are sourced.
- Animation timing as combat authority; GeckoLib remains presentation-only.
