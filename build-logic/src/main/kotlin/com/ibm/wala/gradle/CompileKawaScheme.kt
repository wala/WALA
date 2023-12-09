package com.ibm.wala.gradle

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/** Specialized task for Kawa compilation into jar archive. */
@CacheableTask
abstract class CompileKawaScheme : JavaExec() {

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val schemeFile: RegularFileProperty

  @get:OutputDirectory val outputDir: Provider<Directory> = project.layout.buildDirectory.dir(name)

  init {
    classpath(project.tasks.named("extractKawa"))
    mainClass.set("kawa.repl")

    args("-d", outputDir.get().asFile)

    logging.captureStandardError(LogLevel.INFO)
    args("--main", "-C")
    argumentProviders.add { listOf(schemeFile.get().toString()) }
  }
}
