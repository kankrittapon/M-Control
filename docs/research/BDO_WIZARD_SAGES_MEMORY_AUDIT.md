# BDO Wizard Sage's Memory Audit

Audit date: 2026-07-20

## Verified contract

- One rank, required level 20, required SP 10.
- No MP cost is listed.
- Cooldown: 3 minutes 30 seconds (4200 ticks).
- Duration: 15 seconds (300 ticks).
- Ignores casting actions during the duration.
- Does not apply to sphera (Awakening) or Black Spirit skills.
- Quick-slot compatible.

Sources:

- BDO Codex tooltip: https://bdocodex.com/us/skill/898/
- Raw MCP normalized record: `C:/Users/zexqm/programing/MCP-BRPG/data/BRPG/skills_brpg.json`

## BRPG interpretation

The buff removes the pre-hit wind-up of eligible Main skills. It preserves hit spacing, recovery,
cooldown, resource costs, movement policy, cancel policy, and server hit authority. This prevents a
multi-hit channel from collapsing every hit into one server tick.

Sage's Memory uses provisional logical cast/hit/recovery values `8/4/4` for its own activation. These
values remain presentation-tunable and do not come from the BDO tooltip.
