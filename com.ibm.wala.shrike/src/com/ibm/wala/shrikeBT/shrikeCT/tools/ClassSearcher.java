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
import java.io.Writer;

import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ConstantPoolParser;

/**
 * This is a demo class.
 * 
 * Class files are taken as input arguments (or if there are none, from standard input). We search those class files for all
 * references to the Java library classes "SoftReference" or "WeakReference". This is just a demo to show how to write a simple tool
 * like this. Here we're using the OfflineInstrumenter class to manage loading a set of class files and JARs for analysis; we don't
 * actually modify any code.
 * 
 * In Unix, I run it like this: java -cp ~/dev/shrike/shrike com.ibm.wala.shrikeBT.shrikeCT.tools.ClassSearcher test.jar -o
 * output.jar
 */
public class ClassSearcher {
  private static OfflineInstrumenter instrumenter;

  private static int scanned = 0;

  public static void main(String[] args) throws Exception {
    instrumenter = new OfflineInstrumenter();

    try (final Writer w = new BufferedWriter(new FileWriter("report", true))) {

      instrumenter.parseStandardArgs(args);
      instrumenter.beginTraversal();
      ClassInstrumenter ci;
      while ((ci = instrumenter.nextClass()) != null) {
        doClass(ci, w, instrumenter.getLastClassResourceName());
      }
      instrumenter.close();
    }

    System.out.println("Classes scanned: " + scanned);
  }

  private static void doClass(final ClassInstrumenter ci, Writer w, String resource) throws Exception {
    scanned++;

    String cl1 = "java/lang/ref/WeakReference";
    String cl2 = "java/lang/ref/SoftReference";
    ClassReader r = ci.getReader();
    ConstantPoolParser cp = r.getCP();
    for (int i = 1; i < cp.getItemCount(); i++) {
      if (cp.getItemType(i) == ConstantPoolParser.CONSTANT_Class && (cp.getCPClass(i).equals(cl1) || cp.getCPClass(i).equals(cl2))) {
        w.write(cp.getCPClass(i) + " " + resource + " " + r.getName() + "\n");
      }
    }
  }
}
