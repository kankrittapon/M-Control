# Phase 6 Residual Lightning Tuning

Residual Lightning I-IV is a linked ground AoE and cannot be cast independently. A successful
Lightning hit grants `rpg_project:residual_lightning_ready` for 100 ticks and stores its validated
ground impact. Residual consumes that link and always resolves at the stored impact.

| Rank | MP | Cooldown | Initial damage x5 | Finisher x2 | Critical | Drain/finisher |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 60 | 300 ticks | 280% / 1.6733 | 336% / 1.8330 | 50% | 1% |
| II | 70 | 280 ticks | 377% / 1.9416 | 452.4% / 2.1270 | 57% | 1.2% |
| III | 80 | 260 ticks | 474% / 2.1772 | 568.8% / 2.3850 | 64% | 1.5% |
| IV | 90 | 220 ticks | 571% / 2.3896 | 685.2% / 2.6176 | 75% | 2% |

The conversion is `sqrt(BDO percent / 100)`. Bound is requested only on the first initial hit.
Shocked uses 20% Slow for 100 ticks on all seven hits. Target resource drain is attempted only by
the two finishing hits; mobs without primary resources return `UNSUPPORTED_TARGET`.

Minecraft-only provisional values are radius `4.5`, initial ticks `2/3/4/5/6`, finisher ticks `8/9`,
cast `10`, recovery `4`, and ten targets. Residual uses the Core SP tier and requires Lightning I.
