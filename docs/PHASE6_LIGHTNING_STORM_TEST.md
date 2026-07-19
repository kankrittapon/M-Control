# Phase 6 Lightning Storm Client Test

Prepare Rank III:

```text
/rpg addskillxp 200000
/rpg skills force-upgrade rpg_project:wizard_lightning_storm
/rpg skills force-upgrade rpg_project:wizard_lightning_storm
/rpg skills force-upgrade rpg_project:wizard_lightning_storm
/rpg skills inspect rpg_project:wizard_lightning_storm
```

Stand within 6.5 blocks of several mobs and cast:

```text
/rpg debug cast rpg_project:wizard_lightning_storm
```

Expected Rank III result:

- Cast consumes `150 MP` and does not require an aim target or Lightning Chain link.
- Five `SELF_AOE` windows open at ticks `4/7/10/13/16`, each with at most six targets.
- Every pulse uses coefficient `2.4310`, Critical bonus `75%`, and refreshes Slow.
- Stiffness is requested only by pulse one; later pulses must log `NOT_REQUESTED` for CC.
- No FG, SA, Iframe, Freeze, or target resource drain is applied.
- Recast before `240 ticks` is rejected without spending MP again.
