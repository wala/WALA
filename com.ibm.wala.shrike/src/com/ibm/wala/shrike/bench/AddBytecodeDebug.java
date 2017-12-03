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

import com.ibm.wala.shrikeBT.DupInstruction;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MethodEditor;
import com.ibm.wala.shrikeBT.MethodEditor.Output;
import com.ibm.wala.shrikeBT.Util;
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
 * 
 * @author Rob O' Callahan
 */
public class AddBytecodeDebug {
  private static OfflineInstrumenter instrumenter;

  public static void main(String[] args) throws Exception {
    for (int i = 0; i < 1; i++) {
      instrumenter = new OfflineInstrumenter();

      try (final Writer w = new BufferedWriter(new FileWriter("report", false))) {
        args = instrumenter.parseStandardArgs(args);
        instrumenter.setPassUnmodifiedClasses(true);
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
    final String className = ci.getReader().getName();
    w.write("Class: " + className + "\n");
    w.flush();

    ci.enableFakeLineNumbers(10000);

    for (int m = 0; m < ci.getReader().getMethodCount(); m++) {
      MethodData d = ci.visitMethod(m);
      if (d != null) {
        d.setHasChanged();

        MethodEditor me = new MethodEditor(d);
        me.beginPass();
        ExceptionHandler[][] handlers = me.getHandlers();
        boolean[] putDumperAt = new boolean[handlers.length];
        for (ExceptionHandler[] handler : handlers) {
          for (ExceptionHandler element : handler) {
            int offset = element.getHandler();
            if (!putDumperAt[offset]) {
              putDumperAt[offset] = true;
              me.insertBefore(offset, new MethodEditor.Patch() {
                @Override
                public void emitTo(Output w) {
                  w.emit(DupInstruction.make(0));
                  w.emit(Util.makeInvoke(Throwable.class, "printStackTrace", new Class[0]));
                }
              });
            }
          }
        }
        me.applyPatches();
      }
    }

    if (ci.isChanged()) {
      ClassWriter cw = ci.emitClass();
      instrumenter.outputModifiedClass(ci, cw);
    }
  }
}
