package com.ibm.wala.gradle

import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.AbstractCopyTask

/**
 * Drops the top-level directory from the destination path of every file copied by this task.
 *
 * Effectively, this “unwraps” a directory layout by removing the first path segment from each
 * file's [relative path][RelativePath]. For example:
 * - `foo/bar.txt` ↦ `bar.txt`
 * - `foo/baz/qux.txt` ↦ `baz/qux.txt`
 *
 * In addition, empty directories are excluded by setting [AbstractCopyTask.includeEmptyDirs] to
 * false to avoid creating orphan directories after paths are rewritten.
 */
fun AbstractCopyTask.dropTopDirectory() {
  eachFile {
    relativePath = RelativePath(!isDirectory, *relativePath.segments.drop(1).toTypedArray())
  }
  includeEmptyDirs = false
}
