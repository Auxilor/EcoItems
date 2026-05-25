---
title: Custom Foods
sidebar_position: 2
---

## How to add custom foods
Custom Foods share the same core configuration as any other EcoItem, but with an added section for configuring the behaviour of the food.

This allows you to configure how the food works, such as the nutrition, eat time, potion effects, and if you can eat it without hunger.

### Example Food Config
```yaml
item:
  item: cooked_beef glint item_name:"Enchanted Steak"
  lore: [ ]
  craftable: true
  crafting-permission: "ecoitems.craft.example"
  recipe:
    - ""
    - cooked_beef 64
    - ""
    - cooked_beef 64
    - cooked_beef 64
    - cooked_beef 64
    - ""
    - cooked_beef 64
    - ""
  recipe-give-amount: 1

food:
  nutrition: 12
  saturation: 2
  eat-seconds: 1
  can-always-eat: false

  effects:
    - effect: regeneration
      duration: 40
      level: 1
      ambient: true
      particles: true
      icon: true
      probability: 100
```

## Understanding all the sections
### The Item Section
The Item Section is the same as any other EcoItem, you can see this [here](https://plugins.auxilor.io/ecoitems/how-to-make-a-custom-item)

### The Food Config Section

```yaml
# Options for the food
# These options do not update existing foods, only new ones
food:
  # Read here: https://minecraft.fandom.com/wiki/Food#Hunger_and_saturation
  nutrition: 12
  saturation: 2

  # The time in seconds it takes to eat the food
  eat-seconds: 1

  # (Optional) Set if this food can always be eaten, even if the player is not hungry
  can-always-eat: false

  # (Optional) Potion effects to give when eating the food
  effects:
    - effect: regeneration # The ID of the potion effect to give
      duration: 40 # The duration of the potion effect in ticks
      level: 1 # The level of the potion effect
      ambient: true # If the potion is ambient (defaults to true)
      particles: true # If the potion has particles (defaults to true)
      icon: true # If the potion icon shows up (defaults to true)
      probability: 100 # The probability of the potion effect occurring (defaults to 100)
```