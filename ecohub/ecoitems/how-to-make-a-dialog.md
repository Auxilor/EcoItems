---
title: "How to Make a Dialog"
sidebar_position: 10
---

Dialogs are real client **GUI screens**, using the same native system vanilla uses for server links. Each dialog is one config with a **title**, **body text**, and **buttons** that run commands. This page covers making a dialog and the ways to open one.

:::info
Dialogs use the native Minecraft dialog system: they need a **Paper 1.21.6+** server, and every player sees them with no resource pack involved.
:::

## Quick start

1. Open `/plugins/EcoItems/dialogs/`.
2. Copy `_example_dialog.yml` and rename it to your dialog's ID, e.g. `kit_picker.yml`.
3. Set the `title` and `body`.
4. Add `buttons` with the `commands` or `console-commands` each one runs.
5. Run `/ecoitems reload`.
6. Open it with `/ecoitems dialog kit_picker` to confirm it works.

:::tip
`_example_dialog.yml` is included as a reference and is **never loaded**, so copy or rename it to make a real dialog. You can also organise dialogs into subfolders inside `dialogs/`, and they'll still load.
:::

## Naming and IDs

The file name without `.yml` is the dialog's ID, used in `/ecoitems dialog <id>`.

:::warning ID rules
IDs may only contain lowercase letters, numbers, and underscores (a-z, 0-9, _). No spaces, capitals, or hyphens, or the dialog will not load.
:::

## The structure of a dialog

| Part | What it controls |
| --- | --- |
| **Text** | The title and body lines |
| **Closing** | Whether escape closes the screen |
| **Buttons** | The action buttons and the commands they run |

```yaml
# === Text: title and body ===
title: "&6Choose a Kit"
body: # Lines of body text
  - "&fWelcome to the server."
  - "&7Pick a kit to get started."

# === Closing ===
can-close-with-escape: true # Defaults to true

# === Buttons: what players can click ===
buttons: # Optional; without any, the dialog is a notice with an OK button
  - label: "&aWarrior"
    tooltip: "&7Sword and shield" # Optional
    commands: # Run as the player
      - "kit warrior"
  - label: "&bArcher"
    console-commands: # Run from the console
      - "give %player% bow"
```

### Text

`title` and each `body` line support the full color and [glyph](how-to-make-a-glyph) pipeline.

### Closing

`can-close-with-escape` controls whether pressing escape closes the dialog. It defaults to `true`.

### Buttons

Each button has a `label`, an optional `tooltip`, and the commands it runs: `commands` run as the player, and `console-commands` run from the console. `%player%` is replaced in every command. Without `buttons:`, the dialog is a simple notice with an OK button, which suits rules screens and announcements.

## Opening a dialog

- **Command:** `/ecoitems dialog <id> [player]` (permission `ecoitems.command.dialog`).
- **Command blocks** and any plugin that can run commands.
- **Effects:** a [furniture](how-to-make-furniture) or [block](how-to-make-a-block) effect running `ecoitems dialog <id> %player%` through libreforge's command effect.

<hr/>

## Where to go next

- **Furniture:** [How to Make Furniture](how-to-make-furniture) to open a dialog when players interact with a placed object.
- **Commands:** [Commands and Permissions](commands-and-permissions) for the `/ecoitems dialog` command.
