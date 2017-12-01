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
package com.ibm.wala.ipa.summaries;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.strings.Atom;

/**
 * This class represents class loaders that introduce classes that do not exist
 * in the actual application being analyzed. They may be abstract summaries of
 * unanalyzed library code, wrappers that encode J2EE specialized behavior or
 * other invented classes.
 * 
 * The intention is that there be (at most) one such classloader in a given
 * class hierarchy, and that it be referenced using the "Synthetic" classloader
 * reference. Furthermore, it is required that this synthetic loader be a child
 * loader of the Primordial, Extension and Application loaders.
 * 
 * This special classloader has some interactions with the hierarchy for, while
 * the classes it loads are normal-seeming IClass objects, unlike the other
 * loaders, its set of classes is not fixed, causing special cases in code that
 * caches hierarchy data. Also note that this causes the getNumberfClasses and
 * iterateAllClasses methods to behave differently for those of other
 * classloaders.
 * 
 * Code that wants to introduce synthetic classes uses the registerClass method,
 * giving it an Atom which is the class name, and an IClass which is the class
 * to load. Since the synthetic loader musat be a child of the others, it would
 * be very bad to use an existing name for a new synthetic class.
 * 
 * Class lookup works just as for any other classloader.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 * 
 */
public class BypassSyntheticClassLoader implements IClassLoader {

  private final ClassLoaderReference me;

  private final IClassLoader parent;

  private final IClassHierarchy cha;

  private final HashMap<TypeName, IClass> syntheticClasses = HashMapFactory.make();

  
  /**
   * Don't change my signature!  ClassLoaderFactoryImpl calls me by reflection! yuck.
   * 
   * @param me the name of this class loader
   * @param parent its parent
   * @param exclusions classes to ignore
   * @param cha governing class hierarchy
   */
  public BypassSyntheticClassLoader(ClassLoaderReference me, IClassLoader parent, SetOfClasses exclusions, IClassHierarchy cha) {
    if (cha == null) {
      throw new IllegalArgumentException("null cha");
    }
    this.me = me;
    this.cha = cha;
    this.parent = parent;
  }
  
  @Override
  public String toString() {
    return me.getName().toString();
  }

  @Override
  public IClass lookupClass(TypeName className) {
    IClass pc = parent.lookupClass(className);
    if (pc == null) {
      IClass c = syntheticClasses.get(className);
      return c;
    } else {
      return pc;
    }
  }

  /**
   * Register the existence of a new synthetic class
   */
  public void registerClass(TypeName className, IClass theClass) {
    cha.addClass(theClass);
    syntheticClasses.put(className, theClass);
  }

  /**
   * Return the ClassLoaderReference for this class loader.
   */
  @Override
  public ClassLoaderReference getReference() {
    return me;
  }

  /**
   * @return an Iterator of all classes loaded by this loader
   */
  @Override
  public Iterator<IClass> iterateAllClasses() {
    return syntheticClasses.values().iterator();
  }

  /**
   * @return the number of classes in scope to be loaded by this loader
   */
  @Override
  public int getNumberOfClasses() {
    return syntheticClasses.size();
  }

  /**
   * @return the unique name that identifies this class loader.
   */
  @Override
  public Atom getName() {
    return me.getName();
  }

  /**
   * @return the unique name that identifies the programming language
   *  from which this class loader loads code.
   */
  @Override
  public Language getLanguage() {
    return Language.JAVA;
  }

  /*
   * @see com.ibm.wala.classLoader.IClassLoader#getNumberOfMethods()
   */
  @Override
  public int getNumberOfMethods() {
    // TODO Auto-generated method stub
    return 0;
  }

  /*
   * @see com.ibm.wala.classLoader.IClassLoader#getSourceFileName(com.ibm.wala.classLoader.IClass)
   */
  @Override
  public String getSourceFileName(IClass klass) {
    return null;
  }

  /**
   * @see com.ibm.wala.classLoader.IClassLoader#getParent()
   */
  @Override
  public IClassLoader getParent() {
    return parent;
  }

  @Override
  public void init(List<Module> modules) throws IOException {
  }

  /*
   * @see com.ibm.wala.classLoader.IClassLoader#removeAll(java.util.Collection)
   */
  @Override
  public void removeAll(Collection<IClass> toRemove) {
    if (toRemove == null) {
      throw new IllegalArgumentException("toRemove is null");
    }
    for (IClass klass : toRemove) {
      syntheticClasses.remove(klass.getName());
    }
  }

  @Override
  public Reader getSource(IClass klass) {
    return null;
  }

  @Override
  public SSAInstructionFactory getInstructionFactory() {
    return getLanguage().instructionFactory();
  }

  @Override
  public Reader getSource(IMethod method, int offset) {
    return null;
  }

  @Override
  public String getSourceFileName(IMethod method, int offset) {
    return null;
  }
}
