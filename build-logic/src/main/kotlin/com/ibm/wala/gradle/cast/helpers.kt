package com.ibm.wala.gradle.cast

import java.io.File
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskInstantiationException
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.jvm.Jvm
import org.gradle.kotlin.dsl.closureOf
import org.gradle.language.cpp.CppBinary
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.tasks.AbstractLinkTask
import org.gradle.nativeplatform.tasks.LinkSharedLibrary

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

fun AbstractLinkTask.addCastLibrary(binary: CppBinary) {
  configure(
      closureOf<AbstractLinkTask> {
        project.project(":cast:cast").tasks.named(name, LinkSharedLibrary::class.java) {
          this@addCastLibrary.addRpath(nativeLibraryOutput)
        }
        addJvmLibrary(binary)
      })
}

private fun File.findJvmLibrary(extension: String, subdirs: List<String>) =
    subdirs.map { resolve("$it/libjvm.$extension") }.find { it.exists() }!!

fun AbstractLinkTask.addJvmLibrary(binary: CppBinary) {
  project.dependencies(
      closureOf<DependencyHandler> {
        val currentJavaHome = Jvm.current().javaHome
        val family = binary.targetMachine.operatingSystemFamily

        val (osIncludeSubdir, libJVM) =
            when (family.name) {
              OperatingSystemFamily.LINUX ->
                  "linux" to
                      currentJavaHome.findJvmLibrary(
                          "so", listOf("jre/lib/amd64/server", "lib/amd64/server", "lib/server"))
              OperatingSystemFamily.MACOS ->
                  "darwin" to
                      currentJavaHome.findJvmLibrary(
                          "dylib", listOf("jre/lib/server", "lib/server"))
              OperatingSystemFamily.WINDOWS -> "win32" to currentJavaHome.resolve("lib/jvm.lib")
              else ->
                  throw TaskInstantiationException(
                      "unrecognized operating system family \"$family\"")
            }

        val jniIncludeDir = "$currentJavaHome/include"
        binary.compileTask
            .get()
            .includes(project.files(jniIncludeDir, "$jniIncludeDir/$osIncludeSubdir"))
        add((binary.linkLibraries as Configuration).name, project.files(libJVM))
        addRpath(libJVM)
      })
}

fun AbstractLinkTask.addRpath(library: Provider<File>) {
  if (!targetPlatform.get().operatingSystem.isWindows) {
    linkerArgs.add(project.provider { "-Wl,-rpath,${library.get().parent}" })
  }
}

fun AbstractLinkTask.addRpath(library: File) = addRpath(project.provider { library })

val AbstractLinkTask.nativeLibraryOutput: File
  get() =
      // On all supported platforms, the link task's first two outputs are a directory and a library
      // in that directory. On Windows, the link task also has a third output file: a DLL.
      outputs.files.elementAt(1)
