---
title: "Delivery Modes"
sidebar_position: 4
---

`delivery.mode` in `pack.yml` controls how the built pack reaches players.

| Mode | How it works | Best for |
| --- | --- | --- |
| `hosted` | Uploads the pack to a packhost instance and delivers the returned URL | Most servers — zero setup |
| `self-hosted` | Serves the pack over HTTP from the Minecraft server itself | Servers with an open port and a public address |
| `external` | Exports `pack.zip` to a folder; you host it and set the URL | Servers with an existing CDN or web server |
| `none` | Builds the pack but delivers nothing | Testing, or manual distribution |

## hosted

The pack is uploaded to the packhost instance at `delivery.hosted.url` (the public instance at `packs.auxilor.io` by default) on every reload. Identical packs deduplicate server-side, so reloads without pack changes are free. You can [run your own packhost](packhost) and point the URL at it.

## self-hosted

EcoItems serves the pack itself on `delivery.self-hosted.bind`:`port`. You must set `public-url` to the address players can reach the server on (including the port), e.g. `http://203.0.113.4:8163`. Make sure the port is open in your firewall or exposed by your host.

The current and previous packs both stay downloadable, so players joining mid-reload never hit a dead link.

## external

The pack is written to `delivery.external.directory` as `pack.zip`. Upload or sync it to your own hosting, and set `delivery.external.url` to its public URL. If the URL is empty the pack is only exported, and nothing is sent to players.

## Sending

With any active mode, the pack is sent to players on join (`send-on-join`) and re-sent to everyone after a reload (`send-on-reload`). The prompt, whether the pack is `required`, and whether declining kicks (`kick-on-decline`) are all configurable — see [Pack Configuration](configuration).
