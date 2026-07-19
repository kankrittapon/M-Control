# BDO Wizard Healing Aura Source Audit

Audit date: 2026-07-19

## Sources

- Raw design record: `C:/Users/zexqm/programing/MCP-BRPG/data/BRPG/Wizard_Main_skills.json`
- Raw audit record: `C:/Users/zexqm/programing/MCP-BRPG/data/research/wizard_data_audit.json`
- BDO Codex skill 903 (Healing Aura V)
- Black Desert Wiki rank table, used only to corroborate the raw rank values

The raw MCP audit marks this skill `partial` because it has no source manifest or per-rank source
records. The values below are therefore frozen in this project audit before production import.

## Frozen Rank Values

| Rank | Level | SP | Cooldown | HP recovery | Self MP recovery |
| --- | ---: | ---: | ---: | ---: | ---: |
| I | 10 | 3 | 15s | 12% max HP | 12% max MP |
| II | 19 | 7 | 14s | 14% max HP | 14% max MP |
| III | 27 | 12 | 13s | 16% max HP | 16% max MP |
| IV | 36 | 14 | 12s | 18% max HP | 18% max MP |
| V | 45 | 16 | 10s | 20% max HP | 20% max MP |

The raw field named `mp_cost` contains the same percentages as the recovery effect. It is not a
casting cost. Healing Aura spends no MP in BRPG and restores the listed percentage to the caster.
The raw `cost` values are explicitly accepted as SP for this skill because their sum (`52`) matches
the independently published rank table.

## BRPG Contract

- Heal-only execution; it must never enter `RpgCombatService` or roll Accuracy/Critical.
- The caster is always a valid recipient.
- An optional living player target in range may be healed with the caster.
- Mobs and hostile damage targets are never healed by this contract.
- Rank V source text says self plus one ally while also carrying a generic maximum-ten-allies line.
  BRPG uses self plus one direct ally for this first implementation; wider party behavior remains a
  deliberate future rule, not an inferred AoE.
- Range `30`, cast tick `4`, hit tick `2`, and recovery tick `4` are provisional delivery values.

## Missing Presentation Data

- Exact BDO animation frames and effect timing
- Party/raid priority when more than one ally is under the cursor
- Final BRPG range after multiplayer feel testing
