# BDO Wizard Spellbound Heart Audit

Status: production contract frozen for ranks I-V; animation timing and 2025 synergy remain separate.

## Sources

- MCP raw record: `MCP-BRPG/data/BRPG/Wizard_Main_skills.json`
- BDO Codex skill IDs 904-908: https://bdocodex.com/us/skill/904/ through `/908/`
- Pearl Abyss April 3, 2025 update: https://blackdesert.pearlabyss.com/Asia/en-US/News/Notice/Detail?_boardNo=7714

## Frozen Rank Contract

| Rank | Level | SP | Cooldown | Duration | MP every 10 sec | Move Speed |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 20 | 7 | 30 sec | 5 min | 50 | +10% |
| II | 29 | 10 | 30 sec | 6 min | 100 | +10% |
| III | 38 | 12 | 30 sec | 7 min | 150 | +10% |
| IV | 47 | 15 | 30 sec | 8 min | 200 | +10% |
| V | 55 | 18 | 30 sec | 10 min | 250 | +10% |

The MCP field `mp_cost` is recovery potency, not cast cost. Casting costs zero MP. The MCP `cost`
matches historical SP for this record and is retained explicitly rather than interpreted globally.

## Runtime Decision

- Self-only sustained resource buff; no damage, CC, Status, or protection.
- Server restores flat MP every 200 ticks and syncs the owner.
- Movement Speed +10% composes multiplicatively with derived movement and weight penalties.
- Recasting refreshes duration and recovery countdown.
- State is transient and clears on death/logout/dimension lifecycle through combat-state clearing.
- Logical cast/hit/recovery values `6/4/4` are placeholders pending GeckoLib animation tuning.

The 2025 Flow: Mana Augmentation interactions with Magical Shield and Healing Aura are documented
but intentionally deferred. They require a separately learned passive and must not silently change
already accepted production skills.
