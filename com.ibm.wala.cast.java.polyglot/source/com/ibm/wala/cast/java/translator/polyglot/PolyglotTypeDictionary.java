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
/*
 * Created on Sep 28, 2005
 */
package com.ibm.wala.cast.java.translator.polyglot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import polyglot.types.ArrayType;
import polyglot.types.ClassType;
import polyglot.types.ObjectType;
import polyglot.types.PrimitiveType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

import com.ibm.wala.cast.java.types.JavaPrimitiveTypeMap;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.ibm.wala.util.debug.Assertions;

public class PolyglotTypeDictionary extends CAstTypeDictionaryImpl {
  private final class PolyglotJavaArrayType implements CAstType.Array {
    private final Type fEltPolyglotType;

    private final CAstType fEltCAstType;

    private PolyglotJavaArrayType(ArrayType arrayType) {
      super();
      fEltPolyglotType = arrayType.base();
      fEltCAstType = getCAstTypeFor(fEltPolyglotType);
    }

    @Override
    public int getNumDimensions() {
      return 1; // always 1 for Java
    }

    @Override
    public CAstType getElementType() {
      return fEltCAstType;
    }

    @Override
    public String getName() {
      return "[" + fEltCAstType.getName();
    }

    @Override
    public Collection<CAstType> getSupertypes() {
      if (fEltPolyglotType.isPrimitive())
        return Collections.singleton(getCAstTypeFor(fTypeSystem.Object()));
      Assertions.productionAssertion(fEltPolyglotType.isReference(), "Non-primitive, non-reference array element type!");
      ObjectType baseRefType = (ObjectType) fEltPolyglotType;
      Collection<CAstType> supers = new ArrayList<CAstType>();
      for (Iterator<Type> superIter = baseRefType.interfaces().iterator(); superIter.hasNext(); ) {
        supers.add(getCAstTypeFor(superIter.next()));
      }
      if (baseRefType instanceof ClassType) {
          ClassType baseClassType = (ClassType) baseRefType;
        if (baseClassType.superClass() != null)
              supers.add(getCAstTypeFor(baseRefType.superClass()));
      }
      return supers;
    }
  }

  protected final TypeSystem fTypeSystem;

  protected final PolyglotJava2CAstTranslator fTranslator;

  public PolyglotTypeDictionary(TypeSystem typeSystem, PolyglotJava2CAstTranslator translator) {
    fTypeSystem = typeSystem;
    fTranslator = translator;
  }

  @Override
  public CAstType getCAstTypeFor(Object astType) {
    CAstType type = super.getCAstTypeFor(astType);
    // Handle the case where we haven't seen an AST decl for some type before
    // processing a reference. This can certainly happen with classes in byte-
    // code libraries, for which we never see an AST decl.
    // In this case, just create a new type and return that.
    if (type == null) {
      final Type polyglotType = (Type) astType;

      if (polyglotType.isClass())
        type = fTranslator.new PolyglotJavaType((ClassType) astType, this, fTypeSystem);
      else if (polyglotType.isPrimitive()) {
        type = JavaPrimitiveTypeMap.lookupType(((PrimitiveType) polyglotType).name().toString());
      } else if (polyglotType.isArray()) {
        type = new PolyglotJavaArrayType((ArrayType) polyglotType);
      } else
        Assertions.UNREACHABLE("getCAstTypeFor() passed type that is not primitive, array, or class?");
      super.map(astType, type);
    }
    return type;
  }
}
