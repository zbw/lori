plugins {
    kotlin("jvm")
    id("org.openapi.generator")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    val moshi by System.getProperties()
    implementation("com.squareup.moshi:moshi:$moshi")
    implementation("com.squareup.moshi:moshi-kotlin:$moshi")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
}
repositories {
    mavenLocal()
    mavenCentral()
    google()
}
