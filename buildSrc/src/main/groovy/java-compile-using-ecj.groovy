import de.set.gradle.ecj.EclipseCompilerBasePlugin
import de.set.gradle.ecj.EclipseCompilerExtension
import de.set.gradle.ecj.EclipseCompilerToolChain
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile

import javax.inject.Inject

/**
 * Compiles some Java {@link SourceSet} using ECJ in addition to the standard compilation task.
 */
@CacheableTask
class JavaCompileUsingEcj extends JavaCompile {

	@Inject
	JavaCompileUsingEcj() {
		// Use ECJ tool chain rather than the standard Java compilation tool chain.
		if (!project.hasProperty('eclipseCompilerToolChain')) {
			final extension =
					project.extensions.create(EclipseCompilerBasePlugin.ECJ_EXTENSION, EclipseCompilerExtension)
			extension.tap {
				toolGroupId = 'org.eclipse.jdt'
				toolArtifactId = 'ecj'
				toolVersion = '3.21.0'
			}
			project.configurations {
				ecj
			}
			project.dependencies {
				ecj "$extension.toolGroupId:$extension.toolArtifactId:$extension.toolVersion"
			}

			project.ext.eclipseCompilerToolChain = EclipseCompilerToolChain.create project
		}
		toolChain = project.eclipseCompilerToolChain

		// Add Eclipse JDT configuration, especially for warnings/errors.
		options.compilerArgs << '-properties' << project.layout.projectDirectory.file('.settings/org.eclipse.jdt.core.prefs').asFile

		// ECJ doesn't support the "-h" flag for setting the JNI header output directory.
		options.headerOutputDirectory.set null

		// Allow skipping all ECJ compilation tasks by setting a project property.
		onlyIf { !project.hasProperty('skipJavaUsingEcjTasks') }
	}

	final void setSourceSet(SourceSet sourceSet) {
		// Imitate most of the behavior of the standard compilation task for the given sourceSet.
		final standardCompileTaskName = sourceSet.getCompileTaskName('java')
		final standardCompileTask = project.tasks.named(standardCompileTaskName, JavaCompile).get()
		classpath = standardCompileTask.classpath
		source = standardCompileTask.source

		// However, put generated class files in a different build directory to avoid conflict.
		final destinationSubdir = "ecjClasses/${sourceSet.java.name}/${sourceSet.name}"
		destinationDirectory.set project.layout.buildDirectory.dir(destinationSubdir)
	}

	final static Provider<JavaCompileUsingEcj> withSourceSet(Project project, SourceSet sourceSet) {
		final ecjCompileTaskName = sourceSet.getCompileTaskName('javaUsingEcj')
		final ecjCompileTaskProvider = project.tasks.register(ecjCompileTaskName, JavaCompileUsingEcj) { it ->
			it.sourceSet = sourceSet
		}
		return ecjCompileTaskProvider
	}
}
