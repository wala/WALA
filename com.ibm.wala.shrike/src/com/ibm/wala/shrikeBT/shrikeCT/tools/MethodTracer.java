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
import java.io.PrintStream;
import java.io.Writer;

import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.Disassembler;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MethodEditor;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;

/**
 * This is a demo class.
 * 
 * Class files are taken as input arguments (or if there are none, from standard input). The methods in those files are
 * instrumented: we insert a System.err.println() at ever method call, and a System.err.println() at every method entry.
 * 
 * In Unix, I run it like this: java -cp ~/dev/shrike/shrike com.ibm.wala.shrikeBT.shrikeCT.tools.MethodTracer test.jar -o
 * output.jar
 * 
 * All modified classes are copied into "output.jar". Some classes may not be modified. To run the resulting code, you should put
 * output.jar and test.jar on the classpath, and put output.jar before test.jar. Disassembled code is written to the file "report"
 * under the current directory.
 */
public class MethodTracer {
  private final static boolean disasm = true;

  private final static boolean verify = true;

  private final static boolean INSTRUMENT_CALLERS = false;

  private static OfflineInstrumenter instrumenter;

  public static void main(String[] args) throws Exception {
    for (int i = 0; i < 1; i++) {
      instrumenter = new OfflineInstrumenter();

      try (final Writer w = new BufferedWriter(new FileWriter("report", false))) {

        instrumenter.parseStandardArgs(args);
        instrumenter.setPassUnmodifiedClasses(false);
        instrumenter.beginTraversal();
        ClassInstrumenter ci;
        while ((ci = instrumenter.nextClass()) != null) {
          doClass(ci, w);
        }
      }
      instrumenter.close();
    }
  }

  // Keep these commonly used instructions around. This trick can speed up
  // instrumentation tools a bit. It's always safe because Instruction objects
  // are always immutable and shareable.
  static final Instruction getSysErr = Util.makeGet(System.class, "err");

  static final Instruction callPrintln = Util.makeInvoke(PrintStream.class, "println", new Class[] { String.class });

  private static void doClass(final ClassInstrumenter ci, Writer w) throws Exception {
    w.write("Class: " + ci.getReader().getName() + "\n");
    w.flush();

    for (int i = 0; i < ci.getReader().getMethodCount(); i++) {
      MethodData d = ci.visitMethod(i);

      // d could be null, e.g., if the method is abstract or native
      if (d != null) {
        w.write("Instrumenting " + ci.getReader().getMethodName(i) + " " + ci.getReader().getMethodType(i) + ":\n");
        w.flush();

        if (disasm) {
          w.write("Initial ShrikeBT code:\n");
          (new Disassembler(d)).disassembleTo(w);
          w.flush();
        }

        if (verify) {
          // verify the incoming code
          Verifier v = new Verifier(d);
          v.verify();
        }

        MethodEditor me = new MethodEditor(d);
        me.beginPass();
        final String msg0 = "Call to " + Util.makeClass("L" + ci.getReader().getName() + ";") + "."
            + ci.getReader().getMethodName(i);

        me.insertAtStart(new MethodEditor.Patch() {
          @Override
          public void emitTo(MethodEditor.Output w) {
            w.emit(getSysErr);
            w.emit(ConstantInstruction.makeString(msg0));
            w.emit(callPrintln);
          }
        });
        if (INSTRUMENT_CALLERS) {
          IInstruction[] ins = d.getInstructions();
          for (int k = 0; k < ins.length; k++) {
            if (ins[k] instanceof InvokeInstruction) {
              InvokeInstruction instr = (InvokeInstruction) ins[k];
              final String msg = "Call from " + Util.makeClass("L" + ci.getReader().getName() + ";") + "."
                  + ci.getReader().getMethodName(i) + ":" + k + " to target " + Util.makeClass(instr.getClassType()) + "."
                  + instr.getMethodName();
              me.insertBefore(k, new MethodEditor.Patch() {
                @Override
                public void emitTo(MethodEditor.Output w) {
                  w.emit(getSysErr);
                  w.emit(ConstantInstruction.makeString(msg));
                  w.emit(callPrintln);
                }
              });
            }
          }
        }
        // this updates the data d
        me.applyPatches();

        if (disasm) {
          w.write("Final ShrikeBT code:\n");
          (new Disassembler(d)).disassembleTo(w);
          w.flush();
        }

        if (verify) {
          // verify outgoing code
          Verifier v = new Verifier(d);
          v.verify();
        }
      }
    }

    if (ci.isChanged()) {
      instrumenter.outputModifiedClass(ci);
    }
  }
}
