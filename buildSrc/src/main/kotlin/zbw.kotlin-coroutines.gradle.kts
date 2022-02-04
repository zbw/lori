plugins {
    kotlin("jvm")
}

dependencies {
    val kotlinVersion by System.getProperties()
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinVersion")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}
