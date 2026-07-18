# Lightning Client Test

## Setup

```text
/rpg class wizard
/rpg setlevel 46
/rpg addskillxp 200000
/give @s rpg_project:wizard_staff 1
/rpg equip
/give @s rpg_project:wizard_dagger 1
/rpg equip
/rpg skills force-upgrade rpg_project:wizard_lightning
/rpg skills force-upgrade rpg_project:wizard_lightning
/rpg skills force-upgrade rpg_project:wizard_lightning
/rpg skills force-upgrade rpg_project:wizard_lightning
/rpg skills force-upgrade rpg_project:wizard_lightning
```

Place several mobs around one ground point, aim at that point, then cast:

```text
/rpg debug cast rpg_project:wizard_lightning
```

## Expected Rank V

- The accepted cast uses 60 MP, cooldown 100 ticks, cast/recovery 10/5 ticks.
- Three hit windows occur at ticks 7, 8, and 9 around the same ground anchor.
- Every mob within the 3-block circle can be hit, up to 10 targets per window.
- The first normal hit requests Stun; all hits apply Shocked for 100 ticks.
- Casting again during cooldown starts with `cooldownRecast=true`, consumes MP, and deals 50% damage.
- Cooldown recast does not apply Stun but retains Shocked and Down Attack eligibility.
- A ground point farther than 20 blocks is rejected without consuming MP.

## Deterministic Range Validation

Set MP first so resource validation cannot hide the range result:

```text
/rpg debug mana 716
/rpg debug-ground rpg_project:wizard_lightning 20
```

Distance 20 is the inclusive boundary and must return `STARTED`. Wait for the cast to complete,
restore MP, then test beyond the boundary:

```text
/rpg debug mana 716
/rpg debug-ground rpg_project:wizard_lightning 21
/rpg debug-ground rpg_project:wizard_lightning 25
```

Both out-of-range commands must return `INVALID_TARGET`. They must not consume MP, start or alter
cooldown, queue auto-draw, or create hit windows. The command deliberately bypasses entity auto-aim
and block raycast so its distance is deterministic from the server-side eye position. The
`debug-ground` command intentionally uses whole blocks so the client and server command trees use the
same unambiguous integer argument contract.
