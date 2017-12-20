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

import java.util.HashMap;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;

/**
 * Pseudo-classloader for all array classes; all other IClassLoader
 * implementations should delegate to this one for array classes only.
 */
public class ArrayClassLoader {

  private final static boolean DEBUG = false;

  /**
   * map: TypeReference -&gt; ArrayClass
   */
  final private HashMap<TypeReference, ArrayClass> arrayClasses = HashMapFactory.make();


  /**
   * @param className
   *          name of the array class
   * @param delegator
   *          class loader to look up element type with
   */
  public IClass lookupClass(TypeName className, IClassLoader delegator, IClassHierarchy cha) throws IllegalArgumentException {
    ArrayClass arrayClass;
    if (DEBUG) {
      assert className.toString().startsWith("[");
    }
    if (delegator == null) {
      throw new IllegalArgumentException("delegator must not be null");
    }

    TypeReference type = TypeReference.findOrCreate(delegator.getReference(), className);
    TypeReference elementType = type.getArrayElementType();
    if (elementType.isPrimitiveType()) {
      TypeReference aRef = TypeReference.findOrCreateArrayOf(elementType);
      arrayClass = arrayClasses.get(aRef);
      IClassLoader primordial = getRootClassLoader(delegator);
      if (arrayClass == null) {
        arrayClasses.put(aRef, arrayClass=new ArrayClass(aRef,primordial,cha));
      }
    } else {
      arrayClass = arrayClasses.get(type);
      if (arrayClass == null) {
	// check that the element class is loadable. If not, return null.
	IClass elementCls = delegator.lookupClass(elementType.getName());
        if (elementCls == null) {
          return null;
        }
	
	TypeReference realType = TypeReference.findOrCreateArrayOf(elementCls.getReference());
	arrayClass = arrayClasses.get(realType);
	
	if (arrayClass == null) {
	  arrayClass = new ArrayClass(realType, elementCls.getClassLoader(), cha);
	}
      }
      arrayClasses.put(type, arrayClass);
    }
    return arrayClass;
  }

  private static IClassLoader getRootClassLoader(IClassLoader l) {
    while (l.getParent() != null) {
      l = l.getParent();
    }
    return l;
  }
  
  public int getNumberOfClasses() {
    return arrayClasses.size();
  }
}
