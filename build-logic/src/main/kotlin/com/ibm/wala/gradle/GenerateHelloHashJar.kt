package com.ibm.wala.gradle

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.assign
import org.gradle.process.ExecOperations

/**
 * Compiles an OCaml source file to a JAR of Java bytecode with OCaml-Java.
 *
 * The compiler's result is fully determined by [ocamlSource] and the OCaml-Java distribution, so it
 * is safely cached. This task knows the layout of an unpacked OCaml-Java distribution and of the
 * standard library extracted from it; a build script need only say where those trees are.
 *
 * The work is one [TaskAction] rather than a [org.gradle.api.tasks.JavaExec] plus `doFirst` and
 * `doLast` actions, because an action added to a `JavaExec` task from a constructor is silently
 * dropped, leaving the copy of [ocamlSource] that the compiler is handed never to happen.
 */
@CacheableTask
abstract class GenerateHelloHashJar : DefaultTask() {

  @get:Inject abstract val archive: ArchiveOperations

  @get:Inject abstract val execOperations: ExecOperations

  /** OCaml source file to compile to Java bytecode. */
  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  val ocamlSource = project.layout.projectDirectory.file("ocaml/hello_hash.ml")

  /**
   * Root of the unpacked OCaml-Java distribution, conventionally the working directory of the `tar`
   * extraction that unpacks it.
   */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val ocamlJavaHome: DirectoryProperty

  /**
   * Root of the tree that OCaml-Java's standard library was extracted into, conventionally the
   * destination directory of the `Sync` task that extracts it from `ocaml-lib-files.jar`.
   */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val ocamlLibFilesRoot: DirectoryProperty

  /** Version of the OCaml-Java distribution, which names directories inside both trees. */
  @get:Input abstract val ocamlJavaVersion: Property<String>

  /**
   * Generated JAR archive that will contain the compiled OCaml code, `hello_hash.jar` in this
   * task's build directory.
   */
  @get:OutputFile val jar = project.layout.buildDirectory.file("hello_hash.jar").get()

  init {
    description = "Compile OCaml `hello_hash` to Java bytecode JAR"
  }

  @TaskAction
  fun compile() {
    val versioned = "ocamljava-${ocamlJavaVersion.get()}"

    // `ocamljava` JAR to compile OCaml to Java bytecode; its manifest declares a `Class-Path` entry
    // for `ocaml-lib-files.jar`, which the JVM resolves relative to the JAR itself
    val ocamlJavaJar = ocamlJavaHome.get().dir("$versioned/lib/ocamljava.jar").asFile

    // the compiler hardcodes the standard library's location, which does not exist on any machine
    // we
    // build on, so point `OCAMLLIB` at the tree we extracted it into
    val ocamlLibFiles = ocamlLibFilesRoot.get().dir("$versioned/lib/ocaml").asFile

    // the compiler writes `hello_hash.{cmi,cmj,jo}` next to its input, so compile a copy in this
    // task's temporary directory rather than polluting the source directory
    val source = temporaryDir.resolve(ocamlSource.asFile.name)
    ocamlSource.asFile.copyTo(source, overwrite = true)

    execOperations.javaexec {
      classpath(ocamlJavaJar)
      mainClass = "ocaml.compilers.ocamljavaMain"

      // the compiler requires a deep stack
      jvmArgs("-Xss8M")

      args("-o", jar.asFile.path, source.path)
      environment("OCAMLLIB", ocamlLibFiles.path)
    }

    // validate the generated JAR: `ocamljava` always "succeeds", even if something went wrong
    check(!archive.zipTree(jar).isEmpty)
  }
}
