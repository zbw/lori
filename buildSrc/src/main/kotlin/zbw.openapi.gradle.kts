plugins {
    kotlin("jvm")
    id("org.openapi.generator")
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate)
}
