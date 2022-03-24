plugins {
    id("zbw.kotlin-application")
    id("zbw.kotlin-conventions")
    id("zbw.kotlin-microservice-scaffold")
    id("zbw.kotlin-tests")
    id("zbw.tracing")
    id("zbw.kotlin-json")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    val ktorVersion by System.getProperties()
    val openTelemetry by System.getProperties()
    implementation(project(":app:lori:api"))
    runtimeOnly(project(path = ":app:lori:server:ui", configuration = "npmResources"))
    implementation("com.mchange:c3p0:0.9.5.5")
    implementation("com.github.lamba92.ktor-spa:ktor-spa:1.2.1")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
    implementation("io.ktor:ktor-client-gson:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.opentelemetry:opentelemetry-sdk:$openTelemetry")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:$openTelemetry-alpha")
    implementation("io.zonky.test:embedded-postgres:1.3.1")
    implementation("org.postgresql:postgresql:42.3.1")
    implementation("org.flywaydb:flyway-core:7.15.0")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
}

application {
    mainClass.set("de.zbw.api.lori.server.LoriServer")
    applicationDefaultJvmArgs = listOf("--add-opens", "java.base/java.time=ALL-UNNAMED")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.776".toBigDecimal()
            }
        }
    }
}

tasks.test {
    // https://github.com/mockk/mockk/issues/681
    jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")
}
