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
    implementation(project(":app:handle:api"))

    val ktorVersion by System.getProperties()
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("net.handle:handle-client:9.3.0")
    implementation("com.benasher44:uuid:0.2.4")
}

application {
    mainClass.set("de.zbw.api.handle.server.HandleServer")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.874".toBigDecimal()
            }
        }
    }
}
