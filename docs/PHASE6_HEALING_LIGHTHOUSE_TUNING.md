# Phase 6 Healing Lighthouse Tuning

Healing Lighthouse I-IV is a three-pulse support channel. It extends the generic health payload with
separate caster and ally recovery values.

## Frozen

- MP costs `100/140/180/220`
- Cooldowns `1800/1400/1000/600` ticks
- Three recovery pulses
- Self HP `7/10/15/30%` per pulse
- Ally HP `5/7/10/20%` per pulse
- Self MP `45/75/120/150` per pulse
- Caster plus at most 10 player allies; mobs excluded
- SP `5/8/14/18`, total 45

## Provisional

- Radius 15 blocks
- Pulse ticks `8/16/24`, cast length 26, recovery 6
- Movement cancels the remaining channel

Source decisions: `docs/research/BDO_WIZARD_HEALING_LIGHTHOUSE_AUDIT.md`.
