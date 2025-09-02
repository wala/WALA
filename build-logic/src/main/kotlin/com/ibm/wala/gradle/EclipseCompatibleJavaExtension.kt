package com.ibm.wala.gradle

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.the

/**
 * The earliest Java version that is compatible with bytecode used in Eclipse dependencies.
 *
 * When changing `eclipse` in the `versions` section of `gradle/libs.versions.toml`, consider
 * whether this value needs to be changed as well.
 */
private const val MINIMUM_ECLIPSE_COMPATIBLE_JAVA_VERSION = 17

/**
 * A Gradle [Project] extension providing details about Eclipse-compatible Java toolchains.
 *
 * @constructor Creates an extension instance that will be attached to the given [project].
 * @property project The project to which this extension instance is attached.
 * @property languageVersion A Java language version that is compatible with WALA's Eclipse
 *   dependencies.
 * @property launcher Provides a Java JVM launcher (i.e., `java` command) that is compatible with
 *   WALA's Eclipse dependencies.
 */
open class EclipseCompatibleJavaExtension @Inject constructor(private val project: Project) {

  val languageVersion: JavaLanguageVersion by lazy {
    val projectVersion = project.the<JavaPluginExtension>().toolchain.languageVersion.get()
    val minimumVersion = JavaLanguageVersion.of(MINIMUM_ECLIPSE_COMPATIBLE_JAVA_VERSION)
    if (projectVersion.canCompileOrRun(minimumVersion)) projectVersion else minimumVersion
  }

  val launcher: Provider<JavaLauncher> by lazy {
    project.the<JavaToolchainService>().launcherFor {
      languageVersion.set(this@EclipseCompatibleJavaExtension.languageVersion)
    }
  }
}
