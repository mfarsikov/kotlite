pluginManagement{
    repositories{
        gradlePluginPortal()
        mavenLocal()
        jcenter()
    }
}
include("kotlite-kapt", "kotlite-core", "sqlite-example")
rootProject.name = "kotlite"
