# Input Actions And Skill Transitions

## Input Priority

While the third-person cursor is active, `Ctrl + LMB` is a click command only. Camera drag is not
bound to LMB. Action-camera mouse movement remains responsible for camera rotation.

Client actions resolve in this order:

1. Active skill targeting or channel retarget command.
2. Native skill combo input.
3. Action-bar/quick-slot skill input.
4. Entity selection or ground selection clearing.
5. `Ctrl + RMB` entity-follow or ground movement.
6. Vanilla interaction only after leaving RPG third-person combat behavior.

Native combos and quick slots must resolve to the same stable skill ID and send the same cast packet.

## Third-Person Locomotion And Facing

Manual WASD is camera-relative and supports all eight directions. While its movement vector is
non-zero, body yaw smoothly follows the actual reticle ray while camera yaw remains independent.
With no movement vector, the camera is fully free and does not rotate the idle character.
Click-to-move continues to face the destination. Skill definitions independently select a facing
policy: `none`, `aim_on_cast`, `target_on_cast`, or `track_aim_until_release`, plus `turn_speed` in
degrees per tick. Body-facing updates must never write to the RPG camera yaw.

Fireball tracks the accepted aim direction until its projectile release tick, then stops rotating.

## Server Transition Contract

The outgoing skill owns its cancel windows:

- `movement_cancel_from_tick`
- `movement_until_first_hit`
- `skill_cancel_from_tick`
- `skill_until_first_hit`

The incoming Blink, Dodge, or chain skill must declare `interrupts_casting=true`. A cast transition
is committed only after the incoming skill passes class, weapon, resource, cooldown, link, and target
validation. Failed or forged requests cannot cancel the current cast.

Movement commands send a dedicated server cancel request. The client receives whether movement cancel
is currently allowed and cannot use `Ctrl + RMB`/WASD to bypass an uninterruptible window. RMB by
itself is not an RPG move command.

## Cast Families

Cast families describe input feel; runtime permission still comes from each skill's transition data.

- Quick cast/projectile: short release and recovery, normally no movement-cancel window. Fireball is
  in this family.
- Long cast/channel: owns explicit movement and skill cancel windows. `Ctrl + RMB`, WASD, or an
  interrupting Blink can cancel only inside those windows.
- Movement/Blink: declares `interrupts_casting=true`, but it still has to pass normal server
  validation and the outgoing long cast must allow a skill transition at that tick.

Do not infer cancellation from targeting shape or animation length. Declare it per skill so a large
instant AoE and a visually small channel can have different rules.

Animation responds to accepted runtime transitions. It never grants permission to cancel damage or
cast state.
