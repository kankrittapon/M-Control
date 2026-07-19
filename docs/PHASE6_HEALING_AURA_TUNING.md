# Phase 6 Healing Aura Tuning

Healing Aura I-V is the first production support skill and introduces a generic heal-only payload
plus explicit target disposition. Existing skills default to hostile targeting.

## Frozen

- Required levels: `10/19/27/36/45`
- Explicit SP: `3/7/12/14/16` (`52` total)
- Cooldowns: `300/280/260/240/200` ticks
- Max HP and self MP recovery: `12/14/16/18/20%`
- Targets: caster plus at most one optional player ally
- No damage, Accuracy, Critical, CC, Smash, or hostile Status processing

## Provisional

- Range: `30` blocks
- Cast/hit/recovery: `4/2/4` ticks
- Movement is locked during the short cast
- Facing turns toward a valid ally without moving the camera

Source decisions are recorded in `docs/research/BDO_WIZARD_HEALING_AURA_AUDIT.md`.
