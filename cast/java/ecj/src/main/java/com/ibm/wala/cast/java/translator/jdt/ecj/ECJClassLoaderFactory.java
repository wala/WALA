package com.ibm.wala.cast.java.translator.jdt.ecj;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.ClassLoaderImpl;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.StringFilter;
import java.io.IOException;

public class ECJClassLoaderFactory extends ClassLoaderFactoryImpl {
  private final SSAOptions ssaOptions;

  public ECJClassLoaderFactory(SSAOptions ssaOptions, StringFilter exclusions) {
    super(exclusions);
    this.ssaOptions = ssaOptions;
  }

  // TODO remove code duplication with JDTClassLoaderFactory

  @Override
  protected IClassLoader makeNewClassLoader(
      ClassLoaderReference classLoaderReference,
      IClassHierarchy cha,
      IClassLoader parent,
      AnalysisScope scope)
      throws IOException {
    if (classLoaderReference.equals(JavaSourceAnalysisScope.SOURCE)) {
      ClassLoaderImpl cl = makeSourceLoader(classLoaderReference, cha, parent);
      cl.init(scope.getModules(classLoaderReference));
      return cl;
    } else {
      return super.makeNewClassLoader(classLoaderReference, cha, parent, scope);
    }
  }

  protected JavaSourceLoaderImpl makeSourceLoader(
      ClassLoaderReference classLoaderReference, IClassHierarchy cha, IClassLoader parent) {
    return new ECJSourceLoaderImpl(classLoaderReference, ssaOptions, parent, cha, false);
  }
}
