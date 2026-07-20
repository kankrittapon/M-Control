# Phase 6 Protected Area Client Test

## Setup

1. `/rpg skills force-upgrade rpg_project:wizard_protected_area 5`
2. `/rpg debug mana 764`
3. Stand near a hostile mob and record one unbuffed hit.
4. `/rpg debug cast rpg_project:wizard_protected_area`
5. `/rpg status`

## Expected Rank V Result

- Cast consumes 90 MP and starts a 3000-tick cooldown.
- Hit tick 8 reports `SELF_AOE` and applies to the caster.
- Super Armor blocks vanilla knockback through cast tick 12; log reports `vanilla-knockback blocked`.
- Defensive log reports `damageReduction=0.5` and `damageReductionTicks=160`.
- `/rpg status` reports `DamageReductionBuff=50%` with a decreasing timer.
- The same attack deals approximately half its previous final damage for 8 seconds.
- Recast during cooldown returns `COOLDOWN` without spending MP.
- After 8 seconds, status reports `DamageReductionBuff=0%(0)`.
- Hits after casting completes may knock the player back; the area buff is damage reduction, not
  persistent Super Armor.

## Multiplayer Follow-up

Verify allies inside 15 blocks receive the buff, players outside do not, and at most 10 allies are
selected. This remains pending until a second player is available.
