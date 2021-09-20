/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.types;

import static com.ibm.wala.types.TypeName.ArrayMask;
import static com.ibm.wala.types.TypeName.ElementBits;
import static com.ibm.wala.types.TypeName.PrimitiveMask;

import com.ibm.wala.util.collections.HashMapFactory;
import java.io.Serializable;
import java.util.Map;

/**
 * A class to represent the reference in a class file to some type (class, primitive or array). A
 * type reference is uniquely defined by
 *
 * <ul>
 *   <li>an initiating class loader
 *   <li>a type name
 * </ul>
 *
 * Resolving a TypeReference to a Type can be an expensive operation. Therefore we canonicalize
 * TypeReference instances and cache the result of resolution.
 */
public final class TypeReference implements Serializable {

  /* Serial version */
  private static final long serialVersionUID = -3256390509887654327L;

  /*
   * NOTE: initialisation order is important!
   *
   * <p>TypeReferences are canonical.
   */

  /** Used for fast access to primitives. Primitives appear in the main dictionary also. */
  private static final Map<TypeName, TypeReference> primitiveMap = HashMapFactory.make();

  /** Used to canonicalize TypeReferences. */
  private static final Map<Key, TypeReference> dictionary = HashMapFactory.make();

  /*
   * Primitive Dispatch *
   */

  public static final TypeName BooleanName = TypeName.string2TypeName("Z");

  public static final byte BooleanTypeCode = 'Z';

  public static final TypeReference Boolean = makePrimitive(BooleanName);

  public static final TypeName ByteName = TypeName.string2TypeName("B");

  public static final byte ByteTypeCode = 'B';

  public static final TypeReference Byte = makePrimitive(ByteName);

  public static final TypeName CharName = TypeName.string2TypeName("C");

  public static final byte CharTypeCode = 'C';

  public static final TypeReference Char = makePrimitive(CharName);

  public static final TypeName DoubleName = TypeName.string2TypeName("D");

  public static final byte DoubleTypeCode = 'D';

  public static final TypeReference Double = makePrimitive(DoubleName);

  public static final TypeName FloatName = TypeName.string2TypeName("F");

  public static final byte FloatTypeCode = 'F';

  public static final TypeReference Float = makePrimitive(FloatName);

  public static final TypeName IntName = TypeName.string2TypeName("I");

  public static final byte IntTypeCode = 'I';

  public static final TypeReference Int = makePrimitive(IntName);

  public static final TypeName LongName = TypeName.string2TypeName("J");

  public static final byte LongTypeCode = 'J';

  public static final TypeReference Long = makePrimitive(LongName);

  public static final TypeName ShortName = TypeName.string2TypeName("S");

  public static final byte ShortTypeCode = 'S';

  public static final TypeReference Short = makePrimitive(ShortName);

  public static final TypeName VoidName = TypeName.string2TypeName("V");

  public static final byte VoidTypeCode = 'V';

  public static final TypeReference Void = makePrimitive(VoidName);

  public static final byte OtherPrimitiveTypeCode = 'P';

  /*
   * Primitive Array Dispatch *
   */

  public static final TypeReference BooleanArray = findOrCreateArrayOf(Boolean);

  public static final TypeReference ByteArray = findOrCreateArrayOf(Byte);

  public static final TypeReference CharArray = findOrCreateArrayOf(Char);

  public static final TypeReference DoubleArray = findOrCreateArrayOf(Double);

  public static final TypeReference FloatArray = findOrCreateArrayOf(Float);

  public static final TypeReference IntArray = findOrCreateArrayOf(Int);

  public static final TypeReference LongArray = findOrCreateArrayOf(Long);

  public static final TypeReference ShortArray = findOrCreateArrayOf(Short);

  /*
   * Special object types *
   */

  private static final TypeName JavaLangArithmeticExceptionName =
      TypeName.string2TypeName("Ljava/lang/ArithmeticException");

  public static final TypeReference JavaLangArithmeticException =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangArithmeticExceptionName);

  private static final TypeName JavaLangArrayStoreExceptionName =
      TypeName.string2TypeName("Ljava/lang/ArrayStoreException");

  public static final TypeReference JavaLangArrayStoreException =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangArrayStoreExceptionName);

  private static final TypeName JavaLangArrayIndexOutOfBoundsExceptionName =
      TypeName.string2TypeName("Ljava/lang/ArrayIndexOutOfBoundsException");

  public static final TypeReference JavaLangArrayIndexOutOfBoundsException =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangArrayIndexOutOfBoundsExceptionName);

  private static final TypeName JavaLangClassName = TypeName.string2TypeName("Ljava/lang/Class");

  public static final TypeReference JavaLangClass =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangClassName);

  private static final TypeName JavaLangInvokeMethodHandleName =
      TypeName.string2TypeName("Ljava/lang/invoke/MethodHandle");

  public static final TypeReference JavaLangInvokeMethodHandle =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangInvokeMethodHandleName);

  private static final TypeName JavaLangInvokeMethodHandlesLookupName =
      TypeName.string2TypeName("Ljava/lang/invoke/MethodHandles$Lookup");

  public static final TypeReference JavaLangInvokeMethodHandlesLookup =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangInvokeMethodHandlesLookupName);

  private static final TypeName JavaLangInvokeMethodTypeName =
      TypeName.string2TypeName("Ljava/lang/invoke/MethodType");

  public static final TypeReference JavaLangInvokeMethodType =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangInvokeMethodTypeName);

  private static final TypeName JavaLangClassCastExceptionName =
      TypeName.string2TypeName("Ljava/lang/ClassCastException");

  public static final TypeReference JavaLangClassCastException =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangClassCastExceptionName);

  private static final TypeName JavaLangComparableName =
      TypeName.string2TypeName("Ljava/lang/Comparable");

  public static final TypeReference JavaLangComparable =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangComparableName);

  private static final TypeName JavaLangReflectConstructorName =
      TypeName.string2TypeName("Ljava/lang/reflect/Constructor");

  public static final TypeReference JavaLangReflectConstructor =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangReflectConstructorName);

  private static final TypeName JavaLangReflectMethodName =
      TypeName.string2TypeName("Ljava/lang/reflect/Method");

  public static final TypeReference JavaLangReflectMethod =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangReflectMethodName);

  private static final TypeName JavaLangEnumName = TypeName.string2TypeName("Ljava/lang/Enum");

  public static final TypeReference JavaLangEnum =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangEnumName);

  private static final TypeName JavaLangErrorName = TypeName.string2TypeName("Ljava/lang/Error");

  public static final TypeReference JavaLangError =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangErrorName);

  private static final TypeName JavaLangExceptionName =
      TypeName.string2TypeName("Ljava/lang/Exception");

  public static final TypeReference JavaLangException =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangExceptionName);

  private static final TypeName JavaLangNegativeArraySizeExceptionName =
      TypeName.string2TypeName("Ljava/lang/NegativeArraySizeException");

  public static final TypeReference JavaLangNegativeArraySizeException =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangNegativeArraySizeExceptionName);

  private static final TypeName JavaLangNullPointerExceptionName =
      TypeName.string2TypeName("Ljava/lang/NullPointerException");

  public static final TypeReference JavaLangNullPointerException =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangNullPointerExceptionName);

  private static final TypeName JavaLangRuntimeExceptionName =
      TypeName.string2TypeName("Ljava/lang/RuntimeException");

  public static final TypeReference JavaLangRuntimeException =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangRuntimeExceptionName);

  private static final TypeName JavaLangClassNotFoundExceptionName =
      TypeName.string2TypeName("Ljava/lang/ClassNotFoundException");

  public static final TypeReference JavaLangClassNotFoundException =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangClassNotFoundExceptionName);

  private static final TypeName JavaLangOutOfMemoryErrorName =
      TypeName.string2TypeName("Ljava/lang/OutOfMemoryError");

  public static final TypeReference JavaLangOutOfMemoryError =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangOutOfMemoryErrorName);

  private static final TypeName JavaLangExceptionInInitializerErrorName =
      TypeName.string2TypeName("Ljava/lang/ExceptionInInitializerError");

  public static final TypeReference JavaLangExceptionInInitializerError =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangExceptionInInitializerErrorName);

  private static final TypeName JavaLangObjectName = TypeName.string2TypeName("Ljava/lang/Object");

  public static final TypeReference JavaLangObject =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangObjectName);

  private static final TypeName JavaLangStackTraceElementName =
      TypeName.string2TypeName("Ljava/lang/StackTraceElement");

  public static final TypeReference JavaLangStackTraceElement =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangStackTraceElementName);

  private static final TypeName JavaLangStringName = TypeName.string2TypeName("Ljava/lang/String");

  public static final TypeReference JavaLangString =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangStringName);

  private static final TypeName JavaLangStringBufferName =
      TypeName.string2TypeName("Ljava/lang/StringBuffer");

  public static final TypeReference JavaLangStringBuffer =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangStringBufferName);

  private static final TypeName JavaLangStringBuilderName =
      TypeName.string2TypeName("Ljava/lang/StringBuilder");

  public static final TypeReference JavaLangStringBuilder =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangStringBuilderName);

  private static final TypeName JavaLangThreadName = TypeName.string2TypeName("Ljava/lang/Thread");

  public static final TypeReference JavaLangThread =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangThreadName);

  private static final TypeName JavaLangThrowableName =
      TypeName.string2TypeName("Ljava/lang/Throwable");

  public static final TypeReference JavaLangThrowable =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangThrowableName);

  public static final TypeName JavaLangCloneableName =
      TypeName.string2TypeName("Ljava/lang/Cloneable");

  public static final TypeReference JavaLangCloneable =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangCloneableName);

  private static final TypeName JavaLangSystemName = TypeName.string2TypeName("Ljava/lang/System");

  public static final TypeReference JavaLangSystem =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangSystemName);

  private static final TypeName JavaLangIntegerName =
      TypeName.string2TypeName("Ljava/lang/Integer");

  public static final TypeReference JavaLangInteger =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangIntegerName);

  private static final TypeName JavaLangBooleanName =
      TypeName.string2TypeName("Ljava/lang/Boolean");

  public static final TypeReference JavaLangBoolean =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangBooleanName);

  private static final TypeName JavaLangDoubleName = TypeName.string2TypeName("Ljava/lang/Double");

  public static final TypeReference JavaLangDouble =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangDoubleName);

  private static final TypeName JavaLangFloatName = TypeName.string2TypeName("Ljava/lang/Float");

  public static final TypeReference JavaLangFloat =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangFloatName);

  private static final TypeName JavaLangShortName = TypeName.string2TypeName("Ljava/lang/Short");

  public static final TypeReference JavaLangShort =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangShortName);

  private static final TypeName JavaLangLongName = TypeName.string2TypeName("Ljava/lang/Long");

  public static final TypeReference JavaLangLong =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangLongName);

  private static final TypeName JavaLangByteName = TypeName.string2TypeName("Ljava/lang/Byte");

  public static final TypeReference JavaLangByte =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangByteName);

  private static final TypeName JavaLangCharacterName =
      TypeName.string2TypeName("Ljava/lang/Character");

  public static final TypeReference JavaLangCharacter =
      findOrCreate(ClassLoaderReference.Primordial, JavaLangCharacterName);

  public static final TypeName JavaIoSerializableName =
      TypeName.string2TypeName("Ljava/io/Serializable");

  public static final TypeReference JavaIoSerializable =
      findOrCreate(ClassLoaderReference.Primordial, JavaIoSerializableName);

  private static final TypeName JavaUtilCollectionName =
      TypeName.string2TypeName("Ljava/util/Collection");

  public static final TypeReference JavaUtilCollection =
      findOrCreate(ClassLoaderReference.Primordial, JavaUtilCollectionName);

  private static final TypeName JavaUtilMapName = TypeName.string2TypeName("Ljava/util/Map");

  public static final TypeReference JavaUtilMap =
      findOrCreate(ClassLoaderReference.Primordial, JavaUtilMapName);

  private static final TypeName JavaUtilHashSetName =
      TypeName.string2TypeName("Ljava/util/HashSet");

  public static final TypeReference JavaUtilHashSet =
      findOrCreate(ClassLoaderReference.Primordial, JavaUtilHashSetName);

  private static final TypeName JavaUtilSetName = TypeName.string2TypeName("Ljava/util/Set");

  public static final TypeReference JavaUtilSet =
      findOrCreate(ClassLoaderReference.Primordial, JavaUtilSetName);

  private static final TypeName JavaUtilEnumName =
      TypeName.string2TypeName("Ljava/util/Enumeration");

  public static final TypeReference JavaUtilEnum =
      findOrCreate(ClassLoaderReference.Primordial, JavaUtilEnumName);

  private static final TypeName JavaUtilIteratorName =
      TypeName.string2TypeName("Ljava/util/Iterator");

  public static final TypeReference JavaUtilIterator =
      findOrCreate(ClassLoaderReference.Primordial, JavaUtilIteratorName);

  private static final TypeName JavaUtilVectorName = TypeName.string2TypeName("Ljava/util/Vector");

  public static final TypeReference JavaUtilVector =
      findOrCreate(ClassLoaderReference.Primordial, JavaUtilVectorName);

  public static final byte ClassTypeCode = 'L';

  public static final byte ArrayTypeCode = '[';

  public static final byte PointerTypeCode = '*';

  public static final byte ReferenceTypeCode = '&';

  // TODO! the following two are unsound hacks; kill them.
  static final TypeName NullName = TypeName.string2TypeName("null");

  public static final TypeReference Null = findOrCreate(ClassLoaderReference.Primordial, NullName);

  // TODO: is the following necessary. Used only by ShrikeBT.
  static final TypeName UnknownName = TypeName.string2TypeName("?unknown?");

  public static final TypeReference Unknown =
      findOrCreate(ClassLoaderReference.Primordial, UnknownName);

  public static final TypeReference LambdaMetaFactory =
      findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/invoke/LambdaMetafactory");

  private static TypeReference makePrimitive(TypeName n) {
    return makePrimitive(ClassLoaderReference.Primordial, n);
  }

  public static TypeReference makePrimitive(ClassLoaderReference cl, TypeName n) {
    TypeReference t = new TypeReference(cl, n);
    primitiveMap.put(t.name, t);
    return t;
  }

  /** Could name a represent a primitive type? */
  public static boolean isPrimitiveType(TypeName name) {
    return name.isPrimitiveType();
  }

  /** The initiating class loader */
  private final ClassLoaderReference classloader;

  /** The type name */
  private final TypeName name;

  /**
   * Find or create the canonical TypeReference instance for the given pair.
   *
   * @param cl the classloader (defining/initiating depending on usage)
   */
  public static synchronized TypeReference findOrCreate(
      ClassLoaderReference cl, TypeName typeName) {

    if (cl == null) {
      throw new IllegalArgumentException("null cl");
    }
    TypeReference p = primitiveMap.get(typeName);
    if (p != null) {
      return p;
    }
    // Next actually findOrCreate the type reference using the proper
    // classloader.
    // [This is the only allocation site for TypeReference]
    if (typeName.isArrayType()) {
      TypeName e = typeName.getInnermostElementType();
      if (e.isPrimitiveType()) {
        cl = ClassLoaderReference.Primordial;
      }
    }

    Key key = new Key(cl, typeName);
    TypeReference val = dictionary.get(key);
    if (val == null) {
      val = new TypeReference(cl, typeName);
      dictionary.put(key, val);
    }
    return val;
  }

  /**
   * Find or create the canonical {@code TypeReference} instance for the given pair.
   *
   * @param cl the classloader (defining/initiating depending on usage)
   * @param typeName something like "Ljava/util/Arrays"
   */
  public static synchronized TypeReference findOrCreate(ClassLoaderReference cl, String typeName) {
    return findOrCreate(cl, TypeName.string2TypeName(typeName));
  }

  public static synchronized TypeReference find(ClassLoaderReference cl, String typeName) {
    return find(cl, TypeName.string2TypeName(typeName));
  }

  /**
   * Find the canonical TypeReference instance for the given pair. May return null.
   *
   * @param cl the classloader (defining/initiating depending on usage)
   */
  public static synchronized TypeReference find(ClassLoaderReference cl, TypeName typeName) {
    if (cl == null) {
      throw new IllegalArgumentException("null cl");
    }
    TypeReference p = primitiveMap.get(typeName);
    if (p != null) {
      return p;
    }
    // Next actually findOrCreate the type reference using the proper
    // classloader.
    // [This is the only allocation site for TypeReference]
    if (typeName.isArrayType()) {
      TypeName e = typeName.getInnermostElementType();
      if (e.isPrimitiveType()) {
        cl = ClassLoaderReference.Primordial;
      }
    }

    Key key = new Key(cl, typeName);
    TypeReference val = dictionary.get(key);

    return val;
  }

  public static TypeReference findOrCreateArrayOf(TypeReference t) {
    if (t == null) {
      throw new IllegalArgumentException("t is null");
    }
    TypeName name = t.getName();
    if (t.isPrimitiveType()) {
      return findOrCreate(ClassLoaderReference.Primordial, name.getArrayTypeForElementType());
    } else {
      return findOrCreate(t.getClassLoader(), name.getArrayTypeForElementType());
    }
  }

  public static TypeReference findOrCreateReferenceTo(TypeReference t) {
    if (t == null) {
      throw new IllegalArgumentException("t is null");
    }
    TypeName name = t.getName();
    if (t.isPrimitiveType()) {
      return findOrCreate(ClassLoaderReference.Primordial, name.getReferenceTypeForElementType());
    } else {
      return findOrCreate(t.getClassLoader(), name.getReferenceTypeForElementType());
    }
  }

  public static TypeReference findOrCreatePointerTo(TypeReference t) {
    if (t == null) {
      throw new IllegalArgumentException("t is null");
    }
    TypeName name = t.getName();
    if (t.isPrimitiveType()) {
      return findOrCreate(ClassLoaderReference.Primordial, name.getPointerTypeForElementType());
    } else {
      return findOrCreate(t.getClassLoader(), name.getPointerTypeForElementType());
    }
  }

  /**
   * NB: All type names should use '/' and not '.' as a separator. eg. Ljava/lang/Class
   *
   * @param cl the classloader
   * @param tn the type name
   */
  private TypeReference(ClassLoaderReference cl, TypeName tn) {
    classloader = cl;
    name = tn;
  }

  /** @return the classloader component of this type reference */
  public ClassLoaderReference getClassLoader() {
    return classloader;
  }

  /** @return the type name component of this type reference */
  public TypeName getName() {
    return name;
  }

  /**
   * TODO: specialized form of TypeReference for arrays, please. Get the element type of for this
   * array type.
   */
  public TypeReference getArrayElementType() {
    TypeName element = name.parseForArrayElementName();
    return findOrCreate(classloader, element);
  }

  /** Get array type corresponding to "this" array element type. */
  public TypeReference getArrayTypeForElementType() {
    return findOrCreate(classloader, name.getArrayTypeForElementType());
  }

  /**
   * Return the dimensionality of the type. By convention, class types have dimensionality 0,
   * primitives -1, and arrays the number of [ in their descriptor.
   */
  public int getDerivedMask() {
    return name.getDerivedMask();
  }

  /** Return the innermost element type reference for an array */
  public TypeReference getInnermostElementType() {
    return findOrCreate(classloader, name.getInnermostElementType());
  }

  /** Does 'this' refer to a class? */
  public boolean isClassType() {
    return !isArrayType() && !isPrimitiveType();
  }

  /** Does 'this' refer to an array? */
  public boolean isArrayType() {
    return name.isArrayType();
  }

  /** Does 'this' refer to a primitive type */
  public boolean isPrimitiveType() {
    return isPrimitiveType(name);
  }

  /** Does 'this' refer to a reference type */
  public boolean isReferenceType() {
    return !isPrimitiveType();
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  /**
   * TypeReferences are canonical. However, note that two TypeReferences can be non-equal, yet still
   * represent the same IClass.
   *
   * <p>For example, the there can be two TypeReferences &lt;Application,java.lang.Object&gt; and
   * &lt;Primordial,java.lang.Object&gt;. These two TypeReference are <b>NOT</b> equal(), but they
   * both represent the IClass which is named &lt;Primordial,java.lang.Object&gt;
   */
  @Override
  public boolean equals(Object other) {
    return (this == other);
  }

  @Override
  public String toString() {
    return "<" + classloader.getName() + ',' + name + '>';
  }

  public static TypeReference findOrCreateClass(
      ClassLoaderReference loader, String packageName, String className) {
    TypeName tn = TypeName.findOrCreateClassName(packageName, className);
    return findOrCreate(loader, tn);
  }

  private static class Key {
    /** The initiating class loader */
    private final ClassLoaderReference classloader;

    /** The type name */
    private final TypeName name;

    Key(ClassLoaderReference classloader, TypeName name) {
      this.classloader = classloader;
      this.name = name;
    }

    @Override
    public final int hashCode() {
      return name.hashCode();
    }

    @Override
    public final boolean equals(Object other) {
      assert other != null && other instanceof Key;
      Key that = (Key) other;
      return (name.equals(that.name) && classloader.equals(that.classloader));
    }
  }

  public int getDimensionality() {
    assert isArrayType();

    int mask = getDerivedMask();
    if ((mask & PrimitiveMask) == PrimitiveMask) {
      mask >>= ElementBits;
    }
    int dims = 0;
    while ((mask & ArrayMask) == ArrayMask) {
      mask >>= ElementBits;
      dims++;
    }
    assert dims > 0;
    return dims;
  }
}
