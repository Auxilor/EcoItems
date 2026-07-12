import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("io.papermc.paperweight.userdev")
}

group = "com.willfp"
version = rootProject.version

dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
}

tasks {
    compileJava { options.release.set(25) }
    compileKotlin { compilerOptions.jvmTarget.set(JvmTarget.JVM_25) }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}
