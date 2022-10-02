pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        jcenter()
    }
}
include("kotlite-ksp", "kotlite-core", "sqlite-example")
rootProject.name = "kotlite"
