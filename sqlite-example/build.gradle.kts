import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("com.bnorm.power.kotlin-power-assert") version "0.11.0"
    idea
    application
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

application {
    mainClass.set("my.pack.MainKt")
}

group = "com.example"
java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation(project(":kotlite-core"))
    implementation("org.xerial:sqlite-jdbc:3.36.0.2")
    implementation("org.flywaydb:flyway-core:8.5.0")

    kapt(project(":kotlite-kapt"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    implementation("org.jetbrains.kotlin:kotlin-test:1.6.10")
}

configure<com.bnorm.power.PowerAssertGradleExtension> {
    functions = listOf("kotlin.assert")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

kapt {
    arguments {
        arg("kotlite.log.level", "debug")
        arg("kotlite.db.qualifiedName", "my.pack.DB")
        arg("kotlite.spring", "false")
    }
}
