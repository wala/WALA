package com.ibm.wala.gradle;

import org.gradle.api.Project;
import org.gradle.api.internal.tasks.compile.JavaCompileSpec;
import org.gradle.api.internal.tasks.compile.JavaCompilerFactory;
import org.gradle.api.internal.tasks.compile.NormalizingJavaCompiler;
import org.gradle.language.base.internal.compile.CompileSpec;
import org.gradle.language.base.internal.compile.Compiler;

/** Factory for creating {@link EclipseJavaCompiler} instances. */
class EclipseJavaCompilerFactory implements JavaCompilerFactory {

  private final Project project;

  /**
   * Creates an ECJ compiler factory for the given project.
   *
   * <p>The given project is used for dependency resolution, in order to find the jar archive
   * containing the ECJ implementation. It is also used to run that ECJ compiler.
   *
   * @param project the project that will use compilers that this factory creates
   */
  EclipseJavaCompilerFactory(Project project) {
    this.project = project;
  }

  @Override
  public Compiler<JavaCompileSpec> create(Class<? extends CompileSpec> type) {
    return new NormalizingJavaCompiler(new EclipseJavaCompiler(project));
  }
}
