package com.ibm.wala.gradle

import java.io.File
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.named
import org.gradle.process.ExecOperations

/**
 * Compiles some Java {@link SourceSet} using ECJ, but otherwise imitating the standard {@link
 * JavaCompile} task.
 */
@CacheableTask
abstract class JavaCompileUsingEcj : JavaCompile() {

  /** ECJ compiler, resolved to a JAR archive. */
  @CompileClasspath
  @InputFile
  val ecjJar: File = project.configurations.named("ecj").get().singleFile

  @InputFile
  @PathSensitive(PathSensitivity.NONE)
  val jdtPrefs: RegularFile =
      project.layout.projectDirectory.file(".settings/org.eclipse.jdt.core.prefs")

  @get:Inject abstract val execOperations: ExecOperations

  init {
    options.compilerArgumentProviders.run {
      add {
        listOf(
            "-properties",
            jdtPrefs.toString(),
            "-classpath",
            this@JavaCompileUsingEcj.classpath.joinToString(":"),
            "-d",
            destinationDirectory.get().toString())
      }
      add { source.files.map { it.toString() } }
    }
  }

  @TaskAction
  fun compile() {
    execOperations.javaexec {
      classpath(ecjJar.absolutePath)
      args(options.allCompilerArgs)
    }
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
