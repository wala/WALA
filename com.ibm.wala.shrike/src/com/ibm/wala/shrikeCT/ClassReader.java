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

/**
 * This is the core class for reading class file data.
 * 
 * ClassReader performs lazy parsing, and thus most of the methods can throw an InvalidClassFileException.
 */
public final class ClassReader implements ClassConstants {
  private final byte[] bytes;

  private int[] methodOffsets;

  private int[] fieldOffsets;

  private ConstantPoolParser cpParser;

  private int classInfoOffset;

  private int attrInfoOffset;

  private int interfaceCount;

  /**
   * Build a reader.
   * 
   * If the class file data is corrupt an exception might not be thrown immediately. Instead an exception might be thrown later,
   * during the execution of some access method. This is a consequence of the 'lazy parsing' performed by ClassReader.
   * 
   * @param bytes the class file data
   * @throws InvalidClassFileException the class file data is corrupt
   */
  public ClassReader(byte[] bytes) throws InvalidClassFileException {
    this.bytes = bytes;
    parse();
  }

  private void checkLength(int offset, int required) throws InvalidClassFileException {
    if (bytes.length < offset + required) {
      throw new InvalidClassFileException(offset, "file truncated, expected " + required + " bytes, saw only "
          + (bytes.length - offset));
    }
  }

  private void parse() throws InvalidClassFileException {
    int offset = 0;

    checkLength(offset, 10);
    int magic = getInt(offset);
    int minorVersion = getUShort(offset + 4);
    int majorVersion = getUShort(offset + 6);
    int constantPoolCount = getUShort(offset + 8);
    offset += 10;

    if (magic != MAGIC) {
      throw new InvalidClassFileException(offset, "bad magic number: " + magic);
    }
    if (majorVersion < 45 || majorVersion > 52) {
      throw new InvalidClassFileException(offset, "unknown class file version: " + majorVersion + "." + minorVersion);
    }
    
    cpParser = new ConstantPoolParser(bytes, offset, constantPoolCount);
    offset += cpParser.getRawSize();

    classInfoOffset = offset;
    checkLength(offset, 8);
    // int accessFlags = getUShort(offset);
    // int thisClass = getUShort(offset + 2);
    // int superClass = getUShort(offset + 4);
    interfaceCount = getUShort(offset + 6);
    if (interfaceCount < 0) {
      throw new InvalidClassFileException(offset, "negative interface count: " + interfaceCount);
    }
    offset += 8;
    checkLength(offset, interfaceCount * 2);
    offset += interfaceCount * 2;

    checkLength(offset, 2);
    int fieldCount = getUShort(offset);
    if (fieldCount < 0) {
      throw new InvalidClassFileException(offset, "negative field count: " + interfaceCount);
    }
    offset = parseFields(offset + 2, fieldCount);

    checkLength(offset, 2);
    int methodCount = getUShort(offset);
    if (methodCount < 0) {
      throw new InvalidClassFileException(offset, "negative method count: " + interfaceCount);
    }
    offset = parseMethods(offset + 2, methodCount);

    attrInfoOffset = offset;
    checkLength(offset, 2);
    int attrCount = getUShort(offset);
    offset = skipAttributes(offset + 2, attrCount);

    if (offset != bytes.length) {
      throw new InvalidClassFileException(offset, "extra data in class file");
    }
  }

  private int skipAttributes(int offset, int count) throws InvalidClassFileException {
    if (count < 0) {
      throw new InvalidClassFileException(offset, "negative attribute count: " + interfaceCount);
    }

    for (int i = 0; i < count; i++) {
      checkLength(offset, 6);
      int size = getInt(offset + 2);
      if (size < 0) {
        throw new InvalidClassFileException(offset, "negative attribute size: " + size);
      }
      offset += 6;
      checkLength(offset, size);
      offset += size;
    }
    return offset;
  }

  private int parseFields(int offset, int count) throws InvalidClassFileException {
    fieldOffsets = new int[count + 1];
    for (int i = 0; i < count; i++) {
      fieldOffsets[i] = offset;
      checkLength(offset, 8);
      offset = skipAttributes(offset + 8, getUShort(offset + 6));
    }
    fieldOffsets[count] = offset;
    return offset;
  }

  private int parseMethods(int offset, int count) throws InvalidClassFileException {
    methodOffsets = new int[count + 1];
    for (int i = 0; i < count; i++) {
      methodOffsets[i] = offset;
      checkLength(offset, 8);
      offset = skipAttributes(offset + 8, getUShort(offset + 6));
    }
    methodOffsets[count] = offset;
    return offset;
  }

  /**
   * @return the raw class data bytes
   */
  public byte[] getBytes() {
    return bytes;
  }

  /**
   * @return the magic number at the start of the class file.
   */
  public int getMagic() {
    return getInt(0);
  }

  /**
   * @return the minor version of the class file
   */
  public int getMinorVersion() {
    return getUShort(4);
  }

  /**
   * @return the major version of the class file
   */
  public int getMajorVersion() {
    return getUShort(6);
  }

  /**
   * @return the access flags for the class
   */
  public int getAccessFlags() {
    return getUShort(classInfoOffset);
  }

  /**
   * @return the index of the constant pool entry for the class name
   */
  public int getNameIndex() {
    return getUShort(classInfoOffset + 2);
  }

  String getClassFromAddress(int addr) throws InvalidClassFileException {
    int c = getUShort(addr);
    if (c == 0) {
      return null;
    } else {
      try {
        return cpParser.getCPClass(c);
      } catch (IllegalArgumentException ex) {
        throw new InvalidClassFileException(addr, "Invalid class constant pool index: " + c);
      }
    }
  }

  /**
   * @return the name of the class in JVM format (e.g., java/lang/Object)
   */
  public String getName() throws InvalidClassFileException {
    String s = getClassFromAddress(classInfoOffset + 2);
    if (s == null) {
      throw new InvalidClassFileException(classInfoOffset + 2, "Null class name not allowed");
    } else {
      return s;
    }
  }

  /**
   * @return the constant pool index of the superclass name, or 0 if this is java.lang.Object
   */
  public int getSuperNameIndex() {
    return getUShort(classInfoOffset + 4);
  }

  /**
   * @return the superclass name in JVM format (e.g., java/lang/Object), or null if this class is java.lang.Object
   */
  public String getSuperName() throws InvalidClassFileException {
    return getClassFromAddress(classInfoOffset + 4);
  }

  /**
   * @return the number of interfaces this class implements
   */
  public int getInterfaceCount() {
    return interfaceCount;
  }

  private void verifyInterfaceIndex(int i) {
    if (i < 0 || i >= interfaceCount) {
      throw new IllegalArgumentException("Invalid interface index: " + i);
    }
  }

  /**
   * @return the constant pool index of the name of the i'th implemented interface
   */
  public int getInterfaceNameIndex(int i) {
    verifyInterfaceIndex(i);
    return getUShort(classInfoOffset + 8 + 2 * i);
  }

  /**
   * @return an array of the constant pool indices for the names of the implemented interfaces
   */
  public int[] getInterfaceNameIndices() {
    int[] indices = new int[interfaceCount];
    for (int i = 0; i < interfaceCount; i++) {
      indices[i] = getUShort(classInfoOffset + 8 + 2 * i);
    }
    return indices;
  }

  /**
   * @return the name of the i'th implemented interface
   */
  public String getInterfaceName(int i) throws InvalidClassFileException {
    verifyInterfaceIndex(i);
    String s = getClassFromAddress(classInfoOffset + 8 + 2 * i);
    if (s == null) {
      throw new InvalidClassFileException(classInfoOffset + 8 + 2 * i, "Null interface name not allowed");
    } else {
      return s;
    }
  }

  /**
   * @return an array of the names of the implemented interfaces
   */
  public String[] getInterfaceNames() throws InvalidClassFileException {
    String[] names = new String[interfaceCount];
    for (int i = 0; i < interfaceCount; i++) {
      String s = getClassFromAddress(classInfoOffset + 8 + 2 * i);
      if (s == null) {
        throw new InvalidClassFileException(classInfoOffset + 8 + 2 * i, "Null interface name not allowed");
      }
      names[i] = s;
    }
    return names;
  }

  /**
   * This method allows direct read-only access to the constant pool for the class.
   * 
   * @return the constant pool for the class
   */
  public ConstantPoolParser getCP() {
    return cpParser;
  }

  /**
   * @return the signed 32-bit value at offset i in the class data
   */
  public int getInt(int i) {
    return (bytes[i] << 24) + ((bytes[i + 1] & 0xFF) << 16) + ((bytes[i + 2] & 0xFF) << 8) + (bytes[i + 3] & 0xFF);
  }

  /**
   * @return the unsigned 16-bit value at offset i in the class data
   */
  public int getUShort(int i) {
    return ((bytes[i] & 0xFF) << 8) + (bytes[i + 1] & 0xFF);
  }

  /**
   * @return the signed 16-bit value at offset i in the class data
   */
  public int getShort(int i) {
    return (bytes[i] << 8) + (bytes[i + 1] & 0xFF);
  }

  /**
   * @return the signed 8-bit value at offset i in the class data
   */
  public byte getByte(int i) {
    return bytes[i];
  }
  
  /**
   * @return the unsigned 8-bit value at offset i in the class data
   */
  public int getUnsignedByte(int i) {
    return bytes[i] & 0xff;
  }

  /**
   * @return the number of fields in the class
   */
  public int getFieldCount() {
    return fieldOffsets.length - 1;
  }

  private void verifyFieldIndex(int f) {
    if (f < 0 || f >= fieldOffsets.length - 1) {
      throw new IllegalArgumentException("Invalid field index: " + f);
    }
  }

  /**
   * @return the access flags for the f'th field
   */
  public int getFieldAccessFlags(int f) {
    verifyFieldIndex(f);
    return getUShort(fieldOffsets[f]);
  }

  String getUtf8FromAddress(int addr) throws InvalidClassFileException {
    int s = getUShort(addr);
    if (s == 0) {
      return null;
    } else {
      try {
        return cpParser.getCPUtf8(s);
      } catch (IllegalArgumentException ex) {
        throw new InvalidClassFileException(addr, "Invalid Utf8 constant pool index: " + s);
      }
    }
  }

  /**
   * @return the name of the f'th field
   */
  public String getFieldName(int f) throws InvalidClassFileException {
    verifyFieldIndex(f);
    return getUtf8FromAddress(fieldOffsets[f] + 2);
  }

  /**
   * @return the type of the f'th field, in JVM format (e.g., I, Z, java/lang/Object)
   */
  public String getFieldType(int f) throws InvalidClassFileException {
    verifyFieldIndex(f);
    return getUtf8FromAddress(fieldOffsets[f] + 4);
  }

  /**
   * @return the index of the constant pool entry for the name of the f'th field, in JVM format (e.g., I, Z, Ljava/lang/Object;)
   */
  public int getFieldNameIndex(int f) {
    verifyFieldIndex(f);
    return getUShort(fieldOffsets[f] + 2);
  }

  /**
   * @return the index of the constant pool entry for the type of the f'th field, in JVM format (e.g., I, Z, Ljava/lang/Object;)
   */
  public int getFieldTypeIndex(int f) {
    verifyFieldIndex(f);
    return getUShort(fieldOffsets[f] + 4);
  }

  /**
   * AttrIterator provides access to attributes in the class file.
   * 
   * AttrIterators can be reused for many different iterations, like this:
   * 
   * <pre>
   *   AttrIterator iter = new AttrIterator();
   *    int fieldCount = reader.getFieldCount();
   *    for (int i = 0; i &lt; fieldCount; i++) {
   *      reader.initFieldAttributeIterator(i, iter);
   *      for (; iter.isValid(); iter.advance()) {
   *        if (iter.getName().equals(&quot;ConstantValue&quot;)) {
   *          ConstantValueReader cv = new ConstantValueReader(iter);
   *          ...
   *        }
   *      }
   *    }
   * </pre>
   */
  public static final class AttrIterator {
    ClassReader cr;

    int offset;

    int size;

    private int remaining;

    /**
     * Create a blank iterator. The iterator is not valid until it is initialized by some other class.
     */
    public AttrIterator() {
    }

    private void setSize() {
      if (remaining > 0) {
        size = 6 + cr.getInt(offset + 2);
      }
    }

    void init(ClassReader cr, int offset) {
      this.cr = cr;
      this.offset = offset + 2;
      this.remaining = cr.getUShort(offset);
      setSize();
    }

    void verifyValid() {
      if (remaining <= 0) {
        throw new IllegalArgumentException("Attempt to manipulate invalid AttrIterator");
      }
    }

    public ClassReader getClassReader() {
      verifyValid();
      return cr;
    }

    /**
     * The attribute iterator must be valid.
     * 
     * @return the offset of the raw attribute data (including attribute header) in the class file data
     */
    public int getRawOffset() {
      verifyValid();
      return offset;
    }

    /**
     * The attribute iterator must be valid.
     * 
     * @return the size of the raw attribute data (including attribute header) in the class file data
     */
    public int getRawSize() {
      verifyValid();
      return size;
    }

    /**
     * The attribute iterator must be valid.
     * 
     * @return the offset of the attribute data (excluding attribute header) in the class file data
     */
    public int getDataOffset() {
      verifyValid();
      return offset + 6;
    }

    /**
     * The attribute iterator must be valid.
     * 
     * @return the size of the attribute data (excluding attribute header) in the class file data
     */
    public int getDataSize() {
      verifyValid();
      return size - 6;
    }

    /**
     * @return the number of attributes left in the list, including this attribute (if valid)
     */
    public int getRemainingAttributesCount() {
      return remaining;
    }

    /**
     * The attribute iterator must be valid.
     * 
     * @return the constant pool index of the name of the attribute
     */
    public int getNameIndex() {
      verifyValid();
      return cr.getUShort(offset);
    }

    /**
     * The attribute iterator must be valid.
     * 
     * @return the name of the attribute
     */
    public String getName() throws InvalidClassFileException {
      verifyValid();
      String s = cr.getUtf8FromAddress(offset);
      if (s == null) {
        throw new InvalidClassFileException(offset, "Null attribute name");
      } else {
        return s;
      }
    }

    /**
     * @return whether this iterator is valid
     */
    public boolean isValid() {
      return remaining > 0;
    }

    /**
     * The attribute iterator must be valid.
     * 
     * The iterator is advanced to the next attribute (which might not exist, so the iterator might become invalid).
     */
    public void advance() {
      verifyValid();
      offset += size;
      remaining--;
      setSize();
    }
  }

  /**
   * Point iter at the list of attributes for field f.
   * 
   * @throws IllegalArgumentException if iter is null
   */
  public void initFieldAttributeIterator(int f, AttrIterator iter) {
    if (iter == null) {
      throw new IllegalArgumentException("iter is null");
    }
    verifyFieldIndex(f);
    iter.init(this, fieldOffsets[f] + 6);
  }

  /**
   * @return the offset of the raw class data for field f
   */
  public int getFieldRawOffset(int f) {
    verifyFieldIndex(f);
    return fieldOffsets[f];
  }

  /**
   * @return the size of the raw class data for field f
   */
  public int getFieldRawSize(int f) {
    verifyFieldIndex(f);
    return fieldOffsets[f + 1] - fieldOffsets[f];
  }

  /**
   * @return the number of methods in the class
   */
  public int getMethodCount() {
    return methodOffsets.length - 1;
  }

  private void verifyMethodIndex(int m) {
    if (m < 0 || m >= methodOffsets.length - 1) {
      throw new IllegalArgumentException("Invalid method index: " + m);
    }
  }

  /**
   * @return the offset of the raw class data for method m
   */
  public int getMethodRawOffset(int m) {
    verifyMethodIndex(m);
    return methodOffsets[m];
  }

  /**
   * @return the size of the raw class data for method m
   */
  public int getMethodRawSize(int m) {
    verifyMethodIndex(m);
    return methodOffsets[m + 1] - methodOffsets[m];
  }

  /**
   * @return the access flags for method m
   */
  public int getMethodAccessFlags(int m) {
    verifyMethodIndex(m);
    return getUShort(methodOffsets[m]);
  }

  /**
   * @return the name of method m
   */
  public String getMethodName(int m) throws InvalidClassFileException {
    verifyMethodIndex(m);
    return getUtf8FromAddress(methodOffsets[m] + 2);
  }

  /**
   * @return the method descriptor of method m in JVM format (e.g., (ILjava/lang/Object;)V )
   */
  public String getMethodType(int m) throws InvalidClassFileException {
    verifyMethodIndex(m);
    return getUtf8FromAddress(methodOffsets[m] + 4);
  }

  /**
   * @return the constant pool index of the name of method m
   */
  public int getMethodNameIndex(int m) {
    verifyMethodIndex(m);
    return getUShort(methodOffsets[m] + 2);
  }

  /**
   * @return the constant pool index of the method descriptor of method m
   */
  public int getMethodTypeIndex(int m) {
    verifyMethodIndex(m);
    return getUShort(methodOffsets[m] + 4);
  }

  /**
   * Point iter at the list of attributes for method m.
   * 
   * @throws IllegalArgumentException if iter is null
   */
  public void initMethodAttributeIterator(int m, AttrIterator iter) {
    if (iter == null) {
      throw new IllegalArgumentException("iter is null");
    }
    verifyMethodIndex(m);
    iter.init(this, methodOffsets[m] + 6);
  }

  /**
   * Point iter at the list of attributes for the class.
   * 
   * @throws IllegalArgumentException if iter is null
   */
  public void initClassAttributeIterator(AttrIterator iter) {
    if (iter == null) {
      throw new IllegalArgumentException("iter is null");
    }
    iter.init(this, attrInfoOffset);
  }
}
