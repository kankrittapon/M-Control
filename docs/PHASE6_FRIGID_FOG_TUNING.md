# Phase 6 Frigid Fog Tuning

Frigid Fog I-IV is a self-centered cold AoE. Source-locked values are MP cost, cooldown, damage,
accuracy, three hits, Freezing, Down Attack, Super Armor, and target resource drain.

| Rank | MP | Cooldown | BDO damage | BRPG coefficient per hit | Accuracy | Drain |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 120 | 380 ticks | 205% x3 | 1.4318 | 10% | 2% |
| II | 130 | 340 ticks | 292% x3 | 1.7088 | 12% | 3% |
| III | 140 | 320 ticks | 379% x3 | 1.9468 | 14% | 4% |
| IV | 160 | 300 ticks | 466% x3 | 2.1587 | 16% | 5% |

The conversion is `sqrt(BDO percent / 100)`. The target resource drain and Freeze are attached only
to the final hit so the three-hit source damage resolves before World Core frozen immunity begins.
Frigid Fog uses the Core SP tier and requires Freeze I.

Minecraft-only provisional values are radius `5`, cast `12`, hit ticks `8/9/10`, recovery `6`, and
ten targets. Movement is locked during cast, skill-cancel is allowed before the first hit, and Super
Armor is active through the final hit. Slow is intentionally absent because it exists only in the
design note and not in the audited combat values.
