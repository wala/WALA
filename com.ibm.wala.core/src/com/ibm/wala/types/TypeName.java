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

import java.io.Serializable;
import java.io.UTFDataFormatException;
import java.util.Map;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;
import com.ibm.wala.util.strings.StringStuff;

/**
 * We've introduced this class to canonicalize Atoms that represent package names.
 * 
 * NB: All package names should use '/' and not '.' as a separator. eg. Ljava/lang/Class
 */
public final class TypeName implements Serializable {

  public static final byte ArrayMask = 0x01;
  public static final byte PointerMask = 0x02;
  public static final byte ReferenceMask = 0x03;
  public static final byte PrimitiveMask = 0x04;

  public static final byte ElementMask = 0x07;
  public static final byte ElementBits = 3;
  
  /* Serial version */
  private static final long serialVersionUID = -3256390509887654326L;

  /**
   * canonical mapping from TypeNameKey -&gt; TypeName
   */
  private final static Map<TypeNameKey, TypeName> map = HashMapFactory.make();

  private static synchronized TypeName findOrCreate(TypeNameKey t) {
    TypeName result = map.get(t);
    if (result == null) {
      result = new TypeName(t);
      map.put(t, result);

    }
    return result;
  }

  /**
   * The key object holds all the information about a type name
   */
  private final TypeNameKey key;

  public static TypeName findOrCreate(ImmutableByteArray name, int start, int length) throws IllegalArgumentException {
    Atom className = Atom.findOrCreate(StringStuff.parseForClass(name, start, length));
    ImmutableByteArray p = StringStuff.parseForPackage(name, start, length);
    Atom packageName = (p == null) ? null : Atom.findOrCreate(p);
    int dim = StringStuff.parseForArrayDimensionality(name, start, length);
    boolean innermostPrimitive = StringStuff.classIsPrimitive(name, start, length);
    if (innermostPrimitive) {
      if (dim == 0) {
        dim = -1;
      } else {
        dim <<= ElementBits;
        dim |= PrimitiveMask;
      }
    }
    TypeNameKey t = new TypeNameKey(packageName, className, dim);
    return findOrCreate(t);
  }

  public static TypeName findOrCreate(ImmutableByteArray name) throws IllegalArgumentException {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    return findOrCreate(name, 0, name.length());
  }

  public static TypeName findOrCreate(String name) throws IllegalArgumentException {
    ImmutableByteArray b = ImmutableByteArray.make(name);
    return findOrCreate(b);
  }

  public static TypeName findOrCreateClass(Atom packageName, Atom className) {
    if (packageName == null) {
      throw new IllegalArgumentException("null packageName");
    }
    if (className == null) {
      throw new IllegalArgumentException("null className");
    }
    TypeNameKey T = new TypeNameKey(packageName, className, 0);
    return findOrCreate(T);
  }

  public static TypeName findOrCreate(Atom packageName, Atom className, int dim) {
    TypeNameKey T = new TypeNameKey(packageName, className, dim);
    return findOrCreate(T);
  }

  /**
   * This should be the only constructor
   */
  private TypeName(TypeNameKey key) {
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

  public String toUnicodeString() {
    return key.toUnicodeString();
  }

  /**
   * @param s a String like Ljava/lang/Object
   * @return the corresponding TypeName
   * @throws IllegalArgumentException if s is null
   */
  public static TypeName string2TypeName(String s) throws IllegalArgumentException {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    byte[] val = s.getBytes();

    return findOrCreate(new ImmutableByteArray(val));
  }

  public static TypeName findOrCreateClassName(String packageName, String className) {
    Atom p = Atom.findOrCreateUnicodeAtom(packageName);
    Atom c = Atom.findOrCreateUnicodeAtom(className);
    return findOrCreateClass(p, c);
  }

  /**
   * @return the name of the array element type for an array
   */
  public TypeName parseForArrayElementName() {
    int newDim;
    if ((key.dim&ElementMask) == PrimitiveMask) {
      int tmpDim = key.dim>>(2*ElementBits);
      if (tmpDim == 0) {
        newDim = -1;
      } else {
        newDim = (tmpDim<<ElementBits) | PrimitiveMask;
      }
    } else {
      newDim = key.dim>>ElementBits;
    }
  
    return findOrCreate(key.packageName, key.className, newDim);
  }

  /**
   * @return a type name that represents an array of this element type
   */
  private TypeName getDerivedTypeForElementType(byte mask) {
    int newDim;
    if (key.dim == -1) {
      newDim = mask<<ElementBits | PrimitiveMask;
    } else if ((key.dim & ElementMask) == PrimitiveMask) {
      newDim = (((key.dim & ~ElementMask) | mask)<<ElementBits) | PrimitiveMask;
    } else {
      newDim = key.dim<<ElementBits | mask;
    }
 
    return findOrCreate(key.packageName, key.className, newDim);
  }

  public TypeName getArrayTypeForElementType() {
    return getDerivedTypeForElementType(ArrayMask);
  }
  public TypeName getPointerTypeForElementType() {
    return getDerivedTypeForElementType(PointerMask);
  }
  public TypeName getReferenceTypeForElementType() {
    return getDerivedTypeForElementType(ReferenceMask);
  }
  
  /**
   * @return the dimensionality of the type. By convention, class types have dimensionality 0, primitives -1, and arrays the number
   *         of [ in their descriptor.
   */
  public final int getDerivedMask() {
    return key.dim;
  }

  /**
   * Does 'this' refer to a class?
   */
  public final boolean isClassType() {
    return key.dim == 0;
  }

  /**
   * Does 'this' refer to an array?
   */
  public final boolean isArrayType() {
    return key.dim > 0;
  }

  /**
   * Does 'this' refer to a primitive type
   */
  public final boolean isPrimitiveType() {
    return key.dim == -1;
  }

  /**
   * Return the innermost element type reference for an array
   */
  public final TypeName getInnermostElementType() {
    short newDim = ((key.dim&ElementMask) == PrimitiveMask) ? (short) -1 : 0;
    return findOrCreate(key.packageName, key.className, newDim);
  }

  /**
   * A key into the dictionary; this is just like a type name, but uses value equality instead of object equality.
   */
  private final static class TypeNameKey implements Serializable {
    private static final long serialVersionUID = -8284030936836318929L;

    /**
     * The package, like "java/lang". null means the unnamed package.
     */
    private final Atom packageName;

    /**
     * The class name, like "Object" or "Z"
     */
    private final Atom className;

    /**
     * Dimensionality: -1 => primitive 
     *                  0 => class 
     *                  >0 => mask of levels of array, reference, pointer
     *                  
     *  When the mask is &gt; 0, it represents levels of type qualifiers (in C
     *  terminology) for array, reference and pointer types.  There is also a
     *  special mask for when the innermost type is a primitive.  The mask is
     *  a bitfield laid out in inverse dimension order. 
     *  
     *  For instance, a single-dimension array is simply the value ArrayMask, 
     *  padded with leading zeros.  A single-dimension array of primitives is
     *  ArrayMask<<ElementBits | PrimitiveMask.  An array of pointers to objects
     *  would be (ArrayMask<<ElementBits) | PointerMask; an array of pointers
     *  to a primitive type would have the primitive mask on the end:
     *  ((ArrayMask<<ElementBits) | PointerMask)<<ElementBits | PrimitiveMask
     *  
     */
    private final int dim;

    /**
     * This should be the only constructor
     */
    private TypeNameKey(Atom packageName, Atom className, int dim) {
      this.packageName = packageName;
      this.className = className;
      this.dim = dim;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof TypeNameKey) {
        TypeNameKey other = (TypeNameKey) obj;
        return className == other.className && packageName == other.packageName && dim == other.dim;
      } else {
        return false;
      }
    }

    /**
     * TODO: cache for efficiency?
     */
    @Override
    public int hashCode() {
      int result = className.hashCode() * 5009 + dim * 5011;
      if (packageName != null) {
        result += packageName.hashCode();
      }
      return result;
    }

    @Override
    public String toString() {
      StringBuffer result = new StringBuffer();
      toStringPrefix(result);
      
      if (packageName != null) {
        result.append(packageName.toString());
        result.append("/");
      }
      result.append(className.toString());

      return result.toString();
    }

    private void toStringPrefix(StringBuffer result) {
      boolean isPrimitive = (dim==-1) || (dim&ElementMask)==PrimitiveMask;
      if (dim != -1) {
        for (int d = (dim&ElementMask) == PrimitiveMask? dim>>ElementBits: dim; d != 0; d>>=ElementBits) {
          final int masked = d&ElementMask;
          switch (masked) {
          case ArrayMask:
            result.append("[");
            break;
          case PointerMask:
            result.append("*");
            break;
          case ReferenceMask:
            result.append("&");
            break;
          default:
            throw new UnsupportedOperationException("unexpected masked type-name component " + masked);
          }
        }
      }
      if (!isPrimitive) {
        result.append("L");
      } else if (packageName != null && isPrimitive) {
        result.append("P");        
      }
    }

    public String toUnicodeString() {
      try {
        StringBuffer result = new StringBuffer();
        toStringPrefix(result);
        
        if (packageName != null) {
          result.append(packageName.toUnicodeString());
          result.append("/");
        }
        result.append(className.toUnicodeString());

        return result.toString();
      } catch (UTFDataFormatException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
        return null;
      }
    }
  }

  /**
   * @return the Atom naming the package for this type.
   */
  public Atom getPackage() {
    return key.packageName;
  }

  /**
   * @return the Atom naming the class for this type (without package)
   */
  public Atom getClassName() {
    return key.className;
  }
}
