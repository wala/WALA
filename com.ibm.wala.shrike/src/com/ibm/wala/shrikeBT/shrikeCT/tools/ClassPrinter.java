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
package com.ibm.wala.shrikeBT.shrikeCT.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;

import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.Decoder.InvalidBytecodeException;
import com.ibm.wala.shrikeBT.Disassembler;
import com.ibm.wala.shrikeBT.shrikeCT.CTDecoder;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.AnnotationsReader;
import com.ibm.wala.shrikeCT.AnnotationsReader.AnnotationAttribute;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.CodeReader;
import com.ibm.wala.shrikeCT.ConstantPoolParser;
import com.ibm.wala.shrikeCT.ConstantValueReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.shrikeCT.LineNumberTableReader;
import com.ibm.wala.shrikeCT.LocalVariableTableReader;
import com.ibm.wala.shrikeCT.SignatureReader;
import com.ibm.wala.shrikeCT.SourceFileReader;

/**
 * This class prints the contents of a class file. It's like an alternative to javap that shows more information.
 * 
 * In Unix I run it like this: java -cp ~/dev/shrike/shrike com.ibm.wala.shrikeBT.shrikeCT.tools.ClassPrinter test.jar This will
 * print the contents of every class in the JAR file.
 * 
 * @author roca
 */
public class ClassPrinter {
  final private PrintWriter w;

  private boolean printLineNumberInfo = true;

  private boolean printConstantPool = true;

  /**
   * Get ready to print a class to the given output stream.
   */
  public ClassPrinter(PrintWriter w) {
    this.w = w;
  }

  /**
   * Controls whether to print line number information. The default is 'true'.
   */
  public void setPrintLineNumberInfo(boolean b) {
    printLineNumberInfo = b;
  }

  /**
   * Controls whether to print all the constant pool entries. The default is 'true'.
   */
  public void setPrintConstantPool(boolean b) {
    printConstantPool = b;
  }

  public static void main(String[] args) throws Exception {
    OfflineInstrumenter oi = new OfflineInstrumenter();
    args = oi.parseStandardArgs(args);

    PrintWriter w = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));

    ClassPrinter p = new ClassPrinter(w);

    ClassInstrumenter ci;
    oi.beginTraversal();
    while ((ci = oi.nextClass()) != null) {
      try {
        p.doClass(ci.getReader());
      } finally {
        w.flush();
      }
    }

    oi.close();
  }

  private static final char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

  private static String makeHex(byte[] bytes, int pos, int len, int padTo) {
    StringBuffer b = new StringBuffer();
    for (int i = pos; i < pos + len; i++) {
      byte v = bytes[i];
      b.append(hexChars[(v >> 4) & 0xF]);
      b.append(hexChars[v & 0xF]);
    }
    while (b.length() < padTo) {
      b.append(' ');
    }
    return b.toString();
  }

  private static String makeChars(byte[] bytes, int pos, int len) {
    StringBuffer b = new StringBuffer();
    for (int i = pos; i < pos + len; i++) {
      char ch = (char) bytes[i];
      if (ch < 32 || ch > 127) {
        b.append('.');
      } else {
        b.append(ch);
      }
    }
    return b.toString();
  }

  private static String getClassName(ClassReader cr, int index) throws InvalidClassFileException {
    if (index == 0) {
      return "any";
    } else {
      return cr.getCP().getCPClass(index);
    }
  }

  private static String dumpFlags(int flags) {
    StringBuffer buf = new StringBuffer();
    Class<Constants> c = Constants.class;
    Field[] fs = c.getDeclaredFields();
    for (Field element : fs) {
      String name = element.getName();
      if (name.startsWith("ACC_")) {
        int val;
        try {
          val = element.getInt(null);
        } catch (IllegalArgumentException e) {
          throw new Error(e.getMessage());
        } catch (IllegalAccessException e) {
          throw new Error(e.getMessage());
        }
        if ((flags & val) != 0) {
          if (buf.length() > 0) {
            buf.append(" ");
          }
          buf.append(name.substring(4).toLowerCase());
        }
      }
    }
    return "0x" + Integer.toString(16, flags) + "(" + buf.toString() + ")";
  }

  private void dumpAttributes(ClassReader cr, ClassReader.AttrIterator attrs) throws InvalidClassFileException,
      InvalidBytecodeException, IOException {
    for (; attrs.isValid(); attrs.advance()) {
      String name = attrs.getName();
      w.write("  " + name + ": @" + Integer.toString(attrs.getRawOffset(), 16) + "\n");
      if (name.equals("Code")) {
        CodeReader code = new CodeReader(attrs);

        w.write("    maxstack: " + code.getMaxStack() + "\n");
        w.write("    maxlocals: " + code.getMaxLocals() + "\n");

        w.write("    bytecode:\n");
        int[] rawHandlers = code.getRawHandlers();
        CTDecoder decoder = new CTDecoder(code);
        decoder.decode();
        Disassembler disasm = new Disassembler(decoder.getInstructions(), decoder.getHandlers(), decoder
            .getInstructionsToBytecodes());
        disasm.disassembleTo("      ", w);

        w.write("    exception handlers:\n");
        for (int e = 0; e < rawHandlers.length; e += 4) {
          w.write("      " + rawHandlers[e] + " to " + rawHandlers[e + 1] + " catch " + getClassName(cr, rawHandlers[e + 3])
              + " at " + rawHandlers[e + 2] + "\n");
        }

        ClassReader.AttrIterator codeAttrs = new ClassReader.AttrIterator();
        code.initAttributeIterator(codeAttrs);
        for (; codeAttrs.isValid(); codeAttrs.advance()) {
          String cName = codeAttrs.getName();
          w.write("    " + cName + ": " + Integer.toString(codeAttrs.getRawOffset(), 16) + "\n");
        }

        if (printLineNumberInfo) {
          int[] map = LineNumberTableReader.makeBytecodeToSourceMap(code);
          if (map != null) {
            w.write("    line number map:\n");
            String line = null;
            int count = 0;
            for (int j = 0; j < map.length; j++) {
              String line2 = "      " + j + ": " + map[j];
              if (line == null || !line2.substring(line2.indexOf(':')).equals(line.substring(line.indexOf(':')))) {
                if (count > 1) {
                  w.write(" (" + count + " times)\n");
                } else if (count > 0) {
                  w.write("\n");
                }
                count = 0;
                line = line2;
                w.write(line);
              }
              count++;
            }
            if (count > 1) {
              w.write(" (" + count + " times)\n");
            } else if (count > 0) {
              w.write("\n");
            }
          }
        }

        int[][] locals = LocalVariableTableReader.makeVarMap(code);
        if (locals != null) {
          w.write("    local variable map:\n");
          String line = null;
          int count = 0;
          for (int j = 0; j < locals.length; j++) {
            int[] vars = locals[j];
            String line2 = null;
            if (vars != null) {
              StringBuffer buf = new StringBuffer();
              buf.append("      " + j + ":");
              for (int k = 0; k < vars.length; k += 2) {
                if (vars[k] != 0) {
                  String n = cr.getCP().getCPUtf8(vars[k]) + "(" + cr.getCP().getCPUtf8(vars[k + 1]) + ")";
                  buf.append(" " + (k / 2) + ":" + n);
                }
              }
              line2 = buf.toString();
            }
            if (line == null || line2 == null || !line2.substring(line2.indexOf(':')).equals(line.substring(line.indexOf(':')))) {
              if (count > 1) {
                w.write(" (" + count + " times)\n");
              } else if (count > 0) {
                w.write("\n");
              }
              count = 0;
              line = line2;
              if (line != null) {
                w.write(line);
              }
            }
            if (line != null) {
              count++;
            }
          }
          if (count > 1) {
            w.write(" (" + count + " times)\n");
          } else if (count > 0) {
            w.write("\n");
          }
        }
      } else if (name.equals("ConstantValue")) {
        ConstantValueReader cv = new ConstantValueReader(attrs);
        w.write("    value: " + getCPItemString(cr.getCP(), cv.getValueCPIndex()) + "\n");
      } else if (name.equals("SourceFile")) {
        SourceFileReader sr = new SourceFileReader(attrs);
        w.write("    file: " + cr.getCP().getCPUtf8(sr.getSourceFileCPIndex()) + "\n");
      } else if (name.equals("Signature")) {
        SignatureReader sr = new SignatureReader(attrs);
        w.write("    signature: " + cr.getCP().getCPUtf8(sr.getSignatureCPIndex()) + "\n");
      } else if (AnnotationsReader.isKnownAnnotation(name)) {
        AnnotationsReader r = new AnnotationsReader(attrs, name);
        printAnnotations(r);
      } else {
        int len = attrs.getDataSize();
        int pos = attrs.getDataOffset();
        while (len > 0) {
          int amount = Math.min(16, len);
          w.write("    " + makeHex(cr.getBytes(), pos, amount, 32) + " " + makeChars(cr.getBytes(), pos, amount) + "\n");
          len -= amount;
          pos += amount;
        }
      }
    }
  }

  private void printAnnotations(AnnotationsReader r)
      throws InvalidClassFileException {
    for (AnnotationAttribute annot : r.getAllAnnotations()) {
      w.write("    Annotation type: " + annot.type + "\n");      
    }
  }

  private static String getCPItemString(ConstantPoolParser cp, int i) throws InvalidClassFileException {
    int t = cp.getItemType(i);
    switch (t) {
    case ClassConstants.CONSTANT_Utf8:
      return "Utf8 " + quoteString(cp.getCPUtf8(i));
    case ClassConstants.CONSTANT_Class:
      return "Class " + cp.getCPClass(i);
    case ClassConstants.CONSTANT_String:
      return "String " + quoteString(cp.getCPString(i));
    case ClassConstants.CONSTANT_Integer:
      return "Integer " + cp.getCPInt(i);
    case ClassConstants.CONSTANT_Float:
      return "Float " + cp.getCPFloat(i);
    case ClassConstants.CONSTANT_Double:
      return "Double " + cp.getCPDouble(i);
    case ClassConstants.CONSTANT_Long:
      return "Long " + cp.getCPLong(i);
    case ClassConstants.CONSTANT_MethodRef:
      return "Method " + cp.getCPRefClass(i) + " " + cp.getCPRefName(i) + " " + cp.getCPRefType(i);
    case ClassConstants.CONSTANT_FieldRef:
      return "Field " + cp.getCPRefClass(i) + " " + cp.getCPRefName(i) + " " + cp.getCPRefType(i);
    case ClassConstants.CONSTANT_InterfaceMethodRef:
      return "InterfaceMethod " + cp.getCPRefClass(i) + " " + cp.getCPRefName(i) + " " + cp.getCPRefType(i);
    case ClassConstants.CONSTANT_NameAndType:
      return "NameAndType " + cp.getCPNATType(i) + " " + cp.getCPNATName(i);
    default:
      return "Unknown type " + t;
    }
  }

  private static String quoteString(String string) {
    StringBuffer buf = new StringBuffer();
    buf.append('"');
    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);
      switch (ch) {
      case '\r':
        buf.append("\\r");
        break;
      case '\n':
        buf.append("\\n");
        break;
      case '\\':
        buf.append("\\\\");
        break;
      case '\t':
        buf.append("\\t");
        break;
      case '\"':
        buf.append("\\\"");
        break;
      default:
        if (ch >= 32 && ch <= 127) {
          buf.append(ch);
        } else {
          buf.append("\\u");
          String h = makeHex(new byte[] { (byte) (ch >> 8), (byte) ch }, 0, 2, 0);
          for (int j = 4 - h.length(); j > 0; j--) {
            buf.append('0');
          }
          buf.append(h);
        }
      }
    }
    buf.append('"');
    return buf.toString();
  }

  /**
   * Print a class.
   * 
   * @throws IllegalArgumentException if cr is null
   */
  public void doClass(final ClassReader cr) throws InvalidClassFileException, InvalidBytecodeException, IOException {
    if (cr == null) {
      throw new IllegalArgumentException("cr is null");
    }
    w.write("Class: " + cr.getName() + "\n");

    if (printConstantPool) {
      ConstantPoolParser cp = cr.getCP();
      for (int i = 1; i < cp.getItemCount(); i++) {
        int t = cp.getItemType(i);
        if (t > 0) {
          w.write("  Constant pool item " + i + ": ");
          w.write(getCPItemString(cp, i));
          w.write("\n");
        }
      }
    }

    ClassReader.AttrIterator attrs = new ClassReader.AttrIterator();
    cr.initClassAttributeIterator(attrs);
    dumpAttributes(cr, attrs);
    w.write("\n");

    int fieldCount = cr.getFieldCount();
    w.write(fieldCount + " fields:\n");
    for (int i = 0; i < fieldCount; i++) {
      w.write(cr.getFieldName(i) + " " + cr.getFieldType(i) + " " + dumpFlags(cr.getFieldAccessFlags(i)) + "\n");
      cr.initFieldAttributeIterator(i, attrs);
      dumpAttributes(cr, attrs);
    }
    w.write("\n");

    int methodCount = cr.getMethodCount();
    w.write(methodCount + " methods:\n");
    for (int i = 0; i < methodCount; i++) {
      w.write(cr.getMethodName(i) + " " + cr.getMethodType(i) + " " + dumpFlags(cr.getMethodAccessFlags(i)) + "\n");
      cr.initMethodAttributeIterator(i, attrs);
      dumpAttributes(cr, attrs);
    }
    w.write("\n");
  }
}
