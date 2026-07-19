# Phase 6 Freeze Tuning

| Rank | Level | MP | Cooldown | BDO damage | Targets | BRPG coefficient per hit |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 1 | 30 | 8s | 542% x2 | 2 | 2.3281 |
| II | 15 | 35 | 8s | 596% x2 | 3 | 2.4413 |
| III | 23 | 40 | 7s | 651% x2 | 3 | 2.5515 |
| IV | 31 | 45 | 7s | 706% x2 | 4 | 2.6571 |
| V | 40 | 50 | 6s | 761% x2 | 5 | 2.7586 |

Each source hit uses `sqrt(BDO percent / 100)`. Freeze is applied on the second hit so World Core's
frozen damage immunity cannot erase the tooltip's second damage hit.

Close-range cone delivery, range `4/4.5/5/5.5/6`, cast 6 ticks, recovery 4 ticks, rotate-only
movement, and Basic SP tier are provisional BRPG values. The current tooltip does not list Slow,
so this Main skill does not invent a Slow status despite the older MCP design note.

Source audit: BDO Codex skill IDs 834-838, checked 2026-07-19.
