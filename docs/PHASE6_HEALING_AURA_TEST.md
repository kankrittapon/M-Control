# Phase 6 Healing Aura Client Test

## Setup

```text
/rpg debug mana 0
/rpg skills force-upgrade rpg_project:wizard_healing_aura 5
/rpg debug cast rpg_project:wizard_healing_aura
```

Damage the player before casting. Rank V should restore 20% of maximum HP and 20% of maximum MP.

## Expected Logs

```text
[RPG Skill] result ... wizard_healing_aura result=STARTED
[RPG Skill] cast-resource ... recovered=<20% max MP>
[RPG Skill] hit-window ... targets=1
[RPG Skill] heal ... target=player ... requested=<20% max HP>
[RPG Skill] recovery ... ticks=4
[RPG Skill] complete ... wizard_healing_aura
```

There must be no `[RPG Skill] damage` line for Healing Aura.

## Checklist

- [~] Self-only Rank V cast recovered exactly 20% max HP (`90.8/454`) on 2026-07-19;
  MP recovery and maximum clamps still need explicit evidence.
- [ ] Casting at full HP/MP succeeds but reports zero effective recovery.
- [ ] Cooldown rejects immediate recast without another heal.
- [ ] A second player within 30 blocks is healed together with the caster.
- [ ] A player beyond 30 blocks is ignored while self-heal still succeeds.
- [x] Looking at Bat `#358` resolved only caster Player `#106`; the mob was not healed.
- [ ] A block between caster and selected ally prevents ally healing but preserves self-heal.
- [ ] Camera remains fixed while the body faces a valid ally.
- [ ] Relog preserves cooldown and clears active cast state.

## Client Evidence - 2026-07-19

Rank V force-upgrade reached `currentRank=5`. The cast auto-drew, opened one hit window at tick 2,
resolved one recipient, requested and recovered `90.8` HP from max HP `454`, then completed four
recovery ticks. No Healing Aura damage line appeared. The first resolver invocation took `3886 us`;
this is recorded as a cold-path sample and requires repetition only if later warmed casts remain above
the configured `1000 us` threshold.
