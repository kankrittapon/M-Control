# Multiple Magic Arrows Client Acceptance

## Setup

```mcfunction
/rpg class wizard
/rpg setlevel 49
/rpg addskillxp 200000
/give @s rpg_project:wizard_staff 1
/rpg equip
/give @s rpg_project:wizard_dagger 1
/rpg equip
/rpg skills force-upgrade rpg_project:wizard_multiple_magic_arrows
/rpg skills inspect rpg_project:wizard_multiple_magic_arrows
```

The acceptance force-upgrade bypasses only the metadata-only Magic Arrow V prerequisite. It must
still spend 50 SP and enforce level, class, weapon, rank, and production-definition checks.

## Normal Cast

Place several targets close together, draw with Tab, aim at the group, and cast:

```mcfunction
/summon minecraft:zombie ~ ~ ~10
/summon minecraft:zombie ~1 ~ ~10
/summon minecraft:zombie ~-1 ~ ~10
/rpg debug cast rpg_project:wizard_multiple_magic_arrows
```

Expected:

- MP cost 100 and cooldown 220 ticks.
- Projectiles release on ticks 3 and 4; cast and recovery are both 5 ticks.
- Both impacts use radius 1.5 and may damage at most 10 living targets.
- Normal cast adds 100% Critical chance to both hits.
- Movement does not cancel an accepted cast.

## Cooldown Recast

Restore MP and cast again before 11 seconds elapse:

```mcfunction
/rpg debug mana 680
/rpg debug cast rpg_project:wizard_multiple_magic_arrows
```

Expected:

- Cast is accepted with `cooldownRecast=true` and spends another 100 MP.
- Each hit uses 35% of its normal coefficient.
- Recast cannot Critical and applies no Special Attack, CC, Smash, Status, resource payload, or
  protection window.

Source and provisional delivery decisions are recorded in
`PHASE5_MULTIPLE_MAGIC_ARROWS_TUNING.md`.
