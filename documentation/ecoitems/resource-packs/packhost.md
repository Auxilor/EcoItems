---
title: "Packhost"
sidebar_position: 7
---

Packhost is the small standalone service behind `hosted` delivery mode. Servers upload their pack to it, players download from it. Packs are content-addressed by SHA-1 and stored in S3-compatible object storage, with metadata in Postgres.

The public instance at `https://packs.auxilor.io` is free to use and is the default in `pack.yml` — you only need this page if you want to run your own.

## Running your own

Packhost lives in its own repository at [Auxilor/packhost](https://github.com/Auxilor/packhost) and runs anywhere that can run a container. It needs:

- A Postgres database (`DATABASE_URL`)
- An S3-compatible bucket (`S3_BUCKET`, `S3_ENDPOINT`, `S3_REGION`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`) — AWS S3, Cloudflare R2, Backblaze B2, and MinIO all work

On Railway: create a service from the repository, attach a Postgres database, set the `S3_*` variables, and point the health check at `/healthz`. For any other host, build and run the Dockerfile with the same environment. Full instructions, the API reference, and a local development compose stack are in the [packhost README](https://github.com/Auxilor/packhost#readme).

Then point your server at it:

```yaml
delivery:
  mode: hosted
  hosted:
    url: "https://packs.example.com"
```

## Limits

Uploads are anonymous, capped at 64 MiB per pack, and rate-limited per IP (100 uploads / 1 GiB per day). Identical packs deduplicate, so a reload that doesn't change the pack re-uses the existing upload.
