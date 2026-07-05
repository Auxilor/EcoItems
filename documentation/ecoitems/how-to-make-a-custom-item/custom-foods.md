---
title: "Custom Foods"
sidebar_position: 2
---

A custom food is a normal EcoItem with an added **food** section that makes it edible: you set its **nutrition**, **eat time**, and any **potion effects** it grants. This page covers adding that food behaviour on top of a standard item.

## Quick start

1. Open the `/plugins/EcoItems/items/` folder.
2. Copy `_example.yml` and rename it to your food's ID, e.g. `enchanted_steak.yml`.
3. Set up the `item:` section as you would for any item (see [How to make an Item](how-to-make-a-custom-item)).
4. Add a `food:` section with the nutrition, saturation, and eat time.
5. Run `/ecoitems reload`.
6. Give yourself the item with `/ecoitems give <you> enchanted_steak` and eat it to confirm the hunger and effects apply.

:::tip
`_example.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real food. You can also organise foods into subfolders inside `items/`, and they'll still load.
:::

## Naming and IDs

Foods live in the same `items/` folder as every other EcoItem, so the file name without `.yml` is the item's ID, used in commands and in the [Item Lookup System](https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system).

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the food will not load.
:::

## The structure of a food

| Part | What it controls |
| --- | --- |
| **Item** | The base item, display, and recipe, identical to any EcoItem |
| **Food** | The eating behaviour: nutrition, eat time, and potion effects |

```yaml
# === Item: a normal EcoItem ===
item:
  item: cooked_beef glint item_name:"Enchanted Steak" # The base item
  lore: [] # The item lore
  craftable: true # If the item can be crafted
  crafting-permission: ecoitems.craft.custom_item # Optional; permission required to craft the item
  recipe-give-amount: 1 # Optional; how many items the recipe gives, defaults to 1
  recipe: # The recipe: https://hub.auxilor.io/wiki/eco/the-item-lookup-system-the-item-lookup-system/recipes
    - ""
    - cooked_beef 64
    - ""
    - cooked_beef 64
    - cooked_beef 64
    - cooked_beef 64
    - ""
    - cooked_beef 64
    - ""

# === Food: the eating behaviour ===
food:
  nutrition: 12 # Hunger restored: https://minecraft.wiki/w/Hunger
  saturation: 2 # Saturation restored
  eat-seconds: 1 # Time in seconds it takes to eat
  can-always-eat: false # Optional; if it can be eaten on a full hunger bar
  effects: # Optional; potion effects granted on eating
    - effect: regeneration # The potion effect ID
      duration: 40 # Duration in ticks
      level: 1 # The effect level
      ambient: true # Optional; if the potion is ambient, defaults to true
      particles: true # Optional; if the potion shows particles, defaults to true
      icon: true # Optional; if the potion icon shows, defaults to true
      probability: 100 # Optional; chance the effect is applied, defaults to 100
```

### Item

The `item:` section is exactly the same as any other EcoItem. See [How to make an Item](how-to-make-a-custom-item) for the full breakdown of the base item, display, and recipe fields. A food is still a full EcoItem, so the top-level `effects:` and `conditions:` work too; the `food:` potion effects fire on eating, while `effects:` run on whatever triggers you give them.

### Food

The `food:` section turns the item into something edible.

```yaml
food:
  nutrition: 12 # Hunger restored: https://minecraft.wiki/w/Hunger
  saturation: 2 # Saturation restored
  eat-seconds: 1 # Time in seconds it takes to eat
  can-always-eat: false # Optional; if it can be eaten on a full hunger bar
  effects: # Optional; potion effects granted on eating
    - effect: regeneration # The potion effect ID
      duration: 40 # Duration in ticks
      level: 1 # The effect level
      ambient: true # Optional; if the potion is ambient, defaults to true
      particles: true # Optional; if the potion shows particles, defaults to true
      icon: true # Optional; if the potion icon shows, defaults to true
      probability: 100 # Optional; chance the effect is applied, defaults to 100
```

:::info Food changes apply to new items only
Editing the `food:` section does not update foods already in player inventories; only newly given or crafted copies pick up the change.
:::

:::tip Troubleshooting
- **Can't eat the item?** Foods need a valid `food:` section; check `nutrition` and `eat-seconds` are set.
- **Potion effects not applying?** Confirm the `effect` ID is a valid potion effect and `probability` is high enough.
- **Hunger doesn't change after editing?** Get a fresh copy; existing foods keep their old behaviour.
:::

<hr/>

## Where to go next

- **The base item:** [How to make an Item](how-to-make-a-custom-item) for the shared item, display, and recipe fields.
- **Custom tools:** [Custom Tools](custom-tools) for mining behaviour instead of eating.