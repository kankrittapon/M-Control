# Phase 6 Freeze Client Test

## Setup

```text
/rpg setlevel 40
/rpg addskillxp 1000000
/rpg skills force-upgrade rpg_project:wizard_freeze
/rpg skills force-upgrade rpg_project:wizard_freeze
/rpg skills force-upgrade rpg_project:wizard_freeze
/rpg skills force-upgrade rpg_project:wizard_freeze
/rpg skills force-upgrade rpg_project:wizard_freeze
/rpg debug mana refill
```

Equip the Wizard staff and dagger and enter combat with `Tab`.

## Cast

```text
/rpg debug aim-cast rpg_project:wizard_freeze
```

## Expected Rank V

- MP decreases by 50 and cooldown starts at 120 ticks.
- Two hit windows execute against at most five targets in the short frontal cone.
- Both hit windows deal damage. Only the second reports `type=FREEZE`.
- A frozen target is action locked and immune to later damage for the configured freeze duration.
- Boss and Unstoppable profiles reject Freeze according to Mob Control Profiles.
- The caster rotates toward aim without moving the detached camera.
