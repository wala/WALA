import com.ibm.wala.gradle.EclipseJavaCompilerToolChain
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile

/**
 * Compiles some Java {@link SourceSet} using ECJ, but otherwise imitating the standard {@link JavaCompile} task.
 */
@CacheableTask
class JavaCompileUsingEcj extends JavaCompile {

	JavaCompileUsingEcj() {
		// Use ECJ tool chain rather than the standard Java compilation tool chain.
		if (!project.hasProperty('eclipseJavaCompilerToolChain')) {
			project.ext.eclipseJavaCompilerToolChain = new EclipseJavaCompilerToolChain(project)
		}
		assert project.hasProperty('eclipseJavaCompilerToolChain')
		toolChain = project.eclipseJavaCompilerToolChain

		// Add Eclipse JDT configuration, especially for warnings/errors.
		options.compilerArgs << '-properties' << project.layout.projectDirectory.file('.settings/org.eclipse.jdt.core.prefs').asFile

		// ECJ doesn't support the "-h" flag for setting the JNI header output directory.
		options.headerOutputDirectory.set null

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
