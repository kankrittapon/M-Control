# Phase 6 Earth's Response Tuning

Earth's Response I-III combines a lateral dodge with a forward line attack. Source values are MP
`30/40/50`, cooldown `80/80/60` ticks, damage `223/336/531% x2`, Accuracy `5/10/15%`, target caps
`3/5/7`, Floating, and input `A/D + LMB`. It is not a quick-slot skill in BDO.

Production coefficients are `1.4933/1.8330/2.3043` per hit using the Batch B conversion. Floating
is requested on hit one; hit two can receive Air Attack without spending CC budget twice.

Minecraft-only provisional values are line range `7/7.5/8`, width `1.25`, lateral distance
`1.5/1.75/2.0`, cast `6/6/5`, hit ticks `3-4/3-4/2-3`, and recovery `5/5/4`. The lateral side is
separate from aim direction and server-validates to `-1/0/1`. Production input is `A/D + LMB` while
combat state is `READY`. `SIDE_HOP` uses server collision movement, synchronizes the clipped result,
and rebuilds the hit origin after moving. A request without a lateral side does not move the caster.

This is intentionally distinct from `TELEPORT`: the hop cannot cross solid blocks and
does not grant Iframe unless a future source-backed protection window explicitly adds it.
