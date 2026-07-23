---
title: "Delivery Modes"
sidebar_position: 4
---

`delivery.mode` in `pack.yml` controls how the built pack reaches players.

| Mode | How it works | Best for |
| --- | --- | --- |
| `hosted` | Uploads the pack to a packhost instance and delivers the returned URL | Most servers - zero setup |
| `self-hosted` | Serves the pack over HTTP from the Minecraft server itself | Servers with an open port and a public address |
| `external` | Exports `pack.zip` to a folder; you host it and set the URL | Servers with an existing CDN or web server |
| `s3` | Uploads to S3-compatible object storage | Networks with existing AWS/R2/MinIO infrastructure |
| `none` | Builds the pack but delivers nothing | Testing, or manual distribution |

## hosted

The pack is uploaded to the packhost instance at `delivery.hosted.url` (the public instance at `packs.auxilor.io` by default) on every reload. Identical packs deduplicate server-side, so reloads without pack changes are free. You can [run your own packhost](packhost) and point the URL at it.

## self-hosted

EcoItems serves the pack itself on `delivery.self-hosted.bind`:`port`. You must set `public-url` to the address players can reach the server on (including the port), e.g. `http://203.0.113.4:8163`. Make sure the port is open in your firewall or exposed by your host.

The current and previous packs both stay downloadable, so players joining mid-reload never hit a dead link.

## external

The pack is written to `delivery.external.directory` as `pack.zip`. Upload or sync it to your own hosting, and set `delivery.external.url` to its public URL. If the URL is empty the pack is only exported, and nothing is sent to players.

## s3

The pack uploads straight to S3-compatible object storage - AWS S3, Cloudflare R2, MinIO, Backblaze B2, and friends - as `<sha1>.zip`, no SDK or extra tooling needed. Configure `delivery.s3` with the `endpoint`, `region`, `bucket`, and credentials. Objects are keyed by content hash, so unchanged packs overwrite themselves harmlessly.

Set `public-url` when players should download through a CDN or custom domain instead of the raw endpoint. `public-read: true` (default) sends `x-amz-acl: public-read` with the upload; turn it off when the bucket policy already grants public access (R2 buckets with public access enabled, for example). MinIO wants the default `path-style: true`; AWS accepts either style.

## Sending

With any active mode, the pack is sent to players on join (`send-on-join`) and re-sent to everyone after a reload (`send-on-reload`). The prompt, whether the pack is `required`, and whether declining kicks (`kick-on-decline`) are all configurable - see [Pack Configuration](configuration).
