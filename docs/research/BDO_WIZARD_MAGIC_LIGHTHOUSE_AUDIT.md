# Wizard Magic Lighthouse Source Audit

Checked 2026-07-20 for `rpg_project:wizard_magic_lighthouse`.

## Source Contract

- Three ranks at levels `21/34/49`.
- MP costs are `75/65/55`; cooldowns are `40/35/30s`.
- The skill summons a magical object that attracts enemy attention.
- Local MCP normalized record: `MCP-BRPG/data/BRPG/Wizard_Main_skills.json`.

The MCP record does not currently provide summon duration, attraction radius, target cap, exact
placement, Boss behavior, or rank SP values. These are explicitly BRPG policy below.

## BRPG Delivery

- Uses Core SP tier `25/40/60` and requires Wizard Main plus Sub-Weapon.
- Summons one server-owned placeholder two blocks ahead of the caster.
- Ranks use `10/15/20s` duration and `8/10/12` block attraction radius.
- Aggro refreshes every 10 ticks and selects at most 10 nearest valid mobs.
- Normal and Elite profiles are eligible. Boss, Unstoppable, players, and passive mobs are excluded.
- Recasting replaces the owner's previous Lighthouse. Logout, death, or dimension change clears it.
- `TauntBeaconTargetEvent` lets compatibility integrations cancel individual retargets.
- The placeholder Armor Stand is presentation-only and will be replaced by GeckoLib assets later.

Duration, radius, cap, placement, cast timing, recovery, and SP tier are provisional datapack tuning.
