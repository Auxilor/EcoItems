---
title: Dialogs
sidebar_position: 1
---

# Dialogs

:::info
Dialogs use the native Minecraft dialog system: they need a **Paper 1.21.6+** server, and every player sees them - no resource pack involved.
:::

Dialogs are real client GUI screens - the same system vanilla uses for server links. Each config in the `dialogs/` folder is one dialog; open one with `/ecoitems dialog <id> [player]` (permission `ecoitems.command.dialog`), from a command block, or from any plugin that can run commands - including a [furniture or block effect](../furniture/index) running `ecoitems dialog ...` via libreforge's command effect.

```yaml
# dialogs/kit_picker.yml
title: "&6Choose a Kit"
body:
  - "&fWelcome to the server."
  - "&7Pick a kit to get started."
can-close-with-escape: true
buttons:
  - label: "&aWarrior"
    tooltip: "&7Sword and shield"
    commands:            # run as the player
      - "kit warrior"
  - label: "&bArcher"
    console-commands:    # run from the console; %player% is replaced
      - "give %player% bow"
```

Without `buttons:`, the dialog is a simple notice with an OK button - good for rules screens and announcements. Text supports the full color/glyph pipeline.
