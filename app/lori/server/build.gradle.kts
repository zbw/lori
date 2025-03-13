plugins {
    id("zbw.kotlin-application")
    id("zbw.kotlin-conventions")
    id("zbw.kotlin-coroutines")
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
    maven { setUrl("https://build.shibboleth.net/maven/releases/") }
}

dependencies {
    val apacheCommonsCSV by System.getProperties()
    val flywayVersion by System.getProperties()
    val hikaricp by System.getProperties()
    val ktorVersion by System.getProperties()
    val openTelemetry by System.getProperties()
    val postgresJDBCVersion by System.getProperties()
    val zonkyVersion by System.getProperties()
    val openSaml by System.getProperties()
    implementation(project(":app:lori:api"))
    implementation("io.ktor:ktor-client-gson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    runtimeOnly("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.opentelemetry:opentelemetry-sdk:$openTelemetry")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:$openTelemetry")
    implementation("io.zonky.test:embedded-postgres:$zonkyVersion")
    implementation("org.postgresql:postgresql:$postgresJDBCVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.apache.commons:commons-csv:$apacheCommonsCSV")
    implementation("org.opensaml:opensaml-core:$openSaml")
    implementation("org.opensaml:opensaml-saml-api:$openSaml")
    implementation("org.opensaml:opensaml-saml-impl:$openSaml")
    implementation("com.github.h0tk3y.betterParse:better-parse:0.4.4")
    implementation("com.zaxxer:HikariCP:$hikaricp")
    implementation("commons-logging:commons-logging:1.3.5")

    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    runtimeOnly(project(path = ":app:lori:server:ui", configuration = "npmResources"))
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
}

application {
    mainClass.set("de.zbw.api.lori.server.LoriServer")
    applicationDefaultJvmArgs = listOf("--add-opens", "java.base/java.time=ALL-UNNAMED")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.800".toBigDecimal()
            }
        }
    }
}

tasks.test {
    // See https://github.com/mockk/mockk/issues/681 for more information
    jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")
}
