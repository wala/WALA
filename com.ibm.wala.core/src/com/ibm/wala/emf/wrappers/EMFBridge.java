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
package com.ibm.wala.emf.wrappers;

import com.ibm.wala.ecore.java.EClassLoaderName;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.JavaFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * Utilities that bridge between EMF types and WALA native types
 * 
 * @author sfink
 */
public class EMFBridge {

  public static EJavaClass makeJavaClass(TypeReference t) {
    if (t == null) {
      throw new IllegalArgumentException("t is null");
    }
    EJavaClass klass = JavaFactory.eINSTANCE.createEJavaClass();
    String className = t.getName().toUnicodeString();
    // strip a leading "L"
    if (className.charAt(0) == TypeReference.ClassTypeCode) {
      className = className.substring(1);
    }
    klass.setClassName(className.replace('/', '.'));
    klass.setLoader(getEClassLoaderName(t));
    return klass;
  }

  private static EClassLoaderName getEClassLoaderName(TypeReference t) {
    ClassLoaderReference loader = t.getClassLoader();
    if (loader.equals(ClassLoaderReference.Primordial)) {
      return EClassLoaderName.PRIMORDIAL_LITERAL;
    } else if (loader.equals(ClassLoaderReference.Application)) {
      return EClassLoaderName.APPLICATION_LITERAL;
    } else if (loader.equals(ClassLoaderReference.Extension)) {
      return EClassLoaderName.EXTENSION_LITERAL;
    } else {
      Assertions.UNREACHABLE("unexpected class loader reference: " + loader);
      return null;
    }

  }
}