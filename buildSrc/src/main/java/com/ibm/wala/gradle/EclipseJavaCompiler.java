package com.ibm.wala.gradle;

import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.tasks.compile.CompilationFailedException;
import org.gradle.api.internal.tasks.compile.JavaCompileSpec;
import org.gradle.api.internal.tasks.compile.JavaCompilerArgumentsBuilder;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.process.ArgWriter;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.process.ExecResult;
import org.gradle.process.internal.ExecException;

/**
 * Eclipse Compiler for Java (ECJ) encapsulated as a {@link Compiler} for use by Gradle.
 *
 * <p>Inspired by, and derived from, <a
 * href="https://github.com/TwoStone/gradle-eclipse-compiler-plugin">TwoStone's
 * gradle-eclipse-compiler-plugin</a>.
 */
class EclipseJavaCompiler implements Compiler<JavaCompileSpec> {

  private final Project project;
  private final Configuration ecjConfiguration;

  /**
   * Creates a {@link Compiler} that uses ECJ.
   *
   * <p>The given project is used for dependency resolution, in order to find the jar archive
   * containing the ECJ implementation. It is also used to run that ECJ compiler.
   *
   * @param project the project that will use this compiler
   */
  EclipseJavaCompiler(final Project project) {
    this.project = project;
    ecjConfiguration =
        project
            .getConfigurations()
            .detachedConfiguration(project.getDependencies().create("org.eclipse.jdt:ecj:3.21.0"));
  }

  @Override
  public WorkResult execute(JavaCompileSpec javaCompileSpec) {

    final ExecResult result =
        project.javaexec(
            exec -> {
              exec.setWorkingDir(javaCompileSpec.getWorkingDir());
              exec.setClasspath(ecjConfiguration);
              exec.args(
                  ArgWriter.argsFileGenerator(
                          new File(javaCompileSpec.getTempDir(), "java-compiler-args.txt"),
                          ArgWriter.unixStyleFactory())
                      .transform(
                          new JavaCompilerArgumentsBuilder(javaCompileSpec)
                              .includeSourceFiles(true)
                              .build()));
            });

    try {
      result.assertNormalExitValue();
    } catch (ExecException e) {
      throw new CompilationFailedException(e);
    }

    return () -> true;
  }
}
