group = "com.willfp"
version = rootProject.version

val freeVersion = rootProject.hasProperty("free")
val buildConfigDir = layout.buildDirectory.dir("generated/buildconfig")

sourceSets {
    main {
        kotlin.srcDir(buildConfigDir)
    }
}

val generateBuildConfig by tasks.registering {
    outputs.dir(buildConfigDir)
    doFirst {
        val file = buildConfigDir.get().file("com/willfp/ecoitems/BuildConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.willfp.ecoitems

            object BuildConfig {
                const val FREE_VERSION = $freeVersion
            }
            """.trimIndent()
        )
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks {
    compileKotlin {
        dependsOn(generateBuildConfig)
    }

    sourcesJar {
        dependsOn(generateBuildConfig)
    }

    build {
        dependsOn(publishToMavenLocal)
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            from(components["java"])
            artifactId = if (freeVersion) {
                "${rootProject.name}-Free"
            } else {
                rootProject.name
            }
        }
    }

    publishing {
        repositories {
            maven {
                name = "Auxilor"
                url = uri("https://repo.auxilor.io/repository/maven-releases/")
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }
    }
}
