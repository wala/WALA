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
package com.ibm.wala.shrikeBT;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

import com.ibm.wala.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.util.collections.Pair;

/**
 * This class contains miscellaneous useful functions.
 * 
 * In the documentation below, we refer to a 'Java class name'. These are formatted according to the rules for Class.forName() and
 * Class.getName(). A Java class name must use '$' to separate inner class names from their containing class. There is no way to for
 * Shrike to disambiguate 'A.B' otherwise.
 */
public final class Util {
  private Util() {
    // prevent instantiation
  }

  /**
   * @return the JVM "stack word size" for the given JVM type
   * @throws IllegalArgumentException if s is null
   */
  public static byte getWordSize(String s) {
    if (s == null || s.length() == 0) {
      throw new IllegalArgumentException("invalid s: " + s);
    }
    return getWordSize(s, 0);
  }

  /**
   * @return the JVM "stack word size" for the given JVM type, looking at index 'index'
   */
  static byte getWordSize(String s, int index) {
    switch (s.charAt(index)) {
    case 'V':
      return 0;
    case 'J':
    case 'D':
      return 2;
    default:
      return 1;
    }
  }

  /**
   * Computes the character length of the internal JVM type given by s.substring(i).
   */
  private static int getTypeLength(String s, int i) {
    switch (s.charAt(i)) {
    case 'L':
      return s.indexOf(';', i) - i + 1;
    case '[':
      return getTypeLength(s, i + 1) + 1;
    default:
      return 1;
    }
  }

  /**
   * Compute the total number of JVM "stack words" occupied by the method parameters for method signature "type". Any "this"
   * parameter is not included.
   * 
   * @throws IllegalArgumentException if type is null
   */
  public static int getParamsWordSize(String type) throws IllegalArgumentException {
    if (type == null) {
      throw new IllegalArgumentException("type is null");
    }
    int index = 1;
    int count = 0;

    if (type.indexOf(')', 1) < 0) {
      throw new IllegalArgumentException("Invalid method descriptor (missing ')'): " + type);
    }
    while (type.charAt(index) != ')') {
      count += getWordSize(type, index);
      index += getTypeLength(type, index);
    }
    return count;
  }

  /**
   * Convert a fully-qualified Java class name ('.' separated) into an internal JVM type name ('/' separated, starting with 'L' and
   * ending with ';').
   * 
   * @throws IllegalArgumentException if c is null
   */
  public static String makeType(String c) {
    if (c == null) {
      throw new IllegalArgumentException("c is null");
    }
    if (c.startsWith("[")) {
      return c.replace('.', '/');
    } else if (!c.endsWith(";")) {
      return "L" + c.replace('.', '/') + ";";
    } else {
      return c;
    }
  }

  /**
   * Convert a fully-qualified Java type name (either primitive or class name, '.' separated) into an internal JVM type name (one
   * letter for primitive and '/' separated, starting with 'L' and ending with ';' for class name).
   */
  public static String makeTypeAll(String c) {
    String alias = typeAliases.get(c);
    if (alias != null) {
      return alias;
    } else {
      return makeType(c);
    }
  }

  /**
   * Convert a JVM type name back into a Java class name.
   * 
   * @throws IllegalArgumentException if t is null
   */
  public static String makeClass(String t) throws IllegalArgumentException {
    if (t == null) {
      throw new IllegalArgumentException("t is null");
    }
    if (t.startsWith("[")) {
      return t;
    } else if (!t.startsWith("L")) {
      throw new IllegalArgumentException(t + " is not a valid class type");
    } else {
      return t.substring(1, t.length() - 1).replace('/', '.');
    }
  }

  /**
   * Convert a JVM type name (either for a primitive or a class name) into a Java type name.
   */
  static String makeClassAll(String t) {
    String alias = classAliases.get(t);
    if (alias != null) {
      return alias;
    } else {
      return makeClass(t);
    }
  }

  final private static HashMap<String, String> classAliases;

  final private static HashMap<String, String> typeAliases;

  private static void addAlias(String c, String t) {
    typeAliases.put(c, t);
    classAliases.put(t, c);
  }

  static {
    typeAliases = new HashMap<>();
    classAliases = new HashMap<>();
    addAlias("void", "V");
    addAlias("int", "I");
    addAlias("long", "J");
    addAlias("float", "F");
    addAlias("double", "D");
    addAlias("byte", "B");
    addAlias("char", "C");
    addAlias("short", "S");
    addAlias("boolean", "Z");
  }

  /**
   * Compute the JVM type name for an actual Java class. Names such as "int", "void", etc are also converted to their JVM type
   * names.
   * 
   * @throws IllegalArgumentException if c is null
   */
  public static String makeType(Class<?> c) {
    if (c == null) {
      throw new IllegalArgumentException("c is null");
    }
    String name = c.getName();
    String alias = typeAliases.get(name);
    if (alias != null) {
      return alias;
    } else {
      return makeType(name);
    }
  }

  /**
   * Compute the number of parameters given by method signature "type". Any "this" parameter is not included.
   * 
   * @throws IllegalArgumentException if type == null
   */
  public static int getParamsCount(String type) throws IllegalArgumentException {
    if (type == null || type.length() < 2) {
      throw new IllegalArgumentException("invalid type: " + type);
    }
    int index = 1;
    int count = 0;

    try {
      while (type.charAt(index) != ')') {
        count++;
        index += getTypeLength(type, index);
      }
      return count;
    } catch (StringIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid type: " + type, e);
    }
  }

  /**
   * Extract the types of the parameters given by method signature "type".
   * 
   * @param thisClassType null if the method is static, otherwise the type of "this"
   * @return an array of the parameter types in order, including "this" as the first parameter if thisClassType was non-null
   * @throws IllegalArgumentException if type == null
   */
  public static String[] getParamsTypes(String thisClassType, String type) throws IllegalArgumentException {
    if (type == null) {
      throw new IllegalArgumentException("type == null");
    }
    int count = thisClassType != null ? 1 : 0;
    String[] r = new String[getParamsCount(type) + count];
    int index = 1;

    if (thisClassType != null) {
      r[0] = thisClassType;
    }

    while (type.charAt(index) != ')') {
      int len = getTypeLength(type, index);

      r[count] = type.substring(index, index + len);
      count++;
      index += len;
    }
    return r;
  }

  /**
   * Compute the types of the local variables on entry to a method. Similar to "getParamsTypes" except null array entries are
   * inserted to account for unused local variables because of 2-word parameter values.
   * 
   * @throws IllegalArgumentException if type == null
   */
  public static String[] getParamsTypesInLocals(String thisClassType, String type) throws IllegalArgumentException {
    if (type == null) {
      throw new IllegalArgumentException("type == null");
    }
    int count = thisClassType != null ? 1 : 0;
    String[] r = new String[getParamsWordSize(type) + count];
    int index = 1;

    if (thisClassType != null) {
      r[0] = thisClassType;
    }

    while (type.charAt(index) != ')') {
      int len = getTypeLength(type, index);
      String t = getStackType(type.substring(index, index + len));

      r[count] = t;
      count += getWordSize(t);
      index += len;
    }
    return r;
  }

  /**
   * Compute the promoted type that the JVM uses to manipulate values of type "t" on its working stack.
   * 
   * @throws IllegalArgumentException if t is null
   */
  public static String getStackType(String t) {
    if (t == null || t.length() < 1) {
      throw new IllegalArgumentException("invalid t: " + t);
    }
    switch (t.charAt(0)) {
    case 'Z':
    case 'C':
    case 'B':
    case 'S':
      return "I";
    default:
      return t;
    }
  }

  /**
   * Compute the type "array of t".
   */
  public static String makeArray(String t) {
    return ("[" + t).intern();
  }

  /**
   * @return true iff t is an array type
   */
  public static boolean isArrayType(String t) {
    if (t == null || t.length() == 0) {
      return false;
    } else {
      switch (t.charAt(0)) {
      case '[':
        return true;
      default:
        return false;
      }
    }
  }

  /**
   * @return true iff t is a primitive type
   */
  public static boolean isPrimitiveType(String t) {
    if (t == null || t.length() == 0) {
      return false;
    } else {
      switch (t.charAt(0)) {
      case 'L':
      case '[':
        return false;
      default:
        return true;
      }
    }
  }

  /**
   * Get the return type from a method signature.
   * 
   * @throws IllegalArgumentException if s is null
   */
  public static String getReturnType(String s) {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    return s.substring(s.lastIndexOf(')') + 1);
  }

  /**
   * General "print an error" routine.
   */
  public static void error(String s) {
    System.err.println(s);
    (new Error("Stack Trace")).printStackTrace();
  }

  /**
   * Given a Java Method, compute the VM-style type signature.
   * 
   * @throws IllegalArgumentException if params == null
   */
  public static String computeSignature(Class<?>[] params, Class<?> result) throws IllegalArgumentException {
    if (params == null) {
      throw new IllegalArgumentException("params == null");
    }
    StringBuffer buf = new StringBuffer();
    buf.append("(");
    for (Class<?> param : params) {
      buf.append(makeType(param));
    }
    buf.append(")");
    buf.append(makeType(result));
    return buf.toString();
  }

  /**
   * Make an Instruction which loads the value of a field, given its name and Java Class. The field type is obtained using
   * reflection.
   * 
   * @throws IllegalArgumentException if c is null
   */
  public static GetInstruction makeGet(Class<?> c, String name) {
    if (c == null) {
      throw new IllegalArgumentException("c is null");
    }
    try {
      Field f = c.getField(name);
      return GetInstruction.make(makeType(f.getType()), makeType(c), name, (f.getModifiers() & Constants.ACC_STATIC) != 0);
    } catch (SecurityException e) {
      throw new IllegalArgumentException(e.getMessage());
    } catch (NoSuchFieldException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  /**
   * Make an Instruction which stores the value of a field, given its name and Java Class. The field type is obtained using
   * reflection.
   * 
   * @throws IllegalArgumentException if c is null
   */
  public static PutInstruction makePut(Class<?> c, String name) {
    if (c == null) {
      throw new IllegalArgumentException("c is null");
    }
    try {
      Field f = c.getField(name);
      return PutInstruction.make(makeType(f.getType()), makeType(c), name, (f.getModifiers() & Constants.ACC_STATIC) != 0);
    } catch (SecurityException e) {
      throw new IllegalArgumentException(e.getMessage());
    } catch (NoSuchFieldException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  private static String makeName(String name, Class<?>[] paramTypes) {
    if (paramTypes == null) {
      return name;
    } else {
      StringBuffer buf = new StringBuffer(name);
      buf.append("(");
      for (int i = 0; i < paramTypes.length; i++) {
        if (i > 0) {
          buf.append(",");
        }
        buf.append(paramTypes[i].getName());
      }
      buf.append(")");
      return buf.toString();
    }
  }

  public static Method findMethod(Class<?> c, String name) {
    return findMethod(c, name, null);
  }

  public static Method findMethod(Class<?> c, String name, Class<?>[] paramTypes) {
    if (c == null) {
      throw new IllegalArgumentException("c is null");
    }
    Method[] methods = c.getMethods();
    Method result = null;
    for (Method m : methods) {
      if (m.getName().equals(name) && (paramTypes == null || Arrays.equals(m.getParameterTypes(), paramTypes))) {
        if (result != null) {
          throw new IllegalArgumentException("Method " + makeName(name, paramTypes) + " is ambiguous in class " + c);
        }
        result = m;
      }
    }
    return result;
  }

  /**
   * Make an Instruction which calls a method, given its name, Java Class, and a list of parameter classes to use for overload
   * resolution. Method information is obtained using reflection.
   * 
   * @throws IllegalArgumentException if name is null
   */
  public static InvokeInstruction makeInvoke(Class<?> c, String name, Class<?>[] paramTypes) {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    InvokeInstruction result = null;

    if (name.equals("<init>")) {
      Constructor<?>[] cs = c.getConstructors();
      for (Constructor<?> con : cs) {
        if (paramTypes == null || Arrays.equals(con.getParameterTypes(), paramTypes)) {
          if (result != null) {
            throw new IllegalArgumentException("Constructor " + makeName(name, paramTypes) + " is ambiguous in class " + c);
          }
          result = InvokeInstruction
              .make(computeSignature(con.getParameterTypes(), Void.TYPE), makeType(c), name, Dispatch.SPECIAL);
        }
      }
    } else {
      Method m = findMethod(c, name, paramTypes);
      if (m != null) {
        Dispatch opcode = Dispatch.VIRTUAL;
        if ((m.getModifiers() & Constants.ACC_STATIC) != 0) {
          opcode = Dispatch.STATIC;
        } else if (m.getDeclaringClass().isInterface()) {
          opcode = Dispatch.INTERFACE;
        }
        result = InvokeInstruction.make(computeSignature(m.getParameterTypes(), m.getReturnType()), makeType(c), name, opcode);
      }
    }

    if (result == null) {
      throw new IllegalArgumentException("Method " + makeName(name, paramTypes) + " is not present in class " + c);
    } else {
      return result;
    }
  }

  /**
   * Make an Instruction which calls a method, given its name and Java Class. Method information is obtained using reflection. If
   * there is more than one method with the given name, an error will be thrown.
   * 
   * @throws IllegalArgumentException if name is null
   */
  public static InvokeInstruction makeInvoke(Class<?> c, String name) {
    return makeInvoke(c, name, null);
  }

  /**
   * Compute the type index constant (Constants.TYPE_...) from the JVM type. Returns -1 if the type is not one of the predefined
   * constants.
   */
  static int getTypeIndex(String t) {
    if (t == null) {
      return -1;
    } else {
      int len = t.length();
      if (len < 1) {
        return -1;
      } else {
        char ch = t.charAt(0);
        if (ch < typeIndices.length) {
          int r = typeIndices[ch];
          if (r != 4) {
            if (len > 1) {
              return -1;
            } else {
              return r;
            }
          } else {
            return r;
          }
        } else {
          return -1;
        }
      }
    }
  }

  private static final byte[] typeIndices = makeTypeIndices();

  private static byte[] makeTypeIndices() {
    byte[] r = new byte[128];
    for (int i = 0; i < r.length; i++) {
      r[i] = -1;
    }
    r['I'] = 0;
    r['J'] = 1;
    r['F'] = 2;
    r['D'] = 3;
    r['L'] = 4;
    r['['] = 4;
    r['B'] = 5;
    r['C'] = 6;
    r['S'] = 7;
    r['Z'] = 8;

    return r;
  }

  public static void readFully(InputStream s, byte[] bytes) throws IllegalArgumentException, IllegalArgumentException, IOException {
    if (s == null) {
      throw new IllegalArgumentException("s == null");
    }
    if (bytes == null) {
      throw new IllegalArgumentException("bytes == null");
    }
    int offset = 0;
    while (offset < bytes.length) {
      int r = s.read(bytes, offset, bytes.length - offset);
      if (r < 0) {
        throw new IOException("Class truncated");
      }
      offset += r;
    }
  }

  public static byte[] readFully(InputStream s) throws IOException {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    byte[] bytes = new byte[s.available()];
    readFully(s, bytes);
    int b = s.read();
    if (b < 0) {
      return bytes;
    } else {
      byte[] big = new byte[bytes.length * 2 + 1];
      System.arraycopy(bytes, 0, big, 0, bytes.length);
      big[bytes.length] = (byte) b;
      int offset = bytes.length + 1;
      do {
        if (big.length == offset) {
          // grow array by factor of 2
          bytes = new byte[offset * 2];
          System.arraycopy(big, 0, bytes, 0, offset);
          big = bytes;
        }
        int r = s.read(big, offset, big.length - offset);
        if (r < 0) {
          bytes = new byte[offset];
          System.arraycopy(big, 0, bytes, 0, offset);
          return bytes;
        }
        offset += r;
      } while (true);
    }
  }
  
  public static Pair<boolean[], boolean[]> computeBasicBlocks(IInstruction[] instructions, ExceptionHandler[][] handlers) {

    // Compute r so r[i] == true iff instruction i begins a basic block.
    boolean[] r = new boolean[instructions.length];
    boolean[] catchers = new boolean[instructions.length];
    
    r[0] = true;
    for (int i = 0; i < instructions.length; i++) {
      int[] targets = instructions[i].getBranchTargets();

      // if there are any targets, then break the basic block here.
      // also break the basic block after a return
      if (targets.length > 0 || !instructions[i].isFallThrough()) {
        if (i + 1 < instructions.length && !r[i + 1]) {
          r[i + 1] = true;
        }
      }

      for (int j = 0; j < targets.length; j++) {
        if (!r[targets[j]]) {
          r[targets[j]] = true;
        }
      }
      if (instructions[i].isPEI()) {
        ExceptionHandler[] hs = handlers[i];
        // break the basic block here.
        if (i + 1 < instructions.length && !r[i + 1]) {
          r[i + 1] = true;
        }
        if (hs != null && hs.length > 0) {
          for (int j = 0; j < hs.length; j++) {
            // exceptionHandlers.add(hs[j]);
            if (!r[hs[j].getHandler()]) {
              // we have not discovered the catch block yet.
              // form a new basic block
              r[hs[j].getHandler()] = true;
            }
            catchers[hs[j].getHandler()] = true;
          }
        }
      }
    }

    return Pair.make(r, catchers);
  }
}
