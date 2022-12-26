package com.ibm.wala.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the

/**
 * Compiles some Java {@link SourceSet} using ECJ, but otherwise imitating the standard {@link
 * JavaCompile} task.
 */
@CacheableTask
open class JavaCompileUsingEcj : JavaCompile() {

  init {
    // Resolve ECJ to a JAR archive.  This task will use that archive as a batch Java compiler.
    val ecjConfiguration =
        project.configurations.detachedConfiguration(
            project.dependencies.create("org.eclipse.jdt:ecj:3.21.0"))

    options.run {
      // Add Eclipse JDT configuration, especially for warnings/errors.
      compilerArgs.add("-properties")
      compilerArgs.add(
          project.layout.projectDirectory.file(".settings/org.eclipse.jdt.core.prefs").toString())

      // Compile by running an external process.  Specifically, use the standard "java" command from
      // the current Java toolchain to run the ECJ JAR archive.  Conveniently, that archive is set
      // up to act as a batch compiler when run as a application.
      isFork = true
      forkOptions.run {
        executable =
            project
                .the<JavaToolchainService>()
                .launcherFor(project.the<JavaPluginExtension>().toolchain)
                .get()
                .executablePath
                .toString()
        jvmArgs!!.run {
          add("-jar")
          add(ecjConfiguration.singleFile.absolutePath)
        }
      }

      // ECJ doesn't support the "-h" flag for setting the JNI header output directory.
      headerOutputDirectory.set(project.provider { null })
    }

    // Allow skipping all ECJ compilation tasks by setting a project property.
    onlyIf { !project.hasProperty("skipJavaUsingEcjTasks") }
  }

  fun setSourceSet(sourceSet: SourceSet) {
    // Imitate most of the behavior of the standard compilation task for the given sourceSet.
    val standardCompileTaskName = sourceSet.getCompileTaskName("java")
    val standardCompileTask = project.tasks.named<JavaCompile>(standardCompileTaskName).get()
    classpath = standardCompileTask.classpath
    source = standardCompileTask.source

    // However, put generated class files in a different build directory to avoid conflict.
    val destinationSubdir = "ecjClasses/${sourceSet.java.name}/${sourceSet.name}"
    destinationDirectory.set(project.layout.buildDirectory.dir(destinationSubdir))
  }

  companion object {
    @JvmStatic
    fun withSourceSet(project: Project, sourceSet: SourceSet): TaskProvider<JavaCompileUsingEcj> =
        project.tasks.register(
            sourceSet.getCompileTaskName("javaUsingEcj"), JavaCompileUsingEcj::class.java) {
              setSourceSet(sourceSet)
            }
  }
}
