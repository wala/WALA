/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.classLoader;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * An implementation of the class loader factory that produces ClassLoaderImpls
 */
public class ClassLoaderFactoryImpl implements ClassLoaderFactory {

  /**
   * Set of classes that class loaders should ignore; classloaders should
   * pretend these classes don't exit.
   */
  private SetOfClasses exclusions;

  /**
   * An object to track warnings
   */
  private final WarningSet warnings;

  /**
   * A Mapping from ClassLoaderReference to IClassLoader
   */
  private HashMap<ClassLoaderReference, IClassLoader> map = HashMapFactory.make(3);

  /**
   * @param exclusions
   *          A set of classes that class loaders should pretend don't exist.
   */
  public ClassLoaderFactoryImpl(SetOfClasses exclusions, WarningSet warnings) {
    this.exclusions = exclusions;
    this.warnings = warnings;
  }

  /**
   * Return a class loader corresponding to a given class loader identifier.
   * Create one if necessary.
   * 
   * @param classLoaderReference
   *          identifier for the desired class loader
   * @return IClassLoader
   */
  public IClassLoader getLoader(ClassLoaderReference classLoaderReference, ClassHierarchy cha, AnalysisScope scope)
      throws IOException {
    IClassLoader result = map.get(classLoaderReference);
    if (result == null) {
      ClassLoaderReference parentRef = classLoaderReference.getParent();
      IClassLoader parent = null;
      if (parentRef != null) {
        parent = getLoader(parentRef, cha, scope);
      }
      IClassLoader cl = makeNewClassLoader(classLoaderReference, cha, parent, scope);
      map.put(classLoaderReference, cl);
      result = cl;
    }
    return result;
  }

  /**
   * Create a new class loader for a given key
   * 
   * @param classLoaderReference
   *          the key
   * @param parent
   *          parent classloader to be used for delegation
   * @return a new ClassLoaderImpl
   * @throws IOException
   *           if the desired loader cannot be instantiated, usually because the
   *           specified module can't be found.
   */
  protected IClassLoader makeNewClassLoader(ClassLoaderReference classLoaderReference, ClassHierarchy cha, IClassLoader parent,
      AnalysisScope scope) throws IOException {
    String implClass = scope.getLoaderImpl(classLoaderReference);
    IClassLoader cl;
    if (implClass == null) {
      cl = new ClassLoaderImpl(classLoaderReference, scope.getArrayClassLoader(), parent, exclusions, cha, warnings);
    } else
      try {
        Class impl = Class.forName(implClass);
        Constructor ctor = impl.getDeclaredConstructor(new Class[] { ClassLoaderReference.class, IClassLoader.class,
            SetOfClasses.class, ClassHierarchy.class, WarningSet.class });
        cl = (IClassLoader) ctor.newInstance(new Object[] { classLoaderReference, parent, exclusions, cha, warnings });
      } catch (Exception e) {
        warnings.add(InvalidClassLoaderImplementation.create(implClass));
        cl = new ClassLoaderImpl(classLoaderReference, scope.getArrayClassLoader(), parent, exclusions, cha, warnings);
      }
    cl.init(scope.getModules(classLoaderReference));
    return cl;
  }

  /**
   * @author sfink
   * 
   * A waring when we fail to load an appropriate class loader implementation
   */
  private static class InvalidClassLoaderImplementation extends Warning {

    final String impl;

    InvalidClassLoaderImplementation(String impl) {
      super(Warning.SEVERE);
      this.impl = impl;
    }

    public String getMsg() {
      return getClass().toString() + " : " + impl;
    }

    public static InvalidClassLoaderImplementation create(String impl) {
      return new InvalidClassLoaderImplementation(impl);
    }
  }

  /**
   * @return the set of classes that will be ignored.
   */
  public SetOfClasses getExclusions() {
    return exclusions;
  }

  public WarningSet getWarnings() {
    return warnings;
  }
}
