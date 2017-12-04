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

import static com.ibm.wala.types.TypeName.ArrayMask;
import static com.ibm.wala.types.TypeName.ElementBits;
import static com.ibm.wala.types.TypeName.PointerMask;
import static com.ibm.wala.types.TypeName.ReferenceMask;
import static com.ibm.wala.types.TypeReference.ArrayTypeCode;
import static com.ibm.wala.types.TypeReference.PointerTypeCode;
import static com.ibm.wala.types.TypeReference.ReferenceTypeCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;

/**
 * Some simple utilities used to manipulate Strings
 */
public class StringStuff {

  final private static HashMap<String, String> primitiveClassNames;

  static {
    primitiveClassNames = HashMapFactory.make(10);
    primitiveClassNames.put("int", "I");
    primitiveClassNames.put("long", "J");
    primitiveClassNames.put("short", "S");
    primitiveClassNames.put("byte", "B");
    primitiveClassNames.put("char", "C");
    primitiveClassNames.put("double", "D");
    primitiveClassNames.put("float", "F");
    primitiveClassNames.put("boolean", "Z");
    primitiveClassNames.put("void", "V");
  }

  public static void padWithSpaces(StringBuffer b, int length) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    if (b.length() < length) {
      for (int i = b.length(); i < length; i++) {
        b.append(" ");
      }
    }
  }

  /**
   * Translate a type from a deployment descriptor string into the internal JVM format
   * 
   * eg. [[java/lang/String
   * 
   * @throws IllegalArgumentException if dString is null
   */
  public static String deployment2CanonicalTypeString(String dString) {
    if (dString == null) {
      throw new IllegalArgumentException("dString is null");
    }
    dString = dString.replace('.', '/');
    int arrayIndex = dString.indexOf("[]");
    if (arrayIndex > -1) {
      String baseType = dString.substring(0, arrayIndex);
      int dim = (dString.length() - arrayIndex) / 2;
      baseType = deployment2CanonicalTypeString(baseType);
      StringBuffer result = new StringBuffer("[");
      for (int i = 1; i < dim; i++) {
        result.append("[");
      }
      result.append(baseType);
      return result.toString();
    } else {
      if (primitiveClassNames.get(dString) != null) {
        return primitiveClassNames.get(dString);
      } else {
        return "L" + dString;
      }
    }
  }

  /**
   * Translate a type from a deployment descriptor string into the type expected for use in a method descriptor
   * 
   * eg. [[Ljava.lang.String;
   * 
   * @throws IllegalArgumentException if dString is null
   */
  public static String deployment2CanonicalDescriptorTypeString(String dString) {
    if (dString == null) {
      throw new IllegalArgumentException("dString is null");
    }
    dString = dString.replace('.', '/');
    int arrayIndex = dString.indexOf("[]");
    if (arrayIndex > -1) {
      String baseType = dString.substring(0, arrayIndex);
      int dim = (dString.length() - arrayIndex) / 2;
      baseType = deployment2CanonicalDescriptorTypeString(baseType);
      StringBuffer result = new StringBuffer("[");
      for (int i = 1; i < dim; i++) {
        result.append("[");
      }
      result.append(baseType);
      return result.toString();
    } else {
      if (primitiveClassNames.get(dString) != null) {
        return primitiveClassNames.get(dString);
      } else {
        return "L" + dString + ";";
      }
    }
  }

  public static final TypeName parseForReturnTypeName(String desc) throws IllegalArgumentException {
    return parseForReturnTypeName(Language.JAVA, ImmutableByteArray.make(desc));
  }

  public static final TypeName parseForReturnTypeName(Language l, String desc) throws IllegalArgumentException {
    return parseForReturnTypeName(l, ImmutableByteArray.make(desc));
  }

  /**
   * Parse method descriptor to obtain description of method's return type. TODO: tune this .. probably combine with
   * parseForParameters.
   * 
   * @param b method descriptor - something like "(III)V"
   * @return type description
   * @throws IllegalArgumentException if b is null
   */
  public static final TypeName parseForReturnTypeName(Language l, ImmutableByteArray b) throws IllegalArgumentException {

    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    if (b.length() <= 2) {
      throw new IllegalArgumentException("invalid descriptor: " + b);

    }
    if (b.get(0) != '(') {
      throw new IllegalArgumentException("invalid descriptor: " + b);
    }

    int i = 0;
    while (b.get(i++) != ')')
      ;
    if (b.length() < i + 1) {
      throw new IllegalArgumentException("invalid descriptor: " + b);
    }
    switch (b.get(i)) {
    case TypeReference.VoidTypeCode:
      return TypeReference.Void.getName();
    case TypeReference.BooleanTypeCode:
      return TypeReference.Boolean.getName();
    case TypeReference.ByteTypeCode:
      return TypeReference.Byte.getName();
    case TypeReference.ShortTypeCode:
      return TypeReference.Short.getName();
    case TypeReference.IntTypeCode:
      return TypeReference.Int.getName();
    case TypeReference.LongTypeCode:
      return TypeReference.Long.getName();
    case TypeReference.FloatTypeCode:
      return TypeReference.Float.getName();
    case TypeReference.DoubleTypeCode:
      return TypeReference.Double.getName();
    case TypeReference.CharTypeCode:
      return TypeReference.Char.getName();
    case TypeReference.OtherPrimitiveTypeCode:
      if (b.get(b.length() - 1) == ';') {
        return l.lookupPrimitiveType(new String(b.substring(i + 1, b.length() - i - 2)));
      } else {
        return l.lookupPrimitiveType(new String(b.substring(i + 1, b.length() - i - 1)));
      }
    case TypeReference.ClassTypeCode: // fall through
    case TypeReference.ArrayTypeCode:
      if (b.get(b.length() - 1) == ';') {
        return TypeName.findOrCreate(b, i, b.length() - i - 1);
      } else {
        return TypeName.findOrCreate(b, i, b.length() - i);
      }
    default:
      throw new IllegalArgumentException("unexpected type in descriptor " + b);
    }
  }

  public static final TypeName[] parseForParameterNames(String descriptor) throws IllegalArgumentException {
    return parseForParameterNames(Language.JAVA, ImmutableByteArray.make(descriptor));
  }

  public static final TypeName[] parseForParameterNames(Language l, String descriptor) throws IllegalArgumentException {
    return parseForParameterNames(l, ImmutableByteArray.make(descriptor));
  }

  /**
   * Parse method descriptor to obtain descriptions of method's parameters.
   * 
   * @return parameter descriptions, or null if there are no parameters
   * @throws IllegalArgumentException if b is null
   */
  public static final TypeName[] parseForParameterNames(Language l, ImmutableByteArray b) throws IllegalArgumentException {

    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    if (b.length() <= 2) {
      throw new IllegalArgumentException("invalid descriptor: " + b);

    }
    if (b.get(0) != '(') {
      throw new IllegalArgumentException("invalid descriptor: " + b);
    }

    ArrayList<TypeName> sigs = new ArrayList<>(10);

    int i = 1;
    while (true) {
      switch (b.get(i++)) {
      case TypeReference.VoidTypeCode:
        sigs.add(TypeReference.VoidName);
        continue;
      case TypeReference.BooleanTypeCode:
        sigs.add(TypeReference.BooleanName);
        continue;
      case TypeReference.ByteTypeCode:
        sigs.add(TypeReference.ByteName);
        continue;
      case TypeReference.ShortTypeCode:
        sigs.add(TypeReference.ShortName);
        continue;
      case TypeReference.IntTypeCode:
        sigs.add(TypeReference.IntName);
        continue;
      case TypeReference.LongTypeCode:
        sigs.add(TypeReference.LongName);
        continue;
      case TypeReference.FloatTypeCode:
        sigs.add(TypeReference.FloatName);
        continue;
      case TypeReference.DoubleTypeCode:
        sigs.add(TypeReference.DoubleName);
        continue;
      case TypeReference.CharTypeCode:
        sigs.add(TypeReference.CharName);
        continue;
      case TypeReference.OtherPrimitiveTypeCode: {
        int off = i - 1;
        while (b.get(i++) != ';')
          ;
        sigs.add(l.lookupPrimitiveType(new String(b.substring(off + 1, i - off - 2))));

        continue;
      }
      case TypeReference.ClassTypeCode: {
        int off = i - 1;
        while (b.get(i++) != ';')
          ;
        sigs.add(TypeName.findOrCreate(b, off, i - off - 1));

        continue;
      }
      case TypeReference.ArrayTypeCode: 
      case TypeReference.PointerTypeCode: 
      case TypeReference.ReferenceTypeCode: {
        int off = i - 1;
        while (StringStuff.isTypeCodeChar(b, i)) {
           ++i;
        }
        TypeName T = null;
        byte c = b.get(i++);
        if (c == TypeReference.ClassTypeCode || c == TypeReference.OtherPrimitiveTypeCode) {
          while (b.get(i++) != ';')
            ;
          T = TypeName.findOrCreate(b, off, i - off - 1);
        } else {
          T = TypeName.findOrCreate(b, off, i - off);
        }
        sigs.add(T);

        continue;
      }
      case (byte) ')': // end of parameter list
        int size = sigs.size();
        if (size == 0) {
          return null;
        }
        Iterator<TypeName> it = sigs.iterator();
        TypeName[] result = new TypeName[size];
        for (int j = 0; j < size; j++) {
          result[j] = it.next();
        }
        return result;
      default:
        assert false : "bad descriptor " + b;
      }
    }
  }

  public static boolean isTypeCodeChar(ImmutableByteArray name, int i) {
    return name.b[i] == TypeReference.ArrayTypeCode || 
    name.b[i] == TypeReference.PointerTypeCode || 
    name.b[i] == TypeReference.ReferenceTypeCode;
  }
  
  /**
   * Given that name[start:start+length] is a Type name in JVM format, parse it for the package
   * 
   * @return an ImmutableByteArray that represents the package, or null if it's the unnamed package
   * @throws IllegalArgumentException if name == null
   */
  public static ImmutableByteArray parseForPackage(ImmutableByteArray name, int start, int length) throws IllegalArgumentException {
    try {
      if (name == null) {
        throw new IllegalArgumentException("name == null");
      }
      int lastSlash = -1;
      for (int i = start; i < start + length; i++) {
        if (name.b[i] == '/') {
          lastSlash = i;
        }
      }
      if (lastSlash == -1) {
        return null;
      }
      short dim = 0;
      while (isTypeCodeChar(name, start+dim)) {
        dim++;
      }
      return new ImmutableByteArray(name.b, start + 1 + dim, lastSlash - start - 1 - dim);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid name " + name + " start: " + start + " length: " + length, e);
    }
  }

  /**
   * Given that name[start:start+length] is a Type name in JVM format, parse it for the package
   * 
   * @return an ImmutableByteArray that represents the package, or null if it's the unnamed package
   * @throws IllegalArgumentException if name is null
   */
  public static ImmutableByteArray parseForPackage(ImmutableByteArray name) {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    return parseForPackage(name, 0, name.length());
  }

  /**
   * Given that name[start:start+length] is a Type name in JVM format, strip the package and return the "package-free" class name
   * 
   * TODO: inefficient; needs tuning.
   * 
   * @return an ImmutableByteArray that represents the package, or null if it's the unnamed package
   * @throws IllegalArgumentException if name is null or malformed
   */
  public static ImmutableByteArray parseForClass(ImmutableByteArray name, int start, int length) throws IllegalArgumentException {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    if (name.length() == 0) {
      throw new IllegalArgumentException("invalid class name: zero length");
    }
    try {
      if (parseForPackage(name, start, length) == null) {
        while (isTypeCodeChar(name, start)) {
          start++;
          length--;
        }
        if (name.b[start] == 'L') {
          start++;
          length--;
        }
        return new ImmutableByteArray(name.b, start, length);
      } else {
        int lastSlash = 0;
        for (int i = start; i < start + length; i++) {
          if (name.b[i] == '/') {
            lastSlash = i;
          }
        }
        int L = length - (lastSlash - start + 1);
        return new ImmutableByteArray(name.b, lastSlash + 1, L);
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Malformed name: " + name + " " + start + " " + length, e);
    }
  }

  /**
   * Given that name[start:start+length] is a Type name in JVM format, strip the package and return the "package-free" class name
   * 
   * @return an ImmutableByteArray that represents the package, or null if it's the unnamed package
   * @throws IllegalArgumentException if name is null
   */
  public static ImmutableByteArray parseForClass(ImmutableByteArray name) throws IllegalArgumentException {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    return parseForClass(name, 0, name.length());
  }

  /**
   * Parse an array descriptor to obtain number of dimensions in corresponding array type. b: descriptor - something like
   * "[Ljava/lang/String;" or "[[I"
   * 
   * @return dimensionality - something like "1" or "2"
   * @throws IllegalArgumentException if b == null
   */
  public static int parseForArrayDimensionality(ImmutableByteArray b, int start, int length) throws IllegalArgumentException,
      IllegalArgumentException {

    if (b == null) {
      throw new IllegalArgumentException("b == null");
    }
    try {
      int code = 0;
      for (int i = start; i < start + length; ++i) {
        if (isTypeCodeChar(b, i)) {
           code <<= ElementBits;
           switch (b.b[i]) {
           case ArrayTypeCode: code |= ArrayMask; break;
           case PointerTypeCode: code |= PointerMask; break;
           case ReferenceTypeCode: code |= ReferenceMask; break;
           default:
             throw new IllegalArgumentException("ill-formed array descriptor " + b);
           }
        } else {
          // type codes must be at the start of the descriptor; if we see something else, stop
          break;
        }
      }
      return code;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("ill-formed array descriptor " + b, e);
    }
  }

  /**
   * Parse an array descriptor to obtain number of dimensions in corresponding array type. b: descriptor - something like
   * "[Ljava/lang/String;" or "[[I"
   * 
   * @return dimensionality - something like "Ljava/lang/String;" or "I"
   * @throws IllegalArgumentException if b is null
   */
  public static ImmutableByteArray parseForInnermostArrayElementDescriptor(ImmutableByteArray b, int start, int length) {

    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    try {
      int i = start;
      for (; i < start + length; ++i) {
        if (! isTypeCodeChar(b, i)) {
          break;
        }
      }
      return new ImmutableByteArray(b.b, i, length - (i - start));
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid element desciptor: " + b, e);
    }
  }

  /**
   * Parse an array descriptor to obtain number of dimensions in corresponding array type. b: descriptor - something like
   * "[Ljava/lang/String;" or "[[I"
   * 
   * @return dimensionality - something like "Ljava/lang/String;" or "I"
   * @throws IllegalArgumentException if a is null
   */
  public static ImmutableByteArray parseForInnermostArrayElementDescriptor(Atom a) {
    if (a == null) {
      throw new IllegalArgumentException("a is null");
    }
    ImmutableByteArray b = new ImmutableByteArray(a.getValArray());
    return parseForInnermostArrayElementDescriptor(b, 0, b.length());
  }

  /**
   * @return true iff the class returned by parseForClass is primitive
   * @throws IllegalArgumentException if name is null
   */
  public static boolean classIsPrimitive(ImmutableByteArray name, int start, int length) throws IllegalArgumentException {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    try {
      while (length > 0 && isTypeCodeChar(name, start)) {
        start++;
        length--;
      }
      if (start >= name.b.length) {
        throw new IllegalArgumentException("ill-formed type name: " + name);
      }

      return name.b[start] != 'L';
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException(name.toString(), e);
    }
  }

  /**
   * @param methodSig something like "java_cup.lexer.advance()V"
   * @throws IllegalArgumentException if methodSig is null
   */
  public static MethodReference makeMethodReference(String methodSig) throws IllegalArgumentException {
    return makeMethodReference(Language.JAVA, methodSig);
  }

  public static MethodReference makeMethodReference(Language l, String methodSig) throws IllegalArgumentException {
    if (methodSig == null) {
      throw new IllegalArgumentException("methodSig is null");
    }
    if (methodSig.lastIndexOf('.') < 0) {
      throw new IllegalArgumentException("ill-formed sig " + methodSig);
    }
    String type = methodSig.substring(0, methodSig.lastIndexOf('.'));
    type = deployment2CanonicalTypeString(type);
    TypeReference t = TypeReference.findOrCreate(ClassLoaderReference.Application, type);

    String methodName = methodSig.substring(methodSig.lastIndexOf('.') + 1, methodSig.indexOf('('));
    String desc = methodSig.substring(methodSig.indexOf('('));

    return MethodReference.findOrCreate(l, t, methodName, desc);
  }

  /**
   * Convert a JVM encoded type name to a readable type name.
   * 
   * @param jvmType a String containing a type name in JVM internal format.
   * @return the same type name in readable (source code) format.
   * @throws IllegalArgumentException if jvmType is null
   */
  public static String jvmToReadableType(final String jvmType) throws IllegalArgumentException {
    if (jvmType == null) {
      throw new IllegalArgumentException("jvmType is null");
    }
    StringBuffer readable = new StringBuffer(); // human readable version
    int numberOfDimensions = 0; // the number of array dimensions

    if (jvmType.length() == 0) {
      throw new IllegalArgumentException("ill-formed type : " + jvmType);
    }

    // cycle through prefixes of '['
    char prefix = jvmType.charAt(0);
    while (prefix == '[') {
      numberOfDimensions++;
      prefix = jvmType.charAt(numberOfDimensions);
    }
    if (prefix == 'V') {
      readable.append("void");
    } else if (prefix == 'B') {
      readable.append("byte");
    } else if (prefix == 'C') {
      readable.append("char");
    } else if (prefix == 'D') {
      readable.append("double");
    } else if (prefix == 'F') {
      readable.append("float");
    } else if (prefix == 'I') {
      readable.append("int");
    } else if (prefix == 'J') {
      readable.append("long");
    } else if (prefix == 'S') {
      readable.append("short");
    } else if (prefix == 'Z') {
      readable.append("boolean");
    } else if (prefix == 'L') {
      readable.append(jvmType.substring(numberOfDimensions + 1, // strip
          // all
          // leading
          // '[' &
          // 'L'
          jvmType.length()) // Trim off the trailing ';'
          );
      // Convert to standard Java dot-notation
      readable = new StringBuffer(slashToDot(readable.toString()));
      readable = new StringBuffer(dollarToDot(readable.toString()));
    }
    // append trailing "[]" for each array dimension
    for (int i = 0; i < numberOfDimensions; ++i) {
      readable.append("[]");
    }
    return readable.toString();
  }

  /**
   * Convert a JVM encoded type name to a binary type name. This version does not call dollarToDot.
   * 
   * @param jvmType a String containing a type name in JVM internal format.
   * @return the same type name in readable (source code) format.
   * @throws IllegalArgumentException if jvmType is null
   */
  public static String jvmToBinaryName(String jvmType) throws IllegalArgumentException {
    if (jvmType == null) {
      throw new IllegalArgumentException("jvmType is null");
    }
    StringBuffer readable = new StringBuffer(); // human readable version
    int numberOfDimensions = 0; // the number of array dimensions

    if (jvmType.length() == 0) {
      throw new IllegalArgumentException("ill-formed type : " + jvmType);
    }

    // cycle through prefixes of '['
    char prefix = jvmType.charAt(0);
    while (prefix == '[') {
      numberOfDimensions++;
      prefix = jvmType.charAt(numberOfDimensions);
    }
    if (prefix == 'V') {
      readable.append("void");
    } else if (prefix == 'B') {
      readable.append("byte");
    } else if (prefix == 'C') {
      readable.append("char");
    } else if (prefix == 'D') {
      readable.append("double");
    } else if (prefix == 'F') {
      readable.append("float");
    } else if (prefix == 'I') {
      readable.append("int");
    } else if (prefix == 'J') {
      readable.append("long");
    } else if (prefix == 'S') {
      readable.append("short");
    } else if (prefix == 'Z') {
      readable.append("boolean");
    } else if (prefix == 'L') {
      readable.append(jvmType.substring(numberOfDimensions + 1, // strip
          // all
          // leading
          // '[' &
          // 'L'
          jvmType.length()) // Trim off the trailing ';'
          );
      // Convert to standard Java dot-notation
      readable = new StringBuffer(slashToDot(readable.toString()));
    }
    // append trailing "[]" for each array dimension
    for (int i = 0; i < numberOfDimensions; ++i) {
      readable.append("[]");
    }
    return readable.toString();
  }

  /**
   * Convert '/' to '.' in names.
   * 
   * @return a String object obtained by replacing the forward slashes ('/') in the String passed as argument with ('.').
   * @throws IllegalArgumentException if path is null
   */
  public static String slashToDot(String path) {
    if (path == null) {
      throw new IllegalArgumentException("path is null");
    }
    StringBuffer dotForm = new StringBuffer(path);
    // replace all '/' in the path with '.'
    for (int i = 0; i < dotForm.length(); ++i) {
      if (dotForm.charAt(i) == '/') {
        dotForm.setCharAt(i, '.'); // replace '/' with '.'
      }
    }
    return dotForm.toString();
  }

  /**
   * Convert '$' to '.' in names.
   * 
   * @param path a string object in which dollar signs('$') must be converted to dots ('.').
   * @return a String object obtained by replacing the dollar signs ('S') in the String passed as argument with ('.').
   * @throws IllegalArgumentException if path is null
   */
  public static String dollarToDot(String path) {
    if (path == null) {
      throw new IllegalArgumentException("path is null");
    }
    StringBuffer dotForm = new StringBuffer(path);
    // replace all '$' in the path with '.'
    for (int i = 0; i < dotForm.length(); ++i) {
      if (dotForm.charAt(i) == '$') {
        dotForm.setCharAt(i, '.'); // replace '$' with '.'
      }
    }
    return dotForm.toString();
  }

  /**
   * Convert '.' to '$' in names.
   * 
   * @param path String object in which dollar signs('$') must be converted to dots ('.').
   * @return a String object obtained by replacing the dollar signs ('S') in the String passed as argument with ('.').
   * @throws IllegalArgumentException if path is null
   */
  public static String dotToDollar(String path) {
    if (path == null) {
      throw new IllegalArgumentException("path is null");
    }
    StringBuffer dotForm = new StringBuffer(path);
    // replace all '.' in the path with '$'
    for (int i = 0; i < dotForm.length(); ++i) {
      if (dotForm.charAt(i) == '.') {
        dotForm.setCharAt(i, '$'); // replace '$' with '.'
      }
    }
    return dotForm.toString();
  }

  /**
   * Return the right position of the string up to '.' or '/' stripping ';'
   */
  public static String toBasename(String typeName) {
      int start = 0;
      int stop = typeName.length() - 1;

      if (typeName.contains(".")) {
        start = typeName.lastIndexOf(".");
      } else if (typeName.contains("/")) {
        start = typeName.lastIndexOf("/");
      }

      if (typeName.endsWith(";")) {
        stop--;
      }

      return typeName.substring(start, stop);
  }
}
