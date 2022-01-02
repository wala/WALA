import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

/**
 * Compiles some Java {@link SourceSet} using ECJ, but otherwise imitating the standard {@link JavaCompile} task.
 */
@CacheableTask
class JavaCompileUsingEcj extends JavaCompile {

	JavaCompileUsingEcj() {
		// Resolve ECJ to a JAR archive.  This task will use that archive as a batch Java compiler.
		final ecjConfiguration =
				project.configurations.detachedConfiguration(
						project.dependencies.create('org.eclipse.jdt:ecj:3.21.0'))

		options.with {
			// Add Eclipse JDT configuration, especially for warnings/errors.
			compilerArgs << '-properties' << project.layout.projectDirectory.file('.settings/org.eclipse.jdt.core.prefs').asFile

			// Compile by running an external process.  Specifically, use the standard "java" command from
			// the Java 1.8 toolchain to run the ECJ JAR archive.  Conveniently, that archive is set up to
			// act as a batch compiler when run as a application.
			fork = true
			forkOptions.with {
				executable = project.javaToolchains.launcherFor {
					languageVersion.set(JavaLanguageVersion.of(8))
				}.get().executablePath.toString()
				jvmArgs << '-jar' << ecjConfiguration.singleFile.absolutePath
			}

			// ECJ doesn't support the "-h" flag for setting the JNI header output directory.
			headerOutputDirectory.set(project.provider { null })
		}

		// Allow skipping all ECJ compilation tasks by setting a project property.
		onlyIf { !project.hasProperty('skipJavaUsingEcjTasks') }
	}

	final void setSourceSet(final SourceSet sourceSet) {
		// Imitate most of the behavior of the standard compilation task for the given sourceSet.
		final standardCompileTaskName = sourceSet.getCompileTaskName('java')
		final standardCompileTask = project.tasks.named(standardCompileTaskName, JavaCompile).get()
		classpath = standardCompileTask.classpath
		source = standardCompileTask.source

		// However, put generated class files in a different build directory to avoid conflict.
		final destinationSubdir = "ecjClasses/${sourceSet.java.name}/${sourceSet.name}"
		destinationDirectory.set project.layout.buildDirectory.dir(destinationSubdir)
	}

	final static Provider<JavaCompileUsingEcj> withSourceSet(final Project project, final SourceSet sourceSet) {
		return project.tasks.register(sourceSet.getCompileTaskName('javaUsingEcj'), JavaCompileUsingEcj) { it ->
			it.sourceSet = sourceSet
		}
	}
}
