plugins {
    id("zbw.kotlin-conventions")
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
    implementation(project(":app:helloworld:api"))
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.638".toBigDecimal()
            }
        }
    }
}
