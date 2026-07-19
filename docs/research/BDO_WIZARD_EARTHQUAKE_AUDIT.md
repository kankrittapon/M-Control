# BDO Wizard Earthquake Source Audit

Snapshot date: 2026-07-19

Earthquake is source-complete for combat intake. This audit freezes source-game facts separately
from Minecraft-only tuning. The machine-readable snapshot is
`research/bdo/wizard_earthquake_2026-07-19.json`; it is not loaded by the mod at runtime.

## Current Rank Data

| Rank | Level | MP | Cooldown | BDO damage | Hits | BRPG coefficient/hit | Total coefficient |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 30 | 100 | 120s | 936% | 4 | 3.0594 | 12.2376 |
| II | 38 | 140 | 100s | 1224% | 5 | 3.4986 | 17.4930 |
| III | 45 | 180 | 80s | 1499% | 6 | 3.8717 | 23.2302 |
| IV | 52 | 220 | 60s | 2061% | 6 | 4.5398 | 27.2388 |

The BRPG values use `sqrt(BDO percent / 100)` per hit. They are derived intake values, not BDO
tooltip values, and remain subject to BRPG balance limits.

## Source-Locked Behavior

- Input is `Shift+F`; quick-slot use is supported.
- It is a self-centered AoE and hits at most seven targets.
- It cannot be used during cooldown.
- Every successful hit recovers 15 MP.
- Super Armor is active during the skill.
- Successful hits pull monsters and apply Bound in PvE.
- Successful hits apply Stiffness to players instead of Bound.
- Every damage hit is eligible for Down Attack.
- Rank II onward explicitly has increased casting speed.
- Mounted use and skill add-ons are unavailable in the current tooltip.
- PvP damage uses 62.74% of the listed damage.

## MCP Conflict Resolution

The local MCP record is an older revision and must not be copied directly into production:

| Field | MCP snapshot | Current tooltip | Intake decision |
| --- | --- | --- | --- |
| MP | 200/240/280/320 | 100/140/180/220 | Use current values |
| Accuracy | +5/+7.5/+10/+15% | Not listed per rank | Do not invent a skill bonus |
| Max MP damage | 1/1.5/1.5/2% | Removed | Do not implement |
| Damage | Missing | 936x4/1224x5/1499x6/2061x6 | Use current values |
| Current skill points | Ambiguous `cost` | 0 in current DB | Use BRPG SP tier policy |

The old percentage field describes additional damage proportional to Max MP, not an MP cost. An
official later update removed that mechanic. Keeping it as either damage or resource drain would
reintroduce a retired rule.

## BRPG Decisions Still Required

These values are not published as reusable block/tick measurements and must be tuned in Minecraft:

- AoE radius in blocks and pull strength.
- Cast, pulse, and recovery ticks.
- Whether BRPG keeps the PvE/PvP CC split. With the current single-CC contract, Bound is the closer
  PvE-first choice; Stiffness must remain documented rather than silently discarded.
- Exact Super Armor window and movement-cancel window.
- Particle and future GeckoLib animation timing.
- BRPG SP tier and prerequisite. Source rank progression is sequential, but BRPG progression owns
  its own economy.

Earthquake must remain `playable=false` until those decisions are approved and tested.

## Sources

- [Current Earthquake I-IV tooltip table](https://black.inven.co.kr/dataninfo/skill/?code=788&reqjob=32)
- [Current Earthquake IV tooltip](https://black.inven.co.kr/dataninfo/skill/?code=789&reqjob=32)
- [BDO Codex Earthquake I](https://bdocodex.com/us/skill/786/)
- [BDO Codex Earthquake IV](https://bdocodex.com/gl/skill/789/)
- [Official April 2022 balance history](https://blackdesert.pearlabyss.com/Console/en-US/News/Notice/Detail?_boardNo=10437)

