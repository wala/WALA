/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeCT;

import java.util.Arrays;
import java.util.Map;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

/**
 * This class reads Annotations attributes, e.g., RuntimeInvisibleAnnotations.
 * 
 * @author sjfink
 */
public class AnnotationsReader extends AttributeReader {

  /**
   * offset in class file where this attribute begins
   */
  protected final int beginOffset;

  public AnnotationsReader(ClassReader.AttrIterator iter, String label) throws InvalidClassFileException {
    super(iter, label);
    beginOffset = attr;
  }

  /**
   * @return number of annotations in this attribute
   * @throws InvalidClassFileException
   */
  public int getAnnotationCount() throws InvalidClassFileException {
    int offset = beginOffset + 6;
    checkSize(offset, 2);
    return cr.getUShort(offset);
  }

  /**
   * @return total length of this attribute in bytes, <b>including</b> the
   *         first 6 bytes
   * @throws InvalidClassFileException
   */
  public int getAttributeSize() throws InvalidClassFileException {
    int offset = beginOffset + 2;
    checkSize(offset, 4);
    return cr.getInt(offset) + 6;
  }

  /**
   * get the Utf8 constant pool value, where the constant pool offset is given
   * in the class
   * 
   * @param offset
   *          offset in the class file at which the constant pool offset is
   *          given
   */
  protected String getUtf8ConstantPoolValue(int offset) throws InvalidClassFileException {
    checkSize(offset, 2);
    int cpOffset = cr.getUShort(offset);
    return cr.getCP().getCPUtf8(cpOffset);
  }

  /**
   * Marker interface for possible element values in an annotation attribute.
   * 
   * @see AnnotationsReader#readElementValueAndSize(int)
   * 
   */
  @SuppressWarnings("javadoc")
  public static interface ElementValue {
  }

  /**
   * Represents a constant argument to an annotation. Class arguments (e.g.,
   * <code>Foo.class</code>) are also represented with this type, with the value
   * being the String class name.
   */
  public static class ConstantElementValue implements ElementValue {

    /**
     * the constant value
     */
    public final Object val;

    public ConstantElementValue(Object val) {
      this.val = val;
    }

    @Override
    public String toString() {
      return String.valueOf(val);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((val == null) ? 0 : val.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ConstantElementValue other = (ConstantElementValue) obj;
      if (val == null) {
        if (other.val != null)
          return false;
      } else if (!val.equals(other.val))
        return false;
      return true;
    }
  }

  /**
   * Represents enum constant annotation arguments.
   */
  public static class EnumElementValue implements ElementValue {

    /**
     * the name of the enum type
     */
    public final String enumType;

    /**
     * the enum value
     */
    public final String enumVal;

    public EnumElementValue(String enumType, String enumVal) {
      super();
      this.enumType = enumType;
      this.enumVal = enumVal;
    }

    @Override
    public String toString() {
      return "EnumElementValue [type=" + enumType + ", val=" + enumVal + "]";
    }

  }

  /**
   * represents an annotation argument that itself is an array of arguments
   */
  public static class ArrayElementValue implements ElementValue {

    /**
     * the values contained in the array
     */
    public final ElementValue[] vals;

    public ArrayElementValue(ElementValue[] vals) {
      super();
      this.vals = vals;
    }

    @Override
    public String toString() {
      return "ArrayElementValue [vals=" + Arrays.toString(vals) + "]";
    }

  }

  /**
   * get all the annotations declared in this attribute.
   * <pre>
   * RuntimeVisibleAnnotations_attribute {
   *   u2         attribute_name_index;
   *   u4         attribute_length;
   *   u2         num_annotations;
   *   annotation annotations[num_annotations];
   * }
   * </pre>
   * 
   * @throws InvalidClassFileException
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.16"> JLS (SE8), 4.7.16</a>
   */
  public AnnotationAttribute[] getAllAnnotations() throws InvalidClassFileException {
    AnnotationAttribute[] result = new AnnotationAttribute[getAnnotationCount()];
    int offset = beginOffset + 8; // skip attribute_name_index,
                                  // attribute_length, and num_annotations
    for (int i = 0; i < result.length; i++) {
      Pair<AnnotationAttribute, Integer> attributeAndSize = getAttributeAndSize(offset);
      result[i] = attributeAndSize.fst;
      offset += attributeAndSize.snd;
    }
    return result;
  }
  
  /**
   * <pre>
   * param_annotations {
   *   u2 attribute_name_index;
   *   u4 attribute_length;
   *   u1 num_parameters;
   *   {
   *     u2 num_annotations;
   *     annotation annotations[num_annotations];
   *   } parameter_annotations[num_parameters];
   * </pre>
   */
  public AnnotationAttribute[][] getAllParameterAnnotations() throws InvalidClassFileException {
    int numParamOffset = beginOffset + 6;
    checkSize(numParamOffset, 1);
    int paramCount = cr.getByte(numParamOffset);
    AnnotationAttribute[][] result = new AnnotationAttribute[paramCount][];
    // skip attribute_name_index, attribute_length, and num_parameters
    int offset = beginOffset + 7;
    for (int i = 0; i < result.length; i++) {
      checkSize(offset, 2);
      result[i] = new AnnotationAttribute[cr.getUShort(offset)];
      offset += 2;
      for (int j = 0; j < result[i].length; j++) {
        Pair<AnnotationAttribute, Integer> attributeAndSize = getAttributeAndSize(offset);
        result[i][j] = attributeAndSize.fst;
        offset += attributeAndSize.snd;        
      }
    }
    return result;
  }
  
  
  
  /**
   * <pre>
   * annotation { 
   *   u2 type_index;
   *   u2 num_element_value_pairs; 
   *   { u2 element_name_index; 
   *     element_value value; 
   *   } element_value_pairs[num_element_value_pairs]
   * </pre>
   * 
   * @throws InvalidClassFileException
   */
  protected Pair<AnnotationAttribute, Integer> getAttributeAndSize(int begin) throws InvalidClassFileException {
    String type = getUtf8ConstantPoolValue(begin);
    int numElementValuePairs = cr.getUShort(begin + 2);
    int size = 4;
    int offset = begin + 4;
    Map<String, ElementValue> elementName2Val = HashMapFactory.make();
    for (int i = 0; i < numElementValuePairs; i++) {
      String elementName = getUtf8ConstantPoolValue(offset);
      offset += 2;
      Pair<ElementValue, Integer> elementValAndSize = readElementValueAndSize(offset);
      offset += elementValAndSize.snd;
      size += elementValAndSize.snd + 2;
      elementName2Val.put(elementName, elementValAndSize.fst);
    }
    return Pair.make(new AnnotationAttribute(type, elementName2Val), size);
  }

  /**
   * Representation of an annotation attribute. An annotation has the following
   * format in the bytecode:
   * 
   * <pre>
   * annotation {
   *   u2 type_index;
   *   u2 num_element_value_pairs;
   *   {  u2 element_name_index;
   *      element_value value;
   * } element_value_pairs[num_element_value_pairs];
   * </pre>
   * 
   * See the JVM specification section 4.7.16 for details.
   * 
   * This class implements {@link ElementValue} to handle nested annotations.
   */
  public static class AnnotationAttribute implements ElementValue {

    /**
     * the type of the annotation
     */
    public final String type;

    /**
     * the arguments to the annotation
     */
    public final Map<String, ElementValue> elementValues;

    public AnnotationAttribute(String type, Map<String, ElementValue> elementValues) {
      super();
      this.type = type;
      this.elementValues = elementValues;
    }

    @Override
    public String toString() {
      return "AnnotationElementValue [type=" + type + ", elementValues=" + elementValues + "]";
    }

  }

  /**
   * <pre>
   * element_value { 
   *   u1 tag; 
   *   union {
   *     u2 const_value_index; 
   *     {  u2 type_name_index;
   *        u2 const_name_index; 
   *     } enum_const_value; 
   *     u2 class_info_index; 
   *     annotation annotation_value; 
   *     {  u2 num_values;
   *        element_value values[num_values]; 
   *     } array_value;
   *   } value;
   * </pre>
   * 
   * A constant value (including class info) is represented by a
   * {@link ConstantElementValue}. An enum constant value is represented by an
   * {@link EnumElementValue}. An array value is represented by an
   * {@link ArrayElementValue}. Finally, a nested annotation is represented by
   * an {@link AnnotationAttribute}.
   * 
   * @throws InvalidClassFileException
   * @throws IllegalArgumentException
   */
  protected Pair<ElementValue, Integer> readElementValueAndSize(int offset) throws IllegalArgumentException,
      InvalidClassFileException {
    char tag = (char) cr.getByte(offset);
    // meaning of this short depends on the tag
    int nextShort = cr.getUShort(offset + 1);
    switch (tag) {
    case 'B':
    case 'C':
    case 'I':
    case 'S':
    case 'Z':
      return Pair.<ElementValue, Integer> make(new ConstantElementValue(cr.getCP().getCPInt(nextShort)), 3);
    case 'J':
      return Pair.<ElementValue, Integer> make(new ConstantElementValue(cr.getCP().getCPLong(nextShort)), 3);
    case 'D':
      return Pair.<ElementValue, Integer> make(new ConstantElementValue(cr.getCP().getCPDouble(nextShort)), 3);
    case 'F':
      return Pair.<ElementValue, Integer> make(new ConstantElementValue(cr.getCP().getCPFloat(nextShort)), 3);
    case 's': // string
    case 'c': // class; just represent as a constant element with the type name
      return Pair.<ElementValue, Integer> make(new ConstantElementValue(cr.getCP().getCPUtf8(nextShort)), 3);
    case 'e': // enum
      return Pair.<ElementValue, Integer> make(
          new EnumElementValue(cr.getCP().getCPUtf8(nextShort), cr.getCP().getCPUtf8(cr.getUShort(offset + 3))), 5);
    case '[': // array
      int numValues = nextShort;
      int numArrayBytes = 3; // start with 3 for the tag and num_values bytes
      ElementValue[] vals = new ElementValue[numValues];
      // start curOffset at beginning of array values
      int curArrayOffset = offset + 3;
      for (int i = 0; i < numValues; i++) {
        Pair<ElementValue, Integer> arrayElemValueAndSize = readElementValueAndSize(curArrayOffset);
        vals[i] = arrayElemValueAndSize.fst;
        curArrayOffset += arrayElemValueAndSize.snd;
        numArrayBytes += arrayElemValueAndSize.snd;
      }
      return Pair.<ElementValue, Integer> make(new ArrayElementValue(vals), numArrayBytes);
    case '@': // annotation
      Pair<AnnotationAttribute, Integer> attributeAndSize = getAttributeAndSize(offset + 1);
      // add 1 to size for the tag
      return Pair.<ElementValue, Integer> make(attributeAndSize.fst, attributeAndSize.snd + 1);
    default:
      assert false;
      return null;
    }
  }

  // //////////////
  // utility methods for reading well-known annotation types
  // //////////////

  public static enum AnnotationType {
    RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations,RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations
  }

  
  public static boolean isKnownAnnotation(String name) {
    for (AnnotationType t : AnnotationType.values()) {
      if (t.name().equals(name)) {
        return true;
      }
    }
    return false;
  }
  
  public static AnnotationsReader getReaderForAnnotation(AnnotationType type, ClassReader.AttrIterator iter) {
    // search for the desired attribute
    final String attrName = type.toString();
    try {
      for (; iter.isValid(); iter.advance()) {
        if (iter.getName().equals(attrName)) {
          return new AnnotationsReader(iter, attrName);
        }
      }
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
    }
    return null;
  }
}
