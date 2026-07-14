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
| Concentrated Magic Arrow | Metadata | Definition complete | SP cost and client acceptance |
| Multiple Magic Arrows | Metadata | Missing | Prerequisite, cooldown-recast policy, timing/range |
| Mana Absorption | Metadata | Missing | Resolve 7s/13s conflict, prerequisite, channel timing/range |
| Lightning | Metadata | Missing | Damage/CC source audit, prerequisite, timing/range |
| Lightning Chain | Metadata | Missing | Damage/chain rules, prerequisite, timing/range |
| Meteor Shower | Metadata | Missing | Damage pattern, prerequisite, placement/cancel timing |

Multiple Magic Arrows uses the audited single current rank because the MCP rank array is empty.
Generic MCP `cost` values remain excluded from catalog SP until their meaning is verified per skill.
Runtime remains independent from MCP.

No missing combat value is inferred merely to make a skill playable.
