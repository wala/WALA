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
package com.ibm.wala.shrike.bench;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.Writer;

import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.Disassembler;
import com.ibm.wala.shrikeBT.GetInstruction;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MethodEditor;
import com.ibm.wala.shrikeBT.MethodEditor.Output;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.ThrowInstruction;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrikeBT.shrikeCT.CTDecoder;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassWriter;

/**
 * This is a demo class.
 * 
 * Class files are taken as input arguments (or if there are none, from standard input). The methods in those files are
 * instrumented: we insert a System.err.println() at ever method call, and a System.err.println() at every method entry.
 * 
 * In Unix, I run it like this: java -cp ~/dev/shrike/shrike com.ibm.wala.shrikeBT.shrikeCT.tools.Bench test.jar -o output.jar
 * 
 * The instrumented classes are placed in the directory "output" under the current directory. Disassembled code is written to the
 * file "report" under the current directory.
 */
public class Bench {
  private final static boolean disasm = true;

  private final static boolean verify = true;

  private static OfflineInstrumenter instrumenter;

  final private static boolean doEntry = true;

  private static boolean doExit = false;

  private static boolean doException = false;

  public static void main(String[] args) throws Exception {
    for (int i = 0; i < 1; i++) {

      try (final Writer w = new BufferedWriter(new FileWriter("report", false))) {

        args = instrumenter.parseStandardArgs(args);
        if (args.length > 0) {
          if (args[0].equals("-doexit")) {
            doExit = true;
          } else if (args[0].equals("-doexception")) {
            doExit = true;
            doException = true;
          }
        }
        instrumenter = new OfflineInstrumenter();
        instrumenter.setPassUnmodifiedClasses(true);
        instrumenter.beginTraversal();
        ClassInstrumenter ci;
        while ((ci = instrumenter.nextClass()) != null) {
          doClass(ci, w);
        }
      }
      instrumenter.close();
    }
  }

  static final String fieldName = "_Bench_enable_trace";

  // Keep these commonly used instructions around
  static final Instruction getSysErr = Util.makeGet(System.class, "err");

  static final Instruction callPrintln = Util.makeInvoke(PrintStream.class, "println", new Class[] { String.class });

  private static void doClass(final ClassInstrumenter ci, Writer w) throws Exception {
    final String className = ci.getReader().getName();
    w.write("Class: " + className + "\n");
    w.flush();

    for (int m = 0; m < ci.getReader().getMethodCount(); m++) {
      MethodData d = ci.visitMethod(m);

      // d could be null, e.g., if the method is abstract or native
      if (d != null) {
        w.write("Instrumenting " + ci.getReader().getMethodName(m) + " " + ci.getReader().getMethodType(m) + ":\n");
        w.flush();

        if (disasm) {
          w.write("Initial ShrikeBT code:\n");
          (new Disassembler(d)).disassembleTo(w);
          w.flush();
        }

        if (verify) {
          Verifier v = new Verifier(d);
          v.verify();
        }

        MethodEditor me = new MethodEditor(d);
        me.beginPass();

        if (doEntry) {
          final String msg0 = "Entering call to " + Util.makeClass("L" + ci.getReader().getName() + ";") + "."
              + ci.getReader().getMethodName(m);
          final int noTraceLabel = me.allocateLabel();
          me.insertAtStart(new MethodEditor.Patch() {
            @Override
            public void emitTo(MethodEditor.Output w) {
              w.emit(GetInstruction.make(Constants.TYPE_boolean, CTDecoder.convertClassToType(className), fieldName, true));
              w.emit(ConstantInstruction.make(0));
              w.emit(ConditionalBranchInstruction.make(Constants.TYPE_int, ConditionalBranchInstruction.Operator.EQ, noTraceLabel));
              w.emit(getSysErr);
              w.emit(ConstantInstruction.makeString(msg0));
              w.emit(callPrintln);
              w.emitLabel(noTraceLabel);
            }
          });
        }
        if (doExit) {
          final String msg0 = "Exiting call to " + Util.makeClass("L" + ci.getReader().getName() + ";") + "."
              + ci.getReader().getMethodName(m);
          IInstruction[] instr = me.getInstructions();
          for (int i = 0; i < instr.length; i++) {
            if (instr[i] instanceof ReturnInstruction) {
              final int noTraceLabel = me.allocateLabel();
              me.insertBefore(i, new MethodEditor.Patch() {
                @Override
                public void emitTo(MethodEditor.Output w) {
                  w.emit(GetInstruction.make(Constants.TYPE_boolean, CTDecoder.convertClassToType(className), fieldName, true));
                  w.emit(ConstantInstruction.make(0));
                  w.emit(ConditionalBranchInstruction.make(Constants.TYPE_int, ConditionalBranchInstruction.Operator.EQ,
                      noTraceLabel));
                  w.emit(getSysErr);
                  w.emit(ConstantInstruction.makeString(msg0));
                  w.emit(callPrintln);
                  w.emitLabel(noTraceLabel);
                }
              });
            }
          }
        }
        if (doException) {
          final String msg0 = "Exception exiting call to " + Util.makeClass("L" + ci.getReader().getName() + ";") + "."
              + ci.getReader().getMethodName(m);
          final int noTraceLabel = me.allocateLabel();
          me.addMethodExceptionHandler(null, new MethodEditor.Patch() {
            @Override
            public void emitTo(Output w) {
              w.emit(GetInstruction.make(Constants.TYPE_boolean, CTDecoder.convertClassToType(className), fieldName, true));
              w.emit(ConstantInstruction.make(0));
              w.emit(ConditionalBranchInstruction.make(Constants.TYPE_int, ConditionalBranchInstruction.Operator.EQ, noTraceLabel));
              w.emit(getSysErr);
              w.emit(ConstantInstruction.makeString(msg0));
              w.emit(callPrintln);
              w.emitLabel(noTraceLabel);
              w.emit(ThrowInstruction.make(false));
            }
          });
        }
        // this updates the data d
        me.applyPatches();

        if (disasm) {
          w.write("Final ShrikeBT code:\n");
          (new Disassembler(d)).disassembleTo(w);
          w.flush();
        }
      }
    }

    if (ci.isChanged()) {
      ClassWriter cw = ci.emitClass();
      cw.addField(ClassReader.ACC_PUBLIC | ClassReader.ACC_STATIC, fieldName, Constants.TYPE_boolean, new ClassWriter.Element[0]);
      instrumenter.outputModifiedClass(ci, cw);
    }
  }
}
