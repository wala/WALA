/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT.analysis;

import java.util.HashMap;
import java.util.Iterator;

import com.ibm.wala.shrikeBT.Constants;

/**
 * This implementation of ClassHierarchyProvider is a simple writable data structure representing a class hierarchy. You call
 * setClassInfo to record information about a class.
 */
public final class ClassHierarchyStore implements ClassHierarchyProvider {
  private static final String[] noClasses = new String[0];

  final static class ClassInfo {
    final boolean isInterface;

    final boolean isFinal;

    final String superClass;

    final String[] superInterfaces;

    ClassInfo(boolean isInterface, boolean isFinal, String superClass, String[] superInterfaces) {
      this.isInterface = isInterface;
      this.isFinal = isFinal;
      this.superClass = superClass;
      this.superInterfaces = superInterfaces;
    }
  }

  final private HashMap<String, ClassInfo> contents = new HashMap<>();

  /**
   * Create an empty store.
   */
  public ClassHierarchyStore() {
  }

  /**
   * Append some class information to the store.
   * 
   * @param cl the JVM type of the class being added (e.g., Ljava/lang/Object;)
   * @param isInterface true iff it's an interface
   * @param isFinal true iff it's final
   * @param superClass the JVM type of the superclass, or null if this is Object
   * @param superInterfaces the JVM types of its implemented interfaces
   */
  public void setClassInfo(String cl, boolean isInterface, boolean isFinal, String superClass, String[] superInterfaces)
      throws IllegalArgumentException {
    if (superClass != null && superClass.equals(cl)) {
      throw new IllegalArgumentException("Class " + cl + " cannot be its own superclass");
    }
    contents.put(cl, new ClassInfo(isInterface, isFinal, superClass, superInterfaces));
  }

  /**
   * Delete the class information from the store.
   */
  public void removeClassInfo(String cl) {
    contents.remove(cl);
  }

  /**
   * Iterate through all classes in the store.
   */
  public Iterator<String> iterateOverClasses() {
    return contents.keySet().iterator();
  }

  /**
   * @see ClassHierarchyProvider#getSuperClass(String)
   */
  @Override
  public String getSuperClass(String cl) {
    ClassInfo info = contents.get(cl);
    return info == null ? null : info.superClass;
  }

  /*
   * @see ClassHierarchyProvider#getSuperInterfaces(String)
   */
  @Override
  public String[] getSuperInterfaces(String cl) {
    ClassInfo info = contents.get(cl);
    return info == null ? null : info.superInterfaces;
  }

  /*
   * @see ClassHierarchyProvider#getSubClasses(String)
   */
  @Override
  public String[] getSubClasses(String cl) {
    ClassInfo info = contents.get(cl);
    return (info == null || !info.isFinal) ? null : noClasses;
  }

  /*
   * @see ClassHierarchyProvider#isInterface(String)
   */
  @Override
  public int isInterface(String cl) {
    ClassInfo info = contents.get(cl);
    return info == null ? Constants.MAYBE : (info.isInterface ? Constants.YES : Constants.NO);
  }
}
