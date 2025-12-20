package com.ibm.wala.gradle

import kotlin.io.path.Path
import org.eclipse.jgit.api.Git

/**
 * Validate `.gitignore` patterns and strict source/build separation.
 *
 * All files created during testing should already be ignored, and no Git-tracked file should be
 * modified during testing. In other words, `git status` should still report a clean working
 * directory after testing is done.
 */
@DisableCachingByDefault(because = "Influenced by entire contents of working directory")
abstract class CheckGitCleanlinessTask : DefaultTask() {

  @get:Inject protected abstract val layout: ProjectLayout

  @get:Inject protected abstract val problems: Problems

  private val projectDirectory
    get() = layout.projectDirectory

  init {

    group = "verification"
    description = "Validate `.gitignore` patterns and strict source/build separation"

    // Do nothing if this working directory lacks Git metadata.
    onlyIf { projectDirectory.file(".git").asFile.isDirectory }

    // Run after high-level tasks whose subtasks might modify files in the working directory.
    mustRunAfter("test", "check", "build")
  }

  @TaskAction
  fun check() {
    val status = Git.open(projectDirectory.asFile).use { it.status().call() }
    if (!status.isClean) {
      ids.run {
        sequenceOf(
                addedId to status.added,
                changedId to status.changed,
                removedId to status.removed,
                missingId to status.missing,
                modifiedId to status.modified,
                untrackedId to status.untracked,
                conflictingId to status.conflicting,
            )
            .forEach { (id, files) ->
              if (files.isNotEmpty()) {
                problems.reporter.report(id) {
                  severity(Severity.ERROR)
                  files.sortedBy(::Path).forEach(::fileLocation)
                }
              }
            }
        throw GradleException(dirtyProblemGroup.displayName)
      }
    }
    logger.info("Git working directory is clean")
  }

  companion object {
    private val ids by lazy {
      object {

        val dirtyProblemGroup =
            ProblemGroup.create(
                "com.ibm.wala.gradle.check-git-cleanliness",
                "Git working directory is dirty",
            )

        fun createId(name: String, displayName: String) =
            ProblemId.create(name, displayName, dirtyProblemGroup)

        val addedId = createId("added", "Added to index, but not in HEAD")

        val changedId = createId("changed", "Changed from HEAD to index")

        val removedId = createId("removed", "Removed from HEAD, but in index")

        val missingId = createId("missing", "In index, but not filesystem")

        val modifiedId = createId("modified", "Modified relative to index")

        val untrackedId = createId("untracked", "Not ignored, and not in index")

        val conflictingId = createId("conflicting", "In conflict")
      }
    }
  }
}

if (project == rootProject) {
  tasks.register<CheckGitCleanlinessTask>("checkGitCleanliness")
}
