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
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Random;

import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.Disassembler;
import com.ibm.wala.shrikeBT.DupInstruction;
import com.ibm.wala.shrikeBT.IArrayStoreInstruction;
import com.ibm.wala.shrikeBT.IGetInstruction;
import com.ibm.wala.shrikeBT.IPutInstruction;
import com.ibm.wala.shrikeBT.LoadInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MethodEditor;
import com.ibm.wala.shrikeBT.MethodEditor.Output;
import com.ibm.wala.shrikeBT.StoreInstruction;
import com.ibm.wala.shrikeBT.SwapInstruction;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrikeBT.info.LocalAllocator;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
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
public class Mangler {
  private static OfflineInstrumenter instrumenter;

  private static final boolean verify = true;

  private static final boolean disasm = true;

  public static void main(String[] args) throws Exception {
    for (int i = 0; i < 1; i++) {
      instrumenter = new OfflineInstrumenter();

      try (final Writer w = new BufferedWriter(new FileWriter("report", false))) {

        args = instrumenter.parseStandardArgs(args);
        int seed;
        try {
          seed = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
          System.err.println("Invalid number: " + args[0]);
          return;
        }

        Random r = new Random(seed);
        instrumenter.setPassUnmodifiedClasses(true);
        instrumenter.beginTraversal();
        instrumenter.setOutputJar(new File("output.jar"));
        ClassInstrumenter ci;
        while ((ci = instrumenter.nextClass()) != null) {
          doClass(ci, w, r);
        }
      }
      instrumenter.close();
    }
  }

  private static void doClass(final ClassInstrumenter ci, Writer w, final Random r) throws Exception {
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

        final int passes = r.nextInt(4) + 1;

        for (int i = 0; i < passes; i++) {
          final boolean doGet = true; // r.nextBoolean();
          final boolean doPut = true; // r.nextBoolean();
          final boolean doArrayStore = true; // r.nextBoolean();
          final int tmpInt = LocalAllocator.allocate(d, "I");
          final int tmpAny = LocalAllocator.allocate(d);

          final MethodEditor me = new MethodEditor(d);
          me.beginPass();

          me.visitInstructions(new MethodEditor.Visitor() {
            @Override
            public void visitGet(IGetInstruction instruction) {
              if (doGet && !instruction.isStatic()) {
                insertBefore(new MethodEditor.Patch() {
                  @Override
                  public void emitTo(Output w) {
                    w.emit(DupInstruction.make(0));
                  }
                });
                insertAfter(new MethodEditor.Patch() {
                  @Override
                  public void emitTo(Output w) {
                    w.emit(SwapInstruction.make());
                    w.emit(Util.makePut(Slots.class, "o"));
                  }
                });
              }
            }

            @Override
            public void visitPut(IPutInstruction instruction) {
              if (doPut && !instruction.isStatic()) {
                insertBefore(new MethodEditor.Patch() {
                  @Override
                  public void emitTo(Output w) {
                    w.emit(SwapInstruction.make());
                    w.emit(DupInstruction.make(1));
                    w.emit(SwapInstruction.make());
                  }
                });
                insertAfter(new MethodEditor.Patch() {
                  @Override
                  public void emitTo(Output w) {
                    w.emit(Util.makePut(Slots.class, "o"));
                  }
                });
              }
            }

            @Override
            public void visitArrayStore(final IArrayStoreInstruction instruction) {
              if (doArrayStore) {
                final int label = me.allocateLabel();
                insertBefore(new MethodEditor.Patch() {
                  @Override
                  public void emitTo(Output w) {
                    String t = Util.getStackType(instruction.getType());
                    w.emit(StoreInstruction.make(t, tmpAny));
                    w.emit(StoreInstruction.make(Constants.TYPE_int, tmpInt));
                    w.emit(DupInstruction.make(0));
                    w.emit(LoadInstruction.make(Constants.TYPE_int, tmpInt));
                    w.emit(LoadInstruction.make(t, tmpAny));
                    if (t.equals(Constants.TYPE_int)) {
                      w.emit(DupInstruction.make(0));
                      w.emit(ConstantInstruction.make(0));
                      w.emit(ConditionalBranchInstruction.make(t, ConditionalBranchInstruction.Operator.EQ, label));
                      w.emit(DupInstruction.make(0));
                      w.emit(Util.makePut(Slots.class, "i"));
                      w.emitLabel(label);
                    }
                  }
                });
                insertAfter(new MethodEditor.Patch() {
                  @Override
                  public void emitTo(Output w) {
                    w.emit(Util.makePut(Slots.class, "o"));
                    w.emit(LoadInstruction.make(Constants.TYPE_int, tmpInt));
                    w.emit(Util.makePut(Slots.class, "i"));
                  }
                });
              }
            }
          });

          // this updates the data d
          me.applyPatches();
        }

        if (disasm) {
          w.write("Final ShrikeBT code:\n");
          (new Disassembler(d)).disassembleTo(w);
          w.flush();
        }
      }
    }

    if (ci.isChanged()) {
      ClassWriter cw = ci.emitClass();
      instrumenter.outputModifiedClass(ci, cw);
    }
  }
}
