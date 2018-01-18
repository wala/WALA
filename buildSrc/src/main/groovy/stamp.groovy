////////////////////////////////////////////////////////////////////////
//
//  helper task for checksum-verified downloads
//

class VerifyWithStamp extends de.undercouch.gradle.tasks.download.Verify {
	VerifyWithStamp() {
		def stamp = new File(temporaryDir, 'stamp')
		outputs.file stamp
		doLast { stamp.text = '' }
	}
}
