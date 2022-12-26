package com.ibm.wala.gradle

import de.undercouch.gradle.tasks.download.DownloadExtension
import de.undercouch.gradle.tasks.download.VerifyExtension
import java.net.URL
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the

////////////////////////////////////////////////////////////////////////
//
//  download and use checksum to verify that we got what we expected
//

@CacheableTask
abstract class VerifiedDownload : DefaultTask() {

  // URL of resource to download
  @get:Input abstract val src: Property<URL>

  fun src(url: String) = src.set(URL(url))

  // expected checksum of resource as hex digits
  @get:Input abstract val checksum: Property<String>

  fun checksum(hex: String) = checksum.set(hex)

  // algorithm to use for computing checksum
  @get:Input val algorithm: Property<String> = project.objects.property<String>().convention("MD5")

  fun algorithm(algorithmName: String) = algorithm.set(algorithmName)

  // whether to use ETag for selective downloading
  @get:Input val useETag: Property<Boolean> = project.objects.property<Boolean>().convention(true)

  fun useETag(enabled: Boolean) = useETag.set(enabled)

  // local file into which resource should be saved
  @get:OutputFile abstract val dest: RegularFileProperty

  fun dest(file: Any): RegularFileProperty = dest.value { project.file(file) }

  @TaskAction
  fun downloadAndVerify() =
      project.run {
        the<DownloadExtension>().run {
          src(this@VerifiedDownload.src)
          dest(this@VerifiedDownload.dest)
          overwrite(true)
          onlyIfModified(true)
          useETag(this@VerifiedDownload.useETag.get())
          retries(5)
        }
        the<VerifyExtension>().run {
          src(this@VerifiedDownload.dest.get().asFile)
          algorithm(this@VerifiedDownload.algorithm.get())
          checksum(this@VerifiedDownload.checksum.get())
        }
      }
}
