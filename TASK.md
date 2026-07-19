# Current Tasks

Checkpoint date: 2026-07-19

## Resume Here

Phase 5 Wizard Main Batch A is accepted and Phase 6 Wizard Main Batch B is active. World Core and
the generic Skill Runtime are treated as stable foundations; change them only for a reproduced bug
or a required production-skill contract.

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
- [x] Implement Lightning, Lightning Chain, and Meteor Shower after their damage/targeting contracts are frozen.
- [x] Run client acceptance for Meteor Shower's two opening effects and two meteor descents.
- [x] Run Freeze Rank V client acceptance for resource, cooldown, two-hit timing, damage, and CC.
- [ ] Saturate Freeze Rank V with five targets and manually verify damage immunity during its short
  frozen window using a separate immediate debug hit.
- [x] Implement Frigid Fog I-IV production definitions, progression, source audit, and regression test.
- [x] Run Frigid Fog Rank IV client acceptance for self-AoE radius, ten-target cap, three hits,
  Freeze, SA, recovery, and cooldown rejection.
- [x] Implement Blizzard I-IV seven-pulse channel definitions, progression, visuals, and regression test.
- [x] Run Blizzard Rank III client acceptance for radius, seven pulses, Slow, movement cancel, and cooldown.
- [ ] Retest Blizzard Rank IV values after the test character reaches level 52.
- [x] Implement Lightning Storm I-III burst definitions, progression, visuals, and regression test.
- [ ] Run Lightning Storm Rank III client acceptance for five pulses, target cap, Stiffness, Slow, and cooldown.
- [x] Implement Residual Lightning I-IV linked-anchor definitions, visuals, progression, and tests.
- [~] Residual Lightning Rank IV logs confirm missing/consumed link, fixed Lightning anchor, seven hits,
  Bound, Slow, and finishers; immediate second-cast/cooldown rejection remains a manual check.
- [x] Complete automated profile coverage for Normal, Elite, Boss, and compatibility-overridden
  Unstoppable entities.
- [x] Complete the Earthquake I-IV source audit without enabling the skill. Current rank damage,
  MP, cooldown, hit count, target cap, recovery, protection, CC, and retired MCP fields are frozen
  in `docs/research/BDO_WIZARD_EARTHQUAKE_AUDIT.md`.
- [x] Implement Earthquake I-IV with provisional radius/pulse timing, profile-aware Pull, PvE/PvP
  CC mapping, flat MP recovery, Super Armor, Advanced SP tier, and regression coverage.
- [ ] Run Earthquake Rank IV client acceptance and tune radius, pull strength, pulse timing, and
  cancellation from observed play feel.
- [x] Add the Earthquake client acceptance checklist and expected log evidence.
- [x] Audit and implement Earth's Response I-III with a server-validated lateral-side cast context,
  forward line hits, Floating/Air Attack sequencing, progression, tests, and client checklist.
- [x] Run Earth's Response Rank III acceptance for left/right movement, forward aim, collision,
  Floating, Air Attack, cooldown, and camera stability. Rank III now hops 2 blocks per side and
  remains collision-clipped by the server.
- [x] Run the Phase 6 combat performance checkpoint for 10/25/50 nearby targets before importing
  another production skill. Bounded nearest selection, resolver metrics, configurable per-target logs,
  automated density coverage, repeatable Client commands, and progression-neutral 100,000-HP targets
  are implemented. Client evidence capped `41 -> 7` at `199 us` and `34 -> 7` at `125 us`.
- [x] Prepare the Phase 6 combat-performance checkpoint, verification evidence, and release notes
  before importing another skill.

## Do Not Guess

- SP costs, prerequisites, ambiguous MCP fields, cooldown-recast multipliers, and missing skill damage.
- BDO distance or animation frames when only tooltip combat values are sourced.
- Animation timing as combat authority; GeckoLib remains presentation-only.
