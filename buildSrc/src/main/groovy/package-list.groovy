import org.gradle.api.tasks.*


////////////////////////////////////////////////////////////////////////
//
//  create a Javadoc-style "package-list" file
//

@CacheableTask
class CreatePackageList extends org.gradle.api.DefaultTask {

	@PathSensitive(PathSensitivity.RELATIVE)
	@Input Object sourceSet

	@OutputFile File packageList = new File("$temporaryDir/package-list")

	@TaskAction
	def create() {
		sourceSet.sourceCollections.collect { collection ->
			def sourceRoot = collection.tree.dir.toPath()
			collection.collect { source ->
				def javaSourceFilePath = source.toPath()
				def parentPath = javaSourceFilePath.parent
				def relativePath = sourceRoot.relativize(parentPath)
				relativePath.toString().replace(File.separator, '.')
			}
		}.flatten().sort().unique().each {
			packageList << "$it\n"
		}
	}
}
