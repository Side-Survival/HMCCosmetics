pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "SurvivalCosmetics"
include(
    "common",
    "v1_20_R1",
)
