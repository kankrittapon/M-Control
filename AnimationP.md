# RPG Animation Integration Plan

## Purpose

This document is the contract between Blockbench asset production and the RPG combat runtime.
The user owns models, textures, rigs, and animation clips in Blockbench using the GeckoLib plugin.
The mod owns loading, event dispatch, synchronization, fallback presentation, and validation.

Animation is presentation only. Server hit ticks, damage, resource consumption, cooldowns,
protection windows, Crowd Control, and Status effects must never depend on a client animation event.

## Ownership

### Blockbench Asset Work

- Create Wizard, Staff, Dagger, and later Awakening Orb models and textures.
- Create player/weapon rigs with stable bone names.
- Export GeckoLib `.geo.json`, `.animation.json`, and `.png` assets.
- Author clips using the logical durations supplied by the skill definitions.
- Keep gameplay contact frames visually aligned with documented server hit ticks.
- Do not rename an approved bone, clip, texture, or model ID without updating the asset manifest.

### Mod Integration Work

- Pin a Forge 1.20.1-compatible GeckoLib version in Gradle.
- Provide animation event APIs and network synchronization.
- Map skill IDs and combat state transitions to animation IDs.
- Scale clip playback to logical server timing where appropriate.
- Validate referenced resources and report missing assets clearly.
- Provide safe fallback particles, sounds, pose changes, or tint when an asset is absent.
- Keep dedicated servers free from client-only renderer and GeckoLib model classes.

## Resource Layout

Use these paths unless the implementation checkpoint explicitly changes them:

```text
src/main/resources/assets/rpg_project/geo/player/wizard.geo.json
src/main/resources/assets/rpg_project/geo/weapon/wizard_staff.geo.json
src/main/resources/assets/rpg_project/geo/weapon/wizard_dagger.geo.json
src/main/resources/assets/rpg_project/geo/weapon/wizard_orb.geo.json

src/main/resources/assets/rpg_project/animations/player/wizard.animation.json
src/main/resources/assets/rpg_project/animations/weapon/wizard_staff.animation.json
src/main/resources/assets/rpg_project/animations/weapon/wizard_dagger.animation.json
src/main/resources/assets/rpg_project/animations/weapon/wizard_orb.animation.json

src/main/resources/assets/rpg_project/textures/entity/player/wizard.png
src/main/resources/assets/rpg_project/textures/item/wizard_staff.png
src/main/resources/assets/rpg_project/textures/item/wizard_dagger.png
src/main/resources/assets/rpg_project/textures/item/wizard_orb.png
```

Keep Blockbench source files outside runtime assets, for example:

```text
art/blockbench/wizard/wizard.bbmodel
art/blockbench/wizard/wizard_staff.bbmodel
art/blockbench/wizard/wizard_dagger.bbmodel
art/blockbench/wizard/wizard_orb.bbmodel
```

## Stable Naming Contract

Use lowercase `snake_case` for bones and dot-separated IDs for animation clips.

### Required Player Bones

```text
root
body
head
arm_left
arm_right
hand_left
hand_right
leg_left
leg_right
weapon_main
weapon_sub
weapon_awakening
weapon_back_main
weapon_back_sub
weapon_back_awakening
effect_root
```

Extra clothing, hair, finger, and effect bones are allowed. Required bones must retain their names
and hierarchy after approval. Weapon attachment bones should have zeroed transforms in the bind pose.

### Required Global Clips

```text
rpg.idle.sheathed
rpg.idle.combat
rpg.draw.main
rpg.draw.awakening
rpg.sheathe.main
rpg.sheathe.awakening
rpg.move.walk
rpg.move.run
rpg.move.sprint
rpg.cast.cancel
rpg.cc.stun
rpg.cc.freeze
rpg.cc.knockdown
rpg.cc.bound
rpg.cc.float
rpg.cc.recover
rpg.death
```

Skill clips use the stable runtime skill ID:

```text
rpg.skill.wizard_fireball.cast
rpg.skill.wizard_fireball.recovery
rpg.skill.wizard_teleport.cast
```

Multi-stage skills may add `.start`, `.loop`, `.release`, or `.finish`. The Java/datapack mapping
must explicitly reference every nonstandard stage.

## Animation Event Contract

The integration layer will expose these logical events:

```text
DRAW_START
DRAW_COMPLETE
SHEATHE_START
SHEATHE_COMPLETE
CAST_START
HIT_WINDOW
RECOVERY_START
CAST_CANCEL
MOVEMENT_START
MOVEMENT_STOP
CC_START
CC_END
STATUS_START
STATUS_END
DEATH
```

An `RpgAnimationEvent` should carry only presentation data:

```text
entityId
sequenceId
eventType
skillId (optional)
animationId
logicalStartGameTime
logicalDurationTicks
playbackSpeed
loop
weaponSet
```

`sequenceId` must increase per entity so delayed packets cannot restart an older animation.
Persistent state such as combat stance and active CC must also be available when an entity starts
being tracked, allowing a newly joined client to render the correct current pose.

## Timing Rules

- Skill JSON remains the source of truth for cast, hit, recovery, protection, and cancel ticks.
- Animation clips visually follow that timeline; they do not call damage code.
- A `HIT_WINDOW` event may trigger particles, trails, sounds, or camera feedback only.
- Playback speed may be calculated as `authoredClipTicks / logicalDurationTicks`.
- Cast-speed modifiers change the logical timeline first; the animation adapter follows it.
- Looping clips must stop by sequence/state transition, not only by reaching the end of the file.
- Interrupted casts immediately dispatch cancel or CC presentation without refunding resources unless
  the server-side skill cancel policy says otherwise.

## Integration Phases

### A1: Dependency And Empty Adapter

- Select and pin GeckoLib for Forge 1.20.1 after verifying it against the current Forge version.
- Add `RpgAnimationEvent`, `AnimationDispatchService`, and `AnimationAdapter`.
- Use a no-op adapter on dedicated servers and when no asset is registered.
- Add startup logging for GeckoLib version and adapter availability.

**Gate:** client and dedicated server build and start with no animation assets installed.

### A2: Asset Manifest And Validation

- Add a data-driven manifest mapping model, texture, animation file, required bones, and clips.
- Validate resource IDs and JSON presence during resource reload.
- Report all missing files/bones/clips together instead of failing one at a time.
- Missing optional assets warn and fall back; malformed required assets reject only that presentation
  profile and must not disable the combat skill.

**Gate:** `/reload` prints a concise asset readiness report for Wizard and each weapon.

### A3: Placeholder Wizard Rig

- Import a minimal Wizard GeckoLib rig with required attachment bones.
- Render main/sub/awakening weapon attachments according to logical equipment and weapon set.
- Verify first-person, RPG third-person, inventory preview, and multiplayer tracking.

**Gate:** two clients observe the same weapon set without changing combat calculations.

### A4: Draw, Sheathe, Locomotion, And Stance

- Map `SHEATHED`, `DRAWING`, `READY`, and `SHEATHING` to clips.
- Blend idle/walk/run/sprint while respecting movement policy and action lock.
- Keep camera yaw independent from body/animation yaw in RPG third-person mode.
- Place weapons on back/hand attachment bones at the logical transition point.

**Gate:** repeated Tab, movement, interruption, death, and dimension changes cannot leave a weapon or
looping animation in the wrong state.

### A5: Skill Animation Binding

- Extend combat skill JSON with optional presentation IDs for cast, hit, recovery, cancel, sound,
  particle, and trail assets.
- Bind the first Wizard combat batch only after its server timings are approved.
- Scale clips to cast/recovery timing and dispatch hit visuals at server hit windows.

**Gate:** removing an animation file leaves the skill fully playable with fallback visuals.

### A6: Global CC And Status Presentation

- Dispatch standard CC clip names to GeckoLib entities.
- Use particles/tint/icons and existing movement lock for vanilla or unknown modded entities.
- Provide an adapter/event API for another mod to map its own entity rig to RPG CC states.
- Freeze presentation must not pause server ticks or animation controllers globally.

**Gate:** Wizard, vanilla mobs, and unknown modded LivingEntity targets share the same combat state
while using the best presentation each renderer supports.

### A7: Effects And Polish

- Add hand/weapon locator bones for particles and trails.
- Add sound, trail, impact, ground decal, and camera feedback bindings.
- Keep camera shake local, configurable, short, and independent from aiming/body rotation.
- Add LOD/range limits for particles and animation packets.

**Gate:** effects remain readable in multi-target combat and do not alter targeting or hit results.

### A8: Acceptance

- Test singleplayer, dedicated server, reconnect, dimension change, death, and late entity tracking.
- Test animation cancellation by every CC and movement policy.
- Test missing model, texture, bone, clip, and malformed JSON fallbacks.
- Test first-person and third-person rendering without camera clipping or duplicate held weapons.
- Profile packet rate, controller count, particle count, and frame time during multi-hit AoE combat.

**Gate:** presentation matches server logs/timelines, multiplayer clients agree, and missing assets never
break skill execution.

## Blockbench Delivery Checklist

For each asset delivery, provide:

```text
[ ] .bbmodel source
[ ] exported .geo.json
[ ] exported .animation.json
[ ] texture .png
[ ] model/texture/animation resource IDs
[ ] required bone list unchanged
[ ] clip name list
[ ] authored clip length in ticks or seconds
[ ] intended contact/hit frame notes
[ ] looping clips identified
[ ] tested GeckoLib export with no missing texture or bone warning
```

Do not bake damage values, hitboxes, cooldowns, CC, or invulnerability into Blockbench keyframes.
Those values belong in the server skill and World Core datapacks.

## First Asset Handoff

The first integration package should stay intentionally small:

1. Wizard placeholder body rig and texture.
2. Staff and Dagger models with hand/back attachment tests.
3. `rpg.idle.sheathed`, `rpg.idle.combat`, `rpg.draw.main`, and `rpg.sheathe.main` clips.
4. One cast and recovery pair for the first approved Phase 5 Wizard skill.
5. `rpg.cc.stun` and `rpg.cc.freeze` clips to prove global state dispatch.

After this package passes A1-A6, later Blockbench work can expand without changing the combat core.
