# Phase 6 Magical Shield Client Test

## Setup

1. `/rpg skills force-upgrade rpg_project:wizard_magical_shield 4`
2. `/rpg debug mana 716`
3. Enter combat with the Wizard main and sub weapons equipped.
4. `/rpg debug cast rpg_project:wizard_magical_shield`
5. `/rpg status` shows `ManaShield=25%` and `ResistBuff=30%` with decreasing tick counts.

## Expected logs

- Cast result is `STARTED`.
- At hit tick 4, `defensive` reports mana shield `0.25`, shield ticks `1200`, resistance
  `0.30`, and resistance ticks `600`.
- Incoming damage prints one `[RPG Mana Shield]` line per damage event.
- For 20 incoming damage at rank IV, about 5 MP is spent and about 15 damage remains.
- Recasting before 120 seconds returns `COOLDOWN`.

## Edge cases

- With zero MP, full incoming damage reaches HP.
- With less MP than requested absorption, all remaining MP is spent and uncovered damage reaches HP.
- Damage tagged `BYPASSES_INVULNERABILITY` is not absorbed.
- After 60 seconds, MP is no longer spent on damage.
- After 30 seconds, the temporary CC resistance no longer applies.
