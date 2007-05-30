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
package com.ibm.wala.types;

import java.util.HashMap;

import com.ibm.wala.util.Atom;
import com.ibm.wala.util.ShrikeUtil;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * A class to represent the reference in a class file to a field.
 * 
 * @author Bowen Alpern
 * @author Dave Grove
 * @author Derek Lieber
 * @author Stephen Fink
 */
public final class FieldReference extends MemberReference {
  private final static boolean DEBUG = false;

  /**
   * Used to canonicalize MemberReferences a mapping from Key -> MemberReference
   */
  private static HashMap<Key, FieldReference> dictionary = HashMapFactory.make();

  private final TypeReference fieldType;

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.types.MemberReference#getSignature()
   */
  @Override
  public String getSignature() {
    return getDeclaringClass().getName() + "." + getName() + " " + getFieldType().getName();
  }

  /**
   * Find or create the canonical MemberReference instance for the given tuple.
   * 
   * @param mn
   *          the name of the member
   */
  public static synchronized FieldReference findOrCreate(TypeReference tref, Atom mn, TypeReference fieldType) {
    Key key = new Key(tref, mn, fieldType);

    FieldReference val = dictionary.get(key);
    if (val != null) {
      return val;
    }

    val = new FieldReference(key, fieldType);

    dictionary.put(key, val);
    return val;
  }

  /**
   * Find or create the canonical MemberReference instance for the given tuple.
   */
  public static FieldReference findOrCreate(ClassLoaderReference loader, String classType, String fieldName, String fieldType)
      throws IllegalArgumentException {
    TypeReference c = ShrikeUtil.makeTypeReference(loader, classType);
    TypeReference ft = ShrikeUtil.makeTypeReference(loader, fieldType);
    Atom name = Atom.findOrCreateUnicodeAtom(fieldName);
    return findOrCreate(c, name, ft);
  }

  private FieldReference(Key key, TypeReference fieldType) {
    super(key.type, key.name, key.hashCode());
    this.fieldType = fieldType;
    if (DEBUG) {
      if (getName().toString().indexOf('.') > -1)
        throw new UnimplementedError();
      if (fieldType.toString().indexOf('.') > -1)
        Assertions.UNREACHABLE("Field name: " + fieldType.toString());
      if (getName().toString().length() == 0)
        throw new UnimplementedError();
      if (fieldType.toString().length() == 0)
        throw new UnimplementedError();
    }
  }

  /**
   * @return the descriptor component of this member reference
   */
  public final TypeReference getFieldType() {
    return fieldType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public final String toString() {
    return "< " + getDeclaringClass().getClassLoader().getName() + ", " + getDeclaringClass().getName() + ", " + getName() + ", "
        + fieldType + " >";
  }

  /**
   * An identifier/selector for fields.
   */
  protected static class Key {
    final TypeReference type;

    final Atom name;

    private final TypeReference fieldType;

    Key(TypeReference type, Atom name, TypeReference fieldType) {
      this.type = type;
      this.name = name;
      this.fieldType = fieldType;
    }

    @Override
    public final int hashCode() {
      return 7487 * type.hashCode() + name.hashCode();
    }

    @Override
    public final boolean equals(Object other) {
      if (Assertions.verifyAssertions) {
        Assertions._assert(this.getClass().equals(other.getClass()));
      }

      Key that = (Key) other;
      return type.equals(that.type) && name.equals(that.name) && fieldType.equals(that.fieldType);
    }
  }

}
