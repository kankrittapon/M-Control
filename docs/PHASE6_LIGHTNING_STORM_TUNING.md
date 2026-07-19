# Phase 6 Lightning Storm Tuning

Lightning Storm I-III is a quick self-AoE burst with five pulses. Source-locked values are MP,
cooldown, damage, Critical chance, Accuracy, target cap, Stiffness, and Shocked/Slow.

| Rank | MP | Cooldown | BDO damage | BRPG coefficient | Critical | Accuracy | Targets |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 100 | 280 ticks | 343% x5 | 1.8520 | 60% | 7.5% | 4 |
| II | 125 | 260 ticks | 467% x5 | 2.1610 | 67% | 10% | 5 |
| III | 150 | 240 ticks | 591% x5 | 2.4310 | 75% | 12.5% | 6 |

The conversion is `sqrt(BDO percent / 100)`. Stiffness is requested on pulse one only so one source
skill does not consume CC budget five times. Shocked uses the existing 20% Slow for 100 ticks and
refreshes on successful pulses.

Minecraft-only provisional values are radius `6.5`, cast `17`, hit ticks `4/7/10/13/16`, recovery `5`,
and rotate-only movement. The skill uses the Core SP tier and requires Lightning Chain I. It remains
quick-slot castable; the 140-tick link granted by Lightning Chain is reserved for a future fast-combo
modifier rather than being made a hard cast requirement.

The initial consecutive-tick delivery was widened to three ticks between pulses after play-feel
review. The five hits now span roughly 0.6 seconds and remain a fast burst without visually merging
into one impact.
