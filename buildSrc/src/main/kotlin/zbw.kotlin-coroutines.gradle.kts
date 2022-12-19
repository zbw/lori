plugins {
    kotlin("jvm")
}

dependencies {
    val kotlinxCoroutinesVersion by System.getProperties()
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}
