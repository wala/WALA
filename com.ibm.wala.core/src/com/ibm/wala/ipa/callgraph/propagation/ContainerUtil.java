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
package com.ibm.wala.ipa.callgraph.propagation;

import java.util.Collection;

import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * Utilities for container class analysis
 */
public class ContainerUtil {

  private final static TypeName FreezableListName = TypeName.string2TypeName("Lcom/sun/corba/se/internal/ior/FreezableList");

  public final static TypeReference FreezableList = TypeReference.findOrCreate(ClassLoaderReference.Primordial, FreezableListName);

  private final static TypeName JarAttributesName = TypeName.string2TypeName("Ljava/util/jar/Attributes");

  public final static TypeReference JarAttributes = TypeReference.findOrCreate(ClassLoaderReference.Primordial, JarAttributesName);

  private final static Collection<TypeReference> miscContainers = HashSetFactory.make();
  static {
    miscContainers.add(FreezableList);
    miscContainers.add(JarAttributes);
  }

  /**
   * @return true iff C is a container class from java.util
   * @throws IllegalArgumentException if C is null
   */
  public static boolean isContainer(IClass c) {
    if (c == null) {
      throw new IllegalArgumentException("c is null");
    }
    if (ClassLoaderReference.Primordial.equals(c.getClassLoader().getReference())
        && TypeReference.JavaUtilCollection.getName().getPackage().equals(c.getReference().getName().getPackage())) {
      IClass collection = c.getClassHierarchy().lookupClass(TypeReference.JavaUtilCollection);
      IClass map = c.getClassHierarchy().lookupClass(TypeReference.JavaUtilMap);
      if (c.isInterface()) {
        assert collection != null;
        assert map != null;
        Collection s;
        s = c.getAllImplementedInterfaces();
        if (s.contains(collection) || s.contains(map)) {
          return true;
        }
      } else {
        if (c.getClassHierarchy().implementsInterface(c, collection) || c.getClassHierarchy().implementsInterface(c, map)) {
          return true;
        }
      }
    }
    if (miscContainers.contains(c.getReference())) {
      return true;
    }

    if (c.isArrayClass() && ((ArrayClass) c).getElementClass() != null
        && ((ArrayClass) c).getElementClass().getReference().isReferenceType()) {
      return true;
    }
    return false;
  }
}
