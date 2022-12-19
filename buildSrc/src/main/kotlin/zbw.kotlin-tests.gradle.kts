plugins {
    kotlin("jvm")
    jacoco
}

tasks.test {
    useTestNG()
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.7"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html
        csv
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

dependencies {
    val mockkVersion by System.getProperties()
    testImplementation(kotlin("test-testng"))
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("io.mockk:mockk:$mockkVersion")
}
