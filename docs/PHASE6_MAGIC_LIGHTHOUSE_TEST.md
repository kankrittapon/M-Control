# Magic Lighthouse Client Acceptance

## Setup

```mcfunction
/give @s rpg_project:wizard_staff 1
/give @s rpg_project:wizard_dagger 1
/rpg skills force-upgrade rpg_project:wizard_magic_lighthouse 3
/summon minecraft:zombie ~4 ~ ~ {PersistenceRequired:1b}
/summon minecraft:zombie ~-4 ~ ~ {PersistenceRequired:1b}
/rpg debug cast rpg_project:wizard_magic_lighthouse
```

## Expected

- Rank III spends 55 MP and starts a 30-second cooldown.
- A glowing `Magic Lighthouse` placeholder appears two blocks ahead for 20 seconds.
- Logs show `taunt-beacon summon`, then a pulse every 10 ticks with candidate/accepted counts.
- Nearby hostile Normal/Elite mobs switch target to the Lighthouse; passive mobs do not.
- Wither and Ender Dragon are excluded by the Boss profile.
- A second Lighthouse after cooldown removes the previous one before creating the replacement.
- Logout or dimension change logs `taunt-beacon clear` and leaves no tagged placeholder behind.

Cleanup:

```mcfunction
/kill @e[tag=rpg_magic_lighthouse]
```
