/*
 * Copyright (c) 2021 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.types;

import com.ibm.wala.core.util.shrike.ShrikeUtil;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import java.util.HashMap;

/** A class to represent the reference in a method to a parameter. */
public final class ParameterReference extends MemberReference {
  private static final boolean DEBUG = false;

  /** Used to canonicalize MemberReferences a mapping from Key -&gt; MemberReference */
  private static final HashMap<Key, ParameterReference> dictionary = HashMapFactory.make();

  private final TypeReference parmType;

  @Override
  public String getSignature() {
    return getDeclaringClass().getName() + "." + getName() + ' ' + getParmType().getName();
  }

  /**
   * Find or create the canonical MemberReference instance for the given tuple.
   *
   * @param mn the name of the member
   */
  public static synchronized ParameterReference findOrCreate(
      TypeReference mref, Atom mn, TypeReference parmType) {
    if (mref == null) {
      throw new IllegalArgumentException("null method ref");
    }
    Key key = new Key(mref, mn, parmType);

    ParameterReference val = dictionary.get(key);
    if (val != null) {
      return val;
    }

    val = new ParameterReference(key, parmType);

    dictionary.put(key, val);
    return val;
  }

  /** Find or create the canonical MemberReference instance for the given tuple. */
  public static ParameterReference findOrCreate(
      ClassLoaderReference loader, String methodType, String parmName, String parmType)
      throws IllegalArgumentException {
    TypeReference m = ShrikeUtil.makeTypeReference(loader, methodType);
    TypeReference ft = ShrikeUtil.makeTypeReference(loader, parmType);
    Atom name = Atom.findOrCreateUnicodeAtom(parmName);
    return findOrCreate(m, name, ft);
  }

  private ParameterReference(Key key, TypeReference parmType) {
    super(key.type, key.name, key.hashCode());
    this.parmType = parmType;
    if (DEBUG) {
      if (getName().toString().indexOf('.') > -1) throw new UnimplementedError();
      if (parmType.toString().indexOf('.') > -1)
        Assertions.UNREACHABLE("Parm name: " + parmType.toString());
      if (getName().toString().length() == 0) throw new UnimplementedError();
      if (parmType.toString().length() == 0) throw new UnimplementedError();
    }
  }

  /** @return the descriptor component of this member reference */
  public final TypeReference getParmType() {
    return parmType;
  }

  @Override
  public final String toString() {
    return "< "
        + getDeclaringClass().getClassLoader().getName()
        + ", "
        + getDeclaringClass().getName()
        + ", "
        + getName()
        + ", "
        + parmType
        + " >";
  }

  /** An identifier/selector for parms. */
  protected static class Key {
    final TypeReference type;

    final Atom name;

    private final TypeReference parmType;

    Key(TypeReference type, Atom name, TypeReference parmType) {
      this.type = type;
      this.name = name;
      this.parmType = parmType;
    }

    @Override
    public final int hashCode() {
      return 7487 * type.hashCode() + name.hashCode();
    }

    @Override
    public final boolean equals(Object other) {
      assert other != null && this.getClass().equals(other.getClass());

      Key that = (Key) other;
      return type.equals(that.type) && name.equals(that.name) && parmType.equals(that.parmType);
    }
  }
}
