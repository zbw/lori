package de.zbw.nodeplugin

import com.github.gradle.node.NodeExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/**
 * A custom plugin for node.
 *
 * Created on 05-07-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
open class ZbwNodePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.configurePlugins()
        project.configureNode()
    }
}

open class ZbwNodeExtension {
    var archiveBaseName: String = ""
}

fun Project.`node`(configure: Action<NodeExtension>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("node", configure)

internal fun Project.configureNode(): Unit = this.extensions.getByType<NodeExtension>().run {
    node {
        val nodeVersion by System.getProperties()
        val npmVersion by System.getProperties()
        /* gradle-node-plugin configuration
           https://github.com/srs/gradle-node-plugin/blob/master/docs/node.md
           Task name pattern:
           ./gradlew npm_<command> Executes an NPM command.
        */

        // Version of node to use.
        version.set("$nodeVersion")

        // Version of npm to use.
        this.npmVersion.set("$npmVersion")

        // If true, it will download node using above parameters.
        // If false, it will try to use globally installed node.
        download.set(true)
    }
}

internal fun Project.configurePlugins() {
    plugins.apply("base")
    plugins.apply("com.github.node-gradle.node")
}
