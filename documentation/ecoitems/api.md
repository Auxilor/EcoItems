---
title: "API"
sidebar_position: 7
---

This page is for developers who want to read or modify EcoItems from their own plugin. EcoItems is open-source, so you can depend on it directly and use its API to look up and give items in code.

## Source code

The full source is on GitHub: [Auxilor/EcoItems](https://github.com/Auxilor/EcoItems).

## Adding the dependency

1. Add the Auxilor repository to your `build.gradle.kts`:
2. Add EcoItems as a `compileOnly` dependency, swapping `<version>` for the version you want to build against.

```kotlin
repositories {
    maven("https://repo.auxilor.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.willfp:EcoItems:<version>")
}
```

The latest version available on the repo can be found [here](https://github.com/Auxilor/EcoItems/tags).

<hr/>

## Where to go next

- **eco framework:** [eco](https://github.com/Auxilor/eco) is where the shared APIs (effects, conditions, item lookup) live.
- **Config side:** [How to make an Item](how-to-make-a-custom-item/how-to-make-a-custom-item) shows the config that backs the items you load through the API.