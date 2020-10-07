package com.ibm.wala.gradle;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.internal.tasks.AbstractJavaToolChain;

/**
 * {@link org.gradle.jvm.toolchain.JavaToolChain} that uses the Eclipse Compiler for Java for
 * compilation.
 *
 * <p>Inspired by, and derived from, <a
 * href="https://github.com/TwoStone/gradle-eclipse-compiler-plugin">TwoStone's
 * gradle-eclipse-compiler-plugin</a>.
 */
public class EclipseJavaCompilerToolChain extends AbstractJavaToolChain {

  /** Creates an ECJ tool chain for the given project. */
  public EclipseJavaCompilerToolChain(Project project) {
    super(new EclipseJavaCompilerFactory(project), null);
  }

  @Override
  public JavaVersion getJavaVersion() {
    return JavaVersion.current();
  }

  @Override
  public String getName() {
    return "ecj";
  }
}
