# Phase 5 Meteor Shower Tuning

## Audited BDO Contract

| Rank | Level | MP | Cooldown | Opening damage | Meteor damage | Burn |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| I | 17 | 200 | 130s | 1506% x2 | 1604% x2 | 30 / 3s / 18s |
| II | 40 | 250 | 110s | 1980% x2 | 2136% x2 | 40 / 3s / 18s |
| III | 50 | 300 | 90s | 2454% x2 | 2668% x2 | 50 / 3s / 18s |

The tooltip also specifies maximum 10 targets, Front Guard while casting, Super Armor on attacks,
Stiffness on opening hits, Knockdown on meteor hits, and Down Attack. Rank II and III state that
casting speed is increased.

Each BDO source hit is converted independently with `sqrt(percent / 100)`. The four BRPG hit
windows therefore preserve the original two opening and two meteor hits without copying raw BDO
percentages into World Core damage. Rank III meteor hits calculate to `5.1653` and are clamped to
the configured World Core per-hit maximum of `5.0`.

## Provisional BRPG Delivery

- Ground range: 24 blocks.
- Each impact owns a 4.5-block circle and a 10-target cap.
- Two opening impacts use the selected center; two meteor impacts land at opposite 1.5-block
  side offsets while their circles still overlap the selected center.
- Rank cast times are 36/32/28 ticks; recovery is 8 ticks.
- Movement is locked. Manual movement may cancel before the first hit; after release it cannot
  erase committed impacts.
- Front Guard runs from tick 0 until release. Super Armor covers all attack ticks.
- Ultimate SP costs are 100/150/250.

The cast uses one server-validated ground anchor. Updating later meteor positions with Ctrl+LMB
during the channel requires a dedicated server-authoritative retarget packet and is intentionally
not approximated by client-only movement.

Source audit: BDO Codex skill IDs 790, 791, and 792, checked 2026-07-19.
