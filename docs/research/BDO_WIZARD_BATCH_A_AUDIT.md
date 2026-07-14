# BDO Wizard Batch A Source Audit

Snapshot date: 2026-07-14

This document records source-game facts separately from BRPG implementation and balance. The
machine-readable snapshot is `research/bdo/wizard_main_batch_a_2026-07-14.json`. Nothing in that
file is loaded by the mod at runtime.

## Source Policy

1. Official Black Desert guides and patch notes establish mechanics and dated balance changes.
2. Current tooltip databases provide rank-level values when official pages do not expose a full
   live skill catalog.
3. MCP-BRPG remains a useful imported source, but conflicts are preserved and never silently won.
4. Every value keeps its original unit. BDO damage percent is not a BRPG coefficient.
5. A skill cannot become playable from research data alone; Minecraft range, timing, targeting,
   projectile behavior, and conversion values still require approval.

## Findings

| Skill | Current verified reference | Source confidence | Main conflict or gap |
| --- | --- | --- | --- |
| Staff Attack | Rank X metadata only | Low | Damage tooltip unresolved; MCP resource fields conflict |
| Dagger Stab | Rank V metadata; Absolute cross-check | Medium | Base Rank V damage unresolved |
| Magic Arrow | Rank V metadata | Medium | Damage and hit count unresolved |
| Concentrated Magic Arrow | 899% x2, MP 20, 4s | Community DB high | PvE Knockdown vs PvP Stiffness |
| Multiple Magic Arrows | 1581% x2, MP 100, 11s | Community DB high | MCP has no ranks; cooldown recast behavior unresolved |
| Mana Absorption | 1357% x1, max 2 hits; 30% MP recovery; 7s | Community DB high | MCP reports 13s and a different recovery value |
| Fireball | 1150% x2, MP 30, 3s | Community DB high | Instant-cast reduction and link behavior unresolved |
| Fireball Explosion | 2500% x2, MP 85, 13s | Community DB high | Requires Fireball buff/link contract |

## Mechanics Confirmed By Official Sources

- Down Smash and Air Smash do not consume the normal two-count CC budget.
- Back/Down/Air damage multipliers and CC budget rules are source-game mechanics, but BRPG keeps
  its own configurable values.
- Official balance notes are point-in-time evidence. A later patch can supersede damage values, so
  each snapshot records its verification date instead of claiming timeless accuracy.

## Decisions Needed Before Runtime Import

- Choose whether BRPG uses one CC payload or distinct PvE/PvP payloads.
- Define BDO damage-percent to BRPG coefficient conversion without copying percentages directly.
- Decide whether skills usable during cooldown are rejected or cast with reduced damage/effects.
- Add a link/chain contract for Fireball into Fireball Explosion.
- Define MP percentage recovery and target-resource drain contracts.
- Approve Minecraft range, projectile speed, cast ticks, hit ticks, and recovery ticks.

## Primary References

- [Official Black Desert PvP guide](https://www.jp.playblackdesert.com/ja-jp/Wiki?wikiNo=58)
- [Official August 28, 2025 balance update](https://blackdesert.pearlabyss.com/Asia/en-US/News/Notice/Detail?_boardNo=8034)
- [MCP-BRPG Wizard Main source](https://github.com/kankrittapon/MCP-BRPG/blob/main/data/BRPG/Wizard_Main_skills.json)
- [BDO Codex Wizard skill catalog](https://bdocodex.com/us/skills/wizard/)

Individual tooltip links are stored beside each record in the JSON snapshot.
