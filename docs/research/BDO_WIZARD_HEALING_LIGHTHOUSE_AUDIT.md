# BDO Wizard Healing Lighthouse Source Audit

Audit date: 2026-07-19

## Sources

- Raw MCP record: `C:/Users/zexqm/programing/MCP-BRPG/data/BRPG/Wizard_Main_skills.json`
- Raw MCP audit: `C:/Users/zexqm/programing/MCP-BRPG/data/research/wizard_data_audit.json`
- Black Desert Wiki Healing Lighthouse rank table, used to resolve mislabeled raw fields and omitted
  self/ally HP percentages

## Frozen Values

| Rank | Level | SP | MP cost | Cooldown | Self HP/pulse | Ally HP/pulse | Self MP/pulse |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 27 | 5 | 100 | 90s | 7% | 5% | 45 |
| II | 34 | 8 | 140 | 70s | 10% | 7% | 75 |
| III | 41 | 14 | 180 | 50s | 15% | 10% | 120 |
| IV | 48 | 18 | 220 | 30s | 30% | 20% | 150 |

All three pulses carry the listed recovery. Rank IV can therefore recover up to 90% max HP to the
caster and 60% max HP to each ally over the complete channel. MP recovery occurs once per pulse for
the caster, never once per recipient.

The raw `cost` field is MP casting cost for this skill. The raw `mp_cost` field is actually flat MP
recovery per pulse. Explicit SP values come from the corroborating rank table and total 45.

## BRPG Delivery

- Self-centered ally AoE, caster plus at most 10 player allies.
- Heal-only execution; no damage/accuracy/critical/CC path.
- Mob entities are excluded.
- Radius 15 and pulse ticks `8/16/24` are provisional.
- Movement may cancel the channel, preserving only pulses already completed.
