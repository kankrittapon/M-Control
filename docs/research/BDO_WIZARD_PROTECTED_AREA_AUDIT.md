# BDO Wizard Protected Area Audit

Audit date: 2026-07-20

## Accepted Rank Contract

| Rank | Level | SP | MP | Cooldown | Buff duration |
| --- | ---: | ---: | ---: | ---: | ---: |
| I | 25 | 5 | 70 | 240 sec | 4 sec |
| II | 31 | 9 | 75 | 220 sec | 5 sec |
| III | 38 | 13 | 80 | 200 sec | 6 sec |
| IV | 46 | 18 | 85 | 180 sec | 7 sec |
| V | 54 | 23 | 90 | 150 sec | 8 sec |

All ranks grant 50% timed damage reduction to the caster and nearby player allies, affect at most
10 allies, and provide Super Armor during the cast. BRPG models the target cap as 11 entities so
the caster does not consume one of the ten ally slots.

## Source Reconciliation

- Local MCP-BRPG provides stable ID, rank levels, MP values, and legacy cooldown progression.
  Its generic `cost` field is MP, not SP.
- BDO Codex rank pages provide the current effect, duration, MP, target cap, and Super Armor.
- The historical official Black Desert update documents cooldowns 240/220/200/180/150 seconds.
- Community rank tables provide SP progression 5/9/13/18/23 (68 total).

Cast timing and BRPG radius are game-scale tuning rather than claims about BDO world units. Initial
values are 12 cast ticks, hit tick 8, 6 recovery ticks, and a 15-block radius. Super Armor covers
the complete 0-12 cast window but not the persistent 4-8 second damage-reduction buff.

## Runtime Rules

- Timed damage reduction is a generic defensive payload, not a skill-ID check.
- It applies after permanent RPG DR for tagged damage and directly to vanilla/environment damage.
- Tagged damage skips the Forge event reduction path, preventing double application.
- Stronger timed reduction replaces weaker reduction. Equal strength keeps the longer duration.
- Damage that bypasses invulnerability also bypasses this buff.
