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
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.strings.Atom;

/**
 * Base class for an object that represents a single Java classloader for analysis purposes.
 * 
 * @author sfink
 */
public interface IClassLoader {
  /**
   * Find and return the IClass defined by this class loader that corresponds to the given class name.
   * 
   * @param className name of the class
   * @return the IClass defined by this class loader that corresponds to the given class name, or null if not found.
   */
  public abstract IClass lookupClass(TypeName className);

  /**
   * Return the ClassLoaderReference for this class loader.
   * 
   * @return ClassLoaderReference
   */
  public abstract ClassLoaderReference getReference();

  /**
   * @return an Iterator of all classes loaded by this loader
   */
  public abstract Iterator<IClass> iterateAllClasses();

  /**
   * @return the number of classes in scope to be loaded by this loader
   */
  public abstract int getNumberOfClasses();

  /**
   * @return the unique name that identifies this class loader.
   */
  Atom getName();

  /**
   * @return the unique name that identifies the programming language from which this class loader loads code.
   */
  Language getLanguage();

  SSAInstructionFactory getInstructionFactory();
  
  public abstract int getNumberOfMethods();

  /**
   * @return name of source file corresponding to the class, or null if not available
   */
  public abstract String getSourceFileName(IClass klass);

  /**
   * @return input stream representing the source file for a class, or null if not available
   */
  public abstract InputStream getSource(IClass klass);

  /**
   * @return the parent IClassLoader, if any, or null
   */
  public abstract IClassLoader getParent();

  /**
   * Initialize internal data structures.
   * 
   * @throws IOException
   * @throws IllegalArgumentException if modules is null
   */
  public void init(List<Module> modules) throws IOException;

  /**
   * blow away references to any classes in the set
   * 
   * @param toRemove Collection<IClass>
   */
  public abstract void removeAll(Collection<IClass> toRemove);
}
