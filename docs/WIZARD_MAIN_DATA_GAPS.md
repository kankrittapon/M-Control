# Wizard Main Data Gaps

Source: MCP BRPG `list_skills(class=Wizard, tree=Main)`, per-entry `get_skill`, and the source
repository `kankrittapon/MCP-BRPG` (`data/BRPG/Wizard_Main_skills.json` and
`data/wizard_skills_status_effects.md`), reviewed 2026-07-14. Runtime gameplay does not connect to MCP.

## Imported

- 32 stable Wizard Main IDs and names.
- MCP descriptions where `get_skill` returned the requested ID.
- Rank counts and known required levels.
- Known cooldown seconds, accuracy bonuses, maximum targets, and explicit `mp_cost` values where
  present in the source record.
- Design-level CC and Status mappings from `wizard_skills_status_effects.md`.
- Original MCP icon source paths stored as metadata strings.

## Required Before Playable

All 32 entries still need approved Skill Point cost per rank, prerequisite/exclusive relationships,
weapon requirements, targeting shape, MP cost, cooldown, cast/recovery timing, movement/cancel
policy, hit ticks, coefficients, range/radius, protection windows, confirmed CC variants, Status
potency/duration, and special-attack eligibility. MCP rank `cost` must not be imported automatically:
the repository also has a separate `mp_cost` field, and several records make the meaning of `cost`
ambiguous or internally inconsistent. It requires per-skill classification as SP, MP, another value,
or bad source data before use.
Skill Point costs should be balanced against the independent mob-grind progression in
`rpg_world_core/skill_progression.json`, not against Character Level.

Each playable rank also needs a combat JSON under `rpg_skills`. Multiple files share the catalog
identity with `"skill_id": "rpg_project:<stable_id>"` and select the variant with `"rank": N`.
The server refuses learning a rank whose production combat definition is missing, debug-only, or
assigned to another class.

## Phase 5 Intake Order

1. Approve one complete vertical-slice skill and all of its ranks.
2. Verify learn, cast, rank-specific values, cooldown, targeting, damage, and relog behavior.
3. Approve the remaining seven Batch A skills only after the vertical slice passes.
4. Keep every unapproved rank metadata-only; never copy debug values into production definitions.

## Batch A Source Audit

The source repository contains useful partial records for Staff Attack, Dagger Stab, Magic Arrow,
Concentrated Magic Arrow, Multiple Magic Arrows, Mana Absorption, Fireball, and Fireball Explosion.
They are not yet complete production definitions:

- Multiple Magic Arrows has no MCP rank records; the catalog now carries one audited current rank
  (level 49) while SP and combat remain gated.
- None of the eight provides an approved Minecraft damage/coefficient, block range, cast/recovery
  ticks, hit timing, or projectile speed.
- Cooldowns are present only for Concentrated Magic Arrow, Mana Absorption, Fireball, and Fireball
  Explosion, and are recorded in seconds rather than server ticks.
- Fireball and Fireball Explosion have design-level alternative CC choices (`Knockback or
  Stiffness`, `Knockdown or Floating`) that must be resolved rather than chosen by the importer.
- Staff Attack says it costs no resources while later ranks contain `cost`; this is one example of
  why the generic field cannot be mapped without approval.

## Source Issues

- `wizard_sage_s_memory`, `wizard_magic_power_boost`, and `wizard_multiple_magic_arrows` report
  `ranks_count: 0`. Their rank/passive semantics must be decided before implementation.
- `get_skill(id=wizard_lightning)` returned `wizard_lightning_chain`. Lightning keeps its list
  metadata and placeholder rank levels, and remains gated until the MCP record is corrected.
- MCP icon paths contain spaces and punctuation, so they are source metadata rather than valid
  Minecraft resource locations. UI assets will need normalized paths during the asset phase.
