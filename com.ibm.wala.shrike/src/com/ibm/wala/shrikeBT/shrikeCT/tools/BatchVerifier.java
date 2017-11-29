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
import java.io.FileWriter;
import java.io.PrintWriter;

import com.ibm.wala.shrikeBT.Decoder;
import com.ibm.wala.shrikeBT.Disassembler;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeBT.analysis.ClassHierarchyStore;
import com.ibm.wala.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrikeBT.shrikeCT.CTDecoder;
import com.ibm.wala.shrikeBT.shrikeCT.CTUtils;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.CodeReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

/**
 * This is a demo class.
 * 
 * Class files are taken as input arguments (or if there are none, from standard input). The methods in those files are
 * instrumented: we insert a System.err.println() at ever method call, and a System.err.println() at every method entry.
 * 
 * In Unix, I run it like this: java -cp ~/dev/shrike/shrike com.ibm.wala.shrikeBT.shrikeCT.tools.BatchVerifier test.jar -o
 * output.jar
 * 
 * The instrumented classes are placed in the directory "output" under the current directory. Disassembled code is written to the
 * file "report" under the current directory.
 */
public class BatchVerifier {
  private static boolean disasm = false;

  final private static ClassHierarchyStore store = new ClassHierarchyStore();

  private static int errors = 0;

  public static void main(String[] args) throws Exception {
    OfflineInstrumenter oi = new OfflineInstrumenter();
    args = oi.parseStandardArgs(args);

    for (String arg : args) {
      if (arg.equals("-d")) {
        disasm = true;
      }
    }

    try (final PrintWriter w = new PrintWriter(new BufferedWriter(new FileWriter("report", false)))) {

      oi.beginTraversal();
      ClassInstrumenter ci;
      while ((ci = oi.nextClass()) != null) {
        ClassReader cr = ci.getReader();
        CTUtils.addClassToHierarchy(store, cr);
      }

      oi.beginTraversal();
      while ((ci = oi.nextClass()) != null) {
        doClass(ci.getReader(), w);
      }
    }

    oi.close();

    if (errors > 0) {
      System.err.println(errors + " error" + (errors > 1 ? "s" : "") + " detected");
    }
  }

  private static void doClass(final ClassReader cr, PrintWriter w) throws Exception {
    int methodCount = cr.getMethodCount();
    w.write("Verifying " + cr.getName() + "\n");
    w.flush();

    ClassReader.AttrIterator iter = new ClassReader.AttrIterator();

    for (int i = 0; i < methodCount; i++) {
      cr.initMethodAttributeIterator(i, iter);
      for (; iter.isValid(); iter.advance()) {
        if (iter.getName().equals("Code")) {
          w.write("Verifying " + cr.getName() + "." + cr.getMethodName(i) + " " + cr.getMethodType(i) + ":\n");
          w.flush();

          CodeReader code = new CodeReader(iter);
          CTDecoder d = new CTDecoder(code);
          try {
            d.decode();
          } catch (Decoder.InvalidBytecodeException e) {
            throw new InvalidClassFileException(code.getRawOffset(), e.getMessage());
          }
          MethodData md = new MethodData(d, cr.getMethodAccessFlags(i), CTDecoder.convertClassToType(cr.getName()), cr
              .getMethodName(i), cr.getMethodType(i));

          if (disasm) {
            w.write("ShrikeBT code:\n");
            (new Disassembler(md)).disassembleTo(w);
            w.flush();
          }

          Verifier v = new Verifier(md);
          try {
            v.verify();
          } catch (FailureException e) {
            w.println("ERROR: VERIFICATION FAILED");
            e.printStackTrace(w);
            e.printPath(w);
            errors++;
            w.flush();
          }

          break;
        }
      }
    }
  }
}
