---
title: How to make an Item
sidebar_position: 1
---

## How to add items
Each item is its own config file, placed in the `/items/` folder, and you can add or remove them as you please. There's an example config called `_example.yml` to help you out!

The ID of the EcoItem is the file name. This is what you use in commands and in the [Item Lookup System](https://plugins.auxilor.io/the-item-lookup-system).
ID's must be lowercase letters, numbers, and underscores only.

## Example Item Config

```yaml
item:
  item: iron_sword hide_attributes
  display-name: "<g:#f953c6>Mithril Sword</g:#b91d73>"
  lore:
    - "&7Damage: &c12❤"
    - "&7Attack Speed: &c1.5"
    - ""
    - "<g:#f953c6>MITHRIL BONUS</g:#b91d73>"
    - "&8» &#f953c6Deal 50% more damage in the nether"
  craftable: true
  crafting-permission: "ecoitems.craft.example"
  recipe:
    - ""
    - ecoitems:mithril 2
    - ""
     
    - ""
    - ecoitems:mithril 2
    - ""
     
    - ""
    - stick
    - ""
  recipe-give-amount: 1

slot: mainhand
rarity: rare

base-damage: 12
base-attack-speed: 1.5
base-attack-range: 3.0

effects:
  - id: damage_multiplier
    args:
      multiplier: 1.5
    triggers:
      - melee_attack

conditions:
  - id: in_world
    args:
    world: world_the_nether
```

## Understanding all the sections
### The Item Section

```yaml
item:
  item: iron_sword hide_attributes # The item in-game: https://plugins.auxilor.io/the-item-lookup-system
  display-name: "<g:#f953c6>Mithril Sword</g:#b91d73>" # The display name of the item
  lore: # The item lore
    - "&7Damage: &c12❤"
    - "&7Attack Speed: &c1.5"
    - ""
    - "<g:#f953c6>MITHRIL BONUS</g:#b91d73>"
    - "&8» &#f953c6Deal 50% more damage in the nether"
  craftable: true # If the item can be crafted
  crafting-permission: "ecoitems.craft.example" # (Optional) The permission required to craft this recipe.
  recipe: # The recipe, read here for more: https://plugins.auxilor.io/the-item-lookup-system/recipes
    - ""
    - ecoitems:mithril 2
    - ""
    - ""
    - ecoitems:mithril 2
    - ""
    - ""
    - stick
    - ""
  recipe-give-amount: 1 # Optional, set the amount of items to give in the recipe
```
:::tip

We support shaped and shapeless recipes. Check out [Recipes](https://plugins.auxilor.io/the-item-lookup-system/recipes) for more info on how to configure these.

:::

### The Attributes Section

```yaml
# The slot the item has to be in to activate its effects.
# The options for slot are mainhand, offhand, hands, helmet, chestplate,
# leggings, boots, armor, any, a number from 0-40 (to specify an exact slot),
# or a list of slots like "9, 10, 11, mainhand"
# Use to choose weather this is a weapon, tool, armor piece, charm, etc.
# If you don't specify this, it will default to mainhand.
slot: mainhand

# (Optional) The rarity of the item
rarity: rare

base-damage: 12 # (Optional) The item base damage
base-attack-speed: 1.5 # (Optional) The item base attack speed
base-attack-range: 3.0 # (Optional) The item base attack range (entity interaction range, vanilla default 3.0)
```

Visit the [Minecraft Wiki](https://minecraft.wiki/w/Damage#Dealing_damage) for default attack damage and speeds.

### The Effects Section
:::dangerEffects Section

The effects section is the core functionality of the item. You can configure effects, conditions, filters, mutators and triggers in this section to run whilst the item is active.

Check out [Configuring an Effect](https://plugins.auxilor.io/effects/configuring-an-effect) to understand how to configure this section correctly.

For more advanced users or setups, you can configure chains in this section to string together different effects under one trigger. Check out [Configuring an Effect Chain](https://plugins.auxilor.io/effects/configuring-a-chain) for more info.

:::
```yaml
# The effects of the item (i.e. the functionality)
# See here: https://plugins.auxilor.io/effects/configuring-an-effect
effects:
  - id: damage_multiplier
    args:
      multiplier: 1.5
    triggers:
      - melee_attack

# The conditions required for the effects to activate
conditions:
  - id: in_world
    args:
    world: world_the_nether
```

:::dangerCustom Foods & Tools
You can create custom Tools and Foods using EcoItems by adding a config section.

Check it out here:
[Custom Foods](https://plugins.auxilor.io/ecoitems/how-to-make-a-custom-item/custom-foods),
[Custom Tools](https://plugins.auxilor.io/ecoitems/how-to-make-a-custom-item/custom-tools)
:::

<hr/>

## Default Configs
The default configs can be found [here](https://github.com/Auxilor/EcoItems/blob/master/eco-core/core-plugin/src/main/resources/items/). <br/>
You can find additional user-created configs on [lrcdb](https://lrcdb.auxilor.io/).