# Phase 6 Earthquake Client Test

## Preparation

Use a Creative test world with cheats enabled. Place at least eight normal mobs around the player
inside a six-block radius. Keep one extra mob outside the radius for the boundary check.

Upgrade Earthquake to Rank IV by running this command four times:

```text
/rpg skills force-upgrade rpg_project:wizard_earthquake
```

Confirm Rank IV and refill MP:

```text
/rpg skills inspect rpg_project:wizard_earthquake
/rpg debug mana
```

Cast with:

```text
/rpg debug cast rpg_project:wizard_earthquake
```

## Rank IV Expected Result

- Cast starts only when the Wizard Main weapon contract and at least `220 MP` are available.
- MP decreases by `220` at cast start.
- Super Armor is active from tick `0` through tick `9`.
- Six `SELF_AOE` hit windows open at ticks `4/5/6/7/8/9`.
- Every window uses radius `6`, coefficient `4.5398`, and at most seven targets.
- Bound is requested for mobs only on the first pulse.
- Player targets receive Stiffness instead of Bound on the first pulse.
- Every successful pulse is eligible for Down Attack.
- Eligible normal/Elite mobs move slightly toward the caster on every successful pulse.
- Players, Bosses, and Unstoppable mobs are not forcibly pulled.
- Each pulse that hits at least one target restores `15 MP` once, not once per target.
- With six successful pulses, the maximum expected recovery is `90 MP`, limited by maximum MP.
- Recovery begins after the final pulse and lasts `6 ticks`.
- An immediate second cast returns `COOLDOWN`; it must not spend another `220 MP`.

## Manual Checklist

- [ ] Rank IV inspection reports level 52, Advanced SP progression, and learned rank 4.
- [ ] Six hit windows appear at ticks `4-9` with no missing or duplicate pulse.
- [ ] Seven mobs are damaged while the eighth valid mob remains untouched per pulse.
- [ ] A mob just inside six blocks is hit; a mob clearly outside six blocks is not hit.
- [ ] Normal mobs are pulled inward without being launched or orbiting the player.
- [ ] Bound appears only on pulse one and does not consume CC budget repeatedly.
- [ ] MP recovery is 15 per successful pulse and is independent of target count.
- [ ] Super Armor prevents CC during the active window while damage still applies.
- [ ] Movement before the first pulse cancels the cast without refunding MP or cooldown.
- [ ] Movement after the first pulse does not create extra casts or duplicate hits.
- [ ] Immediate recast is rejected by cooldown without additional resource consumption.
- [ ] Camera remains fixed while Earthquake runs; body rotation does not move or zoom the camera.
- [ ] No rejected definition, exception, NaN movement, or stuck action state appears in `latest.log`.

## Profile Checks

- [ ] Elite mob is pulled but uses its normal reduced CC duration/resistance rules.
- [x] Wither Boss Tank survives all pulses, receives damage, rejects Bound with `IMMUNE`, and is
  excluded from the profile-aware Pull branch; visual position was available for client observation.
- [ ] Unstoppable mob receives damage but is not pulled and rejects harmful control.
- [ ] A second player receives Stiffness on pulse one but is never pulled.

## Log Evidence

Export the block containing:

```text
[RPG Skill] acceptance-force-upgrade
[RPG Skill] request
[RPG Skill] result
[RPG Skill] protection
[RPG Skill] hit-window
[RPG Skill] damage
[RPG Skill] resource
[RPG Skill] recovery
[RPG Skill] complete
```

The important evidence is six hit windows, no more than seven targets per window, first-pulse CC,
flat MP recovery, cooldown rejection, and no runtime error.

## Acceptance Evidence

Client log 2026-07-19 confirms Rank IV, `220 MP`, Super Armor ticks `0-9`, six hit windows at
ticks `4-9`, seven targets per window, first-pulse Bound, later Down Attack, six flat recoveries of
`15 MP`, recovery `6 ticks`, and normal completion. No Earthquake definition or runtime error was
present. Visual Pull, radius boundary, cancellation, cooldown rejection, and non-Normal profile
checks remain pending.

Boss probe 2026-07-19: Wither `#1877` was selected as the only target, received RPG damage, and
returned `IMMUNE` for first-pulse Bound with zero CC points. It died on pulse three from accumulated
damage, so later pulses correctly resolved zero targets. This confirms Boss hard-CC immunity but is
not sufficient evidence for visual no-Pull behavior.

Boss Tank probe 2026-07-19: Wither `#2034` survived all six windows. First-pulse Bound returned
`IMMUNE` with zero CC points; later pulses did not request CC. Four pulses hit and two missed, so
flat MP recovery correctly applied four times for `60 MP` rather than six times. The Boss profile
also excludes the target from forced Pull in the server branch. The cast completed normally.
