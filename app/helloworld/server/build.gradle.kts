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
    implementation(project(":app:helloworld:api"))
    runtimeOnly(project(path = ":app:helloworld:server:ui", configuration = "npmResources"))
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
