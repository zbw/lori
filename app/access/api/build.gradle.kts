
plugins {
    id("zbw.proto-api")
    id("zbw.openapi")
}

openApiValidate {
    inputSpec.set("$projectDir/src/main/openapi/openapi.yaml")
}

openApiGenerate {
    inputSpec.set("$projectDir/src/main/openapi/openapi.yaml")
    outputDir.set("$buildDir/generated")
    generatorName.set("kotlin")
    apiPackage.set("de.zbw.access.api.v1")
    modelPackage.set("de.zbw.access.model")
    configOptions.set(mapOf(
        Pair("dataLibrary", "java8")
        //Pair("sourceFolder", "source/openapi/main/kotlin")
    ).toMutableMap())
}

sourceSets {
    main {
        java {
            srcDir("${buildDir.absolutePath}/generated/src/main/")
        }
    }
}

dependencies {
    implementation("com.squareup.moshi:moshi:1.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
}