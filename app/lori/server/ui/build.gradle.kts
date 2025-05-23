import com.github.gradle.node.npm.task.NpmTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("de.zbw.nodeplugin")
    id("com.github.node-gradle.node")
    id("zbw.openapi")
}

node {
}

tasks.register<GenerateTask>("buildOpenAPIJson") {
    inputSpec.set("${project(":app:lori:api").projectDir.path}/src/main/openapi/openapi.yaml")
    outputDir.set("$projectDir/src/generated-sources/openapijson")
    generatorName.set("openapi")
    modelPackage.set("de.zbw.lori.model")
    configOptions.set(
        mapOf(
            Pair("dateLibrary", "java8")
        ).toMutableMap()
    )
}

tasks.register<GenerateTask>("generateTypescript") {
    inputSpec.set("${project(":app:lori:api").projectDir.path}/src/main/openapi/openapi.yaml")
    outputDir.set("$projectDir/src/generated-sources/openapi")
    generatorName.set("typescript-fetch")
    val npmVersion by System.getProperties()
    additionalProperties.set(
        mapOf(
            Pair("supportsES6", "true"),
            Pair("npmVersion", "$npmVersion"),
            Pair("typescriptThreePlus", "true")
        ).toMutableMap()
    )
}

tasks.named<NpmTask>("npm_run_build") {
    // make sure the build task is executed only when appropriate files change
    dependsOn("buildOpenAPIJson", "generateTypescript")
    environment.set(mapOf("NODE_OPTIONS" to "--openssl-legacy-provider"))
    inputs.files(fileTree("public"))
    inputs.files(fileTree("src"))

    // "node_modules" appeared not reliable for dependency change detection (the task was rerun without changes)
    // though "package.json" and "package-lock.json" should be enough anyway
    inputs.file("package.json")
    inputs.file("package-lock.json")

    outputs.dir("build")
}

// pack output of the build into JAR file
val packageNpmApp by tasks.registering(Jar::class) {
    dependsOn("npm_run_build")
    archiveBaseName.set("lori-ui")
    archiveExtension.set("jar")
    destinationDirectory.set(file("${projectDir}/build_packageNpmApp"))
    from("dist") {
        // optional path under which output will be visible in Java classpath, e.g. static resources path
        into("dist")
    }
}

// declare a dedicated scope for publishing the packaged JAR
val npmResources: Configuration by configurations.creating

configurations.named("default").get().extendsFrom(npmResources)

// expose the artifact created by the packaging task
artifacts {
    add(npmResources.name, packageNpmApp.get().archiveFile) {
        builtBy(packageNpmApp)
        type = "jar"
    }
}

tasks.assemble {
    dependsOn(packageNpmApp)
}
val testsExecutedMarkerName: String = "${projectDir}/.tests.executed"

val testNpm by tasks.registering(NpmTask::class) {
    dependsOn("assemble")
    args.set(listOf("run", "test:unit"))
    environment.set(mapOf("CI" to "true"))

    inputs.files(fileTree("src"))
    inputs.file("package.json")
    inputs.file("package-lock.json")

    // allows easy triggering re-tests
    doLast {
        File(testsExecutedMarkerName).appendText("delete this file to force re-execution JavaScript tests")
    }
    outputs.file(testsExecutedMarkerName)
}

tasks.check {
    // TODO(CB): uncommented until vite is used
    //dependsOn(testNpm)
}

tasks.jar {
    dependsOn("npm_run_build")
}

tasks.clean {
    delete(packageNpmApp.get().archiveFile)
    delete(testsExecutedMarkerName)
}

tasks.npmSetup {
    dependsOn("buildOpenAPIJson", "generateTypescript")
}
