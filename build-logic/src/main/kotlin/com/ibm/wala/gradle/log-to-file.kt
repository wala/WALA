package com.ibm.wala.gradle

import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.process.BaseExecSpec

/**
 * Extension function to redirect standard output and error of an execution task (such as [Exec] or
 * [JavaExec]) to a file under `build/`.
 *
 * @param baseName Base name (without extension) for the log file. The actual file will be created
 *   as `build/<baseName>.log`.
 * @return a [provider][Provider] for the log file, which is also registered as a
 *   [task output][Task.outputs]. This can be used for wiring task dependencies or additional
 *   configuration if needed.
 */
fun <T> T.logToFile(baseName: String) where T : Task, T : BaseExecSpec =
    project.layout.buildDirectory.file("$baseName.log").also { logFile ->
      outputs.file(logFile)

      doFirst {
        logFile.get().asFile.outputStream().let {
          standardOutput = it
          errorOutput = it
        }
      }

      // Try to close the `FileOutputStream` promptly when the task is finished. If the task fails,
      // then `doLast` will not run, so the stream will remain open until the garbage collector
      // reclaims it. See also
      // <https://discuss.gradle.org/t/close-fileoutputstream-after-javaexec-task/51592> for
      // disucssion of alternative approaches and
      // <https://github.com/gradle/gradle/issues/35066> for a Gradle feature request that would
      // make a robust implementation much easier.
      doLast { standardOutput.close() }
    }
