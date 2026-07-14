# Phase 5 Concentrated Magic Arrow Initial Tuning

## Audited Source Values

| Rank | Level | MP | Cooldown | BDO damage | Critical | Targets |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 10 | 40 | 5s | 635% x2 | 20% | 3 |
| II | 26 | 30 | 5s | 767% x2 | 30% | 4 |
| III | 38 | 20 | 4s | 899% x2 | 40% | 5 |

The prerequisite is Magic Arrow I. BRPG currently selects the PvE Knockdown payload and Down Attack.
BDO PvP-only Stiffness is not imported as a second mode-specific payload.

Sources: BDO Codex skill IDs 887, 888, and 889, audited on 2026-07-14.

## BRPG Initial Delivery Tuning

- Aim projectile, range 24 blocks, speed 1.5 blocks/tick.
- Cast 6 ticks, release tick 5, recovery 4 ticks.
- Walk movement; body tracks aim until release at 60 degrees/tick.
- Impact circle radius 1.25 blocks.
- No movement cancellation after acceptance.
- Cooldown recast remains disabled until its reduced-damage percentage is approved.

These delivery values are provisional gameplay tuning, not claims about BDO world units or animation
frames. The catalog remains non-playable until BRPG SP costs and client acceptance are approved.
