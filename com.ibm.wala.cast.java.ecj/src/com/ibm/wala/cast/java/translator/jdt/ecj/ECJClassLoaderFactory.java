package com.ibm.wala.cast.java.translator.jdt.ecj;

import java.io.IOException;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.ClassLoaderImpl;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.SetOfClasses;

public class ECJClassLoaderFactory extends ClassLoaderFactoryImpl {

  public ECJClassLoaderFactory(SetOfClasses exclusions) {
    super(exclusions);
  }

  // TODO remove code duplication with JDTClassLoaderFactory
  
  @Override
  protected IClassLoader makeNewClassLoader(ClassLoaderReference classLoaderReference, IClassHierarchy cha, IClassLoader parent,
      AnalysisScope scope) throws IOException {
    if (classLoaderReference.equals(JavaSourceAnalysisScope.SOURCE)) {
      ClassLoaderImpl cl = makeSourceLoader(classLoaderReference, cha, parent);
      cl.init(scope.getModules(classLoaderReference));
      return cl;
    } else {
      return super.makeNewClassLoader(classLoaderReference, cha, parent, scope);
    }
  }
  
  protected JavaSourceLoaderImpl makeSourceLoader(ClassLoaderReference classLoaderReference, IClassHierarchy cha, IClassLoader parent)
      throws IOException {
    return new ECJSourceLoaderImpl(classLoaderReference, parent, getExclusions(), cha, false);
  }

}
