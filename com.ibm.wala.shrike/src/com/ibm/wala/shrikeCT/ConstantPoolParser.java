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

import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.shrikeCT.ClassReader.AttrIterator;

/**
 * A ConstantPoolParser provides read-only access to the constant pool of a class file.
 */
public final class ConstantPoolParser implements ClassConstants {
  public static class ReferenceToken {
    private final byte kind;
    private final String className;
    private final String elementName;
    private final String descriptor;
    
    public ReferenceToken(byte kind, String className, String elementName, String descriptor) {
      this.kind = kind;
      this.className = className;
      this.elementName = elementName;
      this.descriptor = descriptor;
    }
  
    public byte getKind() {
      return kind;
    }

    public String getClassName() {
      return className;
    }

    public String getElementName() {
      return elementName;
    }

    public String getDescriptor() {
      return descriptor;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((className == null) ? 0 : className.hashCode());
      result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
      result = prime * result + ((elementName == null) ? 0 : elementName.hashCode());
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
      ReferenceToken other = (ReferenceToken) obj;
      if (kind != other.kind) {
        return false;
      }
      if (className == null) {
        if (other.className != null)
          return false;
      } else if (!className.equals(other.className))
        return false;
      if (descriptor == null) {
        if (other.descriptor != null)
          return false;
      } else if (!descriptor.equals(other.descriptor))
        return false;
      if (elementName == null) {
        if (other.elementName != null)
          return false;
      } else if (!elementName.equals(other.elementName))
        return false;
      return true;
    }
  }

  final private byte[] bytes;

  private int[] cpOffsets;

  private String[] cpItems;

  private BootstrapMethodsReader invokeDynamicBootstraps;
  
  // TODO: use JVM spec limit here?
  private final static int MAX_CP_ITEMS = Integer.MAX_VALUE / 4;

  private BootstrapMethodsReader getBootstrapReader() throws InvalidClassFileException {
    if (invokeDynamicBootstraps == null) { 
      ClassReader thisClass = new ClassReader(bytes);
      AttrIterator attrs = new AttrIterator();
      thisClass.initClassAttributeIterator(attrs);
      for (; attrs.isValid(); attrs.advance()) {
        if (attrs.getName().equals("BootstrapMethods")) {
          invokeDynamicBootstraps = new BootstrapMethodsReader(attrs);
          break;
        }
      }
      assert invokeDynamicBootstraps != null;
    }
    
    return invokeDynamicBootstraps;
  }
  
  /**
   * @param bytes the raw class file data
   * @param offset the start of the constant pool data
   * @param itemCount the number of items in the pool
   */
  public ConstantPoolParser(byte[] bytes, int offset, int itemCount) throws InvalidClassFileException {
    this.bytes = bytes;
    if (offset < 0) {
      throw new IllegalArgumentException("invalid offset: " + offset);
    }
    if (itemCount < 0 || itemCount > MAX_CP_ITEMS) {
      throw new IllegalArgumentException("invalid itemCount: " + itemCount);
    }
    parseConstantPool(offset, itemCount);
  }

  /**
   * @return the buffer holding the raw class file data
   */
  public byte[] getRawBytes() {
    return bytes;
  }

  /**
   * @return the offset of the constant pool data in the raw class file buffer
   */
  public int getRawOffset() throws IllegalStateException {
    if (cpOffsets.length < 2) {
      throw new IllegalStateException();
    }
    return cpOffsets[1];
  }

  /**
   * @return the size of the constant pool data in the raw class file buffer
   */
  public int getRawSize() throws IllegalStateException {
    if (cpOffsets.length < 2) {
      throw new IllegalStateException();
    }
    return cpOffsets[cpOffsets.length - 1] - cpOffsets[1];
  }

  /**
   * @return the number of constant pool items (maximum item index plus one)
   */
  public int getItemCount() {
    return cpOffsets.length - 1;
  }

  private void checkLength(int offset, int required) throws InvalidClassFileException {
    if (bytes.length < offset + required) {
      throw new InvalidClassFileException(offset, "file truncated, expected " + required + " bytes, saw only "
          + (bytes.length - offset));
    }
  }

  /**
   * @return the type of constant pool item i, or 0 if i is an unused constant pool item
   */
  public byte getItemType(int i) throws IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0) {
      return 0;
    } else {
      return getByte(offset);
    }
  }

  /**
   * @return the name of the Class at constant pool item i, in JVM format (e.g., java/lang/Object)
   */
  public String getCPClass(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_Class) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a Class");
    }
    String s = cpItems[i];
    if (s == null) {
      try {
        s = getCPUtf8(getUShort(offset + 1));
      } catch (IllegalArgumentException ex) {
        throw new InvalidClassFileException(offset, "Invalid class name at constant pool item #" + i + ": " + ex.getMessage());
      }
      cpItems[i] = s;
    }
    return s;
  }

  /**
   * @return the name of the method at constant pool item i, in JVM format (e.g., java/lang/Object)
   */
  public String getCPMethodType(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_MethodType) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a MethodType");
    }
    String s = cpItems[i];
    if (s == null) {
      try {
        s = getCPUtf8(getUShort(offset + 1));
      } catch (IllegalArgumentException ex) {
        throw new InvalidClassFileException(offset, "Invalid method type at constant pool item #" + i + ": " + ex.getMessage());
      }
      cpItems[i] = s;
    }
    return s;
  }

  /**
   * @return the String at constant pool item i
   */
  public String getCPString(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_String) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a String");
    }
    String s = cpItems[i];
    if (s == null) {
      try {
        s = getCPUtf8(getUShort(offset + 1));
      } catch (IllegalArgumentException ex) {
        throw new InvalidClassFileException(offset, "Invalid string at constant pool item #" + i + ": " + ex.getMessage());
      }
      cpItems[i] = s;
    }
    return s;
  }

  /**
   * Does b represent the tag of a constant pool reference to an (interface)
   * method or field?
   */
  public static boolean isRef(byte b) {
    switch (b) {
    case CONSTANT_MethodRef:
    case CONSTANT_FieldRef:
    case CONSTANT_InterfaceMethodRef:
      return true;
    default:
      return false;
    }
  }

  /**
   * @return the name of the class part of the FieldRef, MethodRef, or InterfaceMethodRef at constant pool item i
   */
  public String getCPRefClass(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || !isRef(getByte(offset))) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a Ref");
    }
    try {
      return getCPClass(getUShort(offset + 1));
    } catch (IllegalArgumentException ex) {
      throw new InvalidClassFileException(offset, "Invalid Ref class at constant pool item #" + i + ": " + ex.getMessage());
    }
  }

  /**
   * @return the name part of the FieldRef, MethodRef, or InterfaceMethodRef at constant pool item i
   */
  public String getCPRefName(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || !isRef(getByte(offset))) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a Ref");
    }
    try {
      return getCPNATName(getUShort(offset + 3));
    } catch (IllegalArgumentException ex) {
      throw new InvalidClassFileException(offset, "Invalid Ref NameAndType at constant pool item #" + i + ": " + ex.getMessage());
    }
  }

  /**
   * @return the type part of the FieldRef, MethodRef, or InterfaceMethodRef at constant pool item i, in JVM format (e.g., I, Z, or
   *         Ljava/lang/Object;)
   */
  public String getCPRefType(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || !isRef(getByte(offset))) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a Ref");
    }
    try {
      return getCPNATType(getUShort(offset + 3));
    } catch (IllegalArgumentException ex) {
      throw new InvalidClassFileException(offset, "Invalid Ref NameAndType at constant pool item #" + i + ": " + ex.getMessage());
    }
  }

  /**
   * @return the name part of the NameAndType at constant pool item i
   */
  public String getCPNATName(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_NameAndType) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a NameAndType");
    }
    try {
      return getCPUtf8(getUShort(offset + 1));
    } catch (IllegalArgumentException ex) {
      throw new InvalidClassFileException(offset, "Invalid NameAndType name at constant pool item #" + i + ": " + ex.getMessage());
    }
  }

  /**
   * @return the type part of the NameAndType at constant pool item i, in JVM format (e.g., I, Z, or Ljava/lang/Object;)
   */
  public String getCPNATType(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_NameAndType) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a NameAndType");
    }
    try {
      return getCPUtf8(getUShort(offset + 3));
    } catch (IllegalArgumentException ex) {
      throw new InvalidClassFileException(offset, "Invalid NameAndType type at constant pool item #" + i + ": " + ex.getMessage());
    }
  }

  /**
   * @return the name part of the MethodHandle at constant pool item i, in JVM format (e.g., I, Z, or Ljava/lang/Object;)
   */
  public String getCPHandleName(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_MethodHandle) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a MethodHandle");
    }
    try {
      return getCPRefName(getUShort(offset + 2));
    } catch (IllegalArgumentException ex) {
      throw new InvalidClassFileException(offset, "Invalid NameAndType type at constant pool item #" + i + ": " + ex.getMessage());
    }
  }

  /**
   * @return the name part of the MethodHandle at constant pool item i, in JVM format (e.g., I, Z, or Ljava/lang/Object;)
   */
  public String getCPHandleType(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_MethodHandle) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a MethodHandle");
    }
    try {
      return getCPRefType(getUShort(offset + 2));
    } catch (IllegalArgumentException ex) {
      throw new InvalidClassFileException(offset, "Invalid NameAndType type at constant pool item #" + i + ": " + ex.getMessage());
    }
  }

  /**
   * @return the name part of the MethodHandle at constant pool item i, in JVM format (e.g., I, Z, or Ljava/lang/Object;)
   */
  public String getCPHandleClass(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_MethodHandle) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a MethodHandle");
    }
    try {
      return getCPRefClass(getUShort(offset + 2));
    } catch (IllegalArgumentException ex) {
      throw new InvalidClassFileException(offset, "Invalid NameAndType type at constant pool item #" + i + ": " + ex.getMessage());
    }
  }

  /**
   * @return the type of the MethodHandle at constant pool item i
   */
  public byte getCPHandleKind(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_MethodHandle) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a MethodHandle");
    }
    try {
      return getByte(offset + 1);
    } catch (IllegalArgumentException ex) {
      throw new InvalidClassFileException(offset, "Invalid NameAndType type at constant pool item #" + i + ": " + ex.getMessage());
    }
  }

  /**
   * @return the value of the Integer at constant pool item i
   */
  public int getCPInt(int i) throws IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_Integer) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not an Integer");
    }
    return getInt(offset + 1);
  }

  /**
   * @return the value of the Float at constant pool item i
   */
  public float getCPFloat(int i) throws IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_Float) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a Float");
    }
    return getFloat(offset + 1);
  }

  /**
   * @return the value of the Long at constant pool item i
   */
  public long getCPLong(int i) throws IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_Long) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a Long");
    }
    return getLong(offset + 1);
  }

  /**
   * @return the value of the Double at constant pool item i
   */
  public double getCPDouble(int i) throws IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_Double) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a Double");
    }
    return getDouble(offset + 1);
  }

  /**
   * @return the BootstrapMethodTable index of the bootstrap method for this invokedynamic
   */
  public BootstrapMethod getCPDynBootstrap(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_InvokeDynamic) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not an InvokeDynamic");
    }
    try {
      int index = getUShort(offset + 1);
      return getBootstrapReader().getEntry(index);
      
    } catch (IllegalArgumentException ex) {
      throw new InvalidClassFileException(offset, "Invalid Ref class at constant pool item #" + i + ": " + ex.getMessage());
    }
  }

  public String getCPDynName(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_InvokeDynamic) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not an InvokeDynamic");
    }
    try {
      return getCPNATName(getUShort(offset + 3));
    } catch (IllegalArgumentException ex) {
      throw new InvalidClassFileException(offset, "Invalid Ref class at constant pool item #" + i + ": " + ex.getMessage());
    }
  }

  public String getCPDynType(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_InvokeDynamic) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not an InvokeDynamic");
    }
    try {
      return getCPNATType(getUShort(offset + 3));
    } catch (IllegalArgumentException ex) {
      throw new InvalidClassFileException(offset, "Invalid Ref class at constant pool item #" + i + ": " + ex.getMessage());
    }
  }

  private InvalidClassFileException invalidUtf8(int item, int offset) {
    return new InvalidClassFileException(offset, "Constant pool item #" + item + " starting at " + cpOffsets[item]
        + ", is an invalid Java Utf8 string (byte is " + getByte(offset) + ")");
  }

  /**
   * @return the value of the Utf8 string at constant pool item i
   */
  public String getCPUtf8(int i) throws InvalidClassFileException, IllegalArgumentException {
    if (i < 1 || i >= cpItems.length) {
      throw new IllegalArgumentException("Constant pool item #" + i + " out of range");
    }
    int offset = cpOffsets[i];
    if (offset == 0 || getByte(offset) != CONSTANT_Utf8) {
      throw new IllegalArgumentException("Constant pool item #" + i + " is not a Utf8");
    }
    String s = cpItems[i];
    if (s == null) {
      int count = getUShort(offset + 1);
      int end = count + offset + 3;
      StringBuffer buf = new StringBuffer(count);
      offset += 3;
      while (offset < end) {
        byte x = getByte(offset);
        if ((x & 0x80) == 0) {
          if (x == 0) {
            throw invalidUtf8(i, offset);
          }
          buf.append((char) x);
          offset++;
        } else if ((x & 0xE0) == 0xC0) {
          if (offset + 1 >= end) {
            throw invalidUtf8(i, offset);
          }
          byte y = getByte(offset + 1);
          if ((y & 0xC0) != 0x80) {
            throw invalidUtf8(i, offset);
          }
          buf.append((char) (((x & 0x1F) << 6) + (y & 0x3F)));
          offset += 2;
        } else if ((x & 0xF0) == 0xE0) {
          if (offset + 2 >= end) {
            throw invalidUtf8(i, offset);
          }
          byte y = getByte(offset + 1);
          byte z = getByte(offset + 2);
          if ((y & 0xC0) != 0x80 || (z & 0xC0) != 0x80) {
            throw invalidUtf8(i, offset);
          }
          buf.append((char) (((x & 0x0F) << 12) + ((y & 0x3F) << 6) + (z & 0x3F)));
          offset += 3;
        } else {
          throw invalidUtf8(i, offset);
        }
      }
      // s = buf.toString().intern(); // removed intern() call --MS
      s = buf.toString();
      cpItems[i] = s;
    }
    return s;
  }

  private void parseConstantPool(int offset, int itemCount) throws InvalidClassFileException {
    cpOffsets = new int[itemCount + 1];
    cpItems = new String[itemCount];
    for (int i = 1; i < itemCount; i++) {
      cpOffsets[i] = offset;
      byte tag = getByte(offset);
      int itemLen;
      switch (tag) {
      case CONSTANT_String:
      case CONSTANT_Class:
        itemLen = 2;
        break;
      case CONSTANT_NameAndType:
      case CONSTANT_MethodRef:
      case CONSTANT_FieldRef:
      case CONSTANT_InterfaceMethodRef:
      case CONSTANT_Integer:
      case CONSTANT_Float:
        itemLen = 4;
        break;
      case CONSTANT_Long:
      case CONSTANT_Double:
        itemLen = 8;
        i++; // ick
        break;
      case CONSTANT_Utf8:
        itemLen = 2 + getUShort(offset + 1);
        break;
      case CONSTANT_MethodHandle:
        itemLen = 3;
        break;
      case CONSTANT_MethodType:
        itemLen = 2;
        break;
      case CONSTANT_InvokeDynamic:
        itemLen = 4;
        break;
      default:
        throw new InvalidClassFileException(offset, "unknown constant pool entry type" + tag);
      }
      checkLength(offset, itemLen);
      offset += itemLen + 1;
    }
    cpOffsets[itemCount] = offset;
  }

  private byte getByte(int i) {
    return bytes[i];
  }

  private int getUShort(int i) {
    return ((bytes[i] & 0xFF) << 8) + (bytes[i + 1] & 0xFF);
  }

  // private short getShort(int i) {
  // return (short) ((bytes[i] << 8) + (bytes[i + 1] & 0xFF));
  // }

  private int getInt(int i) {
    return (bytes[i] << 24) + ((bytes[i + 1] & 0xFF) << 16) + ((bytes[i + 2] & 0xFF) << 8) + (bytes[i + 3] & 0xFF);
  }

  private long getLong(int i) {
    return ((long) getInt(i) << 32) + (getInt(i + 4) & 0xFFFFFFFFL);
  }

  private float getFloat(int i) {
    return Float.intBitsToFloat(getInt(i));
  }

  private double getDouble(int i) {
    return Double.longBitsToDouble(getLong(i));
  }
}
