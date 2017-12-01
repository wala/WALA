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

import java.util.ArrayList;
import java.util.HashMap;

import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.shrikeCT.ConstantPoolParser.ReferenceToken;

/**
 * This class formats and writes class data into JVM format.
 */
public class ClassWriter implements ClassConstants {
  // input
  private int majorVersion = 46;

  private int minorVersion = 0;

  private ConstantPoolParser rawCP;

  private HashMap<Object, Integer> cachedCPEntries = new HashMap<>(1);

  final private ArrayList<Object> newCPEntries = new ArrayList<>(1);

  private int nextCPIndex = 1;

  final private ArrayList<Element> fields = new ArrayList<>(1);

  final private ArrayList<Element> methods = new ArrayList<>(1);

  final private ArrayList<Element> classAttributes = new ArrayList<>(1);

  private int thisClass;

  private int superClass;

  private int[] superInterfaces;

  private int accessFlags;

  private boolean forceAddCPEntries = false;

  // output
  private byte[] buf;

  private int bufLen;

  /**
   * Create a blank ClassWriter with no methods, fields, or attributes, an empty constant pool, no super class, no implemented
   * interfaces, no name, majorVersion 46, and minorVersion 0.
   */
  public ClassWriter() {
  }

  /**
   * Set the class file format major version. You probably don't want to use this unless you really know what you are doing.
   */
  public void setMajorVersion(int major) {
    if (major < 0 || major > 0xFFFF) {
      throw new IllegalArgumentException("Major version out of range: " + major);
    }
    majorVersion = major;
  }

  /**
   * Set the class file format minor version. You probably don't want to use this unless you really know what you are doing.
   */
  public void setMinorVersion(int minor) {
    if (minor < 0 || minor > 0xFFFF) {
      throw new IllegalArgumentException("Major version out of range: " + minor);
    }
    minorVersion = minor;
  }

  static abstract class CWItem {
    abstract byte getType();
  }

  public static class CWStringItem extends CWItem {
    final private String s;
    final private byte type;

    public CWStringItem(String s, byte type) {
      this.s = s;
      this.type = type;
    }

    @Override
    public boolean equals(Object o) {
      return o != null && o.getClass().equals(getClass()) && 
          ((CWStringItem) o).type == type &&
          ((CWStringItem) o).s.equals(s);
    }

    @Override
    public int hashCode() {
      return s.hashCode() + (3901 * type) ;
    }

    @Override
    byte getType() {
      return type;
    }
  }

  static class CWRef extends CWItem {
    final protected String c;

    final protected String n;

    final protected String t;

    final private byte type;

    CWRef(byte type, String c, String n, String t) {
      this.type = type;
      this.c = c;
      this.n = n;
      this.t = t;
    }

    @Override
    public boolean equals(Object o) {
      if (o.getClass().equals(getClass())) {
        CWRef r = (CWRef) o;
        return r.type == type && r.c.equals(c) && r.n.equals(n) && r.t.equals(t);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return type + (c.hashCode() << 5) + (n.hashCode() << 3) + t.hashCode();
    }

    @Override
    byte getType() {
      return type;
    }
  }

  static class CWHandle extends CWRef {
    private final byte kind;

    CWHandle(byte type, byte kind, String c, String n, String t) {
      super(type, c, n, t);
      this.kind = kind;
    }
    
    @Override
    public int hashCode() {
      return super.hashCode() * kind;
    }
    
    @Override
    public boolean equals(Object o) {
      return super.equals(o) && ((CWHandle)o).kind == kind;
    }

    public byte getKind() {
      return kind;
    }
  }
  
  static class CWNAT extends CWItem {
    final private String n;

    final private String t;

    CWNAT(String n, String t) {
      this.n = n;
      this.t = t;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof CWNAT) {
        CWNAT r = (CWNAT) o;
        return r.n.equals(n) && r.t.equals(t);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return (n.hashCode() << 3) + t.hashCode();
    }

    @Override
    byte getType() {
      return CONSTANT_NameAndType;
    }
  }

  static class CWInvokeDynamic extends CWItem {
    final private BootstrapMethod b;

    final private String n;

    final private String t;

    CWInvokeDynamic(BootstrapMethod b, String n, String t) {
      this.b = b;
      this.n = n;
      this.t = t;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof CWInvokeDynamic) {
        CWInvokeDynamic r = (CWInvokeDynamic) o;
        return r.b.equals(b) && r.n.equals(n) && r.t.equals(t);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return (b.hashCode() << 10) + (n.hashCode() << 3) + t.hashCode();
    }

    @Override
    byte getType() {
      return CONSTANT_InvokeDynamic;
    }
  }

  /**
   * Copy a constant pool from some ClassReader into this class. This must be done before any entries are allocated in this
   * ClassWriter's constant pool, and it can only be done once. If and only if this is done, it is safe to copy "raw" fields,
   * methods and attributes from the ClassReader into this class, because the constant pool references in those fields, methods and
   * attributes are guaranteed to point to the same constant pool items in this new class.
   * 
   * @param cacheEntries records whether to parse the raw constant pool completely so that if new entries are required which are the
   *          same as entries already in the raw pool, the existing entries in the raw pool are used instead. Setting this to 'true'
   *          produces smaller constant pools but may slow down performance because the raw pool must be completely parsed
   */
  public void setRawCP(ConstantPoolParser cp, boolean cacheEntries) throws InvalidClassFileException, IllegalArgumentException {
    if (cp == null) {
      throw new IllegalArgumentException();
    }
    if (rawCP != null) {
      throw new IllegalArgumentException("Cannot set raw constant pool twice");
    }
    if (nextCPIndex != 1) {
      throw new IllegalArgumentException("Cannot set raw constant pool after allocating new entries");
    }
    rawCP = cp;
    nextCPIndex = cp.getItemCount();

    if (cacheEntries) {
      for (int i = 1; i < nextCPIndex; i++) {
        byte t = cp.getItemType(i);
        switch (t) {
        case CONSTANT_String:
          cachedCPEntries.put(new CWStringItem(cp.getCPString(i), CONSTANT_String), new Integer(i));
          break;
        case CONSTANT_Class:
          cachedCPEntries.put(new CWStringItem(cp.getCPClass(i), CONSTANT_Class), new Integer(i));
          break;
        case CONSTANT_MethodType:
          cachedCPEntries.put(new CWStringItem(cp.getCPMethodType(i), CONSTANT_MethodType), new Integer(i));
          break;
        case CONSTANT_MethodHandle:
        case CONSTANT_FieldRef:
        case CONSTANT_InterfaceMethodRef:
        case CONSTANT_MethodRef:
          cachedCPEntries.put(new CWRef(t, cp.getCPRefClass(i), cp.getCPRefName(i), cp.getCPRefType(i)), new Integer(i));
          break;
        case CONSTANT_NameAndType:
          cachedCPEntries.put(new CWNAT(cp.getCPNATName(i), cp.getCPNATType(i)), new Integer(i));
          break;
        case CONSTANT_InvokeDynamic:
          cachedCPEntries.put(new CWInvokeDynamic(cp.getCPDynBootstrap(i), cp.getCPDynName(i), cp.getCPDynType(i)), new Integer(i));
          break;
        case CONSTANT_Integer:
          cachedCPEntries.put(new Integer(cp.getCPInt(i)), new Integer(i));
          break;
        case CONSTANT_Float:
          cachedCPEntries.put(new Float(cp.getCPFloat(i)), new Integer(i));
          break;
        case CONSTANT_Long:
          cachedCPEntries.put(new Long(cp.getCPLong(i)), new Integer(i));
          break;
        case CONSTANT_Double:
          cachedCPEntries.put(new Double(cp.getCPDouble(i)), new Integer(i));
          break;
        case CONSTANT_Utf8:
          cachedCPEntries.put(cp.getCPUtf8(i), new Integer(i));
          break;
        default:
          throw new UnsupportedOperationException(String.format("unexpected constant-pool item type %s", t));
        }
      }
    }
  }

  /**
   * @param force true iff you want the addCP methods to always create a new constant pool entry and never reuse an existing
   *          constant pool entry
   */
  public void setForceAddCPEntries(boolean force) {
    forceAddCPEntries = force;
  }

  protected int addCPEntry(Object o, int size) {
    if (cachedCPEntries == null) {
      throw new IllegalArgumentException("Cannot add a new constant pool entry during makeBytes() processing!");
    }

    Integer i = forceAddCPEntries ? null : cachedCPEntries.get(o);
    if (i != null) {
      return i.intValue();
    } else {
      int index = nextCPIndex;
      nextCPIndex += size;
      i = new Integer(index);
      cachedCPEntries.put(o, i);
      newCPEntries.add(o);
      if (nextCPIndex > 0xFFFF) {
        throw new IllegalArgumentException("Constant pool item count exceeded");
      }
      return index;
    }
  }

  /**
   * Add a Utf8 string to the constant pool if necessary.
   * 
   * @return the index of a constant pool item with the right value
   */
  public int addCPUtf8(String s) {
    return addCPEntry(s, 1);
  }

  /**
   * Add an Integer to the constant pool if necessary.
   * 
   * @return the index of a constant pool item with the right value
   */
  public int addCPInt(int i) {
    return addCPEntry(new Integer(i), 1);
  }

  /**
   * Add a Float to the constant pool if necessary.
   * 
   * @return the index of a constant pool item with the right value
   */
  public int addCPFloat(float f) {
    return addCPEntry(new Float(f), 1);
  }

  /**
   * Add a Long to the constant pool if necessary.
   * 
   * @return the index of a constant pool item with the right value
   */
  public int addCPLong(long l) {
    return addCPEntry(new Long(l), 2);
  }

  /**
   * Add a Double to the constant pool if necessary.
   * 
   * @return the index of a constant pool item with the right value
   */
  public int addCPDouble(double d) {
    return addCPEntry(new Double(d), 2);
  }

  private int addCPString(String s, byte type) {
    if (s == null) {
      throw new IllegalArgumentException("null s: " + s);
    }
    return addCPEntry(new CWStringItem(s, type), 1);
  }

  public int addCPMethodHandle(ReferenceToken c) {
    if (c == null) {
      throw new IllegalArgumentException("null c: " + c);
    }
    return addCPEntry(new CWHandle(CONSTANT_MethodHandle, c.getKind(), c.getClassName(), c.getElementName(), c.getDescriptor()), 1);
  }

  /**
   * Add a String to the constant pool if necessary.
   * 
   * @return the index of a constant pool item with the right value
   */
  public int addCPString(String s) {
    return addCPString(s, CONSTANT_String);
  }

  /**
   * Add a Class to the constant pool if necessary.
   * 
   * @param s the class name, in JVM format (e.g., java/lang/Object)
   * @return the index of a constant pool item with the right value
   */
  public int addCPClass(String s) {
    return addCPString(s, CONSTANT_Class);
  }

  /**
   * Add a Class to the constant pool if necessary.
   * 
   * @param s the class name, in JVM format (e.g., java/lang/Object)
   * @return the index of a constant pool item with the right value
   */
  public int addCPMethodType(String s) {
    return addCPString(s, CONSTANT_MethodType);
  }

  /**
   * Add a FieldRef to the constant pool if necessary.
   * 
   * @param c the class name, in JVM format (e.g., java/lang/Object)
   * @param n the field name
   * @param t the field type, in JVM format (e.g., I, Z, or Ljava/lang/Object;)
   * @return the index of a constant pool item with the right value
   */
  public int addCPFieldRef(String c, String n, String t) {
    return addCPEntry(new CWRef(CONSTANT_FieldRef, c, n, t), 1);
  }

  /**
   * Add a MethodRef to the constant pool if necessary.
   * 
   * @param c the class name, in JVM format (e.g., java/lang/Object)
   * @param n the method name
   * @param t the method type, in JVM format (e.g., V(ILjava/lang/Object;) )
   * @return the index of a constant pool item with the right value
   */
  public int addCPMethodRef(String c, String n, String t) {
    return addCPEntry(new CWRef(CONSTANT_MethodRef, c, n, t), 1);
  }

  /**
   * Add an InterfaceMethodRef to the constant pool if necessary.
   * 
   * @param c the class name, in JVM format (e.g., java/lang/Object)
   * @param n the field name
   * @param t the method type, in JVM format (e.g., V(ILjava/lang/Object;) )
   * @return the index of a constant pool item with the right value
   */
  public int addCPInterfaceMethodRef(String c, String n, String t) {
    return addCPEntry(new CWRef(CONSTANT_InterfaceMethodRef, c, n, t), 1);
  }

  /**
   * Add a NameAndType to the constant pool if necessary.
   * 
   * @param n the name
   * @param t the type, in JVM format
   * @return the index of a constant pool item with the right value
   */
  public int addCPNAT(String n, String t) {
    return addCPEntry(new CWNAT(n, t), 1);
  }

  /**
   * Add an InvokeDynamic to the constant pool if necessary.
   * 
   * @param n the name
   * @param t the type, in JVM format
   * @return the index of a constant pool item with the right value
   */
  public int addCPInvokeDynamic(BootstrapMethod b, String n, String t) {
    return addCPEntry(new CWInvokeDynamic(b, n, t), 1);
  }

  
  /**
   * Set the access flags for the class.
   */
  public void setAccessFlags(int f) {
    if (f < 0 || f > 0xFFFF) {
      throw new IllegalArgumentException("Access flags out of range: " + f);
    }
    accessFlags = f;
  }

  /**
   * Set the constant pool index for the name of the class.
   */
  public void setNameIndex(int c) throws IllegalArgumentException {
    if (c < 1 || c > 0xFFFF) {
      throw new IllegalArgumentException("Class name index out of range: " + c);
    }
    thisClass = c;
  }

  /**
   * Set the constant pool index for the name of the superclass.
   */
  public void setSuperNameIndex(int c) {
    if (c < 0 || c > 0xFFFF) {
      throw new IllegalArgumentException("Superclass name index out of range: " + c);
    }
    superClass = c;
  }

  /**
   * Set the constant pool indices for the names of the implemented interfaces.
   */
  public void setInterfaceNameIndices(int[] ifaces) {
    if (ifaces != null) {
      if (ifaces.length > 0xFFFF) {
        throw new IllegalArgumentException("Too many interfaces implemented: " + ifaces.length);
      }
      for (int c : ifaces) {
        if (c < 1 || c > 0xFFFF) {
          throw new IllegalArgumentException("Interface name index out of range: " + c);
        }
      }
    }
    superInterfaces = ifaces;
  }

  /**
   * Set the name of the class.
   */
  public void setName(String c) {
    setNameIndex(addCPClass(c));
  }

  /**
   * Set the name of the superclass; if c is null, then there is no superclass (this must be java/lang/Object).
   */
  public void setSuperName(String c) {
    setSuperNameIndex(c == null ? 0 : addCPClass(c));
  }

  /**
   * Set the names of the implemented interfaces.
   */
  public void setInterfaceNames(String[] ifaces) {
    if (ifaces == null) {
      setInterfaceNameIndices((int[]) null);
    } else {
      int[] ifs = new int[ifaces.length];
      for (int i = 0; i < ifaces.length; i++) {
        ifs[i] = addCPClass(ifaces[i]);
      }
      setInterfaceNameIndices(ifs);
    }
  }

  /**
   * An Element is an object that can be serialized into a byte buffer. Serialization via 'copyInto' is performed when the user
   * calls makeBytes() on the ClassWriter. At this time no new constant pool items can be allocated, so any item indices that need
   * to be emitted must be allocated earlier.
   */
  public static abstract class Element {
    public Element() {
    }

    /**
     * @return the number of bytes that will be generated.
     */
    public abstract int getSize();

    /**
     * Copy the bytes into 'buf' at offset 'offset'.
     * 
     * @return the number of bytes copies, which must be equal to getSize()
     */
    public abstract int copyInto(byte[] buf, int offset);
  }

  /**
   * A RawElement is an Element that is already available as some chunk of a byte buffer.
   */
  public static final class RawElement extends Element {
    final private byte[] buf;

    final private int offset;

    final private int len;

    /**
     * Create an Element for the 'len' bytes in 'buf' at offset 'offset'.
     */
    public RawElement(byte[] buf, int offset, int len) {
      this.buf = buf;
      this.offset = offset;
      this.len = len;
    }

    @Override
    public int getSize() {
      return len;
    }

    @Override
    public int copyInto(byte[] dest, int destOffset) {
      System.arraycopy(buf, offset, dest, destOffset, len);
      return destOffset + len;
    }
  }

  /**
   * Add a method to the class, the method data given as "raw" bytes (probably obtained from a ClassReader).
   */
  public void addRawMethod(Element e) {
    methods.add(e);
  }

  /**
   * Add a field to the class, the field data given as "raw" bytes (probably obtained from a ClassReader).
   */
  public void addRawField(Element e) {
    fields.add(e);
  }

  /**
   * Add a method to the class.
   * 
   * @param access the access flags
   * @param name the method name
   * @param type the method type in JVM format (e.g., V(ILjava/lang/Object;) )
   * @param attributes the attributes in raw form, one Element per attribute
   */
  public void addMethod(int access, String name, String type, Element[] attributes) {
    addMethod(access, addCPUtf8(name), addCPUtf8(type), attributes);
  }

  /**
   * Add a field to the class.
   * 
   * @param access the access flags
   * @param name the field name
   * @param type the field type in JVM format (e.g., I, Z, Ljava/lang/Object;)
   * @param attributes the attributes in raw form, one Element per attribute
   */
  public void addField(int access, String name, String type, Element[] attributes) {
    addField(access, addCPUtf8(name), addCPUtf8(type), attributes);
  }

  static final class MemberElement extends Element {
    final private int access;

    final private int name;

    final private int type;

    final private Element[] attributes;

    public MemberElement(int access, int name, int type, Element[] attributes) {
      if (access < 0 || access > 0xFFFF) {
        throw new IllegalArgumentException("Access flags out of range: " + access);
      }
      if (name < 1 || name > 0xFFFF) {
        throw new IllegalArgumentException("Name constant pool index out of range: " + name);
      }
      if (type < 1 || type > 0xFFFF) {
        throw new IllegalArgumentException("Type constant pool index out of range: " + name);
      }
      if (attributes == null) {
        throw new IllegalArgumentException("Atrtributes are null");
      }
      if (attributes.length > 0xFFFF) {
        throw new IllegalArgumentException("Too many attributes: " + attributes.length);
      }

      this.access = access;
      this.name = name;
      this.type = type;
      this.attributes = attributes;
    }

    @Override
    public int getSize() {
      int size = 8;
      if (attributes != null) {
        for (Element attribute : attributes) {
          size += attribute.getSize();
        }
      }
      return size;
    }

    @Override
    public int copyInto(byte[] buf, int offset) {
      setUShort(buf, offset, access);
      setUShort(buf, offset + 2, name);
      setUShort(buf, offset + 4, type);
      if (attributes != null) {
        setUShort(buf, offset + 6, attributes.length);
        offset += 8;
        for (Element attribute : attributes) {
          offset = attribute.copyInto(buf, offset);
        }
      } else {
        setUShort(buf, offset + 6, 0);
        offset += 8;
      }
      return offset;
    }
  }

  /**
   * Add a method to the class.
   * 
   * @param access the access flags
   * @param name the constant pool index of the method name
   * @param type the constant pool index of the method type in JVM format (e.g., V(ILjava/lang/Object;) )
   * @param attributes the attributes in raw form, one Element per attribute
   */
  public void addMethod(int access, int name, int type, Element[] attributes) {
    // int idx=methods.size()-2;
    // if (idx<0) idx=0;
    // methods.add(0,new MemberElement(access, name, type, attributes));
    methods.add(new MemberElement(access, name, type, attributes));
    if (methods.size() > 0xFFFF) {
      throw new IllegalArgumentException("Too many methods");
    }
  }

  /**
   * Add a field to the class.
   * 
   * @param access the access flags
   * @param name the constant pool index of the field name
   * @param type the constant pool index of the field type in JVM format (e.g., I, Z, Ljava/lang/Object;)
   * @param attributes the attributes in raw form, one Element per attribute
   */
  public void addField(int access, int name, int type, Element[] attributes) {
    fields.add(new MemberElement(access, name, type, attributes));
    if (fields.size() > 0xFFFF) {
      throw new IllegalArgumentException("Too many fields");
    }
  }

  /**
   * Add an atttribute to the class.
   * 
   * @param attribute the attribute in raw form
   */
  public void addClassAttribute(Element attribute) {
    classAttributes.add(attribute);
    if (classAttributes.size() > 0xFFFF) {
      throw new IllegalArgumentException("Too many class attributes: " + classAttributes.size());
    }
  }

  private int reserveBuf(int size) {
    if (buf == null) {
      buf = new byte[size];
    } else if (bufLen + size > buf.length) {
      byte[] newBuf = new byte[Math.max(buf.length * 2, bufLen + size)];
      System.arraycopy(buf, 0, newBuf, 0, bufLen);
      buf = newBuf;
    }
    int offset = bufLen;
    bufLen += size;
    return offset;
  }

  private void emitElement(Element e) {
    int size = e.getSize();
    int offset = reserveBuf(size);
    int finalOffset = e.copyInto(buf, offset);
    if (finalOffset - offset != size) {
      throw new Error("Element failed to output the promised bytes: promised " + size + ", got " + (finalOffset - offset));
    }
  }

  private static final char[] noChars = new char[0];

  private void emitConstantPool() {
    if (rawCP != null) {
      int len = rawCP.getRawSize();
      int offset = reserveBuf(len);
      System.arraycopy(rawCP.getRawBytes(), rawCP.getRawOffset(), buf, offset, len);
    }

    char[] chars = noChars;

    // BE CAREFUL: the newCPEntries array grows during this loop.
    for (int i = 0; i < newCPEntries.size(); i++) {
      Object o = newCPEntries.get(i);
      if (o instanceof CWItem) {
        CWItem item = (CWItem) o;
        byte t = item.getType();
        int offset;
        switch (t) {
        case CONSTANT_Class:
        case CONSTANT_String:
        case CONSTANT_MethodType:
          offset = reserveBuf(3);
          setUShort(buf, offset + 1, addCPUtf8(((CWStringItem) item).s));
          break;
        case CONSTANT_NameAndType: {
          offset = reserveBuf(5);
          CWNAT nat = (CWNAT) item;
          setUShort(buf, offset + 1, addCPUtf8(nat.n));
          setUShort(buf, offset + 3, addCPUtf8(nat.t));
          break;
        }
        case CONSTANT_InvokeDynamic: {
          offset = reserveBuf(5);
          CWInvokeDynamic inv = (CWInvokeDynamic) item;
          setUShort(buf, offset+1, inv.b.getIndexInClassFile());
          setUShort(buf, offset+3, addCPNAT(inv.n, inv.t));
          break;
        }
        case CONSTANT_MethodHandle: {
          offset = reserveBuf(4);
          CWHandle handle = (CWHandle) item;
          final byte kind = handle.getKind();
          setUByte(buf, offset + 1, kind);
          switch (kind) {
          case REF_getStatic:
          case REF_getField:
          case REF_putField:
          case REF_putStatic: {
            int x = addCPFieldRef(handle.c, handle.n, handle.t);
            setUShort(buf, offset + 2, x);
            break;
          }
          case REF_invokeVirtual:
          case REF_newInvokeSpecial: {
            int x = addCPMethodRef(handle.c, handle.n, handle.t);
            setUShort(buf, offset + 2, x);
            break;
          }
          case REF_invokeSpecial:
          case REF_invokeStatic: {
            int x = addCPMethodRef(handle.c, handle.n, handle.t);
            setUShort(buf, offset + 2, x);
            break;
          }
          case REF_invokeInterface: {
            int x = addCPInterfaceMethodRef(handle.c, handle.n, handle.t);
            setUShort(buf, offset + 2, x);
            break;
          }
          default:
            throw new UnsupportedOperationException(String.format("unexpected ref kind %s", kind));
          }
         break; 
        }
        case CONSTANT_MethodRef:
        case CONSTANT_FieldRef:
        case CONSTANT_InterfaceMethodRef: {
          offset = reserveBuf(5);
          CWRef ref = (CWRef) item;
          setUShort(buf, offset + 1, addCPClass(ref.c));
          setUShort(buf, offset + 3, addCPNAT(ref.n, ref.t));
          break;
        }
        default:
          throw new Error("Invalid type: " + t);
        }
        buf[offset] = t;
      } else {
        if (o instanceof String) {
          String s = (String) o;
          int slen = s.length();

          if (chars.length < slen) {
            chars = new char[slen];
          }
          s.getChars(0, slen, chars, 0);

          int offset = reserveBuf(3);
          buf[offset] = CONSTANT_Utf8;

          int maxBytes = slen * 3;
          int p = reserveBuf(maxBytes); // worst case reservation

          for (int j = 0; j < slen; j++) {
            char ch = chars[j];
            if (ch == 0) {
              setUShort(buf, p, 0xC080);
              p += 2;
            } else if (ch < 0x80) {
              buf[p] = (byte) ch;
              p += 1;
            } else if (ch < 0x800) {
              buf[p] = (byte) ((ch >> 6) | 0xC0);
              buf[p + 1] = (byte) ((ch & 0x3F) | 0x80);
              p += 2;
            } else {
              buf[p] = (byte) ((ch >> 12) | 0xE0);
              buf[p + 1] = (byte) (((ch >> 6) & 0x3F) | 0x80);
              buf[p + 2] = (byte) ((ch & 0x3F) | 0x80);
              p += 3;
            }
          }
          int bytes = p - (offset + 3);
          reserveBuf(bytes - maxBytes); // negative reservation to push back buf
          // size
          if (bytes > 0xFFFF) {
            throw new IllegalArgumentException("String too long: " + bytes + " bytes");
          }
          setUShort(buf, offset + 1, bytes);
        } else if (o instanceof Integer) {
          int offset = reserveBuf(5);
          buf[offset] = CONSTANT_Integer;
          setInt(buf, offset + 1, ((Integer) o).intValue());
        } else if (o instanceof Long) {
          int offset = reserveBuf(9);
          buf[offset] = CONSTANT_Long;
          setLong(buf, offset + 1, ((Long) o).longValue());
        } else if (o instanceof Float) {
          int offset = reserveBuf(5);
          buf[offset] = CONSTANT_Float;
          setFloat(buf, offset + 1, ((Float) o).intValue());
        } else if (o instanceof Double) {
          int offset = reserveBuf(9);
          buf[offset] = CONSTANT_Double;
          setDouble(buf, offset + 1, ((Double) o).intValue());
        }
      }
    }
  }

  /**
   * After you've added everything you need to the class, call this method to generate the actual class file data. This can only be
   * called once.
   */
  public byte[] makeBytes() throws IllegalArgumentException {
    if (buf != null) {
      throw new IllegalArgumentException("Can't call makeBytes() twice");
    }

    if (thisClass == 0) {
      throw new IllegalArgumentException("No class name set");
    }

    reserveBuf(10);
    setInt(buf, 0, MAGIC);
    setUShort(buf, 4, minorVersion);
    setUShort(buf, 6, majorVersion);

    emitConstantPool();
    // The constant pool can grow during emmission, so store the size last
    setUShort(buf, 8, nextCPIndex);
    // No new constant pool entries can be allocated; make sure we
    // catch any such error by client code
    cachedCPEntries = null;

    int offset = reserveBuf(8);
    setUShort(buf, offset, accessFlags);
    setUShort(buf, offset + 2, thisClass);
    setUShort(buf, offset + 4, superClass);
    if (superInterfaces != null) {
      setUShort(buf, offset + 6, superInterfaces.length);
      reserveBuf(superInterfaces.length * 2);
      for (int i = 0; i < superInterfaces.length; i++) {
        setUShort(buf, offset + 8 + i * 2, superInterfaces[i]);
      }
    } else {
      setUShort(buf, offset + 6, 0);
    }

    offset = reserveBuf(2);
    int numFields = fields.size();
    setUShort(buf, offset, numFields);
    for (int i = 0; i < numFields; i++) {
      emitElement(fields.get(i));
    }

    offset = reserveBuf(2);
    int numMethods = methods.size();
    // Xiangyu, debug
    // System.out.println("numMethods="+numMethods);
    setUShort(buf, offset, numMethods);
    for (int i = 0; i < numMethods; i++) {
      emitElement(methods.get(i));
    }

    offset = reserveBuf(2);
    int numAttrs = classAttributes.size();
    setUShort(buf, offset, numAttrs);
    for (int i = 0; i < numAttrs; i++) {
      emitElement(classAttributes.get(i));
    }

    if (buf.length == bufLen) {
      return buf;
    } else {
      byte[] b = new byte[bufLen];
      System.arraycopy(buf, 0, b, 0, bufLen);
      return b;
    }
  }

  /**
   * Set the byte at offset 'offset' in 'buf' to the unsigned 8-bit value in v.
   * 
   * @throws IllegalArgumentException if buf is null
   */
  public static void setUByte(byte[] buf, int offset, int v) throws IllegalArgumentException {
    if (buf == null) {
      throw new IllegalArgumentException("buf is null");
    }
    try {
      buf[offset] = (byte) v;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid offset: " + offset, e);
    }
  }

  /**
   * Set the 4 bytes at offset 'offset' in 'buf' to the signed 32-bit value in v.
   * 
   * @throws IllegalArgumentException if buf is null
   */
  public static void setInt(byte[] buf, int offset, int v) throws IllegalArgumentException {
    if (buf == null) {
      throw new IllegalArgumentException("buf is null");
    }
    try {
      buf[offset] = (byte) (v >> 24);
      buf[offset + 1] = (byte) (v >> 16);
      buf[offset + 2] = (byte) (v >> 8);
      buf[offset + 3] = (byte) v;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("illegal offset " + offset, e);
    }
  }

  /**
   * Set the 8 bytes at offset 'offset' in 'buf' to the signed 64-bit value in v.
   */
  public static void setLong(byte[] buf, int offset, long v) throws IllegalArgumentException {
    setInt(buf, offset, (int) (v >> 32));
    setInt(buf, offset + 4, (int) v);
  }

  /**
   * Set the 4 bytes at offset 'offset' in 'buf' to the float value in v.
   */
  public static void setFloat(byte[] buf, int offset, float v) throws IllegalArgumentException {
    setInt(buf, offset, Float.floatToIntBits(v));
  }

  /**
   * Set the 8 bytes at offset 'offset' in 'buf' to the double value in v.
   */
  public static void setDouble(byte[] buf, int offset, double v) throws IllegalArgumentException {
    setLong(buf, offset, Double.doubleToRawLongBits(v));
  }

  /**
   * Set the 2 bytes at offset 'offset' in 'buf' to the unsigned 16-bit value in v.
   * 
   * @throws IllegalArgumentException if buf is null
   */
  public static void setUShort(byte[] buf, int offset, int v) throws IllegalArgumentException {
    if (buf == null) {
      throw new IllegalArgumentException("buf is null");
    }
    if (offset < 0 || offset + 1 >= buf.length) {
      throw new IllegalArgumentException("buf is too short " + buf.length + " " + offset);
    }
    try {
      buf[offset] = (byte) (v >> 8);
      buf[offset + 1] = (byte) v;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid offset: " + offset, e);
    }
  }
}
