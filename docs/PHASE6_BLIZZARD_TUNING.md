# Phase 6 Blizzard Tuning

Blizzard I-IV is a long stationary self-AoE channel with seven pulses. Source-locked values are MP
cost, cooldown, damage, accuracy, seven hits, continuous target Slow, and target resource drain per
hit. The Main version has no protection or hard CC.

| Rank | MP | Cooldown | BDO damage | BRPG coefficient per hit | Accuracy | Target drain/hit |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 250 | 2400 ticks | 788% x7 | 2.8071 | 2.5% | 1% |
| II | 300 | 2000 ticks | 854% x7 | 2.9223 | 7.5% | 1.5% |
| III | 350 | 1600 ticks | 920% x7 | 3.0332 | 12.5% | 2% |
| IV | 150 | 1200 ticks | 986% x7 | 3.1401 | 17.5% | 2.5% |

The conversion is `sqrt(BDO percent / 100)`. Target drain is evaluated on every successful pulse;
entities without an RPG primary resource return `UNSUPPORTED_TARGET`. Slow is a provisional 20% for
100 ticks and refreshes on every pulse. It is not a CC-budget effect.

Minecraft-only provisional values are radius `7`, cast `34`, hit ticks `8/12/16/20/24/28/32`,
recovery `6`, and ten targets. Manual movement or an eligible transition skill cancels the channel;
spent MP and cooldown are not refunded. Blizzard uses the Advanced SP tier and requires Freeze I.
