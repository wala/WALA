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

import com.ibm.wala.util.ImmutableByteArray;
import com.ibm.wala.util.StringStuff;
import com.ibm.wala.util.UTF8Convert;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * A method descriptor; something like:
 * (Ljava/langString;)Ljava/lang/Class;
 * 
 * Descriptors are canonical
 * 
 * @author sfink
 */
public final class Descriptor {

  /**
   * A mapping from Key -> Descriptor
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
  public static Descriptor findOrCreate(ImmutableByteArray b) throws IllegalArgumentException {
    TypeName returnType = StringStuff.parseForReturnTypeName(b);
    TypeName[] parameters = StringStuff.parseForParameterNames(b);
    Key k = new Key(returnType, parameters);
    Descriptor result = map.get(k);
    if (result == null) {
      result = new Descriptor(k);
      map.put(k, result);
    }
    return result;
  }

  /**
   * @param s string representation of this descriptor
   * @return the canonical representative for this descriptor value
   */
  public static Descriptor findOrCreateUTF8(String s) throws IllegalArgumentException {
    byte[] b = UTF8Convert.toUTF8(s);
    return findOrCreate(new ImmutableByteArray(b));
  }
  /**
   * @param key "value" of this descriptor
   */
  private Descriptor(Key key) {
    this.key = key;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return key.hashCode();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
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
   * @author sfink
   *
   * value that defines a descriptor: used to canonicalize instances
   */
  private static class Key {
    private TypeName returnType;
    private TypeName[] parameters;
    private int hashCode; // cached for efficiency
    Key(TypeName returnType, TypeName[] parameters) {
      this.returnType = returnType;
      this.parameters = parameters;
      if (Assertions.verifyAssertions && parameters != null) {
        Assertions._assert(parameters.length > 0);
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
        for (int i = 0; i<parameters.length; i++) {
          result += parameters[i].hashCode() * (5323 ^ i);
        }
      }
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (Assertions.verifyAssertions) {
        Assertions._assert(obj instanceof Key);
      }
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
        for (int i = 0; i < parameters.length; i++) {
          TypeName p = parameters[i];
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
        for (int i = 0; i < parameters.length; i++) {
          TypeName p = parameters[i];
          result.append(p.toUnicodeString());
          appendSemicolonIfNeeded(result, p);
        }
      }
      result.append(")");
      result.append(returnType);
      appendSemicolonIfNeeded(result, returnType);
      return result.toString();
    }
    
    private void appendSemicolonIfNeeded(StringBuffer result, TypeName p) {
      if (p.isArrayType()) {
        TypeName e = p.getInnermostElementType();
        if (!TypeReference.isPrimitiveType(e)) {
          result.append(";");
        }
      } else if (p.isClassType()) {
        result.append(";");
      }
    }
  }


}
