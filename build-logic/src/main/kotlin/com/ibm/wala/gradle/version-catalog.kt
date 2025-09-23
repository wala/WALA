/**
 * Utilities for working with
 * [the Gradle version catalog](https://docs.gradle.org/current/userguide/version_catalogs.html) in
 * build logic.
 *
 * This file defines [Project] extension methods that resolve library and version aliases from the
 * `libs` version catalog, or fail fast if a requested alias is not found.
 *
 * Motivation: Gradle’s [VersionCatalog] API returns [Optional] values which force repetitive
 * presence checks at call sites. Centralizing the checks here keeps build logic concise and
 * produces consistent, descriptive error messages.
 *
 * Note on future compatibility: if Gradle eventually
 * [makes type-safe version catalog accessors accessible from precompiled script plugins](https://github.com/gradle/gradle/issues/15383),
 * then these helper functions will no longer be necessary and can be removed in favor of the
 * official API.
 */
package com.ibm.wala.gradle

import java.util.Optional
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.the

private fun <T> Project.requireAlias(
    finder: VersionCatalog.(String) -> Optional<T>,
    alias: String,
    kind: String,
) =
    the<VersionCatalogsExtension>().named("libs").finder(alias).orElseThrow {
      NoSuchElementException("No $kind with alias `$alias` found in `libs` version catalog")
    }!!

/**
 * Looks up a library dependency by alias from the `libs` version catalog.
 *
 * @param alias the library alias as defined in the version catalog
 * @return the resolved library dependency provider associated with [alias]
 * @throws NoSuchElementException if no library with the given [alias] exists in the catalog
 */
fun Project.catalogLibrary(alias: String) =
    requireAlias(VersionCatalog::findLibrary, alias, "library")

/**
 * Looks up a version by alias from the `libs` version catalog.
 *
 * @param alias the version alias as defined in the version catalog
 * @return a string representation of the resolved version constraint associated with [alias]
 * @throws NoSuchElementException if no version with the given [alias] exists in the catalog
 */
fun Project.catalogVersion(alias: String) =
    requireAlias(VersionCatalog::findVersion, alias, "version").toString()
