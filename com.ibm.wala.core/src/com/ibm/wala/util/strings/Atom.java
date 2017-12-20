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
package com.ibm.wala.util.strings;

import java.io.Serializable;
import java.util.HashMap;

import com.ibm.wala.util.collections.HashMapFactory;

/**
 * An utf8-encoded byte string.
 * 
 * Atom's are interned (canonicalized) so they may be compared for equality using the "==" operator.
 * 
 * Atoms are used to represent names, descriptors, and string literals appearing in a class's constant pool.
 */
public final class Atom implements Serializable {

  /* Serial version */
  private static final long serialVersionUID = -3256390509887654329L;

  /**
   * Used to canonicalize Atoms, a mapping from AtomKey -&gt; Atom. AtomKeys are not canonical, but Atoms are.
   */
  final private static HashMap<AtomKey, Atom> dictionary = HashMapFactory.make();

  /**
   * The utf8 value this atom represents
   */
  private final byte val[];

  /**
   * Cached hash code for this atom key.
   */
  private final int hash;

  /**
   * Find or create an atom.
   * 
   * @param str atom value, as string literal whose characters are unicode
   * @return atom
   */
  public static Atom findOrCreateUnicodeAtom(String str) {
    byte[] utf8 = UTF8Convert.toUTF8(str);
    return findOrCreate(utf8);
  }

  /**
   * Find or create an atom.
   * 
   * @param str atom value, as string literal whose characters are from ascii subset of unicode (not including null)
   * @return atom
   * @throws IllegalArgumentException if str is null
   */
  public static Atom findOrCreateAsciiAtom(String str) {
    if (str == null) {
      throw new IllegalArgumentException("str is null");
    }
    byte[] ascii = str.getBytes();
    return findOrCreate(ascii);
  }

  /**
   * Find or create an atom.
   * 
   * @param utf8 atom value, as utf8 encoded bytes
   * @return atom
   * @throws IllegalArgumentException if utf8 is null
   */
  public static Atom findOrCreateUtf8Atom(byte[] utf8) {
    if (utf8 == null) {
      throw new IllegalArgumentException("utf8 is null");
    }
    return findOrCreate(utf8);
  }

  /**
   * create an Atom from utf8[off] of length len
   * 
   * @throws IllegalArgumentException if utf8.length &lt;= off
   */
  public static Atom findOrCreate(byte utf8[], int off, int len) throws IllegalArgumentException, IllegalArgumentException,
      IllegalArgumentException {

    if (utf8 == null) {
      throw new IllegalArgumentException("utf8 == null");
    }
    if (len < 0) {
      throw new IllegalArgumentException("len must be >= 0, " + len);
    }
    if (off < 0) {
      throw new IllegalArgumentException("off must be >= 0, " + off);
    }
    if (utf8.length < off + len) {
      throw new IllegalArgumentException("utf8.length < off + len");
    }
    if (off + len < 0) {
      throw new IllegalArgumentException("off + len is too big: " + off + " + " + len);
    }
    byte val[] = new byte[len];
    for (int i = 0; i < len; ++i) {
      val[i] = utf8[off++];
    }
    return findOrCreate(val);

  }

  public static synchronized Atom findOrCreate(byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes is null");
    }
    AtomKey key = new AtomKey(bytes);
    Atom val = dictionary.get(key);
    if (val != null) {
      return val;
    }
    val = new Atom(key);
    dictionary.put(key, val);
    return val;
  }

  public static synchronized Atom findOrCreate(ImmutableByteArray b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return findOrCreate(b.b);
  }

  public static synchronized Atom findOrCreate(ImmutableByteArray b, int start, int length) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return findOrCreate(b.b, start, length);
  }

  /**
   * Return printable representation of "this" atom. Does not correctly handle UTF8 translation.
   */
  @Override
  public final String toString() {
    return new String(val);
  }

  /**
   * Return printable representation of "this" atom.
   */
  public final String toUnicodeString() throws java.io.UTFDataFormatException {
    return UTF8Convert.fromUTF8(val);
  }

  /**
   * New Atom containing first count bytes
   */
  public final Atom left(int count) {
    return findOrCreate(val, 0, count);  
  }

  /**
   * New Atom containing last count bytes
   */
  public final Atom right(int count) {
    return findOrCreate(val, val.length - count, count);  
  }

  public final boolean startsWith(Atom start) {
      assert (start != null);

      // can't start with something that's longer.
      if (val.length < start.val.length)
        return false;

      // otherwise, we know that this length is greater than or equal to the length of start.
      for (int i = 0; i < start.val.length; ++i) {
          if (val[i] != start.val[i])
              return false;
      }

      return true;
  }

  /**
   * Return array descriptor corresponding to "this" array-element descriptor. this: array-element descriptor - something like "I"
   * or "Ljava/lang/Object;"
   * 
   * @return array descriptor - something like "[I" or "[Ljava/lang/Object;"
   */
  public final Atom arrayDescriptorFromElementDescriptor() {
    byte sig[] = new byte[1 + val.length];
    sig[0] = (byte) '[';
    for (int i = 0, n = val.length; i < n; ++i)
      sig[i + 1] = val[i];
    return findOrCreate(sig);
  }

/**
   * Is "this" atom a reserved member name? Note: Sun has reserved all member names starting with '&lt;' for future use. At present,
   * only &lt;init&gt; and &lt;clinit&gt; are used.
   */
  public final boolean isReservedMemberName() {
    if (length() == 0) {
      return false;
    }
    return val[0] == '<';
  }

  /**
   * Is "this" atom a class descriptor?
   */
  public final boolean isClassDescriptor() {
    if (length() == 0) {
      return false;
    }
    return val[0] == 'L';
  }

  /**
   * Is "this" atom an array descriptor?
   */
  public final boolean isArrayDescriptor() {
    if (length() == 0) {
      return false;
    }
    return val[0] == '[';
  }

  /**
   * Is "this" atom a method descriptor?
   */
  public final boolean isMethodDescriptor() throws IllegalArgumentException {
    if (length() == 0) {
      return false;
    }
    return val[0] == '(';
  }

  public final int length() {
    return val.length;
  }

  /**
   * Create atom from given utf8 sequence.
   */
  private Atom(AtomKey key) {
    this.val = key.val;
    this.hash = key.hash;
  }

  /**
   * Parse "this" array descriptor to obtain descriptor for array's element type. this: array descriptor - something like "[I"
   * 
   * @return array element descriptor - something like "I"
   */
  public final Atom parseForArrayElementDescriptor() throws IllegalArgumentException {
    if (val.length == 0) {
      throw new IllegalArgumentException("empty atom is not an array");
    }
    return findOrCreate(val, 1, val.length - 1);
  }

  /**
   * Parse "this" array descriptor to obtain number of dimensions in corresponding array type. this: descriptor - something like
   * "[Ljava/lang/String;" or "[[I"
   * 
   * @return dimensionality - something like "1" or "2"
   * @throws IllegalStateException if this Atom does not represent an array
   */
  public final int parseForArrayDimensionality() throws IllegalArgumentException {
    if (val.length == 0) {
      throw new IllegalArgumentException("empty atom is not an array");
    }
    try {
      for (int i = 0;; ++i) {
        if (val[i] != '[') {
          return i;
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalStateException("not an array: " + this, e);
    }

  }

  /**
   * Return the innermost element type reference for an array
   * 
   * @throws IllegalStateException if this Atom does not represent an array descriptor
   */
  public final Atom parseForInnermostArrayElementDescriptor() throws IllegalArgumentException {
    if (val.length == 0) {
      throw new IllegalArgumentException("empty atom is not an array");
    }
    try {
      int i = 0;
      while (val[i] == '[') {
        i++;
      }
      return findOrCreate(val, i, val.length - i);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalStateException("not an array: " + this, e);
    }
  }

  /**
   * key for the dictionary.
   */
  private final static class AtomKey {
    /**
     * The utf8 value this atom key represents
     */
    private final byte val[];

    /**
     * Cached hash code for this atom key.
     */
    private final int hash;

    /**
     * Create atom from given utf8 sequence.
     */
    private AtomKey(byte utf8[]) {
      int tmp = 99989;
      for (int i = utf8.length; --i >= 0;) {
        tmp = 99991 * tmp + utf8[i];
      }
      this.val = utf8;
      this.hash = tmp;
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public final boolean equals(Object other) {

      assert (other != null && this.getClass().equals(other.getClass()));
      if (this == other) {
        return true;
      }

      AtomKey that = (AtomKey) other;
      if (hash != that.hash)
        return false;
      if (val.length != that.val.length)
        return false;
      for (int i = 0; i < val.length; i++) {
        if (val[i] != that.val[i])
          return false;
      }

      return true;

    }

    /**
     * Return printable representation of "this" atom. Does not correctly handle UTF8 translation.
     */
    @Override
    public final String toString() {
      return new String(val);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode() {
      return hash;
    }

  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return hash;
  }

  /*
   * These are canonical
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  /**
   * return an array of bytes representing the utf8 characters in this
   */
  public byte[] getValArray() {
    byte[] result = new byte[val.length];
    System.arraycopy(val, 0, result, 0, val.length);
    return result;
  }
  
  public byte getVal(int i) throws IllegalArgumentException {
    try {
      return val[i];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Illegal index: " + i + " length is " + val.length, e);
    }
  }

  /**
   * @return true iff this atom contains the specified byte
   */
  public boolean contains(byte b) {
    for (byte element : val) {
      if (element == b) {
        return true;
      }
    }
    return false;
  }

  public int rIndex(byte b) {
    for (int i = val.length - 1; i >=0; --i) {
      if (val[i] == b) {
        return val.length - i;
      }
    }
    return -1;
  }

  private static Atom concat(byte c, byte[] bs) {
    byte[] val = new byte[bs.length + 1];
    val[0] = c;
    System.arraycopy(bs, 0, val, 1, bs.length);
    return findOrCreate(val);
  }

  public static Atom concat(byte c, ImmutableByteArray b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return concat(c, b.b);
  }

  public static Atom concat(Atom ma, Atom mb) {
     if ((ma == null ) || (mb == null)) {
      throw new IllegalArgumentException("argument may not be null!");
    }

    byte[] val = new byte[ma.val.length + mb.val.length];
 
    System.arraycopy(ma.val, 0, val, 0, ma.val.length);
    System.arraycopy(mb.val, 0, val, ma.val.length, mb.val.length);

    return findOrCreate(val);
  }

  public static boolean isArrayDescriptor(ImmutableByteArray b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    if (b.length() == 0) {
      return false;
    }
    return b.get(0) == '[';
  }

  /**
   * Special method that is called by Java deserialization process. Any HashCons'ed object should implement it, in order to make
   * sure that all equal objects are consolidated.
   */
  private Object readResolve() {
    return findOrCreate(this.val);
  }

}
