/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.classLoader;

import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/** Base class for an object that represents a single Java classloader for analysis purposes. */
public interface IClassLoader {
  /**
   * Find and return the IClass defined by this class loader that corresponds to the given class
   * name.
   *
   * @param className name of the class
   * @return the IClass defined by this class loader that corresponds to the given class name, or
   *     null if not found.
   */
  IClass lookupClass(TypeName className);

  /**
   * Return the ClassLoaderReference for this class loader.
   *
   * @return ClassLoaderReference
   */
  ClassLoaderReference getReference();

  /** @return an Iterator of all classes loaded by this loader */
  Iterator<IClass> iterateAllClasses();

  /** @return the number of classes in scope to be loaded by this loader */
  int getNumberOfClasses();

  /** @return the unique name that identifies this class loader. */
  Atom getName();

  /**
   * @return the unique name that identifies the programming language from which this class loader
   *     loads code.
   */
  Language getLanguage();

  SSAInstructionFactory getInstructionFactory();

  int getNumberOfMethods();

  /**
   * @param method The method for which information is desired
   * @param offset an offset into the bytecode of the given method.
   * @return name of the source file corresponding to the given offset in the given method. Note
   *     that this api allows a single method to arise from multiple source files, which is
   *     deliberate as it can happen in some languages.
   */
  String getSourceFileName(IMethod method, int offset);

  /**
   * @param method The method for which information is desired
   * @param offset an offset into the bytecode of the given method.
   * @return input stream representing the source file for a given bytecode index of a given method,
   *     or null if not available
   */
  Reader getSource(IMethod method, int offset);

  /**
   * @param klass the class for which information is desired.
   * @return name of source file corresponding to the class, or null if not available
   * @throws NoSuchElementException if this class was generated from more than one source file The
   *     assumption that a class is generated from a single source file is java specific, and will
   *     change in the future. In place of this API, use the version that takes a method and an
   *     offset, since that is now the granularity at which source file information will be
   *     recorded. SJF .. we should think about this deprecation. postponing deprecation for now.
   */
  String getSourceFileName(IClass klass) throws NoSuchElementException;

  /**
   * @return input stream representing the source file for a class, or null if not available
   * @throws NoSuchElementException if this class was generated from more than one source file The
   *     assumption that a class is generated from a single source file is java specific, and will
   *     change in the future. In place of this API, use the version that takes a method and an
   *     offset, since that is now the granularity at which source file information will be
   *     recorded. SJF .. we should think about this deprecation. postponing deprecation for now.
   */
  Reader getSource(IClass klass) throws NoSuchElementException;

  /** @return the parent IClassLoader, if any, or null */
  IClassLoader getParent();

  /**
   * Initialize internal data structures.
   *
   * @throws IllegalArgumentException if modules is null
   */
  void init(List<Module> modules) throws IOException;

  /**
   * blow away references to any classes in the set
   *
   * @param toRemove Collection&lt;IClass&gt;
   */
  void removeAll(Collection<IClass> toRemove);
}
