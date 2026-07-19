# Phase 6 Checkpoint - 2026-07-19

This checkpoint freezes the current Wizard Batch B work and the combat-performance patch before
another production skill is imported.

## Fixed

- Dense AoE selection no longer performs a full sort when candidates exceed `max_targets`; the
  resolver keeps the nearest bounded set and returns it in deterministic distance order.
- Performance-test entities no longer grant Character XP, Skill EXP, or SP and now have enough
  health for repeated casts at the same density.
- `/rpg debug cast` now records the player, skill ID, exception, and stack trace when command
  execution fails instead of leaving only Brigadier's generic error in chat.
- Entity-targeted skill validation includes server-side line of sight.
- Attribute bridge writes use an epsilon guard to avoid redundant synchronized value updates.
- Mob overlay range checks use squared distance.

## Added

- Production definitions, progression data, tests, and client test notes for Freeze, Frigid Fog,
  Blizzard, Lightning Storm, Residual Lightning, Earthquake, Earth's Response, and Staff Attack.
- Directional cast context for Earth's Response, including server-validated lateral movement,
  collision clipping, and forward-facing attacks without moving the camera.
- Combat Debug HUD and development visuals for cast state, movement policy, resources, protection,
  CC, target/aim state, and camera mode.
- Resolver observability for candidate count, accepted count, elapsed microseconds, and slow-query
  classification.
- Datapack controls for performance summaries, per-target logs, and the slow-query threshold.
- Operator stress commands for 10, 25, and 50 stationary targets plus tagged cleanup.
- Automated nearest-target density coverage and expanded Mob profile GameTests.

## Verified

- [x] Unit tests pass (`55` tests).
- [x] Forge build passes.
- [x] Required GameTests pass (`14/14`).
- [x] Earth's Response Rank III moves left/right by up to 2 blocks and collision-clips safely.
- [x] Earth's Response keeps forward aim separate from lateral movement and preserves camera angle.
- [x] Floating followed by Air Attack works across its two hit windows.
- [x] Resolver target cap remains 7 at 10/25/50-target stress density.
- [x] Warm resolver samples remain below the configured 1000 microsecond threshold.
- [x] Stress-target cleanup removes only tagged debug targets.
- [x] Debug stress targets do not award progression.

## Remaining Manual Checks

- [ ] Finish Earthquake radius-boundary, pull feel, movement-cancel, and cooldown-rejection checks.
- [ ] Finish Lightning Storm Rank III acceptance.
- [ ] Verify Residual Lightning's immediate second-cast cooldown rejection.
- [ ] Saturate Freeze Rank V with five valid targets and run the supplementary frozen-immunity probe.
- [ ] Retest Blizzard Rank IV after the test character reaches level 52.
- [ ] Reproduce any future debug-cast failure and inspect the new `debug-cast-error` stack trace.

## Next Gate

Select and source-audit the next Wizard production skill. Do not invent missing BDO values; freeze
its combat contract before adding runtime JSON.
