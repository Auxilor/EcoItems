---
title: "How to Make Furniture"
sidebar_position: 7
---

Furniture is a normal EcoItem with a `furniture:` section: placing the item puts it in the world as a **display entity**, with optional **collision**, **seats**, **lights**, and interactive features like states, doors, storage, beds, and vehicles. This page covers making furniture from scratch and every feature it supports.

:::info
Furniture models are part of the resource pack system, which requires the paid version of EcoItems.
:::

## Quick start

1. Open `/plugins/EcoItems/items/` and create your furniture's file, e.g. `park_bench.yml`. `items/examples/_example_furniture.yml` lists every option with comments if you'd rather copy it.
2. Add an `item:` section with a non-placeable base item such as `paper` and a `model`.
3. Add a `furniture:` section with its `barriers` and `seats`.
4. Put the model in `pack/assets/ecoitems/models/` inside the EcoItems plugin folder.
5. Run `/ecoitems reload` to rebuild the pack.
6. Give yourself the furniture with `/ecoitems give <player> park_bench`, place it, and right-click to sit.

:::tip
Files starting with `_` are **never loaded**, so the example stays a reference. The shipped `park_bench`, `wooden_chair`, `wooden_stool`, `wooden_table`, `coffee_table`, `table_lamp`, and `street_lamp` items are working furniture to learn from.
:::

## Naming and IDs

Furniture shares its item's ID: the file name without `.yml`.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the furniture will not load.
:::

## The structure of a piece of furniture

| Part | What it controls |
| --- | --- |
| **Item** | The item, whose texture or model is what the furniture shows |
| **Placement** | Rotation snapping and which surfaces accept it |
| **Collision and hitboxes** | Solid barrier cells and clickable areas |
| **Seats, beds, and lights** | Where players sit and sleep, and light the furniture gives off |
| **Drops and sounds** | What breaking it gives, and place and break sounds |
| **Display** | Scale, translation, and rendering of the display entity |
| **Effects** | libreforge effects run when players interact with it |

```yaml
# === Item: what the furniture shows ===
item:
  item: paper # A non-placeable base item
  name: "&6Park Bench"
  model: furniture/park_bench

furniture:
  # === Placement: rotation and surfaces ===
  rotation: 4-way # 8-way (default), 4-way, or none
  placement: # Default: floor only
    floor: true
    wall: false
    ceiling: false

  # === Collision and hitboxes ===
  barriers: # Solid cells relative to the placed block
    - "0..1,0,0" # The origin cell and one east

  # === Seats, beds, and lights ===
  seats: # "x,y,z", optional yaw
    - "0,0,0"
    - "1,0,0"

  # === Drops and sounds ===
  drops: # Optional; same format as blocks; without it, drops the item
    items:
      - item: ecoitems:park_bench
        chance: 1.0
  sounds:
    place: block.wood.place
    break: block.wood.break

  # === Display: the display entity ===
  display:
    scale: 1.0 # Or "x,y,z"
    billboard: fixed # fixed, vertical, horizontal, or center

  # === Effects: run on interaction ===
  effects:
    sit:
      - id: send_message
        args:
          message: "&7Take a seat!"
```

### Item

The furniture shows the item itself, so its look comes from the item's normal `texture`, `model`, or `definition`; see [How to Make an Item](how-to-make-an-item). Use a non-placeable base item such as `paper` to avoid client-side ghost flicker. Vanilla *item* models are flat sprites, so reference a block model instead, like the shipped `table_lamp` does with `model: "minecraft:block/lantern"`.

### Placement

`rotation` snaps placement to 8 directions by default; set `4-way` or `none` to change it. Multi-cell furniture always snaps to quarter turns. `placement` controls which surfaces (`floor`, `wall`, `ceiling`) accept it.

### Collision and hitboxes

`barriers` are solid collision cells relative to the placed block, and ranges expand, so `"0..1,0,0"` covers the origin cell and the one east of it. Without barriers, the furniture has no collision. Clicks are caught by interaction entities: a 1×1 box at the origin by default, or `hitboxes` in `"x,y,z widthxheight"` form for custom shapes.

Punch the furniture (its hitbox, or a collision barrier, which breaks instantly for furniture) to break it. Furniture is naturally blast-proof, since barrier blocks and display entities are immune to explosions.

### Seats, beds, and lights

- **Seats** (`"x,y,z"`, optional yaw): right-click to sit and sneak to dismount. The y offset is relative to the natural chair sitting height (0.6 above the block bottom), the same convention as Nexo and Oraxen, so `"0,0,0"` suits a normal chair and imported seat offsets work as-is.
- **Beds** (same format as seats): right-click at night or during a thunderstorm to lie down with the real sleeping pose and overlay, with no bed block involved. Lying down resets the phantom timer, and once enough of the world sleeps (the `playersSleepingPercentage` gamerule), the night skips and the weather clears, like vanilla. Sneak, move, or take damage to get up. The position is the lying position, with the head toward the furniture's facing.
- **Lights** (`"x,y,z level"`): real light blocks placed with the furniture. `toggleable-lights: true` lets players right-click to switch them.

### Drops and sounds

Breaking drops the item itself, or a custom `drops:` table in the same format as [custom blocks](how-to-make-a-block), through eco's drop queue. `sounds` sets the `place` and `break` sounds. Protection plugins see real place and break events, so WorldGuard regions and similar work unchanged.

### Display

`display:` tunes the display entity: `scale` (a number or `"x,y,z"`), `translation`, `transform` (`none`, `fixed`, `head`, `ground`, `gui`, ...), `y-rotation` (extra yaw for the look only; `180` flips models built to face the other way), `billboard`, `brightness` (a fixed block-light level; unset uses world lighting), and `view-range`.

### Effects

Furniture can run [libreforge effects](https://hub.auxilor.io/wiki/libreforge/configuring-an-effect) per event under `furniture.effects`. The events are `punch`, `shift-punch`, `right-click`, `shift-right-click`, `place`, `break`, and `sit`.

## States (lamps, machines, TVs)

A `states:` map gives the furniture named looks that players cycle through by right-clicking. Each state is an alternative model: an empty section keeps the item's own look, and a `texture:` or `model:` gives it a different one (generated into the pack as `<id>_state_<name>`).

```yaml
furniture:
  states:
    off: {} # The item's own look
    on:
      model: furniture/lamp_on
  default-state: off # Optional; the first state otherwise
  cycle-states-on-click: true # Default
```

The current state is saved with the placement. To drive states from effects instead of clicks, set `cycle-states-on-click: false`, or combine states with `toggleable-lights` for lamp-style furniture.

States can also advance by themselves. `next-state-after: <ticks>` on a state moves to the next one (two states pointing at each other blink), and `reset-after: <ticks>` snaps back to the default state. For example, a machine that runs for ten seconds after a click:

```yaml
furniture:
  states:
    idle: {}
    running:
      model: furniture/machine_running
      reset-after: 200
  default-state: idle
```

Timers are in memory: furniture in a freshly loaded chunk stays in its saved state until something switches it again.

## Doors

A `door:` section makes right-clicking open and close the furniture. While open, its collision barriers are removed and an optional `open:` look replaces the model.

```yaml
furniture:
  rotation: 4-way
  barriers: ["0,0,0", "0,1,0"]
  door:
    open:
      model: furniture/gate_open # Optional; omit to keep the closed look
    open-sound: block.wooden_door.open # Defaults shown
    close-sound: block.wooden_door.close
```

## Storage

A `storage:` section opens a chest-style inventory on right-click. Contents are saved with the placement, and everyone looking at the same storage shares a live view.

```yaml
furniture:
  storage:
    rows: 3
    title: "&8Crate"
    type: storage # storage, personal, or disposal
    open-sound: block.chest.open # Defaults shown
    close-sound: block.chest.close
```

| `type` | Behaviour |
| --- | --- |
| `storage` | One shared inventory per placement; contents drop when it's broken, like a chest. |
| `personal` | Each player gets their own inventory in the same furniture; contents are lost if it's broken. |
| `disposal` | A trash can; whatever is inside when it closes is discarded. |

## Connectable rows (benches, counters, curtains)

A `connectable:` section makes pieces placed in a row re-model themselves as ends or middles. Each key is a state-style section (`texture:` or `model:`), and missing keys keep the item's own look. Pieces connect when the neighbour is the same furniture facing the same way.

```yaml
furniture:
  rotation: 4-way
  connectable:
    left: # Left end of a row
      model: furniture/bench_left
    right:
      model: furniture/bench_right
    middle:
      model: furniture/bench_middle
```

A lone piece uses the item's own model. If your left and right models render mirrored, swap the two keys: "left" is from the viewpoint of a player facing the furniture's front.

Counter-style corners work too. `inner:` and `outer:` apply when a piece joins a sideways row to a perpendicular one (the neighbour facing 90° off). Place the corner piece facing the direction you want the bend, and swap the two keys if your models come out inverted.

## Vehicles

A `vehicle:` section makes the furniture driveable: the player in the **first seat** steers with their look direction and W/S, and passengers in the other seats ride along. Vehicles need **Paper 1.21.2+** (elsewhere they stay parked) and can't have `barriers:` or `lights:`, since blocks can't move.

```yaml
furniture:
  seats:
    - "0,0,0" # The driver's seat
    - "0,0,-0.8" # Extra seats ride along
  vehicle:
    speed: 0.3 # Horizontal blocks per tick at full throttle
    fly-speed: 0.0 # Above 0, holding jump ascends and the vehicle hovers
    fuel: # Optional; without it, drives for free
      items:
        - coal
      per-item-seconds: 120
    smoke: # Optional exhaust while driving
      particle: campfire_cosy_smoke
      amount: 3
      offset: "0,0.5,-1" # Local position; rotates with the vehicle
```

Ground vehicles fall off edges and step up single blocks; flying vehicles (a `fly-speed` above 0) hover and climb while jump is held. Fuel burns one matching item from the driver's inventory per `per-item-seconds` of driving, and with an empty tank the vehicle refuses to move and warns in the action bar. Sneak to dismount, and punch the vehicle to break it like any furniture.

## WorldGuard flags

With WorldGuard installed, EcoItems registers region flags that all default to allow: `ecoitems-furniture-interact`, `ecoitems-furniture-sit`, `ecoitems-furniture-storage`, `ecoitems-vehicle`, and `ecoitems-block-interact` for [custom block](how-to-make-a-block) click effects. Deny them per region for spawn furniture that shouldn't be sat on, vehicles in protected zones, and so on. Placing and breaking already respect regions through the normal build flags.

:::caution
Furniture entities are real persistent entities. Don't run `/kill @e`: it strips furniture displays and leaves their barriers behind (breakable by operators).
:::

<hr/>

## Where to go next

- **Blocks:** [How to Make a Block](how-to-make-a-block) for full blocks and the shared `drops:` format.
- **Dialogs:** [How to Make a Dialog](how-to-make-a-dialog) to open a screen from a furniture effect.
- **The pack:** [Resource Packs](resource-packs) for adding your own models.
- **Migrating:** [Migrating to EcoItems](migrating-to-ecoitems) to bring furniture over from Oraxen, Nexo, or ItemsAdder.
