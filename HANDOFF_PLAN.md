# BRPG Development Handoff Plan

Updated: 2026-07-14

This document is the handoff source of truth for the next Codex task. `README.md` describes the
currently working systems. `WhatIdo.md` is historical and may contain outdated targeting or skill
details.

For shared design data, use this source-of-truth order:

1. MCP BRPG `get_world_core` for combat rules and formulas.
2. MCP BRPG `list_classes`, `list_skills`, and `get_skill` for class trees and skill records.
3. `README.md` and the current Java/resources for what is actually implemented in the mod.
4. This handoff for implementation order and accepted product decisions.

MCP catalog data is a design reference, not proof that a skill is playable in the mod.

## Product Direction

- Build a Minecraft Forge 1.20.1 action RPG inspired by BDO and Lost Ark.
- Preserve vanilla first-person gameplay where possible.
- Treat RPG third-person as a separate control and combat experience.
- Finish reusable world, progression, attribute, and combat foundations before class skills,
  models, or animations.
- Keep combat server-authoritative. The client may request actions and provide aim input, but the
  server validates equipment, state, range, direction, resources, cooldowns, and damage.

## Decisions That Must Remain Stable

### Controls And Targeting

- Selected entity is UI/context state only. It is not a hard skill lock.
- Aim-based skills use the cursor ray in cursor mode and camera/reticle ray in action mode.
- AoE skills resolve from their declared targeting shape; only explicitly targeted skills require
  an entity target.
- Selection, move-to-target, auto-attack, and skill aim are separate states.
- `Ctrl + LMB` selects an entity; double-click starts auto-attack.
- `Ctrl + LMB` drag orbits the camera without rotating the player.
- `Ctrl + RMB` moves to ground or follows an entity.
- WASD and jump cancel automatic movement.
- Water and lava retain vanilla movement.
- Third-person RPG mode does not break or place blocks. First-person retains vanilla interactions.

### Camera

- The third-person camera remains anchored to the player.
- Camera orbit and player body rotation are independent.
- Zoom changes camera distance, not FOV.
- Camera collision may reduce distance but must not move the anchor away from the player.
- Auto-movement and auto-attack may rotate the player body but must not steer or zoom the camera.

### Classes And Weapons

- Every class has Main, Sub, and Awakening weapon slots.
- Combat can be entered only when the required weapon state is valid.
- `Tab` toggles logical Draw/Sheathe; using a future skill may draw automatically.
- Succession focuses on the main class skill set and upgrades it further.
- Awakening can use both pre-awakening and awakening skill sets, with primary power in awakening.
- Wizard: Staff, Dagger, Sphera.
- Ninja: Shortsword, Shuriken, Sura Katana.
- Visible weapon models, draw animations, and class animations are deferred.

## Completed Foundation

The following systems exist and should be extended instead of replaced:

- RPG third-person camera and mouse controls.
- Selected target, click-to-move, move-to-entity, and auto-attack foundations.
- Wizard and Ninja class identity plus Succession/Awakening state.
- Logical Main/Sub/Awakening equipment slots and server validation.
- Levels 1-100, EXP, Skill Points, persistence, and death/dimension cloning.
- Data-driven class base stats and per-level growth JSON.
- Runtime attribute bridge and server damage pipeline.
- Breath, Strength, Health training; stamina and carrying weight.
- Front Guard, Iframe, Super Armor, Grab Immunity, and generic CC contracts.
- Persistent mob levels and dimension/distance scaling.
- Generic skill definition contract. No MCP Wizard or Ninja skill is playable in the mod yet.
- MCP BRPG contains 86 class skill records and their icon paths; these must be consumed as the
  canonical catalog instead of being renamed or recreated ad hoc in Java.

See `README.md` for controls, commands, configuration paths, and current test setup.

## Next Development Phases

### Phase 1: Stabilize And Expose World Core

Goal: make all existing foundations observable and tunable before adding playable skills.

Status: implemented on 2026-07-13. The shared LivingEntity combat state, tagged damage service,
datapack combat values, CC budget/runtime, stacking special attacks, debug commands, unit tests, and
Forge GameTests are now present. Remaining balancing belongs to later tuning, not foundation work.

1. Add a developer/debug HUD or commands for player level, EXP, available Skill Points, combat
   attributes, stamina, weight, training levels, protection state, and selected mob level.
2. Add mob level display integration without coupling the core to a third-party UI mod.
3. Move mob scaling ranges/formulas and remaining global balance constants into data/config files.
4. Add focused automated tests for progression overflow, persistence, attribute refresh, damage
   order, protection direction, stamina exhaustion, weight thresholds, and mob scaling idempotency.
5. Perform multiplayer authority checks so clients cannot forge level, equipment, protection, or
   damage state.
6. Tune camera and movement only when a reproducible regression is found; the current mouse
   control behavior has been user-tested and accepted.
7. Add a read-only catalog validation/import path for MCP-exported class and skill data without
   making runtime gameplay depend on a live MCP connection.

Completion gate: `gradlew build` passes and core values can be inspected in-game without reading
logs or NBT manually.

### Phase 2: Combat State And Resource Contract

Goal: provide one reusable state machine for every class.

Status: implemented on 2026-07-13. The server-authoritative skill state machine, persistent
cooldowns, MP/WP and stamina validation, all declared hit shapes, multi-hit timing, movement
policies, protection windows, queued auto-draw, Mob Control Profiles, Status engine, tracking sync,
debug cast command, and debug-only datapack skills are present. Unit tests and all 10 Forge
GameTests pass. Production Wizard/Ninja skill data remains Phase 3/4 work.

1. Define `SHEATHED`, `DRAWING`, `COMBAT`, and `SHEATHING` transitions even while animations are
   placeholders.
2. Validate Main/Sub/Awakening requirements per skill and specialization.
3. Finalize mana, stamina, cooldown, cast, recovery, cancel, movement-during-cast, and hit timing.
4. Define skill protection windows using the existing FG/SA/Iframe contracts.
5. Define reusable hit shapes: aim ray/projectile, cone, circle, line, self AoE, ground AoE, and
   targeted-only.
6. Keep auto-attack chase based on actual attack reach and permit attacks while moving when the
   attack contract allows it.
7. Implement the MCP World Core CC budget: at most 2.0 accumulated CC points, followed by 5 seconds
   of CC immunity. Initial values are Stiffness 0.7 and Stun, Knockdown, Floating, Bound, Knockback,
   and Freezing at 1.0 each.
8. Implement special-hit state contracts and initial multipliers: Back Attack 1.2x, Down Attack
   1.2x, Air Attack 1.7x, Speed Attack 1.2x, and Counter Attack 1.7x.
9. Preserve MCP defensive semantics: Iframe blocks damage and CC; Super Armor blocks CC but takes
   damage; Frontal Guard blocks frontal damage and CC in a 180-degree arc while consuming Guard
   Gauge.

Completion gate: dummy/test skills can exercise every targeting shape and protection window
without class-specific implementation.

### Phase 3: Skill Data And Skill Tree Core

Goal: make skills data-driven before creating the Wizard kit.

Status: backend catalog/progression checkpoint implemented on 2026-07-14. Catalog entries, rank
persistence, server learning validation, commands, replay-protected network requests, and client
availability sync are present. Phase 4.1 hardening added paged/inspect commands, automatic online
player sync after datapack reload, respawn/dimension sync, persistence acceptance tests, packet
size validation, and nonce replay tests. Unit tests, build, and all 10 Forge GameTests pass.
Action slots and UI remain later phases.

1. Define stable skill IDs, ranks, prerequisites, specialization rules, and Skill Point costs.
2. Add server-side learn, upgrade, reset, and validation services.
3. Persist learned skill ranks and synchronize only required client data.
4. Add shortcut/action-slot bindings separately from Minecraft inventory hotbar logic.
5. Add a minimal skill tree and action bar UI after the server contract is tested.
6. Import MCP IDs, names, resource types, rank counts, and icon paths. Resolve what `ranks_count: 0`
   means before treating those records as passives, unranked skills, or incomplete source data.
7. Use MP for Wizard and WP for Ninja. Treat SP in the World Core as stamina, not Skill Points;
   player progression Skill Points remain a separate value.

Completion gate: Skill Points can be spent and refunded safely, and invalid client requests are
rejected by the server.

The Wizard Main catalog has been imported as 32 metadata-only entries. See
`docs/WIZARD_MAIN_DATA_GAPS.md`; no entry becomes playable until its missing source data is approved.

### Phase 4: Wizard Vertical Slice

Goal: prove the complete class pipeline with Wizard before implementing Ninja.

1. Implement a representative subset from the MCP Wizard Main catalog using Staff and conditional
   Dagger requirements; do not invent replacement IDs.
2. Include mostly AoE skills plus at least one aim-ray/projectile and one targeted-only skill.
3. Implement a Succession upgrade branch using the MCP `Prime:` records for the implemented Main
   skills.
4. Implement a representative subset from the MCP Awakening catalog using Godr Sphera while
   retaining allowed pre-awakening skills.
5. Use placeholder particles/sounds; models and full animations remain out of scope.
6. Balance only enough to validate progression, equipment, resources, cooldowns, protection, CC,
   and damage together.

Completion gate: Wizard can level, learn skills, switch specialization, satisfy weapon conditions,
enter combat, cast, deal validated damage, and recover resources in singleplayer and multiplayer.

### Phase 5: Ninja And Content Expansion

- Apply the proven pipeline to MCP Ninja records using Shortsword, Shuriken, and Sura Katana.
- Emphasize mobility, directional attacks, evasion, cancels, and tighter protection windows.
- Add final animations and weapon models only after gameplay timings stabilize.

## MCP Catalog Snapshot

Snapshot queried from MCP BRPG on 2026-07-13:

| Class | Main | Succession | Awakening | Total | Resource |
| --- | ---: | ---: | ---: | ---: | --- |
| Wizard | 32 | 4 | 11 | 47 | MP |
| Ninja | 27 | 7 | 5 | 39 | WP |
| Total | 59 | 11 | 16 | 86 | - |

Canonical trees returned by `list_classes` are `Main`, `Succession`, and `Awakening`.

Wizard Succession currently contains Prime upgrades for Multiple Magic Arrows, Fireball, Lightning,
and Freeze. Wizard Awakening contains Godr Sphera records including Cataclysm, Hellfire, Bolide of
Destruction, Aqua Jail Explosion, and Chilling Wave.

Ninja Succession currently contains seven Prime records. Ninja Awakening currently contains Silent
Charge, Sudden Decapitation, Serpent Ascension, Drastic Measure, and Sura ChaoSpree.

Do not duplicate all 86 records in this handoff. Query `list_skills` for a tree and `get_skill` for
full per-skill design data immediately before implementation.

### Phase 6: Gems, Apotheosis, And Enhancement

Defer this phase until class combat is stable.

- Decide whether Apotheosis is an optional integration, not a hard core dependency.
- Audit every Apotheosis affix hook against Main/Sub/Awakening logical weapons.
- Explicitly whitelist meaningful affixes and reject effects that cannot trigger through custom
  skill damage.
- Design BDO-style enhancement as a separate system with its own failure, downgrade, durability,
  and protection rules.
- Do not mix gems, affixes, and enhancement into one opaque power calculation.

## MCP BRPG Connection

Codex has a global MCP entry named `brpg` configured for:

```text
http://100.68.88.63:3001/sse
```

It uses the `MCP_BRPG_API_KEY` Windows User environment variable through the `x-api-key` header.
Expected tools include class, skill, icon, search, and world-core queries. A new Codex process/task
may still be required for these tools to appear in the task's injected tool list.

Never commit the API key to this repository.

### Current MCP Request Status

Verified on 2026-07-13:

- Docker container `mcmod-mcp-server` is running and healthy with port `3001:3001` published.
- The healthcheck uses `127.0.0.1`; using `localhost` previously resolved to IPv6 `::1` and falsely
  marked the IPv4-bound service unhealthy.
- Authenticated SSE returns HTTP 200; the same request without an API key returns HTTP 401.
- MCP initialization negotiated protocol version `2024-11-05` with server
  `mcmod-mcp-server` version `1.0.0`.
- Live `get_world_core`, `list_classes`, and all six required `list_skills` class/tree queries
  completed successfully.
- The server loaded 86 skill records and its World Core document.
- The current task did not receive injected `brpg` tool functions, so the successful verification
  used the server's MCP SSE `initialize` and `tools/call` protocol directly. This is still an MCP
  call, not a separate REST data API.

### How To Use MCP BRPG

The MCP registration is global to the current Windows user, so it does not need to be added again
for each repository. Follow these steps when starting a new Codex task:

1. Confirm the Windows User environment variable exists without printing its secret value:

   ```powershell
   if ([string]::IsNullOrWhiteSpace(
       [Environment]::GetEnvironmentVariable("MCP_BRPG_API_KEY", "User")
   )) { "MISSING" } else { "SET" }
   ```

2. If it is missing, set it and then fully close and reopen Codex:

   ```powershell
   [Environment]::SetEnvironmentVariable(
       "MCP_BRPG_API_KEY",
       "YOUR_API_KEY",
       "User"
   )
   ```

3. Confirm that the global MCP registration exists:

   ```powershell
   codex mcp get brpg
   ```

   The entry should run `mcp-remote` against `http://100.68.88.63:3001/sse`, use SSE transport,
   and send `MCP_BRPG_API_KEY` as the `x-api-key` header. Do not paste the real key into chat or
   commit it to a config file.

4. Open this project in a newly started Codex process and create a new task. MCP tools are loaded
   when the task starts; an already-open task may not gain newly configured tools.

5. In the new task, explicitly ask Codex to use MCP BRPG. Example prompts:

   ```text
   ใช้ MCP brpg เรียก get_world_core แล้วสรุปสถานะ World Core ก่อนแก้โค้ด
   ใช้ MCP brpg เรียก list_classes แล้วเรียก list_skills โดย class=Wizard tree=Main
   ใช้ MCP brpg เรียก list_skills โดย class=Ninja tree=Awakening
   ใช้ MCP brpg ค้นหาสกิล Wizard ที่เป็น AoE แล้วเรียก get_skill ของผลที่เลือก
   ```

6. Codex should prefer the MCP tools over guessing server data. Expected operations are:

   - `get_world_core`: read the shared world-core plan/status.
   - `list_classes`: list available class definitions.
   - `list_skills`: list skill records for a required `class` and `tree` pair.
   - `get_skill`: retrieve one skill by its identifier.
   - `search_skills`: search skills by class, name, or other supported criteria.
   - `get_skill_icon`: retrieve skill icon information.

7. If the tools do not appear, diagnose in this order:

   - Fully restart Codex after setting the environment variable.
   - Run `codex mcp get brpg` and confirm the entry is enabled.
   - Confirm the server is reachable at `100.68.88.63:3001`.
   - Confirm Docker publishes `3001:3001` and the service listens on `0.0.0.0`.
   - Confirm the API key matches the server's `MCP_API_KEY` value.
   - Check Windows firewall and Tailscale ACL rules.

An SSE request returning HTTP 200 and then remaining open is normal. SSE is a long-lived stream, so
a basic HTTP client may eventually report a timeout even though authentication and connection
succeeded.

## Start Of Next Task Checklist

1. Read `HANDOFF_PLAN.md` and `README.md`.
2. Confirm `brpg` MCP tools are available; query `get_world_core` and `list_classes` before changing
   world-core or class code.
3. Run `git status --short --branch` and preserve unrelated user changes.
4. Run `gradlew build` for a clean baseline.
5. For skill work, call `list_skills` with an explicit class/tree and then `get_skill` for every
   skill selected for implementation.
6. Begin Phase 3 with skill catalog/import and progression contracts, unless the user explicitly
   reprioritizes.
7. Update `README.md` and this handoff whenever a phase changes materially.

## Explicitly Out Of Scope For The Immediate Next Phase

- Playable Wizard or Ninja production skills.
- Character or weapon models and animation production.
- Final combat balance.
- Apotheosis integration, gems, or BDO-style enhancement.
- Replacing accepted mouse controls without a confirmed regression.
