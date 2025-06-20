import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.9.24"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "jp.saltbread1"
version = "1.0"

repositories {
    mavenCentral()
    maven (url = "https://jogamp.org/deployment/maven/")
}

dependencies {
    implementation("org.processing:core:4.4.4")

    /**** JOGL natives ****/
    runtimeOnly("org.jogamp.gluegen:gluegen-rt:2.5.0:natives-macosx-universal")
    runtimeOnly("org.jogamp.jogl:jogl-all:2.5.0:natives-macosx-universal")

    runtimeOnly("org.jogamp.jogl:nativewindow-main:2.5.0")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}

tasks.named<Jar>("jar")
{
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}

tasks.named<ShadowJar>("shadowJar")
{
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}
