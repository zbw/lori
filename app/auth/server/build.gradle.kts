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
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        name = "ktor-eap"
    }
}

dependencies {
    val ktorVersion by System.getProperties()
    implementation(project(":app:auth:api"))
    implementation("org.postgresql:postgresql:42.2.20")
    implementation("io.zonky.test:embedded-postgres:1.3.0")
    implementation("org.flywaydb:flyway-core:7.9.1")
    implementation("com.mchange:c3p0:0.9.5.5")
    implementation("de.mkammerer:argon2-jvm:2.10.1")
    implementation("io.jsonwebtoken:jjwt:0.9.1")
    implementation("io.ktor:ktor-auth-jwt:2.0.0-eap-278")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson-jvm:$ktorVersion")
}

application {
    mainClass.set("de.zbw.api.auth.server.AuthServer")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.733".toBigDecimal()
            }
        }
    }
}
