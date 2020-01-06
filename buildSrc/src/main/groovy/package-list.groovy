import org.gradle.api.tasks.*

import java.nio.file.Path

////////////////////////////////////////////////////////////////////////
//
//  create a Javadoc-style "package-list" file
//

@CacheableTask
class CreatePackageList extends org.gradle.api.DefaultTask {

	@OutputFile
	final File packageList = new File("$temporaryDir/package-list")

	private SortedSet<Path> sourceFileSubdirectories

	@Input final getSourceFileSubdirectories() {
		// serializable representation of subdirs suitable for cache indexing
		return sourceFileSubdirectories*.toString()
	}

	@SuppressWarnings("unused")
	final sourceSet(final SourceSet sourceSet) {
		// gather source subdirs relative to each source root
		sourceFileSubdirectories = new TreeSet<>(
				sourceSet.java.sourceCollections.collect { collection ->
					final sourceRoot = collection.tree.dir.toPath()
					collection.collect { source ->
						final javaSourceFilePath = source.toPath()
						final parentPath = javaSourceFilePath.parent
						sourceRoot.relativize(parentPath)
					}
				}.flatten()
		)
	}

	@TaskAction
	final def create() {
		// relative subbdirs as dot-delimited qualified Java package names, one per line
		packageList.text = getSourceFileSubdirectories()
				*.replace(File.separator, '.').join('\n') + '\n'
	}
}
