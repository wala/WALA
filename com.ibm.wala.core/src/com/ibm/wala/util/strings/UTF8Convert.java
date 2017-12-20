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

import java.io.UTFDataFormatException;

import com.ibm.wala.util.debug.Assertions;

/**
 * Abstract class that contains conversion routines to/from utf8 and/or pseudo-utf8. It does not support utf8 encodings of more than
 * 3 bytes.
 * 
 * The difference between utf8 and pseudo-utf8 is the special treatment of null. In utf8, null is encoded as a single byte directly,
 * whereas in pseudo-utf8, it is encoded as a two-byte sequence. See the JVM spec for more information.
 */
public abstract class UTF8Convert {

  /**
   * Strictly check the format of the utf8/pseudo-utf8 byte array in fromUTF8.
   */
  static final boolean STRICTLY_CHECK_FORMAT = false;

  /**
   * Set fromUTF8 to not throw an exception when given a normal utf8 byte array.
   */
  static final boolean ALLOW_NORMAL_UTF8 = false;

  /**
   * Set fromUTF8 to not throw an exception when given a pseudo utf8 byte array.
   */
  static final boolean ALLOW_PSEUDO_UTF8 = true;

  /**
   * Set toUTF8 to write in pseudo-utf8 (rather than normal utf8).
   */
  static final boolean WRITE_PSEUDO_UTF8 = true;

  /**
   * Convert the given sequence of (pseudo-)utf8 formatted bytes into a String.
   * 
   * The acceptable input formats are controlled by the STRICTLY_CHECK_FORMAT, ALLOW_NORMAL_UTF8, and ALLOW_PSEUDO_UTF8 flags.
   * 
   * @param utf8 (pseudo-)utf8 byte array
   * @throws UTFDataFormatException if the (pseudo-)utf8 byte array is not valid (pseudo-)utf8
   * @return unicode string
   * @throws IllegalArgumentException if utf8 is null
   */
  @SuppressWarnings("unused")
  public static String fromUTF8(byte[] utf8) throws UTFDataFormatException {
    if (utf8 == null) {
      throw new IllegalArgumentException("utf8 is null");
    }
    char[] result = new char[utf8.length];
    int result_index = 0;
    for (int i = 0, n = utf8.length; i < n;) {
      byte b = utf8[i++];
      if (STRICTLY_CHECK_FORMAT && !ALLOW_NORMAL_UTF8)
        if (b == 0)
          throw new UTFDataFormatException("0 byte encountered at location " + (i - 1));
      if (b >= 0) { // < 0x80 unsigned
        // in the range '\001' to '\177'
        result[result_index++] = (char) b;
        continue;
      }
      try {
        byte nb = utf8[i++];
        if (b < -32) { // < 0xe0 unsigned
          // '\000' or in the range '\200' to '\u07FF'
          char c = result[result_index++] = (char) (((b & 0x1f) << 6) | (nb & 0x3f));
          if (STRICTLY_CHECK_FORMAT) {
            if (((b & 0xe0) != 0xc0) || ((nb & 0xc0) != 0x80))
              throw new UTFDataFormatException("invalid marker bits for double byte char at location " + (i - 2));
            if (c < '\200') {
              if (!ALLOW_PSEUDO_UTF8 || (c != '\000'))
                throw new UTFDataFormatException("encountered double byte char that should have been single byte at location "
                    + (i - 2));
            } else if (c > '\u07FF')
              throw new UTFDataFormatException("encountered double byte char that should have been triple byte at location "
                  + (i - 2));
          }
        } else {
          byte nnb = utf8[i++];
          // in the range '\u0800' to '\uFFFF'
          char c = result[result_index++] = (char) (((b & 0x0f) << 12) | ((nb & 0x3f) << 6) | (nnb & 0x3f));
          if (STRICTLY_CHECK_FORMAT) {
            if (((b & 0xf0) != 0xe0) || ((nb & 0xc0) != 0x80) || ((nnb & 0xc0) != 0x80))
              throw new UTFDataFormatException("invalid marker bits for triple byte char at location " + (i - 3));
            if (c < '\u0800')
              throw new UTFDataFormatException("encountered triple byte char that should have been fewer bytes at location "
                  + (i - 3));
          }
        }
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new UTFDataFormatException("unexpected end at location " + i);
      }
    }
    return new String(result, 0, result_index);
  }

  /**
   * Convert the given String into a sequence of (pseudo-)utf8 formatted bytes.
   * 
   * The output format is controlled by the WRITE_PSEUDO_UTF8 flag.
   * 
   * @param s String to convert
   * @return array containing sequence of (pseudo-)utf8 formatted bytes
   * @throws IllegalArgumentException if s is null
   */
  public static byte[] toUTF8(String s) {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    byte[] result = new byte[utfLength(s)];
    int result_index = 0;
    for (int i = 0, n = s.length(); i < n; ++i) {
      char c = s.charAt(i);
      // in all shifts below, c is an (unsigned) char,
      // so either >>> or >> is ok
      if (((!WRITE_PSEUDO_UTF8) || (c >= 0x0001)) && (c <= 0x007F))
        result[result_index++] = (byte) c;
      else if (c > 0x07FF) {
        result[result_index++] = (byte) (0xe0 | (byte) (c >> 12));
        result[result_index++] = (byte) (0x80 | ((c & 0xfc0) >> 6));
        result[result_index++] = (byte) (0x80 | (c & 0x3f));
      } else {
        result[result_index++] = (byte) (0xc0 | (byte) (c >> 6));
        result[result_index++] = (byte) (0x80 | (c & 0x3f));
      }
    }
    return result;
  }

  /**
   * Returns the length of a string's UTF encoded form.
   * 
   * @throws IllegalArgumentException if s is null
   */
  public static int utfLength(String s) {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    int utflen = 0;
    for (int i = 0, n = s.length(); i < n; ++i) {
      int c = s.charAt(i);
      if (((!WRITE_PSEUDO_UTF8) || (c >= 0x0001)) && (c <= 0x007F))
        ++utflen;
      else if (c > 0x07FF)
        utflen += 3;
      else
        utflen += 2;
    }
    return utflen;
  }

  /**
   * Check whether the given sequence of bytes is valid (pseudo-)utf8.
   * 
   * @param bytes byte array to check
   * @return true iff the given sequence is valid (pseudo-)utf8.
   * @throws IllegalArgumentException if bytes is null
   */
  public static boolean check(byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes is null");
    }
    for (int i = 0, n = bytes.length; i < n;) {
      byte b = bytes[i++];
      if (!ALLOW_NORMAL_UTF8)
        if (b == 0)
          return false;
      if (b >= 0) { // < 0x80 unsigned
        // in the range '\001' to '\177'
        continue;
      }
      try {
        byte nb = bytes[i++];
        if (b < -32) { // < 0xe0 unsigned
          // '\000' or in the range '\200' to '\u07FF'
          char c = (char) (((b & 0x1f) << 6) | (nb & 0x3f));
          if (((b & 0xe0) != 0xc0) || ((nb & 0xc0) != 0x80))
            return false;
          if (c < '\200') {
            if (!ALLOW_PSEUDO_UTF8 || (c != '\000'))
              return false;
          } else if (c > '\u07FF')
            return false;
        } else {
          byte nnb = bytes[i++];
          // in the range '\u0800' to '\uFFFF'
          char c = (char) (((b & 0x0f) << 12) | ((nb & 0x3f) << 6) | (nnb & 0x3f));
          if (((b & 0xf0) != 0xe0) || ((nb & 0xc0) != 0x80) || ((nnb & 0xc0) != 0x80))
            return false;
          if (c < '\u0800')
            return false;
        }
      } catch (ArrayIndexOutOfBoundsException e) {
        return false;
      }
    }
    return true;
  }

  public static String fromUTF8(ImmutableByteArray s) {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    try {
      return fromUTF8(s.b);
    } catch (UTFDataFormatException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }
}
