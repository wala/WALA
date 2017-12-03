/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Martin Hecker, KIT - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeCT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

/**
 * This class reads TypeAnnotations attributes, i.e.: RuntimeInvisibleTypeAnnotations and RuntimeVisibleTypeAnnotations
 * 
 * @author Martin Hecker martin.hecker@kit.edu
 */
public class TypeAnnotationsReader extends AnnotationsReader {

  /** 
   * required for {@link TypeAnnotationLocation#method_info} readers
   */
  private final ExceptionsReader exceptionReader;
  
  
  /**
   * required for {@link TypeAnnotationLocation#Code} readers
   */
  private final CodeReader codeReader;
  
  /** 
   * required for {@link TypeAnnotationLocation#method_info} 
   * and for  {@link TypeAnnotationLocation#ClassFile} readers
   */
  private final SignatureReader signatureReader;
  
  private final TypeAnnotationLocation location;
  
  protected TypeAnnotationsReader(
      ClassReader.AttrIterator iter,
      String label,
      ExceptionsReader exceptionReader,
      CodeReader codeReader,
      SignatureReader signatureReader,
      TypeAnnotationLocation location
    ) throws InvalidClassFileException {
    super(iter, label);
    this.exceptionReader = exceptionReader;
    this.codeReader = codeReader;
    this.signatureReader = signatureReader;
    this.location = location;
  }
  
  
  /**
   * @return a TypeAnnotationReader for reading type annotations in the attributes table of a ClassFile structure
   */
  public static TypeAnnotationsReader getTypeAnnotationReaderAtClassfile(
      ClassReader.AttrIterator iter,
      String label,
      SignatureReader signatureReader
    ) throws InvalidClassFileException {
    return new TypeAnnotationsReader(iter, label, null, null, signatureReader, TypeAnnotationLocation.ClassFile);
  }

  /**
   * @return a TypeAnnotationReader for reading type annotations in the attributes table of a method_info structure
   */
  public static TypeAnnotationsReader getTypeAnnotationReaderAtMethodInfo(
      ClassReader.AttrIterator iter,
      String label,
      ExceptionsReader exceptionReader,
      SignatureReader signatureReader
    ) throws InvalidClassFileException {
    return new TypeAnnotationsReader(iter, label, exceptionReader, null, signatureReader, TypeAnnotationLocation.method_info);
  }

  /**
   * @return a TypeAnnotationReader for reading type annotations in the attributes table of a field_info structure
   */
  public static TypeAnnotationsReader getTypeAnnotationReaderAtFieldInfo(
      ClassReader.AttrIterator iter,
      String label
    ) throws InvalidClassFileException {
    return new TypeAnnotationsReader(iter, label, null, null, null, TypeAnnotationLocation.field_info);
  }
  
  /**
   * @return a TypeAnnotationReader for reading type annotations in the attributes table of a Code attribute
   */
  public static TypeAnnotationsReader getTypeAnnotationReaderAtCode(
      ClassReader.AttrIterator iter,
      String label,
      CodeReader codeReader
    ) throws InvalidClassFileException {
    return new TypeAnnotationsReader(iter, label, null, codeReader, null, TypeAnnotationLocation.Code);
  }
  
  
  /**
   * @return an array TypeAnnotationAttribute[] corresponding to the annotations[num_annotations] table
   * specified as:
   * <pre>
   * {@code
   * RuntimeVisibleTypeAnnotations_attribute {
   *   u2              attribute_name_index;
   *   u4              attribute_length;
   *   u2              num_annotations;
   *   type_annotation annotations[num_annotations];
   * }
   * }
   * </pre>
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20"> JLS (SE8), 4.7.20</a>
   */
  public TypeAnnotationAttribute[] getAllTypeAnnotations() throws InvalidClassFileException {
    TypeAnnotationAttribute[] result = new TypeAnnotationAttribute[getAnnotationCount()];
    int offset = beginOffset + 8; // skip attribute_name_index,
                                  // attribute_length, and num_annotations
    for (int i = 0; i < result.length; i++) {
      Pair<TypeAnnotationAttribute, Integer> attributeAndSize = getTypeAttributeAndSize(offset);
      result[i] = attributeAndSize.fst;
      offset += attributeAndSize.snd;
    }
    return result;
  }

  
  /**
   * @param begin the offset from which to read a type annotation
   * @return a Pair (a,i) such that "i" is the number of bytes read in order to construct "a", which is
   * the {@link TypeAnnotationAttribute} that corresponds to the type_annotation structure at offset begin specified as:  
   * <pre>
   * {@code
   * type_annotation {
   *   u1 target_type;
   *   union {
   *     type_parameter_target;
   *     supertype_target;
   *     type_parameter_bound_target;
   *     empty_target;
   *     method_formal_parameter_target;
   *     throws_target;
   *     localvar_target;
   *     catch_target;
   *     offset_target;
   *     type_argument_target;
   *   } target_info;
   *   type_path target_path;
   *   u2        type_index;
   *   u2        num_element_value_pairs;
   *   {   u2            element_name_index;
   *       element_value value;
   *   } element_value_pairs[num_element_value_pairs];
   * }
   * }
   * </pre> 
   * 
   */
  private Pair<TypeAnnotationAttribute, Integer> getTypeAttributeAndSize(int begin) throws InvalidClassFileException {
    TargetType target_type = TargetType.fromValue(cr.getUnsignedByte(begin));
    
    if (target_type == null) {
      throw new InvalidClassFileException(begin, "Unknown target_type: " + cr.getUnsignedByte(begin));
    }
    
    if (target_type.location != this.location) {
      throw new InvalidClassFileException(
        begin,
        target_type + " annotation found while reading " + this.location + " annotations."
                    + " Only valid at " + target_type.location
      );
    }
    
    final Pair<TypeAnnotationTarget, Integer> pAnnotationTargetAndSize =
      getTypeAnnotationTargetAndSize(begin+1, target_type.target_info); 
    
    final int type_path_offset = begin + 1 + pAnnotationTargetAndSize.snd;
    checkSize(type_path_offset, 1);
    final int path_length = cr.getUnsignedByte(type_path_offset);
    checkSize(type_path_offset + 1, 2 * path_length);
    
    ArrayList<Pair<TypePathKind, Integer>> type_path = new ArrayList<>(path_length);
    int current_path_element = type_path_offset + 1;
    for (int i = 0; i < path_length; i++) {
      TypePathKind type_path_kind = TypePathKind.fromValue(cr.getUnsignedByte(current_path_element));
      int type_argument_index = cr.getUnsignedByte(current_path_element + 1);
      type_path.add(i, Pair.make(type_path_kind, type_argument_index));
      current_path_element += 2;
    }
    
    final int annotation_begin = type_path_offset + 1 + 2*path_length;
    
    Pair<AnnotationAttribute, Integer> pAttributeAndSize = getAttributeAndSize(annotation_begin);
    
    
    return Pair.make(
      new TypeAnnotationAttribute(
        pAnnotationTargetAndSize.fst,
        pAttributeAndSize.fst,
        type_path,
        target_type
      ),
      1 + pAnnotationTargetAndSize.snd + 1 + 2*path_length + pAttributeAndSize.snd
    );
  }
  
  private Pair<TypeAnnotationTarget, Integer> getTypeAnnotationTargetAndSize(int begin, TargetInfo target_info) throws InvalidClassFileException {
    switch (target_info) {
      case type_parameter_target: {
        checkSize(begin, 1);
        return Pair.<TypeAnnotationTarget, Integer>make(new TypeParameterTarget(cr.getUnsignedByte(begin)), 1);
      }
      case supertype_target: {
        checkSize(begin, 2);
        final int interfaceIndex = cr.getUShort(begin);
        final String superType;
        if (interfaceIndex == 65535) {
          superType = cr.getSuperName();
        } else {
          superType = cr.getInterfaceName(interfaceIndex);
        }
        return Pair.<TypeAnnotationTarget, Integer>make(new SuperTypeTarget(superType), 2);
      }
      case type_parameter_bound_target: {
        checkSize(begin, 2);
        return Pair.<TypeAnnotationTarget, Integer>make(new TypeParameterBoundTarget(
          cr.getUnsignedByte(begin),
          cr.getUnsignedByte(begin+1),
          signatureReader.getSignature()
        ), 2);
      }
      case empty_target: {
        return Pair.<TypeAnnotationTarget, Integer>make(new EmptyTarget(), 0);
      }
      case formal_parameter_target: {
        checkSize(begin, 1);
        return Pair.<TypeAnnotationTarget, Integer>make(new FormalParameterTarget(cr.getUnsignedByte(begin)), 1);
      }
      case throws_target: {
        assert exceptionReader != null;
        checkSize(begin, 2);
        final int throwsIndex = cr.getUShort(begin);
        return Pair.<TypeAnnotationTarget, Integer>make(new ThrowsTarget(exceptionReader.getClasses()[throwsIndex]), 2);
      }
      /*
       * localvar_target {
       * u2 table_length;
       *   { u2 start_pc;
             u2 length;
             u2 index;
       *   } table[table_length];
       * }
       */
      case localvar_target: {
        checkSize(begin, 2);
        final int table_length = cr.getUShort(begin);
        final int offset = begin+2;
        checkSize(offset, (2+2+2)*table_length);
        int[] start_pc = new int[table_length];
        int[] length = new int[table_length];
        int[] index = new int[table_length];
        
        for (int i = 0; i < table_length; i++) {
          start_pc[i] = cr.getUShort(offset +     (2+2+2)*i);
          length[i]   = cr.getUShort(offset + 2 + (2+2+2)*i);
          index[i]    = cr.getUShort(offset + 4 + (2+2+2)*i);
        }
        return Pair.<TypeAnnotationTarget, Integer>make(new LocalVarTarget(start_pc, length, index), 2 + (2+2+2)*table_length);
      }
      case catch_target: {
        assert codeReader != null;
        checkSize(begin, 2);
        int exception_table_index = cr.getUShort(begin);
        int[] rawHandler = new int[4];
        System.arraycopy(codeReader.getRawHandlers(), exception_table_index*4, rawHandler, 0, 4);
        final String catchType = 
            rawHandler[3] == 0 ? CatchTarget.ALL_EXCEPTIONS
                               : cr.getCP().getCPClass(rawHandler[3]);
        return Pair.<TypeAnnotationTarget, Integer>make(new CatchTarget(rawHandler, catchType), 2);
      }
      case offset_target: {
        checkSize(begin, 2);
        int offset  = cr.getUShort(begin);
        return Pair.<TypeAnnotationTarget, Integer>make(new OffsetTarget(offset), 2);
      }
      case type_argument_target: {
        checkSize(begin, 3);
        int offset  = cr.getUShort(begin);
        int type_argument_index = cr.getUnsignedByte(begin);
        return Pair.<TypeAnnotationTarget, Integer>make(new TypeArgumentTarget(offset, type_argument_index), 3);
      }
      default:
        Assertions.UNREACHABLE();
        return null;
    }
  }
  
  /**
   * Enumeration of those Bytecode locations where type annotation may appear (in the corresponding attribute table).
   *  
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20"> JLS (SE8), 4.7.20</a>
   */
  public static enum TypeAnnotationLocation {
    ClassFile, method_info, field_info, Code;
  }
  
  /**
   * Possible target_type items. 
   *  
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20"> JLS (SE8), 4.7.20</a>
   */
  public static enum TargetInfo {
    type_parameter_target, supertype_target, type_parameter_bound_target, empty_target, formal_parameter_target, throws_target,
    localvar_target, catch_target, offset_target, type_argument_target
  }
  
  /**
   * Known target_types for JSR 308 Type-Annotation.
   * 
   * Constant names taken from <a href="http://types.cs.washington.edu/jsr308/">http://types.cs.washington.edu/jsr308/</a>
   * 
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20"> JLS (SE8), 4.7.20</a>
   */
  // TODO: This somewhat mirrors com.sun.tools.javac.code.TargetType, maybe just use that instead?
  public static enum TargetType {
    CLASS_TYPE_PARAMETER(                0x00, TargetInfo.type_parameter_target,       TypeAnnotationLocation.ClassFile),
    METHOD_TYPE_PARAMETER(               0x01, TargetInfo.type_parameter_target,       TypeAnnotationLocation.method_info),
    
    CLASS_EXTENDS(                       0x10, TargetInfo.supertype_target,            TypeAnnotationLocation.ClassFile),
    CLASS_TYPE_PARAMETER_BOUND(          0x11, TargetInfo.type_parameter_bound_target, TypeAnnotationLocation.ClassFile),
    METHOD_TYPE_PARAMETER_BOUND(         0x12, TargetInfo.type_parameter_bound_target, TypeAnnotationLocation.method_info),
    FIELD(                               0x13, TargetInfo.empty_target,                TypeAnnotationLocation.field_info),
    METHOD_RETURN(                       0x14, TargetInfo.empty_target,                TypeAnnotationLocation.method_info),
    METHOD_RECEIVER(                     0x15, TargetInfo.empty_target,                TypeAnnotationLocation.method_info),
    METHOD_FORMAL_PARAMETER(             0x16, TargetInfo.formal_parameter_target,     TypeAnnotationLocation.method_info),
    THROWS(                              0x17, TargetInfo.throws_target,               TypeAnnotationLocation.method_info),

    LOCAL_VARIABLE(                      0x40, TargetInfo.localvar_target,             TypeAnnotationLocation.Code),
    RESOURCE_VARIABLE(                   0x41, TargetInfo.localvar_target,             TypeAnnotationLocation.Code),
    EXCEPTION_PARAMETER(                 0x42, TargetInfo.catch_target,                TypeAnnotationLocation.Code),
    INSTANCEOF(                          0x43, TargetInfo.offset_target,               TypeAnnotationLocation.Code),
    NEW(                                 0x44, TargetInfo.offset_target,               TypeAnnotationLocation.Code),
    CONSTRUCTOR_REFERENCE(               0x45, TargetInfo.offset_target,               TypeAnnotationLocation.Code),
    METHOD_REFERENCE(                    0x46, TargetInfo.offset_target,               TypeAnnotationLocation.Code),
    CAST(                                0x47, TargetInfo.type_argument_target,        TypeAnnotationLocation.Code),
    CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT(0x48, TargetInfo.type_argument_target,        TypeAnnotationLocation.Code),
    METHOD_INVOCATION_TYPE_ARGUMENT(     0x49, TargetInfo.type_argument_target,        TypeAnnotationLocation.Code),
    CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT( 0x4A, TargetInfo.type_argument_target,        TypeAnnotationLocation.Code),
    METHOD_REFERENCE_TYPE_ARGUMENT(      0x4B, TargetInfo.type_argument_target,        TypeAnnotationLocation.Code);
    
    private static final Map<Integer, TargetType> fromValue;
    
    static {
      final TargetType[] targetTypes = TargetType.values();
      fromValue = HashMapFactory.make(targetTypes.length);
      for (TargetType targetType : targetTypes) {
        fromValue.put(targetType.target_type, targetType);
      }
    }
    
    public static TargetType fromValue(int value) {
      return fromValue.get(value);
    }
    
    public final int target_type;
    public final TargetInfo target_info;
    public final TypeAnnotationLocation location;
    TargetType(int target_type, TargetInfo target_info, TypeAnnotationLocation location) {
      if (!(0 <= target_type && target_type <= Byte.MAX_VALUE)) {
        throw new IllegalArgumentException(
          "Code may break for target_type that does not fit in a Java (signed!) byte"
        );  
      }
      
      this.target_type = target_type;
      this.target_info = target_info;
      this.location = location;
    }
    
  }
  
  /**
   * A {@link TypeAnnotationTarget} represents one of the possible target_info structure 
   * <pre>
   * {@code
   * union {
   *     type_parameter_target;
   *     supertype_target;
   *     type_parameter_bound_target;
   *     empty_target;
   *     method_formal_parameter_target;
   *     throws_target;
   *     localvar_target;
   *     catch_target;
   *     offset_target;
   *     type_argument_target;
   * } target_info;
   * }
   * </pre>
   * @author Martin Hecker martin.hecker@kit.edu  
   */
  public static abstract class TypeAnnotationTarget {
    private final TargetInfo targetInfo;
    protected TypeAnnotationTarget(TargetInfo targetInfo) {
      this.targetInfo = targetInfo;
    }
    public TargetInfo getTargetInfo() {
      return targetInfo;
    }
    
    public abstract <R> R acceptVisitor(TypeAnnotationTargetVisitor<R> visitor);
  }
  
  public static interface TypeAnnotationTargetVisitor<R> {
    R visitTypeParameterTarget(TypeParameterTarget target);
    R visitSuperTypeTarget(SuperTypeTarget target );
    R visitTypeParameterBoundTarget(TypeParameterBoundTarget target);
    R visitEmptyTarget(EmptyTarget target);
    R visitFormalParameterTarget(FormalParameterTarget target);
    R visitThrowsTarget(ThrowsTarget target);
    R visitLocalVarTarget(LocalVarTarget target);
    R visitCatchTarget(CatchTarget target);
    R visitOffsetTarget(OffsetTarget target);
    R visitTypeArgumentTarget(TypeArgumentTarget target);
  }
  
  /**
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20.1-100-A.1"> JLS (SE8), 4.7.20.1 A</a>
   */
  public static class TypeParameterTarget extends TypeAnnotationTarget {
    private final int type_parameter_index;
    public TypeParameterTarget(int type_parameter_index) {
      super(TargetInfo.type_parameter_target);
      this.type_parameter_index = type_parameter_index;
    }

    public int getIndex() {
      return type_parameter_index;
    }
    
    @Override
    public <R> R acceptVisitor(TypeAnnotationTargetVisitor<R> visitor) {
      return visitor.visitTypeParameterTarget(this);
    }
  }

  /**
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20.1-100-B.1"> JLS (SE8), 4.7.20.1 B</a>
   */
  public static class SuperTypeTarget extends TypeAnnotationTarget {
    private final String superType;
    public SuperTypeTarget(String superType) {
      super(TargetInfo.supertype_target);
      this.superType = superType;
    }

    public String getSuperType() {
      return superType;
    }
    
    @Override
    public <R> R acceptVisitor(TypeAnnotationTargetVisitor<R> visitor) {
      return visitor.visitSuperTypeTarget(this);
    }
  }
  
  /**
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20.1-100-C.1"> JLS (SE8), 4.7.20.1 C</a>
   */
  public static class TypeParameterBoundTarget extends TypeAnnotationTarget {
    private final int type_parameter_index;
    private final int bound_index;
    private final String boundSignature;
    
    public TypeParameterBoundTarget(int type_parameter_index, int bound_index, String boundSignature) {
      super(TargetInfo.type_parameter_bound_target);
      this.type_parameter_index = type_parameter_index;
      this.bound_index = bound_index;
      this.boundSignature = boundSignature;
    }

    public int getParameterIndex() {
      return type_parameter_index;
    }

    public int getBoundIndex() {
      return bound_index;
    }
    
    @Override
    public <R> R acceptVisitor(TypeAnnotationTargetVisitor<R> visitor) {
      return visitor.visitTypeParameterBoundTarget(this);
    }

    public String getBoundSignature() {
      return boundSignature;
    }
  }

  /**
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20.1-100-D.1"> JLS (SE8), 4.7.20.1 D</a>
   */
  public static class EmptyTarget extends TypeAnnotationTarget {
    public EmptyTarget() {
      super(TargetInfo.empty_target);
    }
    
    @Override
    public <R> R acceptVisitor(TypeAnnotationTargetVisitor<R> visitor) {
      return visitor.visitEmptyTarget(this);
    }
  }

  
  /**
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20.1-100-E.1"> JLS (SE8), 4.7.20.1 E</a>
   */
  public static class FormalParameterTarget  extends TypeAnnotationTarget {
    private final int formal_parameter_index;
    public FormalParameterTarget(int index) {
      super(TargetInfo.formal_parameter_target);
      this.formal_parameter_index = index;
    }

    public int getIndex() {
      return formal_parameter_index;
    }
    
    @Override
    public <R> R acceptVisitor(TypeAnnotationTargetVisitor<R> visitor) {
      return visitor.visitFormalParameterTarget(this);
    }
  }
  
  /**
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20.1-100-F.1"> JLS (SE8), 4.7.20.1 F</a>
   */
  public static class ThrowsTarget extends TypeAnnotationTarget {
    private final String throwType;
    public ThrowsTarget(String throwType) {
      super(TargetInfo.supertype_target);
      this.throwType = throwType;
    }

    public String getThrowType() {
      return throwType;
    }
    
    @Override
    public <R> R acceptVisitor(TypeAnnotationTargetVisitor<R> visitor) {
      return visitor.visitThrowsTarget(this);
    }
  }
  
  /**
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20.1-100-G.1"> JLS (SE8), 4.7.20.1 G</a>
   */
  public static class LocalVarTarget extends TypeAnnotationTarget {
    private final int[] start_pc;
    private final int[] length;
    private final int[] index;
    public LocalVarTarget(int[] start_pc, int[] length, int[] index) {
      super(TargetInfo.localvar_target);
      if (!(start_pc.length == length.length && length.length == index.length)) throw new IllegalArgumentException();
      // TODO: do we really need to copy here? Can't we trust callees not to change arrays after the fact?
      this.start_pc = Arrays.copyOf(start_pc, start_pc.length);
      this.length   = Arrays.copyOf(length, length.length);
      this.index    = Arrays.copyOf(index, index.length);
    }
    
    public int getNrOfRanges() {
      return start_pc.length;
    }
    
    public int getStartPc(int range) {
      return start_pc[range];
    }

    public int getLength(int range) {
      return length[range];
    }

    public int getIndex(int range) {
      return index[range];
    }
    
    @Override
    public <R> R acceptVisitor(TypeAnnotationTargetVisitor<R> visitor) {
      return visitor.visitLocalVarTarget(this);
    }
  }
  
  /**
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20.1-100-H.1"> JLS (SE8), 4.7.20.1 H</a>
   */
  public static class CatchTarget extends TypeAnnotationTarget {
    private final int[] rawHandler;
    private final String catchType;
    
    public static final String ALL_EXCEPTIONS = null;
    
    public CatchTarget(int[] rawHandler, String catchType) {
      super(TargetInfo.catch_target);
      this.rawHandler = rawHandler;
      this.catchType = catchType;
    }
    /**
     * @return The type-annotations targets raw handler, i.e.: a 4 tuple (startPC, endPC, catchPC, catchClassIndex)
     * @see CodeReader
     */
    public int[] getRawHandler() {
      // TODO: do we really need to copy here? Can't we trust callees not to change arrays after the fact?
      return Arrays.copyOf(rawHandler, rawHandler.length);
    }
    
    @Override
    public <R> R acceptVisitor(TypeAnnotationTargetVisitor<R> visitor) {
      return visitor.visitCatchTarget(this);
    }

    public String getCatchType() {
      return catchType;
    }
    
    public int getStartPC() {
      return rawHandler[0];
    }
    
    public int getEndPC() {
      return rawHandler[1];
    }
    
    public int getCatchPC() {
      return rawHandler[2];
    }
  }
  
  /**
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20.1-100-I.1"> JLS (SE8), 4.7.20.1 I</a>
   */
  public static class OffsetTarget extends TypeAnnotationTarget {
    private final int offset;
    
    public OffsetTarget(int offset) {
      super(TargetInfo.offset_target);
      this.offset = offset;
    }
    public int getOffset() {
      return offset;
    }
    
    @Override
    public <R> R acceptVisitor(TypeAnnotationTargetVisitor<R> visitor) {
      return visitor.visitOffsetTarget(this);
    }
  }
  
  /**
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.20.1-100-J.1"> JLS (SE8), 4.7.20.1 J</a>
   */
  public static class TypeArgumentTarget extends TypeAnnotationTarget {
    private final int offset;
    private final int type_argument_index;
    
    public TypeArgumentTarget(int offset, int type_argument_index) {
      super(TargetInfo.type_argument_target);
      this.offset = offset;
      this.type_argument_index = type_argument_index;
    }
    public int getOffset() {
      return offset;
    }
    public int getTypeArgumentIndex() {
      return type_argument_index;
    }
    
    @Override
    public <R> R acceptVisitor(TypeAnnotationTargetVisitor<R> visitor) {
      return visitor.visitTypeArgumentTarget(this);
    }
  }
  
  public static enum AnnotationType {
    RuntimeInvisibleTypeAnnotations, RuntimeVisibleTypeAnnotations
  }

  public static enum TypePathKind {
    DEEPER_IN_ARRAY(0),
    DEEPER_IN_NESTED(1),
    WILDCARD_BOUND(2),
    TYPE_ARGUMENT(3);
    
    private final int type_path_kind;
    private TypePathKind(int type_path_kind) {
      this.type_path_kind = type_path_kind;
    }
    
    private static final Map<Integer, TypePathKind> fromValue;
    
    static {
      final TypePathKind[] typePathKinds = TypePathKind.values();
      fromValue = HashMapFactory.make(typePathKinds.length);
      for (TypePathKind typePathKind : typePathKinds) {
        fromValue.put(typePathKind.type_path_kind, typePathKind);
      }
    }
    
    public static TypePathKind fromValue(int value) {
      return fromValue.get(value);
    }
  }
  
  public static final List<Pair<TypePathKind, Integer>> TYPEPATH_EMPTY = Collections.emptyList();
  
  public static class TypeAnnotationAttribute {
    public final TypeAnnotationTarget annotationTarget;
    public final AnnotationAttribute annotationAttribute;
    public final List<Pair<TypePathKind, Integer>> typePath;
    public final TargetType targetType;
    
    public TypeAnnotationAttribute(
      TypeAnnotationTarget annotationTarget,
      AnnotationAttribute annotationAttribute,
      List<Pair<TypePathKind, Integer>> typePath,
      TargetType targetType
    ) {
      this.annotationTarget = annotationTarget;
      this.annotationAttribute = annotationAttribute;
      this.typePath = Collections.unmodifiableList(typePath);
      this.targetType = targetType;
    }
  }
  
  public static boolean isKnownAnnotation(String name) {
    for (AnnotationType t : AnnotationType.values()) {
      if (t.name().equals(name)) {
        return true;
      }
    }
    return false;
  }
  
  private static interface Action {
    TypeAnnotationsReader apply() throws InvalidClassFileException;
  }
  public static TypeAnnotationsReader getReaderForAnnotationAtClassfile(final AnnotationType type, final ClassReader.AttrIterator iter, final SignatureReader signatureReader) {
      return advanceIter(type, iter, () -> getTypeAnnotationReaderAtClassfile(iter, type.toString(), signatureReader));
  }
  
  public static TypeAnnotationsReader getReaderForAnnotationAtMethodInfo(final AnnotationType type, final ClassReader.AttrIterator iter, final ExceptionsReader exceptionReader, final SignatureReader signatureReader) {
    return advanceIter(type, iter, () -> getTypeAnnotationReaderAtMethodInfo(iter, type.toString(), exceptionReader, signatureReader));
  }

  public static TypeAnnotationsReader getReaderForAnnotationAtFieldInfo(final AnnotationType type, final ClassReader.AttrIterator iter) {
    return advanceIter(type, iter, () -> getTypeAnnotationReaderAtFieldInfo(iter, type.toString()));
  }
  
  
  public static TypeAnnotationsReader getReaderForAnnotationAtCode(final AnnotationType type, final ClassReader.AttrIterator iter, final CodeReader codereader) {
    return advanceIter(type, iter, () -> getTypeAnnotationReaderAtCode(iter, type.toString(), codereader));
  }

  
  private static TypeAnnotationsReader advanceIter(AnnotationType type, ClassReader.AttrIterator iter, Action newReader){
    // search for the desired attribute
    final String attrName = type.toString();
    try {
      for (; iter.isValid(); iter.advance()) {
        if (iter.getName().equals(attrName)) return newReader.apply();
      }
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
    }
    return null;
  }
}