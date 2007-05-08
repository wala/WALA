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
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * Utilities for container class analysis
 * 
 * @author sfink
 */
public class ContainerUtil {
  
  private final static TypeName FreezableListName = TypeName.string2TypeName("Lcom/sun/corba/se/internal/ior/FreezableList");
  public final static TypeReference FreezableList =
    TypeReference.findOrCreate(ClassLoaderReference.Primordial, FreezableListName);
  
  private final static TypeName JarAttributesName = TypeName.string2TypeName("Ljava/util/jar/Attributes");
  public final static TypeReference JarAttributes =
    TypeReference.findOrCreate(ClassLoaderReference.Primordial, JarAttributesName);
  
  private final static Collection<TypeReference> miscContainers = HashSetFactory.make();
  static {
    miscContainers.add(FreezableList);
    miscContainers.add(JarAttributes);
  }
  
  /**
   * @param C
   * @return true iff C is a container class from java.util
   * @throws IllegalArgumentException  if C is null
   */
  public static boolean isContainer(IClass C, ClassHierarchy cha) {
    if (C == null) {
      throw new IllegalArgumentException("C is null");
    }
    if (ClassLoaderReference.Primordial.equals(C.getClassLoader().getReference())&& 
        TypeReference.JavaUtilCollection.getName().getPackage().equals(C.getReference().getName().getPackage())) {
      if (C.isInterface()) {
        IClass collection = cha.lookupClass(TypeReference.JavaUtilCollection);
        IClass map = cha.lookupClass(TypeReference.JavaUtilMap);
        if (Assertions.verifyAssertions) {
          Assertions._assert(collection != null);
          Assertions._assert(map != null);
        }
        Collection s;
        try {
          s = C.getAllAncestorInterfaces();
        } catch (ClassHierarchyException e) {
          // give up
          return false;
        }
        if (s.contains(collection) || s.contains(map)) {
          return true;
        }
      } else {
        if (cha.implementsInterface(C, TypeReference.JavaUtilCollection) || cha.implementsInterface(C, TypeReference.JavaUtilMap)) {
          return true;
        }
      }
    }
    if (miscContainers.contains(C.getReference())) {
      return true;
    }
    
    if (C.isArrayClass() && ((ArrayClass) C).getElementClass() != null
        && ((ArrayClass) C).getElementClass().getReference().isReferenceType()) {
      return true;
    }
    return false;
  }
}
