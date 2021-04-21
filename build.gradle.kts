plugins {
    kotlin("jvm") version "1.4.32" apply false
    kotlin("plugin.serialization") version "1.4.32" apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
    id("com.github.mfarsikov.kewt-versioning") version "0.6.0"
    id("org.jetbrains.dokka") version "1.4.30" apply false
}

repositories {
    mavenLocal()
    mavenCentral()
}

val v = kewtVersioning.version
version = v
group = "com.github.mfarsikov"
subprojects {
    version = v
    group = "com.github.mfarsikov"
}

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("NEXUS_USERNAME"))
            password.set(System.getenv("NEXUS_PASSWORD"))
        }
    }
}
