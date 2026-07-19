# Project Phase Status

Checkpoint date: 2026-07-19

| Phase | Status | Notes |
| --- | --- | --- |
| World Core Combat Foundation | Complete | Shared damage, protection, CC, Smash, Status, Mob profiles |
| Skill Runtime Foundation | Complete | State machine, resources, cooldowns, targeting, projectile and links |
| Skill Catalog And Progression | Complete foundation | 32 Wizard Main catalog entries; production skills remain gated by data |
| Phase 5 Wizard Main Batch A | Acceptance | 8/8 stable skills have playable combat definitions |
| Phase 6 Wizard Main Batch B | In progress | Production skills implemented through Earth's Response; performance gate passed |
| Skill Tree UI | Not started | Begins after server progression and enough production skills are stable |
| Action Bar And Native Inputs | Not started | Native combos and quick slots must call the same stable skill ID |
| GeckoLib Integration | Planned | See `AnimationP.md`; presentation must not control damage timing |

## Phase 5 Batch A

1. Fireball: playable, client-tested, regression-covered.
2. Fireball Explosion: playable, client-tested, regression-covered.
3. Concentrated Magic Arrow: playable, client-tested, regression-covered.
4. Multiple Magic Arrows: playable, client-tested, regression-covered.
5. Mana Absorption: playable, client-tested, regression-covered.
6. Lightning: playable, client-tested, regression-covered.
7. Lightning Chain: playable, client-tested, regression-covered.
8. Meteor Shower: playable, client-tested, regression-covered.

Phase 6 Batch B has started with Freeze I-V. Rank V client logs confirm MP cost, cooldown,
two-hit timing, damage, and Freeze application. Five-target saturation and a manual damage-immunity
probe remain supplementary coverage; World Core already covers frozen immunity in automated tests.
Frigid Fog I-IV has production definitions, regression coverage, and Rank IV client acceptance.
The provisional radius and timings produced three self-AoE hit windows with ten targets as designed.
Blizzard I-IV now has a seven-pulse channel definition and regression coverage. Radius, Slow potency,
and pulse timing remain provisional until client feel testing.
Blizzard Rank III client logs confirmed all seven pulses, ten targets, Slow refresh, cooldown, and
movement cancellation. Lightning Storm I-III now has production definitions and regression coverage.
Residual Lightning I-IV now uses a consumed server-side link and the validated Lightning impact anchor.
Rank IV client logs confirm seven AoE windows, first-hit Bound, per-hit Slow, and both resource-drain
finishers. Normal, Elite, Boss, and compatibility-overridden Unstoppable profiles now have GameTest
coverage without assigning an arbitrary vanilla entity to the production Unstoppable tag.
Earthquake I-IV now has production definitions and regression coverage. Its source values are frozen;
radius, Pull strength, compact pulse timing, and cancellation remain provisional until client testing.
Earth's Response I-III now has production definitions and a directional cast context that keeps
lateral movement separate from forward aim. Rank III client acceptance confirmed two-block left/right
hops, server collision clipping, forward line hits, Floating into Air Attack, cooldown, and camera stability.
The combat performance checkpoint for dense 10/25/50-target scenes now passes: bounded nearest
selection preserved cap 7
with 41 candidates in 199 us, and debug stress targets are progression-neutral and cleanup-safe.

Checkpoint details: `docs/PHASE6_CHECKPOINT_2026-07-19.md`.

Detailed gates: `docs/PHASE5_BATCH_A_MANIFEST.md` and `docs/PHASE5_CHECKLIST.md`.
