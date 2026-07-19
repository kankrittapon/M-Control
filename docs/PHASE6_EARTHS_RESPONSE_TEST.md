# Phase 6 Earth's Response Client Test

Upgrade three times:

```text
/rpg skills force-upgrade rpg_project:wizard_earth_s_response
```

Production input requires combat `READY`. Hold `A` or `D`, then press `LMB`:

```text
A + LMB = left side-hop and forward attack
D + LMB = right side-hop and forward attack
```

The debug route remains available for diagnosis. Hold `A` or `D`, then use:

```text
/rpg debug aim-cast rpg_project:wizard_earth_s_response
```

Expected Rank III: spend `50 MP`, move one block to the held side, keep the attack aimed forward,
open line hits at ticks `2/3`, hit at most seven targets, apply Floating only on hit one, enable Air
Attack on the airborne second hit, recover for four ticks, and reject immediate recast for cooldown.

- [ ] `A + LMB` logs `side-hop side=-1 requested=1.0`.
- [ ] `D + LMB` logs `side-hop side=1 requested=1.0`.
- [ ] LMB is consumed by the skill and does not select, clear, mine, or perform a vanilla attack.
- [ ] Body moves sideways while both hit lines remain aimed forward.
- [ ] Camera does not rotate or zoom with lateral movement.
- [ ] Hit windows open at ticks `2/3` with cap 7.
- [ ] Floating is requested only on hit one; hit two can log `AIR_ATTACK`.
- [ ] A wall clips the hop and logs `collisionClipped=true`; the player never passes through it.
- [ ] The line attack starts from the post-hop eye position.
- [ ] Immediate recast returns `COOLDOWN` without another MP cost.
- [ ] No rejected definition, exception, or stuck action state appears.
