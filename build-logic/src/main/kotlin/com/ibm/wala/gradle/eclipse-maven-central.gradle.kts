package com.ibm.wala.gradle

import com.diffplug.gradle.eclipse.MavenCentralExtension.ReleaseConfigurer
import com.diffplug.gradle.eclipse.MavenCentralPlugin
import com.diffplug.gradle.pde.EclipseRelease.official

plugins {
  id("com.diffplug.eclipse.mavencentral")
  id("com.ibm.wala.gradle.eclipse-compatible-java")
  id("com.ibm.wala.gradle.java")
}

// This subproject uses one or more Eclipse dependencies. Eensure that it is using an
// Eclipse-compatible Java toolchain. That toolchain might be newer than the Java toolchain that
// WALA uses elsewhere.
java.toolchain.languageVersion = the<EclipseCompatibleJavaExtension>().languageVersion

/**
 * WALA-specialized adaptation of
 * [ReleaseConfigurer](https://javadoc.io/doc/com.diffplug.gradle/goomph/latest/com/diffplug/gradle/eclipse/MavenCentralExtension.ReleaseConfigurer.html).
 *
 * ## WALA-Specific Specializations
 *
 * ### Release Selection
 *
 * The standard
 * [MavenCentralExtension](https://javadoc.io/doc/com.diffplug.gradle/goomph/latest/com/diffplug/gradle/eclipse/MavenCentralExtension.html)
 * offers a few overloaded `release` methods to allow selecting the Eclipse release to configure.
 * However, WALA does not need that flexibility. Instead, this extension always configures the
 * Eclipse release identified by the `eclipse` version in the global `libs.versions.toml` version
 * catalog.
 *
 * ### Default Configuration
 *
 * This extension always applies [ReleaseConfigurer.constrainTransitivesToThisRelease] and
 * [ReleaseConfigurer.useNativesForRunningPlatform] to the selected release.
 *
 * ## Generally Useful Additions
 *
 * ### Variadic Configuration Methods
 *
 * This extension offers variadic analogs of standard unary [ReleaseConfigurer] dependency-declaring
 * methods. For example, [implementation] accepts any number of `bundleId` arguments rather than
 * just one as expected by [ReleaseConfigurer.implementation]
 *
 * ### Test Fixtures Configuration Methods
 *
 * This extension offers variadic methods for configuring dependencies for test fixtures:
 * [testFixturesApi] and [testFixturesImplementation].
 */
open class WalaMavenCentralReleaseConfigurerExtension @Inject constructor(project: Project) {

  /**
   * Internal [ReleaseConfigurer] instance to which all dependency-declaring methods apply.
   *
   * This instance always operates on the Eclipse release identified by the `eclipse` version in the
   * global `libs.versions.toml` version catalog.
   */
  private val configurer =
      project.run {
        eclipseMavenCentral
            .ReleaseConfigurer(
                official(versionCatalogs.named("libs").findVersion("eclipse").get().toString()))
            .apply {
              constrainTransitivesToThisRelease()
              useNativesForRunningPlatform()
            }
      }

  /**
   * Delegates to [configurer] to configure each argument as an `api` dependency.
   *
   * @see ReleaseConfigurer.api
   */
  fun api(vararg bundleIds: String) = bundleIds.forEach(configurer::api)

  /**
   * Delegates to [configurer] to configure each argument as an `implementation` dependency.
   *
   * @see ReleaseConfigurer.implementation
   */
  @Suppress("MemberVisibilityCanBePrivate")
  fun implementation(vararg bundleIds: String) = bundleIds.forEach(configurer::implementation)

  /**
   * Delegates to [configurer] to configure each argument as a `testFixturesApi` dependency.
   *
   * @see ReleaseConfigurer.dep
   */
  @Suppress("MemberVisibilityCanBePrivate")
  fun testFixturesApi(vararg bundleIds: String) =
      bundleIds.forEach { configurer.dep("testFixturesApi", it) }

  /**
   * Delegates to [configurer] to configure each argument as a `testFixturesImplementation`
   * dependency.
   *
   * @see ReleaseConfigurer.dep
   */
  @Suppress("MemberVisibilityCanBePrivate")
  fun testFixturesImplementation(vararg bundleIds: String) =
      bundleIds.forEach { configurer.dep("testFixturesImplementation", it) }

  /**
   * Delegates to [configurer] to configure each argument as a `testImplementation` dependency.
   *
   * @see ReleaseConfigurer.testImplementation
   */
  fun testImplementation(vararg bundleIds: String) =
      bundleIds.forEach(configurer::testImplementation)
}

apply<MavenCentralPlugin>()

extensions.create<WalaMavenCentralReleaseConfigurerExtension>("walaEclipseMavenCentral")
