import org.gradle.api.tasks.*


////////////////////////////////////////////////////////////////////////
//
//  download and use checksum to verify that we got what we expected
//

@CacheableTask
class VerifiedDownload extends org.gradle.api.DefaultTask {

	// URL of resource to download
	def @Input src

	// expected checksum of resource as hex digits
	def @Input checksum

	// algorithm to use for computing checksum
	def @Input algorithm = 'MD5'

	// whether to use ETag for selective downloading
	def @Input useETag = true

	// local file into which resource should be saved
	def @OutputFile dest

	File getDest() {
		return project.file(dest)
	}

	@TaskAction
	downloadAndVerify() {
		def destFile = getDest()
		project.download.run {
			src this.src
			dest destFile
			overwrite true
			onlyIfModified true
			useETag this.useETag
			retries 5
		}
		project.verifyChecksum.run {
			src destFile
			algorithm this.algorithm
			checksum this.checksum
		}
	}
}
