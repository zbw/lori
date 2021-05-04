plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        jvmTarget = "11"
    }
}
