# Project Phase Status

Checkpoint date: 2026-07-14

| Phase | Status | Notes |
| --- | --- | --- |
| World Core Combat Foundation | Complete | Shared damage, protection, CC, Smash, Status, Mob profiles |
| Skill Runtime Foundation | Complete | State machine, resources, cooldowns, targeting, projectile and links |
| Skill Catalog And Progression | Complete foundation | 32 Wizard Main catalog entries; production skills remain gated by data |
| Phase 5 Wizard Main Batch A | In progress | 3/8 stable skills have combat definitions; 2/8 are currently playable |
| Skill Tree UI | Not started | Begins after server progression and enough production skills are stable |
| Action Bar And Native Inputs | Not started | Native combos and quick slots must call the same stable skill ID |
| GeckoLib Integration | Planned | See `AnimationP.md`; presentation must not control damage timing |

## Phase 5 Batch A

1. Fireball: playable, client-tested, regression-covered.
2. Fireball Explosion: playable, client-tested, regression-covered.
3. Concentrated Magic Arrow: combat definition complete, gated by BRPG SP cost and acceptance.
4. Multiple Magic Arrows: metadata only.
5. Mana Absorption: metadata only.
6. Lightning: metadata corrected; combat missing.
7. Lightning Chain: metadata only.
8. Meteor Shower: metadata only.

Detailed gates: `docs/PHASE5_BATCH_A_MANIFEST.md` and `docs/PHASE5_CHECKLIST.md`.
