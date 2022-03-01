plugins {
    id("zbw.kotlin-application")
    id("zbw.kotlin-conventions")
    id("zbw.kotlin-coroutines")
    id("zbw.kotlin-microservice-scaffold")
    id("zbw.kotlin-tests")
    id("zbw.tracing")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {
    val openTelemetry by System.getProperties()
    implementation(project(":app:lori:api"))
    implementation(project(":app:lori:client"))
    implementation("io.opentelemetry:opentelemetry-sdk:$openTelemetry")
}

application {
    mainClass.set("de.zbw.job.loriiimport.Main")
}
