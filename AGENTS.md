# EcoItems

A fully-featured custom items and blocks plugin for Minecraft versions 1.21.8 - 26.2. Supports Spigot and Paper - some
functionality that requires, or even is nicer to write with Paper APIs should require Paper and simply refuse to work on
Spigot, that is okay. It is however a requirement that EcoItems does work (at least to some extent) on Spigot due to
SpigotMC.org rules.

Built using [eco](https://github.com/Auxilor/eco) and [libreforge](https://github.com/Auxilor/libreforge)

Plugin constructors and constructor-registered `onLoad` callbacks must use only
`com.willfp.libreforge.loader/**`. The full libreforge runtime is installed by the base
`LibreforgePlugin` `onLoad(START)` callback, so services that depend on any other libreforge package must be
initialized in `handleEnable()` or later.
