# Skill Damage Area Contract

Skill targeting and skill damage area are separate concerns.

- `targeting` determines how the server validates the cast and obtains the initial aim/ground point.
- Each hit's `impact_shape` determines which entities receive that hit at the resolved impact point.
- Every affected entity is sent independently through `RpgCombatService`.

## Per-Hit Fields

| Field | Meaning |
| --- | --- |
| `impact_shape` | `single`, `circle`, or `auto` for compatibility with the targeting shape |
| `radius` | Radius belonging to this hit, not a global radius shared by every skill |
| `max_targets` | Nearest valid targets allowed for this impact; `0` means unlimited |
| `forward_offset` | Moves this impact forward from the selected ground point |
| `right_offset` | Moves this impact sideways from the selected ground point |

Targets are ordered by distance from the impact to their closest bounding-box point. The caster,
dead entities, and entities allied through Minecraft teams are excluded.

## Delivery Examples

- Fireball: aim projectile delivery, then a small circle at the entity or block collision point.
- Fireball Explosion: consumes the previous Fireball link anchor, then creates one larger circle at
  that projectile's resolved entity/block/range impact. It does not accept a new free ground point.
- Single meteor: selected ground delivery, delayed circle at one impact point.
- Meteor shower: multiple hit entries with separate ticks and offsets; each meteor owns its circle,
  damage, CC, status, and target cap.
- Beam: ray or line delivery; each timed hit may affect the line again according to its definition.

Multi-impact skills intentionally may damage the same target on different hit entries. Preventing
repeat damage must be an explicit future per-skill policy rather than a global AoE rule.
