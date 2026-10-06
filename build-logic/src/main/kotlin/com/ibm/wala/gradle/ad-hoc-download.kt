package com.ibm.wala.gradle

import java.io.File
import java.io.OutputStream.nullOutputStream
import java.net.URI
import java.security.DigestOutputStream
import java.security.MessageDigest.getInstance
import kotlin.io.inputStream
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

/**
 * Creates a provider for downloading an artifact from a specified URI.
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
 * @param sha256 Expected SHA-256 hex digest of the downloaded file. When non-null the digest is
 *   verified on first use and a mismatch fails the build. Required when `uri` uses cleartext HTTP;
 *   optional over HTTPS.
 * @return A provider that yields the single downloaded file
 */
@Suppress("KDocUnresolvedReference")
fun Project.adHocDownload(
    uri: URI,
    name: String,
    ext: String,
    version: String? = null,
    classifier: String? = null,
    sha256: String? = null,
): Provider<RegularFile> {

  val isInsecureProtocol = uri.scheme == "http"
  if (isInsecureProtocol) {
    require(sha256 != null) {
      "adHocDownload($uri): a cleartext download requires a pinned SHA-256 checksum as a tamper guard."
    }
  }

  repositories.exclusiveContent {
    forRepository {
      repositories.ivy {
        isAllowInsecureProtocol = isInsecureProtocol
        url = uri
        patternLayout { artifact("/[artifact](-[revision])(-[classifier])(.[ext])") }
        metadataSources { artifact() }
      }
    }
    filter { includeVersion(uri.authority, name, version ?: "") }
  }

  return layout.projectDirectory
      .file(
          provider {
            configurations
                .detachedConfiguration(
                    this.dependencies.create(
                        "${uri.authority}:$name${version.segment}${classifier.segment}@$ext"
                    )
                )
                .singleFile
                .absolutePath
          }
      )
      .map { file ->
        sha256?.let { verifySha256(file.asFile, it, "$uri/$name") }
        file
      }
}

private fun verifySha256(file: File, expectedHex: String, label: String) {
  val digest = getInstance("SHA-256")
  file.inputStream().use { fileInputStream ->
    DigestOutputStream(nullOutputStream(), digest).use { digestOutputStream ->
      fileInputStream.copyTo(digestOutputStream)
    }
  }
  val actualHex = digest.digest().joinToString("") { "%02x".format(it) }
  require(actualHex.equals(expectedHex, ignoreCase = true)) {
    "Checksum mismatch for $label ($file): expected SHA-256 $expectedHex but was $actualHex. Refusing to use a corrupted or untrustworthy build input."
  }
}

private val String?.segment
  get() = this?.let { ":$it" } ?: ""
