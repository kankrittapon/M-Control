# Phase 6 Combat Performance Checkpoint

## Goal

Prove that dense combat remains bounded before importing another production skill. A seven-target
hit is normal gameplay; performance must be controlled by spatial queries, per-hit target caps, and
bounded observability rather than by disabling AoE behavior.

## Existing Bounds

- Hit resolution queries only the skill AABB, circle, cone, line, or chain neighborhood.
- Every production hit declares `max_targets`; candidates are filtered before the damage pipeline.
- A target is processed at most once per hit window unless the definition intentionally has multiple hits.
- Projectile and linked-skill state already have finite lifetime and cleanup paths.

## Work Items

- Add resolver timing counters for development builds: candidate count, accepted count, and elapsed time.
- Add a config switch for verbose per-target damage/resource logs; keep one cast/window summary available.
- Replace full candidate sorting with a bounded nearest-target selection when candidate density is much
  larger than `max_targets`, while preserving deterministic distance ordering.
- Add automated resolver stress coverage for 10, 25, and 50 nearby LivingEntity candidates.
- Add a repeated multi-hit soak test that verifies target caps, state cleanup, and stable memory usage.
- Keep HUD and particles capped independently from server damage targets.

## Acceptance

- Target caps are never exceeded at 10/25/50 candidate density.
- Resolver timing does not scale with entities outside the skill's spatial query.
- No active cast, projectile, link, CC, or Status state remains after completion/timeout.
- Disabling verbose logs materially reduces output without changing combat results.
- `gradlew test`, `build`, and required GameTests remain green.

## Client Commands

Run each density separately and clear it before moving to the next one:

```text
/rpg debug perf-spawn 10
/rpg debug perf-clear
/rpg debug perf-spawn 25
/rpg debug perf-clear
/rpg debug perf-spawn 50
/rpg debug perf-clear
```

The command accepts only `10`, `25`, or `50`. It creates stationary, silent, persistent Zombies with
100,000 HP in a compact grid beginning two blocks ahead of the player and tags only those entities for
cleanup. Performance targets never award Character XP, Skill XP, SP, or normal progression rewards.

Use Earthquake Rank IV for the radius/cap-7 probe and Earth's Response Rank III for the line/cap-7
probe. Each hit window must emit one summary like:

```text
[RPG Skill Perf] skill=... candidates=... accepted=7 resolverMicros=... slow=false
```

`candidates` may exceed seven; `accepted` must never exceed the hit definition's `max_targets`.
The first invocation may be slower due to JVM warm-up, so compare at least three casts per density.

Runtime log tuning is datapack-backed in `rpg_world_core/skill_runtime.json`:

```json
"observability": {
  "log_hit_performance": true,
  "log_per_target_results": true,
  "slow_resolver_micros": 1000
}
```

Set `log_per_target_results` to `false` for soak tests or normal play to retain cast/window summaries
without writing one damage, Status, Smash, and resource line per target.

## Non-Goals

- Do not lower production target caps merely to hide a performance issue.
- Do not move damage authority to the client or animation system.
- Do not globally scan every entity in the world for a local skill hit.

## Acceptance Evidence

Client run on 2026-07-19 created densities `10`, `25`, and `50`. The highest observed line queries
resolved `41 candidates -> 7 accepted` in `199 us` and `34 -> 7` in `125 us`; subsequent warmed
queries remained below the configured `1000 us` slow threshold. The only slow sample was the first
JVM-warm-up query (`3056 us`) and did not repeat. Target caps never exceeded seven, cleanup removed
all 50 tagged targets, and no resolver, memory, or server exception occurred.

The run also exposed two debug-tool issues now patched: performance targets no longer grant progression
and use 100,000 HP so repeated casts preserve density; `/rpg debug cast` now logs a structured
`debug-cast-error` with skill ID, player, and stack trace instead of returning only Brigadier's generic
unexpected-error message.
