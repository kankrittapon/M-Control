# Phase 6 Residual Lightning Client Test

Prepare Rank IV and cast the combo:

```text
/rpg addskillxp 200000
/rpg skills force-upgrade rpg_project:wizard_residual_lightning
/rpg skills force-upgrade rpg_project:wizard_residual_lightning
/rpg skills force-upgrade rpg_project:wizard_residual_lightning
/rpg skills force-upgrade rpg_project:wizard_residual_lightning
/rpg debug aim-cast rpg_project:wizard_lightning
/rpg debug cast rpg_project:wizard_residual_lightning
```

Expected Rank IV result:

- Residual before a successful Lightning returns `MISSING_SKILL_LINK` without spending MP.
- Lightning logs a link grant and link anchor at its ground impact.
- Residual consumes the link, costs `90 MP`, and ignores subsequent cursor movement.
- Initial hit windows open at `2/3/4/5/6`; finishers open at `8/9`, up to ten targets.
- Bound is requested only on hit one; all seven hits refresh Slow.
- The two finishers attempt 2% target resource drain each.
- A second Residual without another Lightning returns `MISSING_SKILL_LINK`.
