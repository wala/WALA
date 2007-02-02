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
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.shrikeCT.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.*;
import com.ibm.wala.util.debug.Assertions;

import java.net.*;
import java.util.*;

abstract public class AstClass implements IClass, ClassConstants {
  private final CAstSourcePositionMap.Position sourcePosition;
  private final TypeName typeName;
  private final IClassLoader loader;
  private final short modifiers;
  protected final Map declaredFields;
  protected final Map declaredMethods;

  protected AstClass(CAstSourcePositionMap.Position sourcePosition,
		     TypeName typeName,
		     IClassLoader loader,
		     short modifiers,
		     Map declaredFields,
		     Map declaredMethods)
  {
    this.sourcePosition = sourcePosition;
    this.typeName = typeName;
    this.loader = loader;
    this.modifiers = modifiers;
    this.declaredFields = declaredFields;
    this.declaredMethods = declaredMethods;
  }

  public boolean isInterface() {
    return (modifiers&ACC_INTERFACE) != 0;
  }

  public boolean isAbstract() {
    return (modifiers&ACC_ABSTRACT) != 0;
  }

  public boolean isPublic() {
    return (modifiers&ACC_PUBLIC) != 0;
  }

  public boolean isReferenceType() {
    return true;
  }

  public boolean isArrayClass() {
    return false;
  }

  public int getModifiers() {
    return modifiers;
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

  public TypeName getName() {
    return typeName;
  }

  public TypeReference getReference() {
    return TypeReference.findOrCreate(loader.getReference(), typeName);
  }

  public IClassLoader getClassLoader() {
    return loader;
  }

  public abstract IClass getSuperclass()  throws ClassHierarchyException;
  
  private Collection gatherInterfaces() throws ClassHierarchyException {
    Set result = new HashSet();
    result.addAll( getDirectInterfaces() );
    if (getSuperclass() != null)
      result.addAll( getSuperclass().getAllImplementedInterfaces() );
    return result;
  }

  public abstract Collection getDirectInterfaces() throws ClassHierarchyException;
   
  public Collection getAllImplementedInterfaces() throws ClassHierarchyException {
    Assertions._assert(! isInterface());
    return gatherInterfaces();
  }
  
  public Collection getAllAncestorInterfaces() throws ClassHierarchyException {
    Assertions._assert(isInterface());
    return gatherInterfaces();
  }

  public IMethod getClassInitializer() {
    return getMethod( MethodReference.clinitSelector );
  }

  public IMethod getMethod(Selector selector) {
	try {
      if (declaredMethods.containsKey(selector)) {
        return (IMethod)declaredMethods.get(selector);
      } else if (getSuperclass() != null) {
        return getSuperclass().getMethod(selector);
      } else {
        return null;
      }
	} catch (ClassHierarchyException e) {
		Assertions.UNREACHABLE();
		return null;
	}
  }
  
  public IField getField(Atom name) {
    try {
	  if (declaredFields.containsKey(name)) {
        return (IField)declaredFields.get(name);
      } else if (getSuperclass() != null) {
        return getSuperclass().getField(name);
      } else {
        return null;
      }
	} catch (ClassHierarchyException e) {
		Assertions.UNREACHABLE();
		return null;
	}
  }

  public Collection<IMethod> getDeclaredMethods() {
    return declaredMethods.values();
  }

  public Collection getDeclaredInstanceFields() {
    Set result = new HashSet();
    for(Iterator FS = declaredFields.values().iterator(); FS.hasNext();) {
      IField F = (IField) FS.next();
      if (! F.isStatic()) {
	result.add( F );
      }
    }
    
    return result;
  }
    
  public Collection getDeclaredStaticFields() {
    Set result = new HashSet();
    for(Iterator FS = declaredFields.values().iterator(); FS.hasNext();) {
      IField F = (IField) FS.next();
      if (F.isStatic()) {
	result.add( F );
      }
    }
    
    return result;
  }

  public Collection getAllInstanceFields() throws ClassHierarchyException {
    Collection result = new HashSet();
    result.addAll( getDeclaredInstanceFields() );
    if (getSuperclass() != null) {
      result.addAll( getSuperclass().getAllInstanceFields() );
    }

    return result;
  }

  public Collection getAllStaticFields() throws ClassHierarchyException {
    Collection result = new HashSet();
    result.addAll( getDeclaredStaticFields() );
    if (getSuperclass() != null) {
      result.addAll( getSuperclass().getAllStaticFields() );
    }

    return result;
  }

  public Collection getAllFields() throws ClassHierarchyException {
    Collection result = new HashSet();
    result.addAll( getAllInstanceFields() );
    result.addAll( getAllStaticFields() );
    return result;
  }
  
  public Collection getAllMethods() throws ClassHierarchyException {
    Collection result = new HashSet();
    for(Iterator ms = getDeclaredMethods().iterator(); ms.hasNext(); ) {
      result.add( ms.next() );
    }
    if (getSuperclass() != null) {
      result.addAll( getSuperclass().getAllMethods() );
    }

    return result;
  }

}
