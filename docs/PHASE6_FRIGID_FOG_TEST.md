# Phase 6 Frigid Fog Client Test

Prepare Rank IV:

```text
/rpg addskillxp 200000
/rpg skills force-upgrade rpg_project:wizard_frigid_fog
/rpg skills force-upgrade rpg_project:wizard_frigid_fog
/rpg skills force-upgrade rpg_project:wizard_frigid_fog
/rpg skills force-upgrade rpg_project:wizard_frigid_fog
/rpg skills inspect rpg_project:wizard_frigid_fog
```

Stand within five blocks of several mobs and cast:

```text
/rpg debug cast rpg_project:wizard_frigid_fog
```

Expected Rank IV result:

- Cast starts without requiring an aimed entity or ground point and consumes `160 MP`.
- Three self-AoE hit windows open at ticks `8/9/10`, each with at most ten targets.
- Successful hits use coefficient `2.1587`; Down Attack applies when a target is downed.
- Freeze and the `5%` target resource drain occur only on hit three.
- Super Armor protects the caster from CC through tick `10`, while incoming damage still applies.
- Movement is locked; a compatible skill cancel before tick `8` stops the cast and does not refund MP.
- Recast before `300 ticks` is rejected by cooldown without consuming MP again.

## Client Acceptance

Passed 2026-07-19 at Rank IV. Logs confirmed all three hit windows with ten targets, final-hit
Freeze, Elite duration/resistance behavior, Super Armor, recovery, and repeated cooldown rejection.
Warden resource drain correctly returned `UNSUPPORTED_TARGET`; verify the 5% drain separately in PvP
against a player with a primary resource pool.
