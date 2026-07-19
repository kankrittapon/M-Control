# Phase 6 Wizard Staff Attack (LMB Slot 0) Test

Staff Attack is the Wizard Main-weapon innate attack. Rank I is always castable without learning;
Ranks II-X still use normal level and SP progression.

## Input priority

1. `A/D + LMB` casts Earth's Response.
2. Bare `LMB` while combat state is `READY` casts Staff Attack.
3. `Ctrl + LMB` in cursor mode remains target selection and never casts Staff Attack.
4. When combat is `SHEATHED`, LMB is not routed to the RPG basic attack.

## Setup

```text
/give @s rpg_project:wizard_staff 1
/give @s rpg_project:wizard_dagger 1
/rpg debug mana
```

Equip the Wizard weapon set and press `Tab` until combat is `READY`.

## Acceptance checklist

- [ ] Bare LMB logs a request for `rpg_project:wizard_staff_attack`.
- [ ] Rank I costs 0 MP and hits only one target within 3 blocks.
- [ ] A miss deals no damage and restores no MP.
- [ ] A successful Rank I hit restores 5 MP, up to maximum MP.
- [ ] The hit can receive Down Attack when the target is downed.
- [ ] LMB does not invoke vanilla entity attack or block mining while combat is `READY`.
- [ ] `A/D + LMB` casts Earth's Response only; Staff Attack is not requested.
- [ ] `Ctrl + LMB` selects or clears a target without requesting Staff Attack.
- [ ] The detached camera does not rotate or zoom when Staff Attack faces the body toward aim.
- [ ] Leaving combat stops Slot 0 routing.

Rank upgrades can be tested with:

```text
/rpg skills force-upgrade rpg_project:wizard_staff_attack
```

Rank X should use coefficient `1.8028`, Accuracy `+40%`, and restore `15 MP` per successful hit.
