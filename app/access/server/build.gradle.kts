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
    implementation("org.postgresql:postgresql:42.3.1")
    implementation("io.zonky.test:embedded-postgres:1.3.1")
    implementation("org.flywaydb:flyway-core:7.15.0")
    implementation("com.mchange:c3p0:0.9.5.5")
    implementation("com.github.lamba92", "ktor-spa", "1.2.1")
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
