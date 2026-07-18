# Lightning Chain Tuning

Lightning Chain is a targeted channel. The first target must be intersected by the client aim ray;
later targets are resolved server-side from the previous target and can never repeat in one pulse.

| Rank | Level | SP | MP | BDO damage | BRPG pulse coefficient | Max targets |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 1 | 25 | 20 | 580% x3, max 2 hits | 2.2847 x2 | 3 |
| II | 28 | 40 | 35 | 651% x3, max 2 hits | 2.4203 x2 | 4 |
| III | 37 | 60 | 50 | 721% x3, max 2 hits | 2.5478 x2 | 5 |
| IV | 46 | 80 | 65 | 792% x3, max 2 hits | 2.6698 x2 | 6 |

The pulse coefficient is `sqrt(BDO percent / 100) * 3`. Initial BRPG delivery values are range 16,
chain jump radius 4, pulses at ticks 5 and 10, cast 12 and recovery 4. The first pulse requests
Stiffness; both pulses refresh Shocked (20% Slow for 100 ticks). Cast completion grants
`rpg_project:lightning_storm_ready` for 140 ticks.

Per-additional-target damage uses the generic hit falloff contract: `1.00/0.85/0.70/0.55/0.40/0.40`.
Holding the original BDO input means requesting another complete cast after recovery; it does not
change the audited two-pulse contract. Physical held-input dispatch belongs to the action-bar input
phase and does not alter this server skill definition.
