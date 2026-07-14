# Wizard Main Data Gaps

Source: MCP BRPG `list_skills(class=Wizard, tree=Main)` and per-entry `get_skill`, queried
2026-07-14. Runtime gameplay does not connect to MCP.

## Imported

- 32 stable Wizard Main IDs and names.
- MCP descriptions where `get_skill` returned the requested ID.
- Rank counts and known required levels.
- Original MCP icon source paths stored as metadata strings.

## Required Before Playable

All 32 entries still need approved Skill Point cost per rank, prerequisite/exclusive relationships,
weapon requirements, targeting shape, MP cost, cooldown, cast/recovery timing, movement/cancel
policy, hit ticks, coefficients, range/radius, protection windows, CC, Status, and special-attack
eligibility. MCP rank `cost` is treated as a casting resource value, not Skill Point cost.

## Source Issues

- `wizard_sage_s_memory`, `wizard_magic_power_boost`, and `wizard_multiple_magic_arrows` report
  `ranks_count: 0`. Their rank/passive semantics must be decided before implementation.
- `get_skill(id=wizard_lightning)` returned `wizard_lightning_chain`. Lightning keeps its list
  metadata and placeholder rank levels, and remains gated until the MCP record is corrected.
- MCP icon paths contain spaces and punctuation, so they are source metadata rather than valid
  Minecraft resource locations. UI assets will need normalized paths during the asset phase.
