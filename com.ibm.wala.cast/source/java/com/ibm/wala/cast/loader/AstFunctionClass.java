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

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.types.AstTypeReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

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
    try {
      return "function " + functionBody.getReference().getDeclaringClass().getName();
    } catch (NullPointerException e) {
      return "<need to set code body>";
    }
  }

  public IClassLoader getClassLoader() {
    return loader;
  }

  public boolean isInterface() {
    return false;
  }

  public boolean isAbstract() {
    return functionBody == null;
  }

  public boolean isPublic() {
    return true;
  }
  
  public boolean isPrivate() {
    return false;
  }
  
  public boolean isStatic() {
    return false;
  }

  public int getModifiers() {
    return ACC_PUBLIC;
  }

  public IClass getSuperclass() {
    return loader.lookupClass(superReference.getName());
  }

  public Collection<IClass> getDirectInterfaces() {
    return Collections.emptySet();
  }

  public Collection<IClass> getAllImplementedInterfaces() {
    return Collections.emptySet();
  }

  public Collection<IClass> getAllAncestorInterfaces() {
    return Collections.emptySet();
  }

  public IMethod getMethod(Selector selector) {
    if (selector.equals(AstMethodReference.fnSelector)) {
      return functionBody;
    } else {
      return loader.lookupClass(superReference.getName()).getMethod(selector);
    }
  }

  public IField getField(Atom name) {
    return loader.lookupClass(superReference.getName()).getField(name);
  }

  public IField getField(Atom name, TypeName type) {
    // assume that for AST classes, you can't have multiple fields with the same name    
    return loader.lookupClass(superReference.getName()).getField(name);
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
  
  public InputStream getSource() {
    return null;
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

  public Collection<IField> getDeclaredInstanceFields() {
    return Collections.emptySet();
  }

  public Collection<IField> getDeclaredStaticFields() {
    return Collections.emptySet();
  }

  public Collection<IField> getAllInstanceFields() {
    return Collections.emptySet();
  }

  public Collection<IField> getAllStaticFields() {
    return Collections.emptySet();
  }

  public Collection<IField> getAllFields() {
    return Collections.emptySet();
  }

  public Collection<IMethod> getAllMethods() {
    return Collections.singleton(functionBody);
  }

  public TypeName getName() {
    return reference.getName();
  }

  public boolean isReferenceType() {
    return true;
  }
}
