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
    jcenter()
    google()
}

dependencies {
    val ktorVersion by System.getProperties()
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation(project(":app:access:api"))
    implementation("org.postgresql:postgresql:42.2.20")
    implementation("io.zonky.test:embedded-postgres:1.3.0")
    implementation("org.flywaydb:flyway-core:7.9.1")
    implementation("com.mchange:c3p0:0.9.5.5")
    runtimeOnly(project(path = ":app:access:server:ui", configuration = "npmResources"))
}

application {
    mainClass.set("de.zbw.api.access.server.AccessServer")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.886".toBigDecimal()
            }
        }
    }
}
