# Mouse Control Smoothing Acceptance

Use third-person mouse movement mode in a flat, unobstructed area.

1. Issue one ground move command about 15 blocks away. Movement should ramp up instead of starting
   at full input, then slow near the destination.
2. Click destinations roughly 90 degrees apart. Body rotation should accelerate into the turn;
   camera yaw and camera distance must remain unchanged.
3. Click several nearby points rapidly. Commands inside 0.35 blocks are merged and must not produce
   repeated body snaps.
4. Double-click an entity to auto-attack. While chasing, movement owns body rotation. Attack-facing
   rotation begins only after entering attack range, so the two systems cannot rotate in one tick.
5. Move the target just beyond attack reach. Chase should resume without camera zoom or yaw changes.
6. Press WASD or jump. Auto movement must cancel immediately and return manual control.
7. Fly in Creative or enter water/lava. Vanilla movement must remain active.

Initial tuning values are intentionally code-level development defaults: acceleration `0.12`,
deceleration `0.18`, maximum turn speed `6 degrees/tick`, turn acceleration `0.75 degrees/tick^2`,
and slow radius `2.5 blocks`. Move these into Minecraft settings only after the feel is accepted.
