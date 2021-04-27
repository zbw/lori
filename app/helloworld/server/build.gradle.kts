plugins {
    id("zbw.kotlin-application")
    id("zbw.kotlin-conventions")
    id("zbw.kotlin-microservice-scaffold")
    id("zbw.kotlin-tests")
    id("zbw.tracing")
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    google()
}

dependencies {
    implementation(project(":app:helloworld:api"))
}

application {
    mainClass.set("de.zbw.api.helloworld.server.HelloWorldServer")
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.785".toBigDecimal()
            }
        }
    }
}
