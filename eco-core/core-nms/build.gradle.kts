plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21" apply false
}

group = "com.willfp"
version = rootProject.version

subprojects {
    dependencies {
        compileOnly(project(":eco-core:core-plugin"))
    }

    pluginManager.withPlugin("io.papermc.paperweight.userdev") {
        dependencies {
            add("pluginRemapper", "net.fabricmc:tiny-remapper:0.13.1")
        }
    }

}
