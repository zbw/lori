plugins {
    kotlin("jvm")
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
    apiPackage.set("de.zbw.lori.api.v1")
    modelPackage.set("de.zbw.lori.model")
    configOptions.set(mapOf(
        Pair("dataLibrary", "java8")
    ).toMutableMap())
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate)
}

sourceSets {
    main {
        java {
            srcDir("${buildDir.absolutePath}/generated/src/main/")
        }
    }
}
