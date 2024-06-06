package com.ibm.wala.gradle.cast

import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.TaskInstantiationException
import org.gradle.internal.jvm.Jvm
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.extra
import org.gradle.language.cpp.CppBinary
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.tasks.AbstractLinkTask
import org.gradle.nativeplatform.tasks.LinkSharedLibrary

////////////////////////////////////////////////////////////////////////
//
//  helpers for building native CAst components
//

fun addCastLibrary(binary: CppBinary, linkTask: AbstractLinkTask, project: Project) {
  linkTask.configure(
      project.closureOf<AbstractLinkTask> {
        project.project(":cast:cast").tasks.named(name, LinkSharedLibrary::class.java) {
          addRpath(linkTask, nativeLibraryOutput)
        }
        addJvmLibrary(binary, linkTask, project)
      })
}

fun findJvmLibrary(
    project: Project,
    extension: String,
    currentJavaHome: File,
    subdirs: List<String>
) = subdirs.map { project.file("$currentJavaHome/$it/libjvm.$extension") }.find { it.exists() }!!

fun addJvmLibrary(binary: CppBinary, linkTask: AbstractLinkTask, project: Project) =
    project.dependencies(
        project.closureOf<DependencyHandler> {
          val currentJavaHome = Jvm.current().javaHome
          val family = binary.targetMachine.operatingSystemFamily

          data class Details(
              val osIncludeSubdir: String,
              val libJVM: File,
          )
          when (family.name) {
            OperatingSystemFamily.LINUX ->
                Details(
                    "linux",
                    findJvmLibrary(
                        project,
                        "so",
                        currentJavaHome,
                        listOf(
                            "jre/lib/amd64/server",
                            "lib/amd64/server",
                            "lib/server",
                        )))
            OperatingSystemFamily.MACOS ->
                Details(
                    "darwin",
                    findJvmLibrary(
                        project,
                        "dylib",
                        currentJavaHome,
                        listOf(
                            "jre/lib/server",
                            "lib/server",
                        )))
            OperatingSystemFamily.WINDOWS ->
                Details("win32", project.file("$currentJavaHome/lib/jvm.lib"))
            else ->
                throw TaskInstantiationException("unrecognized operating system family \"$family\"")
          }.run {
            val jniIncludeDir = "$currentJavaHome/include"
            binary.compileTask
                .get()
                .includes(project.files(jniIncludeDir, "$jniIncludeDir/$osIncludeSubdir"))
            add((binary.linkLibraries as Configuration).name, project.files(libJVM))
            addRpath(linkTask, libJVM)
          }
        })

fun addRpath(linkTask: AbstractLinkTask, library: File) {
  if (!(linkTask.project.rootProject.extra["isWindows"] as Boolean)) {
    linkTask.linkerArgs.add("-Wl,-rpath,${library.parent}")
  }
}

val AbstractLinkTask.nativeLibraryOutput: File
  get() =
      // On all supported platforms, the link task's first two outputs are a directory and a library
      // in that directory. On Windows, the link task also has a third output file: a DLL.
      outputs.files.asSequence().elementAt(1)
