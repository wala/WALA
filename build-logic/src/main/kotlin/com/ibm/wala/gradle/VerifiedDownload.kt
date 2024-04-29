package com.ibm.wala.gradle

import de.undercouch.gradle.tasks.download.DownloadExtension
import de.undercouch.gradle.tasks.download.VerifyExtension
import java.net.URI
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

/** Download and use checksum to verify that we got what we expected. */
@CacheableTask
abstract class VerifiedDownload : DefaultTask() {

  /** URI of resource to download. */
  @get:Input abstract val src: Property<URI>

  /** Expected checksum of resource as hex digits. */
  @get:Input abstract val checksum: Property<String>

  /** Algorithm to use for computing checksum. */
  @get:Input val algorithm: Property<String> = project.objects.property<String>().convention("MD5")

  /** Whether to use ETag for selective downloading. */
  @get:Input val useETag: Property<Boolean> = project.objects.property<Boolean>().convention(true)

  /** Local file into which resource should be saved. */
  @get:OutputFile abstract val dest: RegularFileProperty

  /** Plugin-provided extension for downloading a resource from some URL. */
  @Internal
  @Suppress("LeakingThis")
  val downloadExtension: DownloadExtension =
      project.objects.newInstance(DownloadExtension::class.java, this)

  /** Plugin-provided extension for verifying that a file has the expected checksum. */
  @Internal
  @Suppress("LeakingThis")
  val verifyExtension: VerifyExtension =
      project.objects.newInstance(VerifyExtension::class.java, this)

  @TaskAction
  fun downloadAndVerify() {
    downloadExtension.run {
      src(this@VerifiedDownload.src)
      dest(this@VerifiedDownload.dest)
      overwrite(true)
      onlyIfModified(true)
      useETag(this@VerifiedDownload.useETag.get())
      retries(5)
    }
    verifyExtension.run {
      src(this@VerifiedDownload.dest)
      algorithm(this@VerifiedDownload.algorithm.get())
      checksum(this@VerifiedDownload.checksum.get())
    }
  }
}
