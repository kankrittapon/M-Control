# Phase 6 Sage's Memory Client Test

## Setup

1. `/rpg setlevel 20`
2. `/rpg skills force-upgrade rpg_project:wizard_sage_s_memory 1`
3. Learn a visible follow-up such as Fireball IV.
4. `/rpg debug mana 1000`

## Cast and inspect

1. `/rpg debug cast rpg_project:wizard_sage_s_memory`
2. `/rpg status`
3. Confirm `InstantCast` is near 300 ticks.
4. Cast Fireball while the buff is active.

## Expected

- Sage's Memory starts with MP cost 0 and cooldown 4200 ticks.
- Fireball reports a positive `castTimeOffset` and reaches its first hit without the original wind-up.
- Fireball still spends MP, starts its own cooldown, and keeps recovery/projectile behavior.
- A multi-hit skill keeps the original spacing between its hit windows.
- Immediate Sage's Memory recast returns `COOLDOWN`.
- After 15 seconds `/rpg status` shows `InstantCast=0`; Fireball returns to its normal cast timeline.
