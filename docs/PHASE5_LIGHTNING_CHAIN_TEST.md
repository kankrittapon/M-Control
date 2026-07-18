# Lightning Chain Client Test

## Setup

```text
/rpg class wizard
/rpg setlevel 46
/rpg addskillxp 200000
/give @s rpg_project:wizard_staff 1
/rpg equip
/give @s rpg_project:wizard_dagger 1
/rpg equip
/rpg skills force-upgrade rpg_project:wizard_lightning_chain
/rpg skills force-upgrade rpg_project:wizard_lightning_chain
/rpg skills force-upgrade rpg_project:wizard_lightning_chain
/rpg skills force-upgrade rpg_project:wizard_lightning_chain
```

## Aim Acceptance

Use the detached-camera command so the cast receives the real reticle ray:

```text
/rpg debug aim-cast rpg_project:wizard_lightning_chain
```

- Aim at empty ground: expect `INVALID_TARGET` and no MP use.
- Aim directly through one mob hitbox: expect `STARTED` and two hit windows at ticks 5 and 10.
- Body turns toward the first target while the detached camera remains still.

## Chain Acceptance

Place six mobs so each next mob is within 4 blocks of the previous one. Aim only at the first mob.
Rank IV must report up to six targets in each hit window. Move the third mob more than 4 blocks from
the second; the chain must stop at the second mob and must not jump across the gap. A target appears
at most once in each pulse.

The first pulse requests Stiffness and both pulses apply or refresh Slow for 100 ticks. Completion
must log a 140-tick `rpg_project:lightning_storm_ready` link grant.
