# Phase 5 Fireball Client Acceptance

Run these commands as an operator in a development world.

## Setup

```mcfunction
/rpg class wizard
/rpg setlevel 46
/rpg addskillxp 200000
/give @s rpg_project:wizard_staff 1
/rpg equip
/give @s rpg_project:wizard_dagger 1
/rpg equip
/rpg skills upgrade rpg_project:wizard_fireball
/rpg skills upgrade rpg_project:wizard_fireball
/rpg skills upgrade rpg_project:wizard_fireball
/rpg skills upgrade rpg_project:wizard_fireball
/rpg skills upgrade rpg_project:wizard_fireball_explosion
/rpg skills upgrade rpg_project:wizard_fireball_explosion
/rpg skills upgrade rpg_project:wizard_fireball_explosion
/rpg status
```

Press `Tab` if the weapon is sheathed. Spawn a target at a clear distance:

```mcfunction
/summon minecraft:zombie ~ ~ ~10
```

## Fireball

Aim at the zombie, then run:

```mcfunction
/rpg debug cast rpg_project:wizard_fireball
```

Expected behavior:

- The exact Fireball icon flashes during the 2-tick quick cast; the projectile releases on tick 1.
- `Ctrl + RMB` or WASD does not cancel Fireball after its cast has been accepted.
- Flame particles travel at 1.25 blocks per server tick after release.
- The projectile hits only an intersected entity bounding box and stops at a solid block.
- A successful hit applies tagged magic damage, Knockdown, Burn, and Down Attack eligibility.
- The log contains `projectile-spawn`, followed by `projectile-hit` or `projectile-end`.

## Fireball Explosion Link

After Fireball resolves its projectile impact and within 140 ticks (7 seconds) of starting it, run:

```mcfunction
/rpg debug cast rpg_project:wizard_fireball_explosion
```

Expected behavior:

- Explosion consumes the Fireball link and damages entities inside 2.75 blocks of Fireball's stored
  impact point. Moving the cursor elsewhere must not move the explosion.
- The Explosion icon appears during its 4-tick cast.
- A second Explosion without another Fireball returns `MISSING_SKILL_LINK` and spends no MP.
- Explosion is a quick follow-up and is not canceled by `Ctrl + RMB` or WASD after acceptance.
- The log contains `link-grant`, `link-consume`, `hit-window`, and damage/status results.

## Collision Checks

1. Aim slightly outside the zombie hitbox: Fireball must miss and end at range or a block.
2. Place a solid block between player and zombie: Fireball must end with `reason=BLOCK`.
3. Select one entity but aim at another: Fireball must follow the aim direction, not selection.
4. Relog after Fireball, then cast Explosion: the transient link must be gone.

## Rejection Checks

Set MP below Fireball's rank IV cost, attempt the cast, then restore it:

```mcfunction
/rpg debug mana 29
/rpg debug cast rpg_project:wizard_fireball
/rpg debug mana 680
```

The cast must return `INSUFFICIENT_RESOURCE`. It must not grant the Explosion link or start a
cooldown. After a successful Fireball and Explosion, casting Explosion a second time must return
`MISSING_SKILL_LINK` without spending another 85 MP.
