plugins {
    kotlin("jvm") version "1.9.24"
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
    // macos
    // TODO: switch based on OS
    runtimeOnly("org.jogamp.gluegen:gluegen-rt:2.5.0:natives-macosx-universal")
    runtimeOnly("org.jogamp.jogl:jogl-all:2.5.0:natives-macosx-universal")

    // native window
    runtimeOnly("org.jogamp.jogl:nativewindow-main:2.5.0")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}

tasks.register<JavaExec>("runHalfEdgeMeshTest") {
    group = "application"
    description = "Run HalfEdgeMesh test"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("mesh.HalfEdgeMeshTestKt")
}
