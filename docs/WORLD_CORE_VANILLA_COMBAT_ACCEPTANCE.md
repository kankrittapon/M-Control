# World Core Vanilla Combat Acceptance

## Deterministic Probe

Run as an operator. The probe spends Guard Gauge but does not reduce player HP.

1. `/rpg debug perfect-guard 600`
2. `/rpg debug impact melee front 10`
3. `/rpg debug impact projectile side 10`
4. `/rpg debug impact explosion rear 10`

Expected:

- Front reports `PERFECT_GUARD`, damage `10 -> 0`, Guard `100 -> 90`, knockback blocked.
- Side/rear reports `SUPER_ARMOR`, damage `10 -> 10`, Guard unchanged, knockback blocked.
- `/rpg status` reports `PerfectGuard=true` while both FG and SA remain active.
- `latest.log` contains `[RPG Impact Debug]` with category, origin, angle, FG/SA/IF/PG,
  damage before/after, guard before/after, protection reason, and knockback decision.

Repeat with `/rpg protection fg 600`:

- Front is `FRONT_GUARD`; side/rear are `NONE` and knockback is not blocked.

Repeat with `/rpg protection iframe 600`:

- Every direction is `IFRAME`, damage is zero, Guard is unchanged, knockback is blocked.

## Live Vanilla Sources

- [x] Zombie/Zoglin melee from the front: Perfect Guard blocks damage and knockback.
- [x] Skeleton arrow from behind: damage reports `SUPER_ARMOR blocked=false`; knockback is blocked.
- [x] Creeper explosion from behind: damage reports `EXPLOSION`, `SUPER_ARMOR`, `blocked=false`.
- [ ] Player melee and Knockback-enchanted player weapon from front and rear.
- [ ] Arrow and explosion from the front in a controlled live setup.
- Knockback-enchanted Vanilla weapon outside RPG combat.
- The same input while an RPG weapon is drawn; Vanilla attack and enchant effects must not double-apply.
- Fall, fire, lava, and damage without a directional origin; FG must not block them.

Verbose impact probe logs are controlled by `observability.log_impact_decisions` in
`data/rpg_project/rpg_world_core/combat.json` and reload with `/reload`.
