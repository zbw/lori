plugins {
    kotlin("jvm")
    id("org.openapi.generator")
}
dependencies {
    val moshi by System.getProperties()
    implementation("com.squareup.moshi:moshi:$moshi")
    implementation("com.squareup.moshi:moshi-kotlin:$moshi")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
}
repositories {
    mavenLocal()
    mavenCentral()
    google()
}
