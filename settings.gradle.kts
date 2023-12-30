pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "RealmsCosmetics"
include(
    "common",
    "v1_20_R1",
)
