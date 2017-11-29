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
import java.io.Writer;

import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassReader;

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
public class Statistics {
  private static OfflineInstrumenter instrumenter;

  public static void main(String[] args) throws Exception {
    for (int i = 0; i < 1; i++) {
      instrumenter = new OfflineInstrumenter();

      try (Writer w = new BufferedWriter(new FileWriter("report", false))) {

        args = instrumenter.parseStandardArgs(args);

        instrumenter.beginTraversal();
        ClassInstrumenter ci;
        while ((ci = instrumenter.nextClass()) != null) {
          doClass(ci, w);
        }
        instrumenter.close();
      }
    }
  }

  private static void doClass(final ClassInstrumenter ci, Writer w) throws Exception {
    ClassReader cr = ci.getReader();
    final String className = cr.getName();
    w.write("Class: " + className + "\n");

    boolean allPrivateConstructors = true;
    boolean methodCallsConstructor = false;
    boolean classInitCallsConstructor = false;

    for (int m = 0; m < cr.getMethodCount(); m++) {
      MethodData d = ci.visitMethod(m);

      // d could be null, e.g., if the method is abstract or native
      if (d != null) {
        if (d.getName().equals("<init>")) {
          int f = cr.getMethodAccessFlags(m);
          if ((f & Constants.ACC_PRIVATE) == 0
              && ((f & Constants.ACC_PROTECTED) == 0 || (cr.getAccessFlags() & Constants.ACC_FINAL) == 0)) {
            allPrivateConstructors = false;
          }
        }

        int constructorCalls = 0;
        IInstruction[] instrs = d.getInstructions();
        for (IInstruction instr : instrs) {
          if (instr instanceof InvokeInstruction) {
            InvokeInstruction invoke = (InvokeInstruction) instr;
            if (invoke.getMethodName().equals("<init>") && invoke.getClassType().equals(Util.makeType(className))) {
              constructorCalls++;
            }
          }
        }
        if (!d.getName().equals("<init>") && !d.getName().equals("<clinit>")) {
          if (constructorCalls > 0) {
            methodCallsConstructor = true;
          }
        } else if (d.getName().equals("<clinit>")) {
          classInitCallsConstructor = true;
        }
      }
    }

    if (allPrivateConstructors && !methodCallsConstructor && classInitCallsConstructor) {
      w.write("Restricted Creation\n");
    }
  }
}
