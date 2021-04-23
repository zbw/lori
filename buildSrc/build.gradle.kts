plugins{
    `kotlin-dsl`
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:10.0.0")
    implementation("com.parmet:buf-gradle-plugin:0.1.0")
    implementation("gradle.plugin.com.google.protobuf:protobuf-gradle-plugin:0.8.15")
}
