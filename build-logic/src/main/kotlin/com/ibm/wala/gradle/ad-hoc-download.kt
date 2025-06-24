package com.ibm.wala.gradle

import java.net.URI
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.create

/**
 * Creates a configuration for downloading an artifact from a specified URI.
 *
 * This function sets up an Ivy repository with a specific pattern layout to download an artifact
 * from the given URI. The final download URL is constructed as follows:
 * `{uri}/{name}`(`-{version}`)(`-{classifier}`)`.{ext}`. Parenthetic components are omitted if the
 * corresponding argument is omitted or `null`.
 *
 * For example, with `uri="https://example.com/downloads"`, `name="tool"`, `version="1.0"`,
 * `classifier="linux"`, `ext="jar"`, the resulting download URL would be
 * `https://example.com/downloads/tool-1.0-linux.jar`.
 *
 * @param uri The base URI where the artifact is located
 * @param name The name of the artifact (becomes `[artifact]` in
 *   [the Ivy URL pattern](https://ant.apache.org/ivy/history/master/concept.html#patterns))
 * @param ext The file extension of the artifact (becomes `[ext]` in
 *   [the Ivy URL pattern](https://ant.apache.org/ivy/history/master/concept.html#patterns))
 * @param version Optional version of the artifact (becomes `[revision]` in
 *   [the Ivy URL pattern](https://ant.apache.org/ivy/history/master/concept.html#patterns) if
 *   provided)
 * @param classifier Optional classifier for the artifact (becomes `[classifier]` in
 *   [the Ivy URL pattern](https://ant.apache.org/ivy/history/master/concept.html#patterns) if
 *   provided)
 * @return A detached configuration that can be used to resolve and download the artifact
 */
@Suppress("KDocUnresolvedReference")
fun Project.adHocDownload(
    uri: URI,
    name: String,
    ext: String,
    version: String? = null,
    classifier: String? = null,
): Configuration {

  repositories.exclusiveContent {
    forRepository {
      repositories.ivy {
        isAllowInsecureProtocol = true
        url = uri
        patternLayout { artifact("/[artifact](-[revision])(-[classifier])(.[ext])") }
        metadataSources { artifact() }
      }
    }
    filter { includeVersion(uri.authority, name, version ?: "") }
  }

  return configurations.detachedConfiguration(
      dependencies.create(
          group = uri.authority,
          name = name,
          version = version,
          classifier = classifier,
          ext = ext))
}
