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
- [x] Source-audit and implement Healing Aura I-V with generic heal-only payloads, self plus optional
  player-ally targeting, explicit 52 SP progression, production definitions, and regression coverage.
- [ ] Run Healing Aura Rank V client acceptance for self HP/MP recovery, ally filtering, line of sight,
  cooldown persistence, and camera-independent body facing.
- [x] Confirm Healing Aura Rank V self HP recovery (`90.8/454 = 20%`), mob rejection, heal-only
  execution, auto-draw, hit tick, recovery, and completion from client logs.
- [x] Source-audit and implement Healing Lighthouse I-IV with three support pulses, separate self/ally
  HP recovery, once-per-pulse caster MP recovery, ally-only AoE filtering, and regression coverage.
- [ ] Run Healing Lighthouse Rank IV client acceptance for three pulses, movement cancel, resource
  accounting, cooldown, Mob exclusion, and multiplayer ally behavior.
- [x] Confirm Healing Lighthouse Rank IV three-pulse timing, 30% self HP recovery, HP/MP maximum
  clamps, recovery completion, no damage path, and warmed resolver performance from client logs.
- [x] Add Resurrection lifecycle API skeleton, verified rank metadata, and an explicit metadata-only
  gate without intercepting player death or reusing combat CC downed state.
- [x] Audit and implement Magical Shield I-IV with generic timed mana absorption, CC Resistance,
  partial protection when MP is insufficient, structured logs, `/rpg status`, and regression coverage.
- [x] Run Magical Shield Rank IV client acceptance for 25% MP absorption, zero/insufficient MP,
  60-second expiry, 30-second resistance expiry, cooldown rejection, and bypass damage.
- [x] Select, source-audit, and implement Protected Area I-V after Magical Shield acceptance.
- [ ] Run Protected Area Rank V client acceptance and keep multiplayer ally-cap coverage pending.
- [x] Source-audit and implement Spellbound Heart I-V with server-timed MP recovery and Movement Speed.
- [ ] Run Spellbound Heart Rank V client acceptance for 250 MP recovery every 10 seconds, +10%
  Movement Speed, refresh, cooldown, expiry, relog/dimension clear, and zero cast cost.
- [x] Source-audit and implement Dagger Stab I-V with Main+Sub weapon validation, two melee hits,
  Critical, Stiffness, Counter Attack, reduced-damage cooldown recast, and regression coverage.
- [ ] Run Dagger Stab Rank V client acceptance for weapon rejection, two-hit timing, normal effects,
  cooldown recast suppression, five-target cap, and detached-camera facing.
- [x] Add the generic taunt-beacon runtime and implement Magic Lighthouse I-III with bounded aggro,
  Mob profile filtering, compatibility cancellation, replacement, cleanup, and regression coverage.
- [ ] Run Magic Lighthouse Rank III client acceptance for placement, pulse targeting, Boss exclusion,
  ten-target cap, expiry, replacement, logout/dimension cleanup, and placeholder rendering.

## World Core Vanilla Combat Bridge

- [x] Interim: SA/Iframe cancel Vanilla knockback and Protected Area SA covers its full cast window.
- [x] Phase A: add per-hit `CombatImpactContext` and source classification.
- [x] Phase B: route RPG and Vanilla protection decisions through one resolver.
- [x] Phase C: add directional FG handling for Vanilla knockback and explosions.
- [x] Phase D: add explicit Vanilla enchantment policy for RPG combat and RPG weapons.
- [x] Phase E: expose mod compatibility hooks with a safe Vanilla fallback.
- [x] Phase F: add Perfect Guard derived status, debug commands, logs, and acceptance matrix.

Detailed plan: `docs/WORLD_CORE_VANILLA_COMBAT_BRIDGE_PLAN.md`.

## Do Not Guess

- SP costs, prerequisites, ambiguous MCP fields, cooldown-recast multipliers, and missing skill damage.
- BDO distance or animation frames when only tooltip combat values are sourced.
- Animation timing as combat authority; GeckoLib remains presentation-only.
- [x] Source-audit and implement Speed Spell I-III with a generic timed speed-buff state.
- [ ] Run Speed Spell Rank III client acceptance and deferred multiplayer ally-cap test.
- [x] Source-audit and implement Sage's Memory with a generic Main-skill cast-time override.
- [ ] Run Sage's Memory client acceptance with a single-hit and a multi-hit follow-up.
- [x] Implement Magical Evasion I-V with audited Stamina/SP ranks, omnidirectional collision-clipped
  movement, and a PvE-only iframe contract.
- [ ] Client-test Magical Evasion in all four directions, against a wall, at insufficient Stamina,
  and against both Mob and Player damage sources.
- [x] Implement Teleport I-III with audited SP/cooldowns, BRPG-scaled Stamina, full Iframe,
  collision-safe server movement, and directional/cursor input paths.
- [ ] Client-test Teleport forward/backward, cursor destination, wall clipping, cooldown, Stamina,
  Iframe, Movement Speed, and detached-camera stability.
- [x] Add data-driven instant, confirm, and channel aim modes with server-owned targeting sessions.
- [x] Move Teleport mouse destinations to `Shift+Space -> Ctrl+LMB` confirmation without consuming
  Stamina or cooldown during target selection.
- [x] Allow Meteor Shower to accept validated ground-anchor updates for later hit windows during casting.
- [ ] Client-test targeting timeout/cancel, rapid confirmation, invalid range, and Meteor multi-anchor impacts.
