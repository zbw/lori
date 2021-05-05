import com.github.gradle.node.npm.task.NpmTask

buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("com.github.node-gradle:gradle-node-plugin:3.0.1")
    }
}

plugins {
    base
    id("com.github.node-gradle.node") version "3.0.1"
    //id("com.moowork.node") version "1.3.1" // gradle-node-plugin
}

node {
    /* gradle-node-plugin configuration
       https://github.com/srs/gradle-node-plugin/blob/master/docs/node.md
       Task name pattern:
       ./gradlew npm_<command> Executes an NPM command.
    */

    // Version of node to use.
    version.set("10.19.0")

    // Version of npm to use.
    npmVersion.set("6.14.4")

    // If true, it will download node using above parameters.
    // If false, it will try to use globally installed node.
    download.set(true)
}

tasks.named<NpmTask>("npm_run_build") {
    // make sure the build task is executed only when appropriate files change
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
    archiveBaseName.set("npm-app")
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

val test by tasks.registering(NpmTask::class) {
    dependsOn("assemble")
    args.set(listOf("run", "test"))
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
    dependsOn(test)
}

tasks.clean {
    delete(packageNpmApp.get().archiveFile)
    delete(testsExecutedMarkerName)
}
