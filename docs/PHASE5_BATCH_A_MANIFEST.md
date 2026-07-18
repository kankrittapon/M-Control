# Phase 5 Wizard Main Batch A Manifest

The eight frozen stable IDs are:

1. `rpg_project:wizard_fireball`
2. `rpg_project:wizard_fireball_explosion`
3. `rpg_project:wizard_concentrated_magic_arrow`
4. `rpg_project:wizard_multiple_magic_arrows`
5. `rpg_project:wizard_mana_absorption`
6. `rpg_project:wizard_lightning`
7. `rpg_project:wizard_lightning_chain`
8. `rpg_project:wizard_meteor_shower`

## Readiness

| Skill | Catalog | Combat | Remaining gate |
| --- | --- | --- | --- |
| Fireball | Playable | Complete | Client rejection-path acceptance |
| Fireball Explosion | Playable | Complete | Client rejection-path acceptance |
| Concentrated Magic Arrow | Playable | Definition complete | Client acceptance; real prerequisite remains gated |
| Multiple Magic Arrows | Playable | Definition complete | Client acceptance and final icon replacement |
| Mana Absorption | Playable | Definition complete | Client acceptance and final icon replacement |
| Lightning | Playable | Complete | Final icon replacement |
| Lightning Chain | Playable | Complete | Final icon replacement |
| Meteor Shower | Metadata | Missing | Damage pattern, prerequisite, placement/cancel timing |

Multiple Magic Arrows uses the audited single current rank from the normalized MCP record. Its raw
generic `cost=21` remains excluded from SP; BRPG assigns the Advanced tier at 50 SP. Delivery tuning
and the cooldown recast policy are documented in `PHASE5_MULTIPLE_MAGIC_ARROWS_TUNING.md`.
Runtime remains independent from MCP.

Mana Absorption follows the current BDO tooltip audit: ranks I-III recover `10/20/30%`
of the caster's maximum MP when the cast is accepted, use `12/9/7s` cooldowns, hit twice,
apply a 20% Slow for 5 seconds, and provide Front Guard during the channel. Vanilla mobs
do not expose an MP pool, so this slice does not invent target-resource drain behavior.

No missing combat value is inferred merely to make a skill playable.

Lightning is a ground-targeted strike, not a chaining attack. Ranks I-V preserve the current
BDO MP, cooldown, damage, 10-target cap, Stun, Down Attack, and Shocked contract. Range, radius,
timing, Shocked potency, and the 50% cooldown-recast damage are provisional BRPG delivery values
documented in `PHASE5_LIGHTNING_TUNING.md`.

Lightning Chain ranks I-IV use audited current damage `580/651/721/792% x3, max 2 hits`, MP
`20/35/50/65`, target caps `3/4/5/6`, Stiffness and Shocked. BRPG models each BDO x3 group as
one pulse coefficient and grants the future Lightning Storm link for 7 seconds on completion.
Initial range 16, chain jump radius 4, timing and Core SP tier are provisional delivery values.
