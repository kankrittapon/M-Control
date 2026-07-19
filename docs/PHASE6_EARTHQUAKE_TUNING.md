# Phase 6 Earthquake Tuning

Earthquake I-IV is a stationary self-AoE burst. Source-locked values are MP cost, cooldown, damage,
hit count, seven-target cap, 15 MP recovery per successful hit, Super Armor, PvE Pull/Bound, PvP
Stiffness, and Down Attack.

| Rank | MP | Cooldown | BDO damage | Hits | BRPG coefficient/hit |
| --- | ---: | ---: | ---: | ---: | ---: |
| I | 100 | 2400 ticks | 936% | 4 | 3.0594 |
| II | 140 | 2000 ticks | 1224% | 5 | 3.4986 |
| III | 180 | 1600 ticks | 1499% | 6 | 3.8717 |
| IV | 220 | 1200 ticks | 2061% | 6 | 4.5398 |

The production conversion follows the existing Batch B convention, `sqrt(BDO percent / 100)`.
Bound is applied to mobs and Stiffness to players on the first pulse only. This preserves the source
CC split without spending the CC budget repeatedly. Every pulse deals damage, pulls eligible mobs,
recovers 15 MP once when at least one target is hit, and is eligible for Down Attack.

Minecraft-only provisional values are radius `6`, pull strength `0.18`, recovery `6`, and compact
pulse spacing. Rank I casts in 14 ticks; ranks II-IV speed up to 13/13/12 ticks. Movement or an
eligible transition may cancel before the first pulse. Super Armor lasts through the final pulse.
Boss, Unstoppable, and player targets are never forcibly pulled.

Earthquake uses the Advanced SP tier and has no external skill prerequisite. Rank upgrades remain
sequential through the normal progression service.
