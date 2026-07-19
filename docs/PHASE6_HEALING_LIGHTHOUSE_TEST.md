# Phase 6 Healing Lighthouse Client Test

## Setup

```text
/rpg skills force-upgrade rpg_project:wizard_healing_lighthouse 4
/rpg debug mana 0
/damage @s 400 minecraft:generic
/rpg debug cast rpg_project:wizard_healing_lighthouse
```

Use Survival mode and enough remaining health to survive the damage command.

## Expected Rank IV

- Spend 220 MP at cast start.
- Hit windows at ticks `8/16/24`.
- Each pulse requests 30% max HP for caster and restores 150 MP once.
- Complete channel can recover up to 90% max HP and 450 MP.
- No Healing Lighthouse damage line.
- Immediate recast returns `COOLDOWN` with no recovery.

## Checklist

- [x] Three and only three hit windows occur at ticks `8/16/24`.
- [x] Self requests 30% max HP (`136.2/454`) per pulse and clamps at max HP.
- [~] MP restored 150 then 65 before reaching max; once-per-target behavior still needs multiplayer.
- [ ] Walking before tick 8 cancels with no pulse.
- [ ] Walking after pulse one preserves pulse one and cancels pulses two/three.
- [ ] Up to 10 nearby player allies receive 20% max HP per pulse.
- [ ] Mob entities inside radius are never healed.
- [ ] Player beyond radius is never healed.
- [ ] No damage, Accuracy, Critical, CC, Status, or Smash log is emitted.
- [ ] Cooldown and relog behavior are correct.

## Client Evidence - 2026-07-19

Rank IV spent 220 MP and completed three `SELF_AOE` windows with one caster target. HP recovery was
`136.2`, `132.13333`, then `0` as health clamped at `454/454`. MP recovery was `150`, then `65`; the
third pulse correctly emitted no resource transaction after MP reached maximum. Recovery ran for six
ticks and the cast completed without a damage line or runtime error. Resolver timing was `3326 us`
on the cold first pulse and `44/43 us` on the warmed pulses, below the 1000-us threshold.
