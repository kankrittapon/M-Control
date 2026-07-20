# BDO Wizard Speed Spell Audit

Audit date: 2026-07-20

## Verified rank contract

| Rank | Level | SP | MP | Cooldown | Buff | Duration |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 15 | 5 | 120 | 90s | +10% | 30s |
| II | 32 | 9 | 130 | 75s | +15% | 30s |
| III | 48 | 14 | 140 | 50s | +20% | 30s |

The same rank bonus applies to Movement Speed, Attack Speed, and Casting Speed for the caster and
up to 10 nearby allies. Speed Spell is quick-slot compatible and does not reduce cooldowns.

Sources:

- BDO Codex rank III tooltip: https://bdocodex.com/us/skill/1122/
- Inven Global rank and SP table: https://www.invenglobal.com/blackdesertonline/skill/detail/1122
- Raw MCP snapshot: `C:/Users/zexqm/programing/MCP-BRPG/data/BRPG/Wizard_Main_skills.json`

## BRPG provisional values

- Radius: 15 blocks.
- Cast/hit/recovery: 14/10/6 ticks.
- Maximum resolved targets: caster plus 10 players.
- Main Staff and Sub Dagger remain required under the current Wizard combat contract.

These spatial and timing values are not claimed as BDO source values. They remain configurable skill
definition fields and require client feel testing.

## Runtime boundary

Speed Spell writes a generic transient timed-speed state. Movement and vanilla attack-speed attributes
consume it now. The RPG cast-speed attribute also receives the bonus; skill timeline compression and
GeckoLib animation-rate scaling remain separate consumers so cooldown duration is never modified.
