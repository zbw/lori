plugins{
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}
