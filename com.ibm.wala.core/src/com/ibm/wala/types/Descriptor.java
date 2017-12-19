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

import java.util.Map;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.strings.ImmutableByteArray;
import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.util.strings.UTF8Convert;

/**
 * A method descriptor; something like: (Ljava/langString;)Ljava/lang/Class;
 * 
 * Descriptors are canonical
 */
public final class Descriptor {

  /**
   * A mapping from Key -&gt; Descriptor
   */
  private static final Map<Key, Descriptor> map = HashMapFactory.make();

  /**
   * key holds the logical value of this descriptor
   */
  private final Key key;

  /**
   * @param parameters the parameters for a descriptor
   * @param returnType the return type
   * @return the canonical representative for this descriptor value
   */
  public static Descriptor findOrCreate(TypeName[] parameters, TypeName returnType) {
    if (returnType == null) {
      throw new IllegalArgumentException("null returnType");
    }
    if (parameters != null && parameters.length == 0) {
      parameters = null;
    }
    Key k = new Key(returnType, parameters);
    Descriptor result = map.get(k);
    if (result == null) {
      result = new Descriptor(k);
      map.put(k, result);
    }
    return result;
  }

  /**
   * @param b a byte array holding the string representation of this descriptor
   * @return the canonical representative for this descriptor value
   */
  public static Descriptor findOrCreate(Language l, ImmutableByteArray b) throws IllegalArgumentException {
    TypeName returnType = StringStuff.parseForReturnTypeName(l, b);
    TypeName[] parameters = StringStuff.parseForParameterNames(l, b);
    Key k = new Key(returnType, parameters);
    Descriptor result = map.get(k);
    if (result == null) {
      result = new Descriptor(k);
      map.put(k, result);
    }
    return result;
  }

  public static Descriptor findOrCreate(ImmutableByteArray b) throws IllegalArgumentException {
    return findOrCreate(Language.JAVA, b);
  }

  /**
   * @param s string representation of this descriptor
   * @return the canonical representative for this descriptor value
   */
  public static Descriptor findOrCreateUTF8(String s) throws IllegalArgumentException {
    return findOrCreateUTF8(Language.JAVA, s);
  }

  /**
   * @param s string representation of this descriptor
   * @return the canonical representative for this descriptor value
   */
  public static Descriptor findOrCreateUTF8(Language l, String s) throws IllegalArgumentException {
    byte[] b = UTF8Convert.toUTF8(s);
    return findOrCreate(l, new ImmutableByteArray(b));
  }

  /**
   * @param key "value" of this descriptor
   */
  private Descriptor(Key key) {
    this.key = key;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return key.toString();
  }

  /**
   * @return a unicode string representation of this descriptor
   */
  public String toUnicodeString() {
    return key.toUnicodeString();
  }

  /**
   * @return the name of the return type of this descriptor
   */
  public TypeName getReturnType() {
    return key.returnType;
  }

  /**
   * @return the type names for the parameters in this descriptor
   */
  public TypeName[] getParameters() {
    return key.parameters;
  }

  /**
   * @return number of parameters in this descriptor
   */
  public int getNumberOfParameters() {
    return key.parameters == null ? 0 : key.parameters.length;
  }

  /**
   * value that defines a descriptor: used to canonicalize instances
   */
  private static class Key {
    final private TypeName returnType;

    final private TypeName[] parameters;

    final private int hashCode; // cached for efficiency

    Key(TypeName returnType, TypeName[] parameters) {
      this.returnType = returnType;
      this.parameters = parameters;
      if (parameters != null) {
        assert parameters.length > 0;
      }
      hashCode = computeHashCode();
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    public int computeHashCode() {
      int result = returnType.hashCode() * 5309;
      if (parameters != null) {
        for (int i = 0; i < parameters.length; i++) {
          result += parameters[i].hashCode() * (5323 ^ i);
        }
      }
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      assert obj instanceof Key;
      Key other = (Key) obj;
      if (!returnType.equals(other.returnType)) {
        return false;
      }
      if (parameters == null) {
        return (other.parameters == null) ? true : false;
      }
      if (other.parameters == null) {
        return false;
      }
      if (parameters.length != other.parameters.length) {
        return false;
      }
      for (int i = 0; i < parameters.length; i++) {
        if (!parameters[i].equals(other.parameters[i])) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      StringBuffer result = new StringBuffer();
      result.append("(");
      if (parameters != null) {
        for (TypeName p : parameters) {
          result.append(p);
          appendSemicolonIfNeeded(result, p);
        }
      }
      result.append(")");
      result.append(returnType);
      appendSemicolonIfNeeded(result, returnType);
      return result.toString();
    }

    public String toUnicodeString() {
      StringBuffer result = new StringBuffer();
      result.append("(");
      if (parameters != null) {
        for (TypeName p : parameters) {
          result.append(p.toUnicodeString());
          appendSemicolonIfNeeded(result, p);
        }
      }
      result.append(")");
      result.append(returnType);
      appendSemicolonIfNeeded(result, returnType);
      return result.toString();
    }

    private static void appendSemicolonIfNeeded(StringBuffer result, TypeName p) {
      if (p.isArrayType()) {
        TypeName e = p.getInnermostElementType();
        String x = e.toUnicodeString();
        if (x.startsWith("L") || x.startsWith("P")) {
          result.append(";");
        }
      } else {
        String x = p.toUnicodeString();
        if (x.startsWith("L") || x.startsWith("P")) {
          result.append(";");
        }
      }
    }
  }

}
