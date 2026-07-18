# Lightning Tuning

Lightning is a ground-targeted lightning strike. It does not bounce or choose a new target between hits;
that behavior belongs to Lightning Chain.

| Rank | Level | SP | MP | Cooldown | BDO damage | BRPG coefficient |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 1 | 10 | 20 | 140 ticks | 944% x3 | 0.9716 x3 |
| II | 21 | 20 | 30 | 140 ticks | 1101% x3 | 1.0493 x3 |
| III | 29 | 30 | 40 | 120 ticks | 1259% x3 | 1.1221 x3 |
| IV | 37 | 40 | 50 | 120 ticks | 1416% x3 | 1.1900 x3 |
| V | 46 | 50 | 60 | 100 ticks | 1574% x3 | 1.2546 x3 |

## Provisional BRPG Delivery

- Ground range: 20 blocks
- Circle radius: 3 blocks
- Maximum targets: 10 per hit window
- Cast/recovery: 10/5 ticks
- Hit windows: ticks 7, 8, and 9 at one fixed server-validated anchor
- Movement: rotate-only; body faces the selected ground direction once without rotating the camera
- First hit applies Stun; all hits qualify for Down Attack and refresh Shocked
- Shocked uses the current Slow framework at 20% for 100 ticks
- Cooldown recast uses 50% damage, keeps Shocked and Down Attack, and disables Stun

The radius, timing, Slow potency, and cooldown-recast multiplier are tuning values rather than claims
about exact BDO world units. They remain datapack-owned for client feel testing.
