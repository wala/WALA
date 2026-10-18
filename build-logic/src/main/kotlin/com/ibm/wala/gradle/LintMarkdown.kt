package com.ibm.wala.gradle

import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.assign
import org.gradle.work.DisableCachingByDefault

/**
 * Lints Markdown files with a Markdown linter fetched and run by `npx`.
 *
 * `npx` downloads the linter over the network on first use, so an execution depends on more than
 * this build's declared inputs and outputs. The stamp file records only that the linter ran, not
 * what it found, so a cached result could hide a failure.
 */
@DisableCachingByDefault(because = "runs an `npx`-fetched linter over the network")
abstract class LintMarkdown : NpxTask() {

  /**
   * Markdown files to lint, defaulting to the Markdown files at the top level of the directory of
   * the project that registers this task.
   */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val markdownFiles: ConfigurableFileCollection

  /** Linter configuration file. Defaults to `.markdownlint-cli2.yaml` in the project directory. */
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val configurationFile: RegularFileProperty

  init {
    description = "Lint Markdown files"
    group = "verification"
    command = "markdownlint-cli2@0.18.1"
    configurationFile.convention(project.layout.projectDirectory.file(".markdownlint-cli2.yaml"))
    markdownFiles.from(project.fileTree(project.layout.projectDirectory) { include("*.md") })
    // sorted so that the command line does not vary with file-system traversal order
    args.addAll(markdownFiles.elements.map { files -> files.map { it.asFile.path }.sorted() })
    outputs.file(project.layout.buildDirectory.file("$name.stamp"))
    doLast { outputs.files.singleFile.createNewFile() }
  }
}
