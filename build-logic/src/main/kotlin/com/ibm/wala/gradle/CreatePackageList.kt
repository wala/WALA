package com.ibm.wala.gradle

import java.io.File
import java.nio.file.Path
import java.util.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction

/** Create a Javadoc-style `package-list` file. */
@CacheableTask
open class CreatePackageList : DefaultTask() {

  @get:OutputDirectory
  val packageListDirectory: DirectoryProperty =
      project.objects.directoryProperty().convention(project.layout.buildDirectory.dir(name))

  private var sourceFileSubdirectories: SortedSet<Path>? = null

  /** Serializable representation of subdirs suitable for cache indexing. */
  @Input fun getSourceFileSubdirectories() = sourceFileSubdirectories!!.map { it.toString() }

  fun sourceSet(sourceSet: SourceSet) {
    // gather source subdirs relative to each source root
    sourceFileSubdirectories =
        sourceSet.java.srcDirTrees
            .asSequence()
            .flatMap { sourceDirectoryTree ->
              val sourceRoot = sourceDirectoryTree.dir.toPath()
              project.files(sourceDirectoryTree).map { source ->
                val javaSourceFilePath = source.toPath()
                val parentPath = javaSourceFilePath.parent
                sourceRoot.relativize(parentPath)
              }
            }
            .toSortedSet()
  }

  @TaskAction
  fun create() =
      // relative subdirs as dot-delimited qualified Java package names, one per line
      packageListDirectory.get().file("package-list").asFile.printWriter().use { out ->
        getSourceFileSubdirectories().forEach { out.println(it.replace(File.separator, ".")) }
      }
}
