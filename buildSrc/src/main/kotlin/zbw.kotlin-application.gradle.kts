import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins{
    application
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}
