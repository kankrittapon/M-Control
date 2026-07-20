# World Core Vanilla Combat Bridge Plan

Status: Phases A-F implemented; client acceptance pending

## Purpose

Unify protection decisions for RPG skills, Vanilla attacks, explosions, enchantments, and compatible
modded damage without forcing every source through the RPG AP/Accuracy/Critical formula.

## Protection Contract

| State | Front | Side/rear | Grab | Vanilla knockback |
| --- | --- | --- | --- | --- |
| Front Guard | Blocks direct damage and CC; consumes Guard Gauge | Damage and CC apply | Bypasses | Blocked only when the guarded hit has a valid front origin |
| Super Armor | Damage applies; CC blocked | Damage applies; CC blocked | Bypasses | Blocked from every direction |
| Iframe | Blocks damage and CC | Blocks damage and CC | Blocked | Blocked |
| Perfect Guard | FG damage rule plus SA CC rule | Damage applies; SA blocks CC | Bypasses | Front blocked by FG; side/rear blocked by SA |

Perfect Guard is not a fifth protection type. A skill or stance creates it by overlapping Front Guard
and Super Armor windows. This keeps guard break, facing, HUD, and skill JSON behavior composable.
Grab Immunity remains an independent window and can be combined explicitly when a skill requires it.

## Phase A: Impact Context

- Add `CombatImpactContext` containing target, direct attacker, causing entity, source position,
  damage source/type, incoming damage, direct/projectile/explosion/environment flags, and RPG tag.
- Add `CombatImpactResult` with damage, knockback policy, protection reason, guard damage, and logs.
- Build context at `LivingAttackEvent`/`LivingHurtEvent`; do not store global mutable last-hit state.
- Carry context only for the current server damage call so simultaneous hits cannot overwrite it.

**Gate:** melee, arrow, TNT, Creeper, environmental, tagged RPG, and synthetic modded sources classify
correctly without changing their damage formula.

## Phase B: Unified Protection Resolver

- Resolve in order: bypass rules, Iframe, directional FG, SA, then normal damage/CC.
- FG requires a trustworthy origin and applies only to direct melee/projectile/explosion impacts.
- Damage without an origin, including fall and generic environment damage, bypasses FG.
- Perfect Guard emerges when both FG and SA are active: front damage is guarded while side/rear damage
  applies; CC and knockback remain blocked from all directions.
- Guard break disables FG immediately but does not remove an independently active SA window.
- Grab bypasses FG/SA and is stopped only by Iframe or Grab Immunity.

**Gate:** one parameterized test matrix covers front/side/rear for FG, SA, Iframe, Perfect Guard,
guard break, and grab.

## Phase C: Vanilla Knockback And Explosions

- Cancel `LivingKnockBackEvent` during SA/Iframe.
- Associate ordinary hit knockback with the active impact context so FG can make a directional decision.
- For explosions, use explosion/source position as origin; front FG may guard damage and impulse,
  side/rear bypasses FG.
- If origin is unavailable, FG must not guess. SA/Iframe may still block knockback because they are
  direction-independent.
- Keep Vanilla knockback when no applicable protection is active.

**Gate:** Zombie melee, player attack, Knockback-enchanted weapon, arrow, TNT, and Creeper each produce
the expected damage and displacement under every protection state.

## Phase D: Vanilla Enchantment Policy

- Vanilla weapons outside RPG combat retain Sharpness, Smite, Fire Aspect, Knockback, and other
  Vanilla behavior.
- An item tagged as an RPG weapon uses the RPG basic-attack/skill contract while in combat mode.
- RPG attacks do not automatically inherit Vanilla damage or Knockback enchantments.
- Add an enchantment compatibility registry with explicit policies: `VANILLA_ONLY`, `RPG_BRIDGED`,
  `DISABLED_IN_RPG_COMBAT`.
- Initial policy keeps damage enchantments Vanilla-only. Knockback remains Vanilla-only until a
  reviewed mapping to an RPG impulse payload exists.
- Vanilla Knockback Resistance affects Vanilla impulses only; RPG CC Resistance affects RPG CC only.

**Gate:** the same enchanted item behaves normally outside RPG combat and cannot double-apply damage,
fire, CC, or impulse inside RPG combat.

## Phase E: Mod Compatibility API

- Expose an event/API allowing another mod to classify a source, supply an origin, override impulse
  category, or mark its damage as RPG-tagged.
- Unknown modded sources default to Vanilla damage and Vanilla impulse behavior.
- Never infer AP scaling merely because the attacker is a player or holds an RPG weapon.

**Gate:** a synthetic unknown mod source remains compatible, while an opted-in source receives the
same protection resolution as native RPG damage.

## Phase F: Observability And Acceptance

- Add structured debug output: source category, origin, facing angle, active protections, damage before
  and after protection, guard spent, and knockback decision.
- Extend `/rpg status` with `PerfectGuard=true` when FG and SA overlap; this is a derived display value.
- Add operator test commands for melee/projectile/explosion impacts from front/side/rear.
- Keep verbose impact logs controlled by the existing development observability config.

**Completion Gate:** unit tests, Forge GameTests, build, and client tests prove that Vanilla and RPG
sources share protection rules without sharing damage formulas or applying effects twice.

## Current Interim Behavior

- SA and Iframe now cancel Vanilla `LivingKnockBackEvent`.
- Protected Area SA covers its complete cast window, then expires; its timed DR buff does not grant
  persistent knockback immunity.
- FG now uses directional damage origins and knockback vectors; unknown impulses without direction do
  not guess a frontal result.

## Implemented Checkpoint

- `CombatImpactContext` classifies direct, projectile, explosion, environment, and tagged RPG damage.
- `ProtectionResolver` is shared by RPG damage, Vanilla damage, and Vanilla knockback.
- Knockback direction is reconstructed from Forge's original knockback ratio without cross-hit state.
- Perfect Guard remains FG+SA composition and is shown as a derived value in `/rpg status`.
- Automated coverage proves SA blocks Vanilla knockback and Perfect Guard guards frontal damage while
  allowing rear damage with rear CC blocked by SA.
- Third-party override API and deterministic impact probes are implemented. The manual client matrix
  remains the final acceptance step.
- Offensive Vanilla enchantments are keyed by stable resource ID and marked
  `DISABLED_IN_RPG_COMBAT`; unrelated enchantments default to `VANILLA_ONLY`.
- The server cancels Vanilla entity attacks while RPG weapons are drawn, so client or modded input
  cannot apply Sharpness, Fire Aspect, Knockback, or Sweeping alongside an RPG attack.
- `CombatImpactResolveEvent` lets integrations override category, finite origin, and RPG metadata;
  unknown sources remain Vanilla and RPG metadata never enables RPG damage math automatically.
- `EnchantmentCombatPolicyEvent` and stable-ID registration let integrations declare enchant policy
  without requiring BRPG to link against the other mod.
- `/rpg debug perfect-guard <ticks>` activates the derived FG+SA combination for testing.
- `/rpg debug impact <melee|projectile|explosion> <front|side|rear> <damage>` runs the shared resolver,
  spends real Guard Gauge, preserves HP, and reports angle, protection, damage, guard, and knockback.
- `observability.log_impact_decisions` in `combat.json` controls structured World Core impact logs.
