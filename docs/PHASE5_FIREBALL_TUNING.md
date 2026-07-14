# Phase 5 Fireball Initial Tuning

These values are the first BRPG spatial/timeline baseline. They are intentionally independent
from BDO damage percentages and remain datapack-tunable.

| Field | Fireball | Fireball Explosion |
| --- | ---: | ---: |
| Range | 24 blocks | 24 blocks |
| Projectile speed | 1.25 blocks/tick | N/A; resolves at linked impact |
| Impact radius | 1.75 blocks at projectile collision | 2.75 blocks |
| Cast | 8 ticks | 4 ticks |
| Release/hit | Tick 8 | Tick 3 |
| Recovery | 6 ticks | 8 ticks |
| Movement | Walk | Rotate-only |
| Fireball link | Grants 140 ticks on cast start | Requires and consumes on cast start |

Fireball target caps scale by rank as `4/5/6/7`. Fireball Explosion currently caps each impact at
10 targets. These caps are datapack values and do not alter the damage formula per target.

`AIM_PROJECTILE` hit windows now create a transient server-authoritative projectile. It advances by
`projectile_speed` each server tick, clips blocks before entities, and intersects exact entity
bounding boxes without aim-assist inflation. Flame particles are the Phase 5 placeholder visual.

Fireball rank coefficients use provisional BRPG progression ending at the audited Fireball IV value
`1150% x2 -> 2.1448 total`. Explosion ends at the audited rank III value
`2500% x2 -> 3.1623 total`. Lower-rank scaling remains balance data, not a claimed BDO tooltip value.
