import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("maven-publish")
    id("com.jfrog.bintray")
    id("org.jetbrains.dokka") version "0.10.1"
    id("com.bnorm.power.kotlin-power-assert") version "0.5.3"

    idea
}

repositories {
    jcenter()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.4.10")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

tasks {
    val dokka by getting(DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
    }
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    this.archiveClassifier.set("javadoc")
    from(tasks.dokka)
}

publishing {
    publications {
        create<MavenPublication>("bintray") {
            groupId = project.group as String
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("kotlite core")
                description.set("Kotlin repository generator for Sqliteql (compile time dependencies)")
                url.set("https://github.com/mfarsikov/kotlite")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set("Max Farsikov")
                        email.set("farsikovmax@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/mfarsikov/kotlite.git")
                    developerConnection.set("scm:git:ssh://github.com/mfarsikov/kotlite.git")
                    url.set("https://github.com/mfarsikov/kotlite")
                }
            }
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")
    setPublications("bintray")
    isPublish = true
    with(pkg) {
        repo = "Kotlite"
        name = "kotlite-core"
        userOrg = System.getenv("BINTRAY_USER")
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/mfarsikov/kotlite"
        with(version) {
            name = project.version.toString()
            desc = "kotlite compile time dependencies"
            //released = yyyy-MM-dd'T'HH:mm:ss.SSSZZ
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
        if (name == "compileTestKotlin") {
            useIR = true
        }
    }
}

configure<com.bnorm.power.PowerAssertGradleExtension> {
    functions = listOf("kotlin.assert")
}

tasks.test {
    useJUnitPlatform()
}
