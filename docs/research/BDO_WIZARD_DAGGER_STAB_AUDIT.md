# Wizard Dagger Stab Source Audit

Checked 2026-07-20 for `rpg_project:wizard_dagger_stab`.

## Source Contract

- Five base ranks at levels `1/12/24/37/49` and SP costs `0/7/12/14/16`.
- Archived rank damage is `550/650/750/850/950% x2` with Accuracy `4/8/12/16/20%`.
- Current BDO behavior retains two hits, Critical Hit Rate `+100%`, Stiffness, PvE push,
  Counter Attack, and use during cooldown with reduced damage.
- The current cooldown is 10 seconds after the documented 18-to-10-second class reboot change.
- Current BDO Codex Rank I reports `575% x2`, while the normalized five-rank source still carries
  the archived progression above. BRPG keeps the complete audited progression until all five
  current base-rank tooltips can be captured together; Absolute Dagger Stab is a separate skill.

Sources:

- BDO Codex skill 893, Dagger Stab I: https://bdocodex.com/us/skill/893/
- BDO Codex skill 3144, Absolute: Dagger Stab: https://bdocodex.com/us/skill/3144/
- Black Desert class reboot notes, 2021-12-22: Dagger Stab cooldown `18 sec -> 10 sec`.
- Local MCP normalized record: `MCP-BRPG/data/BRPG/Wizard_Main_skills.json`.

## BRPG Delivery

- Uses both Wizard Main Weapon and Sub-Weapon requirements; the dagger is the skill weapon.
- Two melee ray windows on ticks `1/2`, range `3`, width `0.3`, maximum five targets.
- Coefficients are `sqrt(BDO percent / 100)`: `2.3452/2.5495/2.7386/2.9155/3.0822` per hit.
- Critical, Stiffness, and Counter Attack are attached to both normal hit windows.
- Cooldown recast uses the approved provisional 35% multiplier and disables Critical, CC,
  Special Attack, Smash, Status, resource payloads, and protection.
- PvE push is intentionally deferred. It must use the RPG impulse policy and must not restore
  vanilla damage knockback.

Range, width, cast/recovery ticks, turn speed, and cooldown-recast multiplier are BRPG tuning.
