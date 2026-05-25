---
title: Custom Tools
sidebar_position: 3
---

## How to add custom tools
Custom Tools share the same core configuration as any other EcoItem, but with an added section for configuring the behaviour of the tool.

This allows you to configure how the tool works, such as the blocks, speed, and if it should have drops.

### Example Tool Config
```yaml
item:
  item: netherite_pickaxe glint max_damage:4096 item_name:"Hardened Netherite Pickaxe"
  lore: [ ]
  craftable: true
  crafting-permission: "ecoitems.craft.example"
  recipe:
    - netherite_ingot
    - netherite_ingot
    - netherite_ingot
    
    - netherite_ingot
    - netherite_pickaxe
    - netherite_ingot
    
    - netherite_ingot
    - netherite_ingot
    - netherite_ingot
  recipe-give-amount: 1

tool:
  mining-speed: 1.0
  damage-per-block: 1

  rules:
    - blocks:
        - "#mineable_pickaxe"
      speed: 45.8
    - blocks:
        - "#incorrect_for_netherite_tool"
      speed: 1
      drops: false
```
## Understanding all the sections
### The Item Section
The Item Section is the same as any other EcoItem, you can see this [here](https://plugins.auxilor.io/ecoitems/how-to-make-a-custom-item)

### The Tool Config Section

```yaml
# Options for the tool
# These options do not update existing tools, only new ones
tool:
  # The default mining speed, if not overridden by any rules
  mining-speed: 1.0
  # The amount of durability to remove from the tool when it is used
  damage-per-block: 1

  # (Optional) Rules for the tool
  # Blocks are lists of block names or block tags
  # A list of block names is here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html
  # A list of block (Material) tags is here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Tag.html
  rules:
    - blocks:
        - "#mineable_pickaxe" # Tags start with a #
      speed: 45.8 # The mining speed for these blocks
    - blocks:
        - "#incorrect_for_netherite_tool"
      speed: 1
      drops: false # (Optional) If the block should drop items when mined with this tool
```
