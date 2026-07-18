# Concentrated Magic Arrow Client Acceptance

The production skill keeps its real Magic Arrow I prerequisite. Until Magic Arrow is production
ready, operators may use the acceptance-only command that bypasses prerequisites but still spends
SP and checks class, level, rank, and combat definition.

## Setup

```mcfunction
/rpg class wizard
/rpg setlevel 38
/rpg addskillxp 200000
/give @s rpg_project:wizard_staff 1
/rpg equip
/give @s rpg_project:wizard_dagger 1
/rpg equip
/rpg skills force-upgrade rpg_project:wizard_concentrated_magic_arrow
/rpg skills force-upgrade rpg_project:wizard_concentrated_magic_arrow
/rpg skills force-upgrade rpg_project:wizard_concentrated_magic_arrow
/rpg skills inspect rpg_project:wizard_concentrated_magic_arrow
```

Press Tab to draw, summon targets, then cast:

```mcfunction
/summon minecraft:zombie ~ ~ ~10
/rpg debug cast rpg_project:wizard_concentrated_magic_arrow
```

Expected rank III behavior:

- Total spent cost for the three ranks is 125 SP.
- Six-tick cast, projectile release on tick 5, four-tick recovery.
- Projectile speed 1.5 blocks/tick and exact hitbox collision.
- Impact radius 1.25 blocks, maximum five targets.
- 40% skill Critical bonus is added to the caster Critical chance and clamped to 100%.
- Knockdown and Down Attack use the shared World Core pipeline.
- Movement does not cancel the accepted cast.

Normal `/rpg skills upgrade` must continue returning `PREREQUISITE_REQUIRED` until Magic Arrow I is learned.
