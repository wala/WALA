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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassReader;

/**
 * @author roca
 */
public class InterfaceAnalyzer {
  final static class TypeStats {
    int totalOccurrences;

    int methodOccurrences;

    int publicMethodOccurrences;

    int foreignPublicMethodOccurrences;

    int lastMUID;
  }

  final static HashMap<String, TypeStats> typeStats = new HashMap<>();

  public static void main(String[] args) throws Exception {
    OfflineInstrumenter instrumenter = new OfflineInstrumenter();

    try (final Writer w = new BufferedWriter(new OutputStreamWriter(System.out))) {

      args = instrumenter.parseStandardArgs(args);

      instrumenter.beginTraversal();
      ClassInstrumenter ci;
      while ((ci = instrumenter.nextClass()) != null) {
        doClass(ci.getReader());
      }
      instrumenter.close();

      w.write("Type\t# Total\t# Method\t# Public Method\t# Public Method as Foreign\n");
      for (String k : typeStats.keySet()) {
        TypeStats t = typeStats.get(k);
        w.write(k + "\t" + t.totalOccurrences + "\t" + t.methodOccurrences + "\t" + t.publicMethodOccurrences + "\t"
            + t.foreignPublicMethodOccurrences + "\n");
      }
    }
  }

  static int methodUID = 0;

  /**
   * @param reader
   */
  private static void doClass(ClassReader reader) throws Exception {
    if ((reader.getAccessFlags() & Constants.ACC_INTERFACE) != 0 && (reader.getAccessFlags() & Constants.ACC_PUBLIC) != 0) {
      String cType = Util.makeType(reader.getName());
      for (int m = 0; m < reader.getMethodCount(); m++) {
        String sig = reader.getMethodType(m);
        String[] params = Util.getParamsTypes(null, sig);
        int flags = reader.getMethodAccessFlags(m);
        int mUID = methodUID++;
        for (String param : params) {
          doType(flags, param, cType, mUID);
        }
        doType(flags, Util.getReturnType(sig), cType, mUID);
      }
    }
  }

  private static void doType(int flags, String type, String containerType, int mUID) {
    TypeStats t = typeStats.get(type);
    if (t == null) {
      t = new TypeStats();
      typeStats.put(type, t);
    }
    t.totalOccurrences++;
    if (t.lastMUID != mUID) {
      t.methodOccurrences++;
      if ((flags & Constants.ACC_PUBLIC) != 0) {
        t.publicMethodOccurrences++;
        String elemType = type;
        while (Util.isArrayType(elemType)) {
          elemType = elemType.substring(1);
        }
        if (!Util.isPrimitiveType(elemType) && !packagePart(elemType, 2).equals(packagePart(containerType, 2))) {
          t.foreignPublicMethodOccurrences++;
        }
      }
    }
    t.lastMUID = mUID;
  }

  private static String packagePart(String t, int count) {
    String c = Util.makeClass(t);
    int lastDot = -1;
    for (int i = 0; i < count; i++) {
      int dot = c.indexOf('.', lastDot + 1);
      if (dot < 0) {
        return c;
      }
      lastDot = dot;
    }
    return c.substring(0, lastDot);
  }
}
