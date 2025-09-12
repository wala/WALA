package com.ibm.wala.gradle

/**
 * Utility functions and properties for working with Java projects in the WALA build system.
 *
 * This file provides functionality to [identify Java projects][Project.isJavaProject], [collect all
 * Java projects from the root project's subprojects][Project.allJavaProjects], and
 * [perform operations on those Java projects][Project.forEachJavaProject]. These utilities help
 * streamline build configuration by making it easier to apply common settings across multiple Java
 * projects.
 *
 * See also
 * [a related Gradle Forums discussion of alternate strategies](https://discuss.gradle.org/t/configuration-on-demand-versus-plugins-withid/51264).
 */
import org.gradle.api.Project

/**
 * Determines whether this project is a Java project.
 *
 * @return `false` for the root project and specific cast-related projects, `true` for all other
 *   projects
 */
val Project.isJavaProject
  get() =
      when (path) {
        ":",
        ":cast:cast",
        ":cast:java:test",
        ":cast:js:html",
        ":cast:smoke_main",
        ":cast:xlator_test",
        -> false
        else -> true
      }

/**
 * Gets all Java projects from [rootProject][the root project]'s subprojects.
 *
 * @return A filtered list of subprojects that are Java projects (as determined by the
 *   [isJavaProject] property)
 */
val Project.allJavaProjects
  get() = rootProject.subprojects.filter(Project::isJavaProject)

/**
 * Performs the given [action] for each of [rootProject][the root project]'s Java subprojects.
 *
 * @param action The action to perform on each Java project
 */
fun Project.forEachJavaProject(action: (Project) -> Unit) {
  allJavaProjects.forEach(action)
}
