import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.7.20-1.0.6"
    kotlin("plugin.serialization")
    id("com.bnorm.power.kotlin-power-assert") version "0.12.0"
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation(project(":kotlite-core"))
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("org.flywaydb:flyway-core:8.5.13")

    // implementation(project(":kotlite-kapt"))
    ksp(project(":kotlite-ksp"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    implementation("org.jetbrains.kotlin:kotlin-test:1.7.0")
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

ksp {
    arg("kotlite.db.qualifiedName", "my.pack.DB")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
