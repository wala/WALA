package com.ibm.wala.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Installs an Android SDK platform with the `sdkmanager` command-line tool.
 *
 * `sdkmanager` needs a shell to feed it a license confirmation on the platforms it installs, so
 * this task drives it through one: `PowerShell` on Windows, `sh` elsewhere. The command line is
 * itself an input, because `Exec` declares its executable and arguments as such, and it names this
 * task's [platformsVersion] and [sdkRoot]. A build script need only say where the command-line
 * tools came from and which platform to install.
 */
@CacheableTask
abstract class InstallAndroidSdk : Exec() {

  /** Root of the unpacked Android SDK command-line tools. */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val sdkCommandLineTools: DirectoryProperty

  /** Version of the Android platform to install. */
  @get:Input abstract val platformsVersion: Property<String>

  /** Directory into which the platform is installed: this task's build directory. */
  @get:OutputDirectory val sdkRoot = project.layout.buildDirectory.dir(name)

  init {
    description = "Install Android SDK platform"

    // Running the Android SDK manager requires that `$JAVA_HOME` be set.
    useCurrentJavaHome()

    val sdkManager = sdkCommandLineTools.file("cmdline-tools/bin/sdkmanager").valueToString
    val destination = sdkRoot.valueToString
    val platform = platformsVersion.valueToString

    // `sdkmanager` needs a shell to feed it a license confirmation on the platforms it installs
    data class Details(
        val shell: String,
        val shellFlags: String,
        val confirm: String,
        val separator: String,
        val discard: String,
    )
    val details =
        if (System.getProperty("os.name").startsWith("Windows "))
            Details("PowerShell", "-Command", "echo y", "`;", "\$null")
        else Details("sh", "-ceu", "yes 2>/dev/null", "\\;", "/dev/null")
    executable = details.shell
    argumentProviders.add {
      listOf(
          details.shellFlags,
          "${details.confirm} | $sdkManager --sdk_root=$destination platforms${details.separator}$platform >${details.discard}",
      )
    }
  }
}
