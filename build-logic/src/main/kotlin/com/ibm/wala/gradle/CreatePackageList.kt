package com.ibm.wala.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Create a Javadoc-style `package-list` file. */
@CacheableTask
abstract class CreatePackageList : DefaultTask() {

  /**
   * The directory containing the `package-list` file to write. Defaults to this task's build
   * directory.
   */
  @get:OutputDirectory abstract val packageListDirectory: DirectoryProperty

  /**
   * Java source roots to scan for packages. Wired as a lazy file collection, so nothing is resolved
   * at configuration time.
   */
  @get:IgnoreEmptyDirectories
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sourceRoots: ConfigurableFileCollection

  init {
    packageListDirectory.convention(project.layout.buildDirectory.dir(name))
  }

  /**
   * Generates a Javadoc-style `package-list` file containing the fully qualified names of all
   * packages found within the configured source roots. Scans each root directory for Java source
   * files, extracts their parent directory paths as dot-separated package names, deduplicates and
   * sorts them alphabetically, and writes the result to the output `package-list` file.
   */
  @TaskAction
  fun create() {
    packageListDirectory.get().file("package-list").asFile.printWriter().use { out ->
      sourceRoots.files
          .asSequence()
          .flatMap { root ->
            root
                .walkTopDown()
                .filter { it.isFile && it.extension == "java" }
                .map { it.parentFile.relativeTo(root).invariantSeparatorsPath }
                .filter { it.isNotEmpty() }
          }
          .map { it.replace('/', '.') }
          .distinct()
          .sorted()
          .forEach(out::println)
    }
  }
}
