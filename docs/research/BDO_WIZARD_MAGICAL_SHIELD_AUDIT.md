# BDO Wizard Magical Shield Audit

## Approved source baseline

Use the post-March-2025 behavior as the production baseline. Older references remain useful for
level and SP metadata, but their cooldown, duration, and conversion values are legacy balance.

| Rank | Level | SP | Cooldown | Mana shield duration | Damage paid from MP | All Resistance | Resistance duration |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 11 | 3 | 120s | 60s | 10% | 12% | 30s |
| II | 20 | 5 | 120s | 60s | 15% | 18% | 30s |
| III | 29 | 8 | 120s | 60s | 20% | 24% | 30s |
| IV | 40 | 12 | 120s | 60s | 25% | 30% | 30s |

## BRPG runtime interpretation

- Incoming final damage is split once: the configured portion is drained from MP and the remainder
  reaches HP.
- If MP is insufficient, only the amount actually drained is absorbed; remaining damage reaches HP.
- The mana shield should apply to tagged RPG and ordinary damage, except damage that bypasses
  invulnerability or an explicit future bypass contract.
- All Resistance is a separate timed buff. It modifies CC resistance, not health damage reduction.
- Both effects are transient and clear on death/logout/dimension change.
- Skill JSON must activate generic buff payloads; damage events must never check the Magical Shield ID.

## Legacy warning

Older sources describe cooldowns of 60-75 seconds, 30-second shield duration, 25-40% conversion,
and 6-15% resistance. Do not mix those values with the current baseline.

## Sources

- Pearl Abyss Global Lab update summary, 28 March 2025:
  <https://www.blackdesertfoundry.com/global-lab-updates-28th-march-2025/>
- Current BDO Codex rank IV: <https://bdocodex.com/us/skill/871/>
- Legacy rank table retained for historical comparison:
  <https://blackdesertonline.fandom.com/wiki/Magical_Shield>
