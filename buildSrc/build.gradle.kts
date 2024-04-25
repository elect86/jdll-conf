plugins {
    `kotlin-dsl`
}
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.bioimage:JDLL")
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:$embeddedKotlinVersion")
    implementation("de.undercouch:gradle-download-task:5.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1-Beta")

    implementation(platform("org.scijava:pom-scijava:37.0.0"))
    implementation("net.imglib2:imglib2")
}