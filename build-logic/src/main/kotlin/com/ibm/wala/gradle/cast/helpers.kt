package com.ibm.wala.gradle.cast

import java.io.File
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskInstantiationException
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.closureOf
import org.gradle.language.cpp.CppBinary
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.tasks.AbstractLinkTask

////////////////////////////////////////////////////////////////////////
//
//  helpers for building native CAst components
//

/**
 * Configures the provided [Task] using the given action.
 *
 * [TaskProvider] already offers a [TaskProvider.configure] method that is compatible with Gradle's
 * [task configuration avoidance APIs](https://docs.gradle.org/current/userguide/task_configuration_avoidance.html).
 * Unfortunately, many of the APIs for native compilation provide access only to [Provider]<[Task]>
 * instances, which have no configuration-avoiding `configure` method. Instead, the best we can do
 * is to [get][Provider.get] the provided [Task], then configure it using [Task.configure].
 *
 * See also
 * [an existing request to improve these APIs](https://github.com/gradle/gradle-native/issues/683).
 *
 * @param action The configuration action to be applied to the task.
 */
fun <T : Task> Provider<T>.configure(action: T.() -> Unit) {
  get().configure(closureOf(action))
}

private fun resolveJvmLibrary(javaHome: File, osFamilyName: String): File {
  val (libraryName, subdirs) =
      when (osFamilyName) {
        OperatingSystemFamily.LINUX ->
            "libjvm.so" to listOf("jre/lib/amd64/server", "lib/amd64/server", "lib/server")
        OperatingSystemFamily.MACOS -> "libjvm.dylib" to listOf("jre/lib/server", "lib/server")
        OperatingSystemFamily.WINDOWS -> "jvm.lib" to listOf("lib")
        else ->
            throw TaskInstantiationException(
                "unrecognized operating system family \"$osFamilyName\""
            )
      }
  return subdirs.map { javaHome.resolve("$it/$libraryName") }.find(File::exists)
      ?: throw TaskInstantiationException(
          "could not locate $libraryName under JDK home $javaHome; probed ${subdirs.map { "$it/$libraryName" }}"
      )
}

fun CppBinary.addJvmLibrary(project: Project) {
  val osFamilyName = targetMachine.operatingSystemFamily.name

  val osIncludeSubdir =
      when (osFamilyName) {
        OperatingSystemFamily.LINUX -> "linux"
        OperatingSystemFamily.MACOS -> "darwin"
        OperatingSystemFamily.WINDOWS -> "win32"
        else ->
            throw TaskInstantiationException(
                "unrecognized operating system family \"$osFamilyName\""
            )
      }

  // Lazy and configuration-cache compatible: no eager `System.getProperty`, no eager disk probes.
  // `resolveJvmLibrary` → `File::exists` runs only when the `FileCollection` is queried at
  // execution time.
  val javaHome = project.providers.systemProperty("java.home").map(::File)
  val jniIncludeDirs =
      project.files(
          listOf("include", "include/$osIncludeSubdir").map { subdir ->
            javaHome.map { it.resolve(subdir) }
          }
      )
  val libJvm = javaHome.map { resolveJvmLibrary(it, osFamilyName) }

  compileTask.configure { includes(jniIncludeDirs) }

  project.dependencies.add((linkLibraries as Configuration).name, project.files(libJvm))
}

/**
 * Adds runtime search paths (rpaths) for all library dependencies of the link task.
 *
 * This extension method configures the underlying [AbstractLinkTask] to add linker arguments that
 * specify runtime search paths for all libraries that the task links against. This ensures that the
 * runtime loader can find these libraries when the resulting binary is executed.
 *
 * The method only adds rpaths on non-Windows platforms, as the rpath concept is not applicable to
 * Windows.
 */
fun Provider<out AbstractLinkTask>.addRpaths() {
  configure {
    linkerArgs.addAll(
        project.provider {
          if (!targetPlatform.get().operatingSystem.isWindows) {
            libs.map { "-Wl,-rpath,${it.parentFile}" }
          } else {
            emptyList()
          }
        }
    )
  }
}
