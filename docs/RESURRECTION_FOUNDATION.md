# Resurrection Foundation (Deferred)

`wizard_resurrection` remains metadata-only. It must not use `RpgCombatState.downed()`, which is
a short combat CC state and is unrelated to player death or revival eligibility.

## Skeleton added

- `RevivalPhase`: `ALIVE`, `DOWNED`, `REVIVABLE`, `REVIVING`
- `PlayerRevivalState`: transient lifecycle contract; not attached as a capability yet
- `RevivalRequest` and `RevivalResult`: future server-authoritative service boundary

## Required before playable

- Decide whether BRPG uses a downed window, a corpse entity, or a respawn interception flow.
- Define logout, dimension change, disconnect, timeout, and respawn behavior.
- Add server-owned target selection, range and line-of-sight validation.
- Restore HP and MP/WP/SP atomically, then sync the revived player.
- Add multiplayer GameTests for death, revival races, duplicate packets, and invalid targets.

No death event, capability, packet, command, or production combat definition is installed by this
skeleton. The skill therefore cannot affect current gameplay.
