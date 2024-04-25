package bioimage.io

plugins {
    kotlin("jvm")
    id("model")
    id("de.undercouch.download")
}

group = "bioimage.io"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

//println(models.size)