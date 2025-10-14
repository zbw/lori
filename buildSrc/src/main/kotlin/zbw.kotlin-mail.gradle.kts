plugins {
    kotlin("jvm")
}

dependencies {
    val jakarta by System.getProperties()
    implementation("com.sun.mail:jakarta.mail:$jakarta")
}
repositories {
    mavenLocal()
    mavenCentral()
    google()
}
