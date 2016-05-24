package com.ibm.wala.cast.java.translator.jdt.ejc;

import java.io.IOException;

import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.cast.java.translator.jdt.JDTClassLoaderFactory;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.SetOfClasses;

public class ECJClassLoaderFactory extends JDTClassLoaderFactory {

  public ECJClassLoaderFactory(SetOfClasses exclusions) {
    super(exclusions);
  }

  @Override
  protected JavaSourceLoaderImpl makeSourceLoader(ClassLoaderReference classLoaderReference, IClassHierarchy cha, IClassLoader parent)
      throws IOException {
    return new ECJSourceLoaderImpl(classLoaderReference, parent, getExclusions(), cha, dump);
  }

}
