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
package com.ibm.wala.shrikeBT.shrikeCT;

import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.analysis.ClassHierarchyStore;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassWriter;
import com.ibm.wala.shrikeCT.ClassWriter.Element;
import com.ibm.wala.shrikeCT.CodeWriter;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.shrikeCT.LineNumberTableWriter;

/**
 * This is a dumping ground for useful functions that manipulate class info.
 * 
 * @author roca@us.ibm.com
 */
public class CTUtils {
  public static void addClassToHierarchy(ClassHierarchyStore store, ClassReader cr) throws InvalidClassFileException,
      IllegalArgumentException {
    if (store == null) {
      throw new IllegalArgumentException("store is null");
    }
    if (cr == null) {
      throw new IllegalArgumentException();
    }
    String[] superInterfaces = new String[cr.getInterfaceCount()];
    for (int i = 0; i < superInterfaces.length; i++) {
      superInterfaces[i] = CTDecoder.convertClassToType(cr.getInterfaceName(i));
    }
    String superName = cr.getSuperName();
    if ("java/io/File".equals(cr.getName()) || "java/lang/Throwable".equals(cr.getName())) {
      System.err.println(superName);
    }
    store.setClassInfo(CTDecoder.convertClassToType(cr.getName()), (cr.getAccessFlags() & Constants.ACC_INTERFACE) != 0, (cr
        .getAccessFlags() & Constants.ACC_FINAL) != 0, superName != null ? CTDecoder.convertClassToType(superName) : null,
        superInterfaces);
  }

  /**
   * Compile and add a method to a {@link ClassWriter}.
   * 
   * @param md the method data
   * @param classWriter the target class writer
   * @param rawLines line number information if available, otherwise <code>null</code>
   */
  public static void compileAndAddMethodToClassWriter(MethodData md, ClassWriter classWriter, ClassWriter.Element rawLines) {
    if (classWriter == null) {
      throw new IllegalArgumentException("classWriter is null");
    }
    if (md == null) {
      throw new IllegalArgumentException("md is null");
    }
    CTCompiler compiler = CTCompiler.make(classWriter, md);
    compiler.compile();
    CTCompiler.Output output = compiler.getOutput();
    CodeWriter code = new CodeWriter(classWriter);
    code.setMaxStack(output.getMaxStack());
    code.setMaxLocals(output.getMaxLocals());
    code.setCode(output.getCode());
    code.setRawHandlers(output.getRawHandlers());
  
    LineNumberTableWriter lines = null;
    // I guess it is the line numbers in the java files.
    if (rawLines == null) {
      // add fake line numbers: just map each bytecode instruction to its own
      // 'line'
  
      // NOTE:Should not use md.getInstructions().length, because the
      // the length of the created code can be smaller than the md's instruction
      // length
  
      // WRONG: int[] newLineMap = new int[md.getInstructions().length];
      int[] newLineMap = new int[code.getCodeLength()];
      for (int i = 0; i < newLineMap.length; i++) {
        newLineMap[i] = i;
      }
      int[] rawTable = LineNumberTableWriter.makeRawTable(newLineMap);
      lines = new LineNumberTableWriter(classWriter);
      lines.setRawTable(rawTable);
    }
    code.setAttributes(new ClassWriter.Element[] { rawLines == null ? lines : rawLines });
    Element[] elements = { code };
    // System.out.println("Name:"+md.getName()+" Sig:"+md.getSignature());
    classWriter.addMethod(md.getAccess(), md.getName(), md.getSignature(), elements);
  }
}
