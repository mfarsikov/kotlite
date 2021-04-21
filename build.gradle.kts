import com.github.mfarsikov.kewt.versioning.plugin.Incrementer.MINOR
import com.github.mfarsikov.kewt.versioning.plugin.Incrementer.PATCH

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
kewtVersioning {
    configuration {
        branches {
            clear()
            add {
                regexes = mutableListOf("master".toRegex())
                incrementer = MINOR
                stringify = stringifier(useBranch = false, useSha = false, useTimestamp = false)
            }
            add {
                regexes = mutableListOf("fix/.*".toRegex())
                incrementer = PATCH
                stringify = stringifier(useSha = false, useTimestamp = false)
            }
            add {
                regexes = mutableListOf(".*".toRegex())
                incrementer = MINOR
                stringify = { version -> stringifier(useBranch = version.isSnapshot, useSha = false, useTimestamp = false)(version)}
            }
        }
    }
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
