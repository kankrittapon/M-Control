# Phase 5 Meteor Shower Client Test

## Setup

1. Equip the Wizard staff and dagger, then enter combat with `Tab`.
2. Place several mobs inside and outside a 4.5-block circle around the aimed ground point.
3. Set level 50, provide at least 500 available SP, then run the operator force-upgrade command
   three times. The command advances one rank per use and still validates level and SP.
4. Refill MP with `/rpg debug mana refill` when needed.

```text
/rpg setlevel 50
/rpg addskillxp 1000000
/rpg skills force-upgrade rpg_project:wizard_meteor_shower
/rpg skills force-upgrade rpg_project:wizard_meteor_shower
/rpg skills force-upgrade rpg_project:wizard_meteor_shower
/rpg skills inspect rpg_project:wizard_meteor_shower
```

## Cast

Use the real client aim path:

```text
/rpg debug aim-cast rpg_project:wizard_meteor_shower
```

## Expected

- The body faces the selected ground point without rotating the detached camera.
- Rank III consumes 300 MP and starts a 1800-tick cooldown.
- Four hit windows appear: two ground magic opening impacts followed by exactly two offset meteor
  descents. The opening hits must not render falling meteors.
- Every valid mob in each impact circle is processed, up to 10 targets per impact.
- Stiffness appears only on the first opening hit; Knockdown appears only on the first meteor hit.
- Burn refreshes with potency 50, interval 60 ticks, and duration 360 ticks.
- Front Guard is active while casting and Super Armor is active across attack ticks.
- Walking before the first hit cancels the cast. Walking after release does not remove committed hits.
- A ground point farther than 24 blocks returns `INVALID_TARGET` without consuming MP.
