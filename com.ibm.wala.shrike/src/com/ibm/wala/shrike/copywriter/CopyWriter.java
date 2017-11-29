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
package com.ibm.wala.shrike.copywriter;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.zip.ZipEntry;

import com.ibm.wala.shrikeBT.Compiler;
import com.ibm.wala.shrikeBT.Decoder.InvalidBytecodeException;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.shrikeCT.CTCompiler;
import com.ibm.wala.shrikeBT.shrikeCT.CTDecoder;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassReader.AttrIterator;
import com.ibm.wala.shrikeCT.ClassWriter;
import com.ibm.wala.shrikeCT.ClassWriter.Element;
import com.ibm.wala.shrikeCT.CodeReader;
import com.ibm.wala.shrikeCT.CodeWriter;
import com.ibm.wala.shrikeCT.ConstantPoolParser;
import com.ibm.wala.shrikeCT.ConstantValueReader;
import com.ibm.wala.shrikeCT.ConstantValueWriter;
import com.ibm.wala.shrikeCT.ExceptionsReader;
import com.ibm.wala.shrikeCT.ExceptionsWriter;
import com.ibm.wala.shrikeCT.InnerClassesReader;
import com.ibm.wala.shrikeCT.InnerClassesWriter;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.shrikeCT.LocalVariableTableReader;
import com.ibm.wala.shrikeCT.LocalVariableTableWriter;
import com.ibm.wala.shrikeCT.SourceFileReader;
import com.ibm.wala.shrikeCT.SourceFileWriter;

public class CopyWriter {
  private static final String USAGE = "IBM CopyWriter Tool\n" + "This tool takes the following command line options:\n"
      + "    <jarname> <jarname> ...   Process the classes from these jars\n"
      + "    -o <jarname>              Put the resulting classes into <jarname>\n"
      + "    -c <copyright>            Make the copyright string be\n"
      + "                              '\u00A9 Copyright <copyright>'";

  private static OfflineInstrumenter instrumenter;

  public static String copyright;

  public static final String copyrightAttrName = "com.ibm.Copyright";

  private int replaceWith;

  private int replace;

  static class UnknownAttributeException extends Exception {
    private static final long serialVersionUID = 8845177787110364793L;

    UnknownAttributeException(String t) {
      super("Attribute '" + t + "' not understood");
    }
  }

  public static void main(String[] args) throws Exception {
    if (args == null || args.length == 0) {
      System.err.println(USAGE);
      System.exit(1);
    }

    for (int i = 0; i < args.length - 1; i++) {
      if (args[i] == null) {
        throw new IllegalArgumentException("args[" + i + "] is null");
      }
      if (args[i].equals("-c")) {
        copyright = "\u00A9 Copyright " + args[i + 1];
        String[] newArgs = new String[args.length - 2];
        System.arraycopy(args, 0, newArgs, 0, i);
        System.arraycopy(args, i + 2, newArgs, i, newArgs.length - i);
        args = newArgs;
        break;
      }
    }
    if (copyright == null) {
      System.err.println(USAGE);
      System.exit(1);
    }

    final ArrayList<ZipEntry> entries = new ArrayList<>();

    instrumenter = new OfflineInstrumenter();
    instrumenter.setManifestBuilder(entries::add);
    instrumenter.parseStandardArgs(args);
    instrumenter.setJARComment(copyright);
    instrumenter.beginTraversal();
    ClassInstrumenter ci;
    CopyWriter cw = new CopyWriter();
    while ((ci = instrumenter.nextClass()) != null) {
      try {
        cw.doClass(ci);
      } catch (UnknownAttributeException ex) {
        System.err.println(ex.getMessage() + " in " + instrumenter.getLastClassResourceName());
      }
    }

    instrumenter.writeUnmodifiedClasses();

    Writer w = new OutputStreamWriter(instrumenter.addOutputJarEntry(new ZipEntry("IBM-Copyright")));
    w.write(copyright + "\n");
    for (ZipEntry ze : entries) {
      w.write("  " + ze.getName() + "\n");
    }
    w.write(copyright + "\n");
    w.flush();
    instrumenter.endOutputJarEntry();

    instrumenter.close();
  }

  private int transformCPIndex(int i) {
    if (i == replace) {
      return replaceWith;
    } else {
      return i;
    }
  }

  private Element transformAttribute(ClassReader cr, int m, ClassWriter w, AttrIterator iter) throws InvalidClassFileException,
      UnknownAttributeException, InvalidBytecodeException {
    String name = iter.getName();

    boolean needTransform = true;
    if (name.equals("Synthetic") || name.equals("Deprecated") || name.equals("LineNumberTable")) {
      needTransform = false;
    }

    int offset = iter.getRawOffset();
    int end = offset + iter.getRawSize();

    if (needTransform) {
      needTransform = false;
      for (int i = offset; i + 1 < end; i++) {
        if (cr.getUShort(i) == replace) {
          break;
        }
      }
    }

    if (!needTransform) {
      return new ClassWriter.RawElement(cr.getBytes(), offset, end - offset);
    }

    if (name.equals("Code")) {
      CodeReader r = new CodeReader(iter);
      CTDecoder decoder = new CTDecoder(r);
      decoder.decode();
      MethodData md = new MethodData(decoder, cr.getMethodAccessFlags(m), CTDecoder.convertClassToType(cr.getName()), cr
          .getMethodName(m), cr.getMethodType(m));
      CTCompiler compiler = CTCompiler.make(w, md);
      compiler.compile();
      if (compiler.getAuxiliaryMethods().length > 0)
        throw new Error("Where did this auxiliary method come from?");
      Compiler.Output out = compiler.getOutput();
      CodeWriter cw = new CodeWriter(w);
      cw.setMaxLocals(out.getMaxLocals());
      cw.setMaxStack(out.getMaxStack());
      cw.setCode(out.getCode());
      cw.setRawHandlers(out.getRawHandlers());
      ClassReader.AttrIterator iterator = new ClassReader.AttrIterator();
      r.initAttributeIterator(iterator);
      cw.setAttributes(collectAttributes(cr, m, w, iterator));
      return cw;
    } else if (name.equals("ConstantValue")) {
      ConstantValueReader r = new ConstantValueReader(iter);
      ConstantValueWriter cw = new ConstantValueWriter(w);
      cw.setValueCPIndex(transformCPIndex(r.getValueCPIndex()));
      return cw;
    } else if (name.equals("SourceFile")) {
      SourceFileReader r = new SourceFileReader(iter);
      SourceFileWriter cw = new SourceFileWriter(w);
      cw.setSourceFileCPIndex(transformCPIndex(r.getSourceFileCPIndex()));
      return cw;
    } else if (name.equals("LocalVariableTableReader")) {
      LocalVariableTableReader lr = new LocalVariableTableReader(iter);
      LocalVariableTableWriter lw = new LocalVariableTableWriter(w);
      int[] table = lr.getRawTable();
      for (int i = 0; i < table.length; i += 5) {
        table[i + 2] = transformCPIndex(table[i + 2]);
        table[i + 3] = transformCPIndex(table[i + 3]);
      }
      lw.setRawTable(table);
      return lw;
    } else if (name.equals("Exceptions")) {
      ExceptionsReader lr = new ExceptionsReader(iter);
      ExceptionsWriter lw = new ExceptionsWriter(w);
      int[] table = lr.getRawTable();
      for (int i = 0; i < table.length; i++) {
        table[i] = transformCPIndex(table[i]);
      }
      lw.setRawTable(table);
      return lw;
    } else if (name.equals("InnerClasses")) {
      InnerClassesReader lr = new InnerClassesReader(iter);
      InnerClassesWriter lw = new InnerClassesWriter(w);
      int[] table = lr.getRawTable();
      for (int i = 0; i < table.length; i += 4) {
        table[i] = transformCPIndex(table[i]);
        table[i + 1] = transformCPIndex(table[i + 1]);
        table[i + 2] = transformCPIndex(table[i + 2]);
      }
      lw.setRawTable(table);
      return lw;
    }

    throw new UnknownAttributeException(name);
  }

  private Element[] collectAttributes(ClassReader cr, int m, ClassWriter w, AttrIterator iter) throws InvalidClassFileException,
      UnknownAttributeException, InvalidBytecodeException {
    Element[] elems = new Element[iter.getRemainingAttributesCount()];
    for (int i = 0; i < elems.length; i++) {
      elems[i] = transformAttribute(cr, m, w, iter);
      iter.advance();
    }
    return elems;
  }

  private static int copyEntry(ConstantPoolParser cp, ClassWriter w, int i) throws InvalidClassFileException {
    byte t = cp.getItemType(i);
    switch (t) {
    case ClassConstants.CONSTANT_String:
      return w.addCPString(cp.getCPString(i));
    case ClassConstants.CONSTANT_Class:
      return w.addCPClass(cp.getCPClass(i));
    case ClassConstants.CONSTANT_FieldRef:
      return w.addCPFieldRef(cp.getCPRefClass(i), cp.getCPRefName(i), cp.getCPRefType(i));
    case ClassConstants.CONSTANT_InterfaceMethodRef:
      return w.addCPInterfaceMethodRef(cp.getCPRefClass(i), cp.getCPRefName(i), cp.getCPRefType(i));
    case ClassConstants.CONSTANT_MethodRef:
      return w.addCPMethodRef(cp.getCPRefClass(i), cp.getCPRefName(i), cp.getCPRefType(i));
    case ClassConstants.CONSTANT_NameAndType:
      return w.addCPNAT(cp.getCPNATName(i), cp.getCPNATType(i));
    case ClassConstants.CONSTANT_Integer:
      return w.addCPInt(cp.getCPInt(i));
    case ClassConstants.CONSTANT_Float:
      return w.addCPFloat(cp.getCPFloat(i));
    case ClassConstants.CONSTANT_Long:
      return w.addCPLong(cp.getCPLong(i));
    case ClassConstants.CONSTANT_Double:
      return w.addCPDouble(cp.getCPDouble(i));
    case ClassConstants.CONSTANT_Utf8:
      return w.addCPUtf8(cp.getCPUtf8(i));
    default:
      return -1;
    }
  }

  private void doClass(final ClassInstrumenter ci) throws Exception {
    /*
     * Our basic strategy is to make the first element of the constant pool be the copyright string (as a UTF8 constant pool item).
     * This requires us to parse and emit any class data which might refer to that constant pool item (#1). We will assume that any
     * attribute which refers to that constant pool item must contain the byte sequence '00 01', so we can just copy over any
     * attributes which don't contain that byte sequence. If we detect an unknown attribute type containing the sequence '00 01',
     * then we will abort.
     */
    ClassReader cr = ci.getReader();
    ClassWriter w = new ClassWriter();

    // Make sure that when we're moving over the old constant pool,
    // anytime we add a new entry it really is added and we don't just
    // reuse an existing entry.
    w.setForceAddCPEntries(true);

    // Make the first string in the constant pool be the copyright string
    int r = w.addCPUtf8(copyright);
    if (r != 1)
      throw new Error("Invalid constant pool index: " + r);

    // Now add the rest of the CP entries
    ConstantPoolParser cp = cr.getCP();
    int CPCount = cp.getItemCount();

    if (1 < CPCount) {
      final byte itemType = cp.getItemType(1);
      switch (itemType) {
      case ClassConstants.CONSTANT_Long:
      case ClassConstants.CONSTANT_Double:
        // item 1 is a double-word item, so the next real item is at 3
        // to make sure item 3 is allocated at index 3, we'll need to
        // insert a dummy entry at index 2
        r = w.addCPUtf8("");
        if (r != 2)
          throw new Error("Invalid constant pool index for dummy: " + r);
        break;
      default:
        throw new UnsupportedOperationException(String.format("unexpected constant-pool item type %s", itemType));
      }
    }
    for (int i = 2; i < CPCount; i++) {
      r = copyEntry(cp, w, i);
      if (r != -1 && r != i)
        throw new Error("Invalid constant pool index allocated: " + r + ", expected " + i);
    }
    w.setForceAddCPEntries(false);

    // add CP entry we replaced
    replaceWith = copyEntry(cp, w, 1);
    replace = 1;

    // emit class
    w.setMajorVersion(cr.getMajorVersion());
    w.setMinorVersion(cr.getMinorVersion());
    w.setAccessFlags(cr.getAccessFlags());
    w.setName(cr.getName());
    w.setSuperName(cr.getSuperName());
    w.setInterfaceNames(cr.getInterfaceNames());

    ClassReader.AttrIterator iter = new ClassReader.AttrIterator();

    int fieldCount = cr.getFieldCount();
    for (int i = 0; i < fieldCount; i++) {
      cr.initFieldAttributeIterator(i, iter);
      w.addField(cr.getFieldAccessFlags(i), cr.getFieldName(i), cr.getFieldType(i), collectAttributes(cr, i, w, iter));
    }

    int methodCount = cr.getMethodCount();
    for (int i = 0; i < methodCount; i++) {
      cr.initMethodAttributeIterator(i, iter);
      w.addMethod(cr.getMethodAccessFlags(i), cr.getMethodName(i), cr.getMethodType(i), collectAttributes(cr, i, w, iter));
    }

    cr.initClassAttributeIterator(iter);
    for (; iter.isValid(); iter.advance()) {
      w.addClassAttribute(transformAttribute(cr, 0, w, iter));
    }

    instrumenter.outputModifiedClass(ci, w);
  }
}
