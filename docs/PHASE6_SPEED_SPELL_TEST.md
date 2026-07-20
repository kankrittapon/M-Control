# Phase 6 Speed Spell Client Test

## Setup

1. `/rpg setlevel 48`
2. `/rpg skills force-upgrade rpg_project:wizard_speed_spell 3`
3. `/rpg debug mana 1000`
4. Enable the RPG Debug HUD.

## Cast

`/rpg debug cast rpg_project:wizard_speed_spell`

## Expected Rank III result

- Cast starts with MP cost 140 and cooldown 1000 ticks (50 seconds).
- One self AoE window applies to the caster and at most 10 nearby player allies.
- `/rpg status` shows `SpeedBuff` near 600 ticks and `ATK/CAST/MOVE=+20/+20/+20%`.
- Debug HUD movement and attack attributes rise; walking speed in blocks/second is visible. With the
  current Staff penalty, Rank III should change Attack Speed from about `1.60` to `1.92`, not `2.40`.
- Recasting immediately returns `COOLDOWN` without spending MP.
- After 30 seconds all three bonuses return to their previous values.
- A weaker rank must not overwrite an active stronger rank; an equal rank refreshes duration.

## Deferred multiplayer check

- Verify two nearby players receive the buff.
- Verify a player outside 15 blocks does not receive it.
- Verify no more than 10 allies are affected.
