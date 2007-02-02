/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.loader;


import com.ibm.wala.cast.tree.*;
import com.ibm.wala.cast.types.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.shrikeCT.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.debug.Assertions;

import java.net.*;
import java.util.*;

abstract public class AstFunctionClass implements IClass, ClassConstants {
  private final IClassLoader loader;

  protected IMethod functionBody;

  private final CAstSourcePositionMap.Position sourcePosition;

  private final TypeReference reference;

  private final TypeReference superReference;

  protected AstFunctionClass(TypeReference reference, TypeReference superReference, IClassLoader loader,
      CAstSourcePositionMap.Position sourcePosition) {
    this.superReference = superReference;
    this.sourcePosition = sourcePosition;
    this.reference = reference;
    this.loader = loader;
  }

  protected AstFunctionClass(TypeReference reference, IClassLoader loader, CAstSourcePositionMap.Position sourcePosition) {
    this(reference, TypeReference.findOrCreate(reference.getClassLoader(), AstTypeReference.functionTypeName), loader,
        sourcePosition);
  }

  public String toString() {
    return "function " + functionBody.getReference().getDeclaringClass().getName();
  }

  public IClassLoader getClassLoader() {
    return loader;
  }

  public boolean isInterface() {
    return false;
  }

  public boolean isAbstract() {
    return false;
  }

  public boolean isPublic() {
    return true;
  }

  public int getModifiers() {
    return ACC_PUBLIC;
  }

  public IClass getSuperclass() throws ClassHierarchyException {
    return loader.lookupClass(superReference.getName(), getClassHierarchy());
  }

  public Collection getDirectInterfaces() {
    return Collections.EMPTY_SET;
  }

  public Collection getAllImplementedInterfaces() {
    return Collections.EMPTY_SET;
  }

  public Collection getAllAncestorInterfaces() {
    return Collections.EMPTY_SET;
  }

  public IMethod getMethod(Selector selector) {
    if (selector.equals(AstMethodReference.fnSelector)) {
      return functionBody;
    } else {
      return loader.lookupClass(superReference.getName(), getClassHierarchy()).getMethod(selector);
    }
  }

  public IField getField(Atom name) {
    return loader.lookupClass(superReference.getName(), getClassHierarchy()).getField(name);
  }

  public TypeReference getReference() {
    return reference;
  }

  public CAstSourcePositionMap.Position getSourcePosition() {
    return sourcePosition;
  }

  public URL getSourceURL() {
    return sourcePosition.getURL();
  }

  public String getSourceFileName() {
    return sourcePosition.getURL().getFile();
  }

  public IMethod getClassInitializer() {
    return null;
  }

  public boolean isArrayClass() {
    return false;
  }

  public Collection<IMethod> getDeclaredMethods() {
    if (functionBody != null) {
      return Collections.singleton(functionBody);
    } else {
      throw new Error("function " + reference + " has no body!");
    }
  }

  public Collection getDeclaredInstanceFields() {
    return Collections.EMPTY_SET;
  }

  public Collection getDeclaredStaticFields() {
    return Collections.EMPTY_SET;
  }

  public Collection getAllInstanceFields() {
    return Collections.EMPTY_SET;
  }

  public Collection getAllStaticFields() {
    return Collections.EMPTY_SET;
  }

  public Collection getAllFields() {
    return Collections.EMPTY_SET;
  }

  public Collection getAllMethods() {
    return Collections.singleton(functionBody);
  }

  public TypeName getName() {
    return reference.getName();
  }

  public boolean isReferenceType() {
    return true;
  }
}
