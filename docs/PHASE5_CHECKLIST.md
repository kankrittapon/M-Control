# Phase 5: Wizard Main Batch A Checklist

## 5.1 Damage Conversion

- [x] Define BDO damage percentage to BRPG coefficient formula.
- [x] Keep per-hit and total coefficients explicit.
- [x] Load conversion tuning from `rpg_world_core/skill_runtime.json`.
- [x] Validate invalid percentages, hit counts, and unsafe tuning.
- [x] Add known-value and clamp unit tests.
- [ ] Apply approved coefficients to the first eight production skill definitions. (Fireball pair complete.)

## 5.2 Shared Combat Rules

- [x] Use the single World Core CC pipeline.
- [x] Keep Down Smash and Air Smash outside the CC budget.
- [x] Define per-skill cooldown-recast damage multiplier contract.
- [x] Disable CC, Smash, protection, Critical, Special Attack, Status, and resource effects by default during cooldown recasts.
- [x] Separate cast targeting from per-hit impact shape, radius, target cap, and impact offset.
- [x] Resolve AoE against exact bounding boxes and filter caster/Minecraft-team allies.

## 5.3 Resources And Links

- [x] Add percentage Max MP recovery contract.
- [x] Add target resource-drain contract and unsupported-target/result reasons.
- [x] Add generic transient skill-link buff window contract.
- [x] Add server-side require/consume behavior for Fireball into Fireball Explosion.

## 5.4 Spatial And Timeline Tuning

- [x] Freeze initial Fireball/Explosion range and radius tuning.
- [x] Add validated projectile-speed contract and freeze Fireball speed.
- [x] Freeze initial Fireball/Explosion cast, hit, and recovery ticks.
- [x] Confirm initial Fireball walk and Explosion rotate-only policies.
- [x] Implement traveling projectile collision in the Fireball vertical slice.
- [x] Import the exact 32x32 Fireball and Fireball Explosion icons and show the active cast icon.

## 5.5 Vertical Slice

- [x] Implement Fireball production definitions.
- [x] Implement Fireball Explosion production definitions.
- [ ] Verify learn/rank, MP, aim, damage, cooldown, link, relog, and rejection paths.
- [x] Add server debug output and production-definition regression tests for all Fireball/Explosion ranks.

Client acceptance commands and expected logs are documented in `PHASE5_FIREBALL_TEST.md`.

## 5.6 Batch A Completion

- [x] Select and freeze the remaining six Batch A skills.
- [ ] Implement all eight skills through `RpgCombatService`.
- [ ] Verify Normal, Elite, Boss, and Unstoppable behavior.
- [ ] Run unit tests, build, GameTests, and `runClient` acceptance.
