# Mana Absorption Client Test

## Setup

```text
/rpg class wizard
/rpg setlevel 45
/rpg addskillxp 200000
/give @s rpg_project:wizard_staff 1
/rpg equip
/give @s rpg_project:wizard_dagger 1
/rpg equip
/rpg skills force-upgrade rpg_project:wizard_mana_absorption
/rpg skills force-upgrade rpg_project:wizard_mana_absorption
/rpg skills force-upgrade rpg_project:wizard_mana_absorption
/rpg debug mana 100
```

Spawn several mobs inside an 8-block cone, then aim through the group:

```text
/rpg debug cast rpg_project:wizard_mana_absorption
```

## Expected Rank III Result

- Cast starts with `castTicks=14`, `recoveryTicks=6`, and `mpCost=0`.
- MP rises by 30% of maximum MP exactly once at cast start.
- Front Guard is active through tick 12.
- Hit windows occur at ticks 8 and 12 and can each affect at most 10 targets.
- Every successful target receives 20% Slow for 100 ticks.
- Recast before 140 ticks returns `COOLDOWN` and does not restore more MP.
- Turning tracks the aim while the camera remains independent.

Look for `[RPG Skill] cast-resource`, `hit-window`, `damage`, `protection`, and `result` entries.
