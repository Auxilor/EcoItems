---
title: Furniture
sidebar_position: 1
---

# Furniture

:::info
Furniture models are part of the resource pack system, which requires the paid version of EcoItems.
:::

Add a `furniture:` section to any item config and the item places itself into the world as a display entity - with optional solid collision, sittable seats, and toggleable lights. The furniture shows the item itself, so its look comes from the item's normal `texture`/`model`/`definition`. Use a non-placeable base item (paper) to avoid client-side ghost flicker, and note vanilla *item* models are flat sprites - reference a block model instead, like the shipped `table_lamp` example (`model: "minecraft:block/lantern"`).

```yaml
# items/park_bench.yml
item:
  item: paper
  name: "&6Park Bench"
  model: furniture/park_bench

furniture:
  rotation: 4-way
  barriers:
    - "0..1,0,0"       # collision: origin cell + one east
  seats:
    - "0,0,0"
    - "1,0,0"
```

## How it works

- **Placement** snaps to 8 directions by default (`rotation: 4-way` or `none` to change it; multi-cell furniture always snaps to quarter turns). `placement: {floor, wall, ceiling}` controls which surfaces accept it.
- **Breaking**: punch the furniture (its hitbox or a collision barrier - barriers insta-break for furniture) and it drops its item, or a custom `drops:` table, same format as blocks, through the eco drop queue.
- **Seats** (`seats:` offsets): right-click to sit, sneak-dismount as usual. The y offset is relative to the natural chair sitting height (0.6 above the block bottom), the same convention as Nexo and Oraxen, so `"0,0,0"` is right for a normal chair and imported seat offsets work verbatim.
- **Lights** (`lights: ["x,y,z level"]`): real light blocks placed with the furniture; `toggleable-lights: true` lets players right-click to switch them.
- **Hitboxes**: clicks are caught by interaction entities - a 1×1 box by default, or `hitboxes: ["x,y,z widthxheight"]` for custom shapes.
- **Display tuning** under `display:` - scale, translation, transform, billboard, fixed brightness, view range.
- **Protection plugins** see real place/break events, so WorldGuard regions and friends work unchanged. Furniture is naturally blast-proof (barrier blocks and display entities are immune to explosions).

- **Interaction effects**: run [libreforge effects](https://plugins.auxilor.io/effects) per event under `furniture.effects` - events are `punch`, `shift-punch`, `right-click`, `shift-right-click`, `place`, `break`, and `sit`:

```yaml
furniture:
  effects:
    sit:
      - id: send_message
        args:
          message: "&7Take a seat!"
```

See `items/_example_furniture.yml` for every option with comments.

## States (lamps, machines, TVs...)

A `states:` map gives the furniture named looks that players cycle through by right-clicking. Each state is an alternative model: an empty section keeps the item's own look, a `texture:` or `model:` gives it a different one (generated into the pack as `<id>_state_<name>`).

```yaml
furniture:
  states:
    off: {}                     # the item's own look
    on:
      model: furniture/lamp_on
  default-state: off            # optional; first state otherwise
  cycle-states-on-click: true   # default
```

The current state persists with the placement. To drive states from effects instead of clicks, set `cycle-states-on-click: false` and swap looks by breaking/placing, or combine with `toggleable-lights` for lamp-style furniture.

States can also advance by themselves: `next-state-after: <ticks>` on a state moves to the next one (two states pointing at each other = blinking), and `reset-after: <ticks>` snaps back to the default state - a machine that "runs" for ten seconds after a click:

```yaml
furniture:
  states:
    idle: {}
    running:
      model: furniture/machine_running
      reset-after: 200
  default-state: idle
```

Timers are in-memory: furniture in a freshly loaded chunk stays in its saved state until something switches it again.

## Doors

A `door:` section makes right-clicking open and close the furniture: while open, its collision barriers are removed and an optional `open:` look replaces the model.

```yaml
furniture:
  rotation: 4-way
  barriers: ["0,0,0", "0,1,0"]
  door:
    open:
      model: furniture/gate_open   # optional; omit to keep the closed look
    open-sound: block.wooden_door.open     # defaults shown
    close-sound: block.wooden_door.close
```

## Storage

A `storage:` section opens a chest-style inventory on right-click. Contents persist with the placement, and everyone looking at the same storage shares a live view.

```yaml
furniture:
  storage:
    rows: 3
    title: "&8Crate"
    type: storage        # storage | personal | disposal
    open-sound: block.chest.open     # defaults shown
    close-sound: block.chest.close
```

- **`storage`** - one shared inventory per placement; contents drop when it's broken, like a chest.
- **`personal`** - each player gets their own inventory in the same furniture (contents are lost if it's broken).
- **`disposal`** - a trash can; whatever is inside when it closes is discarded.

## Connectable rows (benches, counters, curtains)

A `connectable:` section makes pieces placed in a row re-model themselves as ends or middles. Each key is a state-style section (`texture:`/`model:`); missing keys keep the item's own look. Pieces connect when the neighbor is the same furniture facing the same way.

```yaml
furniture:
  rotation: 4-way
  connectable:
    left:                       # left end of a row
      model: furniture/bench_left
    right:
      model: furniture/bench_right
    middle:
      model: furniture/bench_middle
```

The item's own model is used for a lone piece. If your left/right models render mirrored, swap the two keys - "left" is from the viewpoint of a player facing the furniture's front.

Counter-style corners work too: `inner:` and `outer:` states apply when a piece joins a sideways row to a perpendicular one (the neighbor facing 90° off). Place the corner piece facing the direction you want the bend; swap the two keys if your models come out inverted.

## Beds

`beds:` cells (same `"x,y,z [yaw]"` format as seats) make furniture sleepable: right-click at night (or during a thunderstorm) to lie down with the real sleeping pose and overlay - no bed block involved. Lying down resets the phantom timer, and once enough of the world sleeps (`playersSleepingPercentage` gamerule), the night skips and the weather clears, vanilla-style. Sneak, move, or take damage to get up.

```yaml
furniture:
  rotation: 4-way
  barriers: ["0,0,0", "1,0,0"]
  beds:
    - "0.5,0,0"      # lying position, head toward the furniture's facing
```

## Vehicles

A `vehicle:` section makes the furniture driveable: the player in the **first seat** steers with their look direction and W/S. Requires **Paper 1.21.2+** (vehicles stay parked elsewhere). Vehicles can't have `barriers:` or `lights:` - blocks can't move.

```yaml
furniture:
  seats:
    - "0,0,0"          # the driver's seat
    - "0,0,-0.8"       # extra seats ride along
  vehicle:
    speed: 0.3          # horizontal blocks per tick at full throttle
    fly-speed: 0.0      # > 0 = holding jump ascends and the vehicle hovers
    fuel:               # optional; no fuel section = drives for free
      items:
        - coal
      per-item-seconds: 120
    smoke:              # optional exhaust while driving
      particle: campfire_cosy_smoke
      amount: 3
      offset: "0,0.5,-1"   # local position, rotates with the vehicle
```

Passengers in the other seats ride along; only the first seat steers.

Ground vehicles fall off edges and step up single blocks; flying vehicles (a `fly-speed` above 0) hover and climb while jump is held. Fuel burns one matching item from the driver's inventory per `per-item-seconds` of driving; with an empty tank the vehicle refuses to move and warns in the action bar. Sneak dismounts, and the vehicle is broken like any furniture (punch it).

:::caution
Furniture entities are real persistent entities. Don't `/kill @e` - you'd strip furniture displays and leave their barriers behind (breakable by ops).
:::
