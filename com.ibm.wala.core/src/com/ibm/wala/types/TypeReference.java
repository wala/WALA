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

import static com.ibm.wala.types.TypeName.ArrayMask;
import static com.ibm.wala.types.TypeName.ElementBits;
import static com.ibm.wala.types.TypeName.PrimitiveMask;

import java.io.Serializable;
import java.util.Map;

import com.ibm.wala.util.collections.HashMapFactory;

/**
 * A class to represent the reference in a class file to some type (class, primitive or array). A type reference is
 * uniquely defined by
 * <ul>
 * <li> an initiating class loader
 * <li> a type name
 * </ul>
 * Resolving a TypeReference to a Type can be an expensive operation. Therefore we canonicalize TypeReference instances
 * and cache the result of resolution.
 */
public final class TypeReference implements Serializable {

  /* Serial version */
  private static final long serialVersionUID = -3256390509887654327L;  
  
  /**
   * NOTE: initialisation order is important!
   * 
   * TypeReferences are canonical.
   */

  /**
   * Used for fast access to primitives. Primitives appear in the main dictionary also.
   */
  private final static Map<TypeName, TypeReference> primitiveMap = HashMapFactory.make();

  /**
   * Used to canonicalize TypeReferences.
   */
  private final static Map<Key, TypeReference> dictionary = HashMapFactory.make();

  /*********************************************************************************************************************
   * Primitive Dispatch *
   ********************************************************************************************************************/

  public final static TypeName BooleanName = TypeName.string2TypeName("Z");

  public final static byte BooleanTypeCode = 'Z';

  public final static TypeReference Boolean = makePrimitive(BooleanName);

  public final static TypeName ByteName = TypeName.string2TypeName("B");

  public final static byte ByteTypeCode = 'B';

  public final static TypeReference Byte = makePrimitive(ByteName);

  public final static TypeName CharName = TypeName.string2TypeName("C");

  public final static byte CharTypeCode = 'C';

  public final static TypeReference Char = makePrimitive(CharName);

  public final static TypeName DoubleName = TypeName.string2TypeName("D");

  public final static byte DoubleTypeCode = 'D';

  public final static TypeReference Double = makePrimitive(DoubleName);

  public final static TypeName FloatName = TypeName.string2TypeName("F");

  public final static byte FloatTypeCode = 'F';

  public final static TypeReference Float = makePrimitive(FloatName);

  public final static TypeName IntName = TypeName.string2TypeName("I");

  public final static byte IntTypeCode = 'I';

  public final static TypeReference Int = makePrimitive(IntName);

  public final static TypeName LongName = TypeName.string2TypeName("J");

  public final static byte LongTypeCode = 'J';

  public final static TypeReference Long = makePrimitive(LongName);

  public final static TypeName ShortName = TypeName.string2TypeName("S");

  public final static byte ShortTypeCode = 'S';

  public final static TypeReference Short = makePrimitive(ShortName);

  public final static TypeName VoidName = TypeName.string2TypeName("V");

  public final static byte VoidTypeCode = 'V';

  public final static TypeReference Void = makePrimitive(VoidName);

  public final static byte OtherPrimitiveTypeCode = 'P';
  
  /*********************************************************************************************************************
   * Primitive Array Dispatch *
   ********************************************************************************************************************/

  public final static TypeReference BooleanArray = findOrCreateArrayOf(Boolean);

  public final static TypeReference ByteArray = findOrCreateArrayOf(Byte);

  public final static TypeReference CharArray = findOrCreateArrayOf(Char);

  public final static TypeReference DoubleArray = findOrCreateArrayOf(Double);

  public final static TypeReference FloatArray = findOrCreateArrayOf(Float);

  public final static TypeReference IntArray = findOrCreateArrayOf(Int);

  public final static TypeReference LongArray = findOrCreateArrayOf(Long);

  public final static TypeReference ShortArray = findOrCreateArrayOf(Short);

  /*********************************************************************************************************************
   * Special object types *
   ********************************************************************************************************************/

  private final static TypeName JavaLangArithmeticExceptionName = TypeName.string2TypeName("Ljava/lang/ArithmeticException");

  public final static TypeReference JavaLangArithmeticException = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangArithmeticExceptionName);

  private final static TypeName JavaLangArrayStoreExceptionName = TypeName.string2TypeName("Ljava/lang/ArrayStoreException");

  public final static TypeReference JavaLangArrayStoreException = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangArrayStoreExceptionName);

  private final static TypeName JavaLangArrayIndexOutOfBoundsExceptionName = TypeName
      .string2TypeName("Ljava/lang/ArrayIndexOutOfBoundsException");

  public final static TypeReference JavaLangArrayIndexOutOfBoundsException = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangArrayIndexOutOfBoundsExceptionName);

  private final static TypeName JavaLangClassName = TypeName.string2TypeName("Ljava/lang/Class");

  public final static TypeReference JavaLangClass = findOrCreate(ClassLoaderReference.Primordial, JavaLangClassName);

  private final static TypeName JavaLangInvokeMethodHandleName = TypeName.string2TypeName("Ljava/lang/invoke/MethodHandle");

  public final static TypeReference JavaLangInvokeMethodHandle = findOrCreate(ClassLoaderReference.Primordial, JavaLangInvokeMethodHandleName);

  private final static TypeName JavaLangInvokeMethodTypeName = TypeName.string2TypeName("Ljava/lang/invoke/MethodType");

  public final static TypeReference JavaLangInvokeMethodType = findOrCreate(ClassLoaderReference.Primordial, JavaLangInvokeMethodTypeName);

  private final static TypeName JavaLangClassCastExceptionName = TypeName.string2TypeName("Ljava/lang/ClassCastException");

  public final static TypeReference JavaLangClassCastException = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangClassCastExceptionName);

  private final static TypeName JavaLangComparableName = TypeName.string2TypeName("Ljava/lang/Comparable");

  public final static TypeReference JavaLangComparable = findOrCreate(ClassLoaderReference.Primordial, JavaLangComparableName);

  private final static TypeName JavaLangReflectConstructorName = TypeName.string2TypeName("Ljava/lang/reflect/Constructor");

  public final static TypeReference JavaLangReflectConstructor = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangReflectConstructorName);
  
  private final static TypeName JavaLangReflectMethodName = TypeName.string2TypeName("Ljava/lang/reflect/Method");

  public final static TypeReference JavaLangReflectMethod = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangReflectMethodName);

  private final static TypeName JavaLangEnumName = TypeName.string2TypeName("Ljava/lang/Enum");

  public final static TypeReference JavaLangEnum = findOrCreate(ClassLoaderReference.Primordial, JavaLangEnumName);
  
  private final static TypeName JavaLangErrorName = TypeName.string2TypeName("Ljava/lang/Error");

  public final static TypeReference JavaLangError = findOrCreate(ClassLoaderReference.Primordial, JavaLangErrorName);

  private final static TypeName JavaLangExceptionName = TypeName.string2TypeName("Ljava/lang/Exception");

  public final static TypeReference JavaLangException = findOrCreate(ClassLoaderReference.Primordial, JavaLangExceptionName);

  private final static TypeName JavaLangNegativeArraySizeExceptionName = TypeName
      .string2TypeName("Ljava/lang/NegativeArraySizeException");

  public final static TypeReference JavaLangNegativeArraySizeException = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangNegativeArraySizeExceptionName);

  private final static TypeName JavaLangNullPointerExceptionName = TypeName.string2TypeName("Ljava/lang/NullPointerException");

  public final static TypeReference JavaLangNullPointerException = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangNullPointerExceptionName);

  private final static TypeName JavaLangRuntimeExceptionName = TypeName.string2TypeName("Ljava/lang/RuntimeException");

  public final static TypeReference JavaLangRuntimeException = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangRuntimeExceptionName);

  private final static TypeName JavaLangClassNotFoundExceptionName = TypeName.string2TypeName("Ljava/lang/ClassNotFoundException");

  public final static TypeReference JavaLangClassNotFoundException = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangClassNotFoundExceptionName);

  private final static TypeName JavaLangOutOfMemoryErrorName = TypeName.string2TypeName("Ljava/lang/OutOfMemoryError");

  public final static TypeReference JavaLangOutOfMemoryError = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangOutOfMemoryErrorName);

  private final static TypeName JavaLangExceptionInInitializerErrorName = TypeName
      .string2TypeName("Ljava/lang/ExceptionInInitializerError");

  public final static TypeReference JavaLangExceptionInInitializerError = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangExceptionInInitializerErrorName);

  private final static TypeName JavaLangObjectName = TypeName.string2TypeName("Ljava/lang/Object");

  public final static TypeReference JavaLangObject = findOrCreate(ClassLoaderReference.Primordial, JavaLangObjectName);

  private final static TypeName JavaLangStackTraceElementName = TypeName.string2TypeName("Ljava/lang/StackTraceElement");

  public final static TypeReference JavaLangStackTraceElement = findOrCreate(ClassLoaderReference.Primordial,
      JavaLangStackTraceElementName);

  private final static TypeName JavaLangStringName = TypeName.string2TypeName("Ljava/lang/String");

  public final static TypeReference JavaLangString = findOrCreate(ClassLoaderReference.Primordial, JavaLangStringName);

  private final static TypeName JavaLangStringBufferName = TypeName.string2TypeName("Ljava/lang/StringBuffer");

  public final static TypeReference JavaLangStringBuffer = findOrCreate(ClassLoaderReference.Primordial, JavaLangStringBufferName);

  private final static TypeName JavaLangStringBuilderName = TypeName.string2TypeName("Ljava/lang/StringBuilder");

  public final static TypeReference JavaLangStringBuilder = findOrCreate(ClassLoaderReference.Primordial, JavaLangStringBuilderName);

  private final static TypeName JavaLangThreadName = TypeName.string2TypeName("Ljava/lang/Thread");

  public final static TypeReference JavaLangThread = findOrCreate(ClassLoaderReference.Primordial, JavaLangThreadName);

  private final static TypeName JavaLangThrowableName = TypeName.string2TypeName("Ljava/lang/Throwable");

  public final static TypeReference JavaLangThrowable = findOrCreate(ClassLoaderReference.Primordial, JavaLangThrowableName);

  public final static TypeName JavaLangCloneableName = TypeName.string2TypeName("Ljava/lang/Cloneable");

  public final static TypeReference JavaLangCloneable = findOrCreate(ClassLoaderReference.Primordial, JavaLangCloneableName);

  private final static TypeName JavaLangSystemName = TypeName.string2TypeName("Ljava/lang/System");

  public final static TypeReference JavaLangSystem = findOrCreate(ClassLoaderReference.Primordial, JavaLangSystemName);

  private final static TypeName JavaLangIntegerName = TypeName.string2TypeName("Ljava/lang/Integer");

  public final static TypeReference JavaLangInteger = findOrCreate(ClassLoaderReference.Primordial, JavaLangIntegerName);
  
  private final static TypeName JavaLangBooleanName = TypeName.string2TypeName("Ljava/lang/Boolean");

  public final static TypeReference JavaLangBoolean = findOrCreate(ClassLoaderReference.Primordial, JavaLangBooleanName);
  
  private final static TypeName JavaLangDoubleName = TypeName.string2TypeName("Ljava/lang/Double");

  public final static TypeReference JavaLangDouble = findOrCreate(ClassLoaderReference.Primordial, JavaLangDoubleName);
  
  private final static TypeName JavaLangFloatName = TypeName.string2TypeName("Ljava/lang/Float");

  public final static TypeReference JavaLangFloat = findOrCreate(ClassLoaderReference.Primordial, JavaLangFloatName);

  private final static TypeName JavaLangShortName = TypeName.string2TypeName("Ljava/lang/Short");

  public final static TypeReference JavaLangShort = findOrCreate(ClassLoaderReference.Primordial, JavaLangShortName);
  
  private final static TypeName JavaLangLongName = TypeName.string2TypeName("Ljava/lang/Long");

  public final static TypeReference JavaLangLong = findOrCreate(ClassLoaderReference.Primordial, JavaLangLongName);
  
  private final static TypeName JavaLangByteName = TypeName.string2TypeName("Ljava/lang/Byte");

  public final static TypeReference JavaLangByte = findOrCreate(ClassLoaderReference.Primordial, JavaLangByteName);
  
  private final static TypeName JavaLangCharacterName = TypeName.string2TypeName("Ljava/lang/Character");

  public final static TypeReference JavaLangCharacter = findOrCreate(ClassLoaderReference.Primordial, JavaLangCharacterName);
  
  public final static TypeName JavaIoSerializableName = TypeName.string2TypeName("Ljava/io/Serializable");

  public final static TypeReference JavaIoSerializable = findOrCreate(ClassLoaderReference.Primordial, JavaIoSerializableName);

  private final static TypeName JavaUtilCollectionName = TypeName.string2TypeName("Ljava/util/Collection");

  public final static TypeReference JavaUtilCollection = findOrCreate(ClassLoaderReference.Primordial, JavaUtilCollectionName);

  private final static TypeName JavaUtilMapName = TypeName.string2TypeName("Ljava/util/Map");

  public final static TypeReference JavaUtilMap = findOrCreate(ClassLoaderReference.Primordial, JavaUtilMapName);

  private final static TypeName JavaUtilHashSetName = TypeName.string2TypeName("Ljava/util/HashSet");

  public final static TypeReference JavaUtilHashSet = findOrCreate(ClassLoaderReference.Primordial, JavaUtilHashSetName);

  private final static TypeName JavaUtilSetName = TypeName.string2TypeName("Ljava/util/Set");

  public final static TypeReference JavaUtilSet = findOrCreate(ClassLoaderReference.Primordial, JavaUtilSetName);

  private final static TypeName JavaUtilEnumName = TypeName.string2TypeName("Ljava/util/Enumeration");

  public final static TypeReference JavaUtilEnum = findOrCreate(ClassLoaderReference.Primordial, JavaUtilEnumName);

  private final static TypeName JavaUtilIteratorName = TypeName.string2TypeName("Ljava/util/Iterator");

  public final static TypeReference JavaUtilIterator = findOrCreate(ClassLoaderReference.Primordial, JavaUtilIteratorName);

  private final static TypeName JavaUtilVectorName = TypeName.string2TypeName("Ljava/util/Vector");

  public final static TypeReference JavaUtilVector = findOrCreate(ClassLoaderReference.Primordial, JavaUtilVectorName);

  public final static byte ClassTypeCode = 'L';

  public final static byte ArrayTypeCode = '[';

  public final static byte PointerTypeCode = '*';

  public final static byte ReferenceTypeCode = '&';

  // TODO! the following two are unsound hacks; kill them.
  final static TypeName NullName = TypeName.string2TypeName("null");

  public final static TypeReference Null = findOrCreate(ClassLoaderReference.Primordial, NullName);

  // TODO: is the following necessary. Used only by ShrikeBT.
  final static TypeName UnknownName = TypeName.string2TypeName("?unknown?");

  public final static TypeReference Unknown = findOrCreate(ClassLoaderReference.Primordial, UnknownName);

  public final static TypeReference LambdaMetaFactory = findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/invoke/LambdaMetafactory");
  
  private static TypeReference makePrimitive(TypeName n) {
    return makePrimitive(ClassLoaderReference.Primordial, n);
  }

  public static TypeReference makePrimitive(ClassLoaderReference cl, TypeName n) {
    TypeReference t = new TypeReference(cl, n);
    primitiveMap.put(t.name, t);
    return t;
  }

  /**
   * Could name a represent a primitive type?
   */
  public static boolean isPrimitiveType(TypeName name) {
    return name.isPrimitiveType();
  }

  /**
   * The initiating class loader
   */
  private final ClassLoaderReference classloader;

  /**
   * The type name
   */
  private final TypeName name;

  /**
   * Find or create the canonical TypeReference instance for the given pair.
   * 
   * @param cl the classloader (defining/initiating depending on usage)
   */
  public static synchronized TypeReference findOrCreate(ClassLoaderReference cl, TypeName typeName) {

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
    if (val != null) {
      return val;
    } else {
      val = new TypeReference(cl, typeName);
      dictionary.put(key, val);
      return val;
    }
  }

  /**
   * Find or create the canonical {@link TypeReference} instance for the given pair.
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
  protected TypeReference(ClassLoaderReference cl, TypeName tn) {
    classloader = cl;
    name = tn;
  }

  /**
   * @return the classloader component of this type reference
   */
  public final ClassLoaderReference getClassLoader() {
    return classloader;
  }

  /**
   * @return the type name component of this type reference
   */
  public final TypeName getName() {
    return name;
  }

  /**
   * TODO: specialized form of TypeReference for arrays, please. Get the element type of for this array type.
   */
  public final TypeReference getArrayElementType() {
    TypeName element = name.parseForArrayElementName();
    return findOrCreate(classloader, element);
  }

  /**
   * Get array type corresponding to "this" array element type.
   */
  public final TypeReference getArrayTypeForElementType() {
    return findOrCreate(classloader, name.getArrayTypeForElementType());
  }

  /**
   * Return the dimensionality of the type. By convention, class types have dimensionality 0, primitives -1, and arrays
   * the number of [ in their descriptor.
   */
  public final int getDerivedMask() {
    return name.getDerivedMask();
  }

  /**
   * Return the innermost element type reference for an array
   */
  public final TypeReference getInnermostElementType() {
    return findOrCreate(classloader, name.getInnermostElementType());
  }

  /**
   * Does 'this' refer to a class?
   */
  public final boolean isClassType() {
    return !isArrayType() && !isPrimitiveType();
  }

  /**
   * Does 'this' refer to an array?
   */
  public final boolean isArrayType() {
    return name.isArrayType();
  }

  /**
   * Does 'this' refer to a primitive type
   */
  public final boolean isPrimitiveType() {
    return isPrimitiveType(name);
  }

  /**
   * Does 'this' refer to a reference type
   */
  public final boolean isReferenceType() {
    return !isPrimitiveType();
  }

  @Override
  public final int hashCode() {
    return name.hashCode();
  }

  /**
   * TypeReferences are canonical. However, note that two TypeReferences can be non-equal, yet still represent the same
   * IClass.
   * 
   * For example, the there can be two TypeReferences &lt;Application,java.lang.Object&gt; and &lt;Primordial,java.lang.Object&gt;.
   * These two TypeReference are <b>NOT</b> equal(), but they both represent the IClass which is named
   * &lt;Primordial,java.lang.Object&gt;
   */
  @Override
  public final boolean equals(Object other) {
    return (this == other);
  }

  @Override
  public final String toString() {
    return "<" + classloader.getName() + "," + name + ">";
  }

  public static TypeReference findOrCreateClass(ClassLoaderReference loader, String packageName, String className) {
    TypeName tn = TypeName.findOrCreateClassName(packageName, className);
    return findOrCreate(loader, tn);
  }

  private static class Key {
    /**
     * The initiating class loader
     */
    private final ClassLoaderReference classloader;

    /**
     * The type name
     */
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
    if ((mask&PrimitiveMask) == PrimitiveMask) {
      mask >>= ElementBits;
    }
    int dims = 0;
    while ((mask&ArrayMask) == ArrayMask) {
      mask >>= ElementBits;
      dims++;
    }
    assert dims>0;
    return dims;
  }

}
