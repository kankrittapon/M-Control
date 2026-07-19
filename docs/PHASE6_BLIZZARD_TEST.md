# Phase 6 Blizzard Client Test

Prepare Rank IV:

```text
/rpg addskillxp 200000
/rpg skills force-upgrade rpg_project:wizard_blizzard
/rpg skills force-upgrade rpg_project:wizard_blizzard
/rpg skills force-upgrade rpg_project:wizard_blizzard
/rpg skills force-upgrade rpg_project:wizard_blizzard
/rpg skills inspect rpg_project:wizard_blizzard
```

Stand within seven blocks of several mobs and cast:

```text
/rpg debug cast rpg_project:wizard_blizzard
```

Expected Rank IV result:

- Cast consumes `150 MP` and does not require an aim target.
- Seven `SELF_AOE` windows open at ticks `8/12/16/20/24/28/32`, up to ten targets each.
- Every successful pulse uses coefficient `3.1401`, refreshes Slow, and attempts 2.5% resource drain.
- No Freeze, hard CC, FG, SA, or Iframe is applied.
- Pressing manual movement during the channel cancels remaining pulses without refunding MP/cooldown.
- A future Blink/eligible transition skill can cancel it through the same server contract.
- Recast before `1200 ticks` is rejected without spending MP again.
