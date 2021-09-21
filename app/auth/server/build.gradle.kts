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
    implementation(project(":app:auth:api"))
    implementation("org.postgresql:postgresql:42.2.20")
    implementation("io.zonky.test:embedded-postgres:1.3.0")
    implementation("org.flywaydb:flyway-core:7.9.1")
}

application {
    mainClass.set("de.zbw.api.auth.server.AuthServer")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.818".toBigDecimal()
            }
        }
    }
}
