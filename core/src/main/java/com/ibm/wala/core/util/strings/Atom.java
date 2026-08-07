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
package com.ibm.wala.core.util.strings;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import org.jspecify.annotations.NonNull;

/**
 * An utf8-encoded byte string.
 *
 * <p>Atom's are interned (canonicalized) so they may be compared for equality using the "=="
 * operator.
 *
 * <p>Atoms are used to represent names, descriptors, and string literals appearing in a class's
 * constant pool.
 *
 * <p>Some of {@link Atom}'s methods are deprecated. They are not used within WALA itself except by
 * correctness tests, and we do not believe they are used anywhere outside of WALA. If you do use
 * them outside of WALA, please <a href="https://github.com/wala/WALA/issues/new">let the WALA
 * maintainers know</a> so that we don't remove them in the future.
 */
public final class Atom implements Serializable {

  /* Serial version */
  @Serial private static final long serialVersionUID = -3256390509887654329L;

  /**
   * Used to canonicalize Atoms. Since {@code Atom} has value-based {@link #equals(Object)} and
   * {@link #hashCode()}, interning returns the single canonical {@link Atom} for any given content.
   */
  private static Interner<Atom> interner = Interners.newStrongInterner();

  /** The utf8 value this atom represents */
  private final byte[] val;

  /** Cached hash code for this atom. */
  private final int hash;

  /**
   * Find or create an atom.
   *
   * @param str atom value, as string literal whose characters are unicode
   * @return atom
   */
  public static @NonNull Atom findOrCreateUnicodeAtom(String str) {
    byte[] utf8 = UTF8Convert.toUTF8(str);
    return findOrCreate(utf8);
  }

  /**
   * Find or create an atom.
   *
   * @param str atom value, as string literal whose characters are from ascii subset of unicode (not
   *     including null)
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
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
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
  public static Atom findOrCreate(byte[] utf8, int off, int len)
      throws IllegalArgumentException, IllegalArgumentException {

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
    byte[] val = new byte[len];
    for (int i = 0; i < len; ++i) {
      val[i] = utf8[off++];
    }
    return findOrCreate(val);
  }

  public static synchronized @NonNull Atom findOrCreate(byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes is null");
    }
    return interner.intern(new Atom(bytes));
  }

  public static synchronized Atom findOrCreate(ImmutableByteArray b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return findOrCreate(b.b);
  }

  /**
   * Find or create an atom from {@code b[start]} of length {@code length}.
   *
   * @param b the immutable byte array
   * @param start the offset of the first byte
   * @param length the number of bytes
   * @return atom
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
  public static synchronized Atom findOrCreate(ImmutableByteArray b, int start, int length) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return findOrCreate(b.b, start, length);
  }

  /**
   * Clears the interning dictionary. <strong>For use only by unit tests and benchmarks.</strong>
   *
   * <p>After this method returns, the dictionary is empty, so any subsequent call to an interning
   * factory ({@link #findOrCreate(byte[])}, {@link #findOrCreateUnicodeAtom(String)}, {@link
   * #left(int)}, and the like) returns a newly created {@link Atom} that is not identical to any
   * previously returned {@link Atom} for the same content. All previously returned {@link Atom}
   * instances remain valid and usable; only the "one canonical instance per content" interning
   * invariant is broken.
   *
   * <p>This method is synchronized with the interning factories, but it is still not safe to call
   * concurrently with application code, because the interning invariant cannot be restored until
   * the affected content is interned again. Do not call this method from production code.
   */
  static synchronized void resetDictionaryForTesting() {
    interner = Interners.newStrongInterner();
  }

  /** Return printable representation of "this" atom. Does not correctly handle UTF8 translation. */
  @Override
  public String toString() {
    return new String(val);
  }

  /** Return printable representation of "this" atom. */
  public String toUnicodeString() throws java.io.UTFDataFormatException {
    return UTF8Convert.fromUTF8(val);
  }

  /**
   * New Atom containing first count bytes.
   *
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
  public Atom left(int count) {
    return findOrCreate(val, 0, count);
  }

  /** New Atom containing last count bytes */
  public Atom right(int count) {
    return findOrCreate(val, val.length - count, count);
  }

  public boolean startsWith(Atom start) {
    assert (start != null);

    // can't start with something that's longer.
    if (val.length < start.val.length) return false;

    // otherwise, we know that this length is greater than or equal to the length of start.
    for (int i = 0; i < start.val.length; ++i) {
      if (val[i] != start.val[i]) return false;
    }

    return true;
  }

  /**
   * Return array descriptor corresponding to "this" array-element descriptor. this: array-element
   * descriptor - something like "I" or "Ljava/lang/Object;"
   *
   * @return array descriptor - something like "[I" or "[Ljava/lang/Object;"
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
  public Atom arrayDescriptorFromElementDescriptor() {
    byte[] sig = new byte[1 + val.length];
    sig[0] = (byte) '[';
    for (int i = 0, n = val.length; i < n; ++i) sig[i + 1] = val[i];
    return findOrCreate(sig);
  }

  /**
   * Is "this" atom a reserved member name? Note: Sun has reserved all member names starting with
   * '&lt;' for future use. At present, only &lt;init&gt; and &lt;clinit&gt; are used.
   *
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
  public boolean isReservedMemberName() {
    if (length() == 0) {
      return false;
    }
    return val[0] == '<';
  }

  /**
   * Is "this" atom a class descriptor?
   *
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
  public boolean isClassDescriptor() {
    if (length() == 0) {
      return false;
    }
    return val[0] == 'L';
  }

  /**
   * Is "this" atom an array descriptor?
   *
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
  public boolean isArrayDescriptor() {
    if (length() == 0) {
      return false;
    }
    return val[0] == '[';
  }

  /**
   * Is "this" atom a method descriptor?
   *
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
  public boolean isMethodDescriptor() throws IllegalArgumentException {
    if (length() == 0) {
      return false;
    }
    return val[0] == '(';
  }

  public int length() {
    return val.length;
  }

  /** Create atom from given utf8 sequence. */
  private Atom(byte[] utf8) {
    int tmp = 99989;
    for (int i = utf8.length; --i >= 0; ) {
      tmp = 99991 * tmp + utf8[i];
    }
    this.val = utf8;
    this.hash = tmp;
  }

  /**
   * Parse "this" array descriptor to obtain descriptor for array's element type. this: array
   * descriptor - something like "[I"
   *
   * @return array element descriptor - something like "I"
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
  public Atom parseForArrayElementDescriptor() throws IllegalArgumentException {
    if (val.length == 0) {
      throw new IllegalArgumentException("empty atom is not an array");
    }
    return findOrCreate(val, 1, val.length - 1);
  }

  /**
   * Parse "this" array descriptor to obtain number of dimensions in corresponding array type. this:
   * descriptor - something like "[Ljava/lang/String;" or "[[I"
   *
   * @return dimensionality - something like "1" or "2"
   * @throws IllegalStateException if this Atom does not represent an array
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
  public int parseForArrayDimensionality() throws IllegalArgumentException {
    if (val.length == 0) {
      throw new IllegalArgumentException("empty atom is not an array");
    }
    try {
      for (int i = 0; ; ++i) {
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
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
  public Atom parseForInnermostArrayElementDescriptor() throws IllegalArgumentException {
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

  @Override
  public int hashCode() {
    return hash;
  }

  /**
   * Compare atoms by content, rather than by reference. Interning normally ensures that any two
   * {@code Atom}s with equal content are the same instance, in which case this method is equivalent
   * to reference equality. It also correctly distinguishes atoms whose content differs even when
   * their cached hash codes collide.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Atom that)) {
      return false;
    }
    if (hash != that.hash) {
      return false;
    }
    if (val.length != that.val.length) {
      return false;
    }
    for (int i = 0; i < val.length; i++) {
      if (val[i] != that.val[i]) {
        return false;
      }
    }
    return true;
  }

  /** return an array of bytes representing the utf8 characters in this */
  public byte[] getValArray() {
    return val.clone();
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
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
  public boolean contains(byte b) {
    for (byte element : val) {
      if (element == b) {
        return true;
      }
    }
    return false;
  }

  public int rIndex(byte b) {
    for (int i = val.length - 1; i >= 0; --i) {
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

  /**
   * Concatenate a byte with an immutable byte array to form an atom.
   *
   * @param c the leading byte
   * @param b the following bytes
   * @return atom
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
  public static Atom concat(byte c, ImmutableByteArray b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    return concat(c, b.b);
  }

  public static Atom concat(Atom ma, Atom mb) {
    if ((ma == null) || (mb == null)) {
      throw new IllegalArgumentException("argument may not be null!");
    }

    byte[] val = Arrays.copyOf(ma.val, ma.val.length + mb.val.length);
    System.arraycopy(mb.val, 0, val, ma.val.length, mb.val.length);

    return findOrCreate(val);
  }

  /**
   * Is the given immutable byte array an array descriptor?
   *
   * @param b the immutable byte array
   * @return true iff the first byte is '['
   * @deprecated This method is used only by WALA's own unit tests. It may be removed in a future
   *     release.
   */
  @Deprecated(since = "1.9.0")
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
   * Special method that is called by Java deserialization process. Any hash-cons'ed object should
   * implement it, in order to make sure that all equal objects are consolidated.
   */
  @Serial
  private Object readResolve() {
    return findOrCreate(this.val);
  }
}
