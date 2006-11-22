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
package com.ibm.wala.util;

import java.util.HashMap;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;

/*
 * (C) Copyright IBM Corp. 2001
 */

/**
 * An utf8-encoded byte string.
 * 
 * Atom's are interned (canonicalized) so they may be compared for equality
 * using the "==" operator.
 * 
 * Atoms are used to represent names, descriptors, and string literals appearing
 * in a class's constant pool.
 * 
 * @author Bowen Alpern
 * @author Dave Grove
 * @author Derek Lieber
 * @author Stephen Fink
 */
public final class Atom {

  /**
   * Used to canonicalize Atoms, a mapping from AtomKey -> Atom. AtomKeys are
   * not canonical, but Atoms are.
   */
  private static HashMap<AtomKey, Atom> dictionary = HashMapFactory.make();

  /**
   * The utf8 value this atom represents
   */
  final byte val[];

  /**
   * Cached hash code for this atom key.
   */
  private final int hash;

  /**
   * Find or create an atom.
   * 
   * @param str
   *          atom value, as string literal whose characters are unicode
   * @return atom
   */
  public static Atom findOrCreateUnicodeAtom(String str) {
    byte[] utf8 = UTF8Convert.toUTF8(str);
    return findOrCreate(utf8);
  }

  /**
   * Find or create an atom.
   * 
   * @param str
   *          atom value, as string literal whose characters are from ascii
   *          subset of unicode (not including null)
   * @return atom
   */
  public static Atom findOrCreateAsciiAtom(String str) {
    byte[] ascii = str.getBytes();
    return findOrCreate(ascii);
  }

  /**
   * Find or create an atom.
   * 
   * @param utf8
   *          atom value, as utf8 encoded bytes
   * @return atom
   */
  public static Atom findOrCreateUtf8Atom(byte[] utf8) {
    return findOrCreate(utf8);
  }

  public static Atom findOrCreate(byte utf8[], int off, int len) {
    byte val[] = new byte[len];
    for (int i = 0; i < len; ++i)
      val[i] = utf8[off++];
    return findOrCreate(val);
  }

  public static synchronized Atom findOrCreate(byte[] bytes) {
    AtomKey key = new AtomKey(bytes);
    Atom val = dictionary.get(key);
    if (val != null)
      return val;
    val = new Atom(key);
    dictionary.put(key, val);
    return val;
  }

  public static synchronized Atom findOrCreate(ImmutableByteArray b) {
    return findOrCreate(b.b);
  }

  public static synchronized Atom findOrCreate(ImmutableByteArray b, int start, int length) {
    return findOrCreate(b.b, start, length);
  }

  /**
   * Return printable representation of "this" atom. Does not correctly handle
   * UTF8 translation.
   */
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
   * Return array descriptor corresponding to "this" array-element descriptor.
   * this: array-element descriptor - something like "I" or "Ljava/lang/Object;"
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
   * Is "this" atom a reserved member name? Note: Sun has reserved all member
   * names starting with '<' for future use. At present, only <init> and
   * <clinit> are used.
   */
  public final boolean isReservedMemberName() {
    return val[0] == '<';
  }

  /**
   * Is "this" atom a class descriptor?
   */
  public final boolean isClassDescriptor() {
    return val[0] == 'L';
  }

  /**
   * Is "this" atom an array descriptor?
   */
  public final boolean isArrayDescriptor() {
    return val[0] == '[';
  }

  /**
   * Is "this" atom a method descriptor?
   */
  public final boolean isMethodDescriptor() {
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
   * Parse "this" array descriptor to obtain descriptor for array's element
   * type. this: array descriptor - something like "[I"
   * 
   * @return array element descriptor - something like "I"
   */
  public final Atom parseForArrayElementDescriptor() {

    return findOrCreate(val, 1, val.length - 1);
  }

  /**
   * Parse "this" array descriptor to obtain number of dimensions in
   * corresponding array type. this: descriptor - something like
   * "[Ljava/lang/String;" or "[[I"
   * 
   * @return dimensionality - something like "1" or "2"
   */
  public final int parseForArrayDimensionality() {

    for (int i = 0;; ++i)
      if (val[i] != '[')
        return i;
  }

  /**
   * Return the innermost element type reference for an array
   */
  public final Atom parseForInnermostArrayElementDescriptor() {
    int i = 0;
    while (val[i] == '[')
      i++;
    return findOrCreate(val, i, val.length - i);
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
      for (int i = utf8.length; --i >= 0;)
        tmp = 99991 * tmp + utf8[i];
      this.val = utf8;
      this.hash = tmp;
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    public final boolean equals(Object other) {
      if (Assertions.verifyAssertions) {
        Assertions._assert(this.getClass().equals(other.getClass()));
      }
      if (this == other)
        return true;

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
     * Return printable representation of "this" atom. Does not correctly handle
     * UTF8 translation.
     */
    public final String toString() {
      return new String(val);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public final int hashCode() {
      return hash;
    }

  }

  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return hash;
  }

  /*
   * These are canonical
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    return this == obj;
  }

  public byte getVal(int i) {
    return val[i];
  }

  /**
   * @return true iff this atom contains the specified byte
   */
  public boolean contains(byte b) {
    for (int i = 0; i < val.length; i++) {
      if (val[i] == b) {
        return true;
      }
    }
    return false;
  }

  private static Atom concat(byte c, byte[] bs) {
    byte[] val = new byte[bs.length + 1];
    val[0] = c;
    System.arraycopy(bs, 0, val, 1, bs.length);
    return findOrCreate(val);
  }

  public static Atom concat(byte c, ImmutableByteArray b) {
    return concat(c, b.b);
  }

  public static boolean isArrayDescriptor(ImmutableByteArray b) {
    return b.get(0) == '[';
  }
}
