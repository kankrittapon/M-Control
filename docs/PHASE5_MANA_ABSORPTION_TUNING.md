# Mana Absorption Tuning

## Audited Contract

| Rank | Required level | SP | Cooldown | Max MP recovery | Damage coefficient | Max targets |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 1 | 25 | 240 ticks | 10% | 0.9905 | 4 |
| II | 30 | 40 | 180 ticks | 20% | 1.1189 | 7 |
| III | 45 | 60 | 140 ticks | 30% | 1.1649 | 10 |

Current BDO tooltip data is authoritative over the older MCP cooldown and negative-cost fields.
The BRPG coefficient uses the Phase 5 square-root conversion from BDO damage percentage.

## Provisional Delivery

- Cone range: 8 blocks
- Cast/recovery: 14/6 ticks
- Hit windows: ticks 8 and 12
- Movement: rotate-only
- Facing: track aim until the final hit, capped at 60 degrees per tick
- Protection: Front Guard from tick 0 through 12
- Status: Slow 20% for 100 ticks, refresh policy

MP recovery happens once when the server accepts the cast. It is based on caster maximum MP,
does not scale with target count, and cannot run again from either hit window.

Target MP drain is deliberately absent. It can be added later only for targets that expose the
shared RPG resource capability; Vanilla and unknown modded mobs must not receive a fabricated pool.
