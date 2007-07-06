/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.translator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceURLModule;

public abstract class TranslatorBase implements TranslatorToIR {

  protected static final Set<String> bootstrapFileNames;

  private static String prologueFileName = "prologue.js";

  public static void resetPrologueFile() {
    prologueFileName = "prologue.js";
  }

  public static void setPrologueFile(String name) {
    prologueFileName = name;
  }

  public static void addBootstrapFile(String fileName) {
    bootstrapFileNames.add(fileName);
  }

  static {
    bootstrapFileNames = new HashSet<String>();
    bootstrapFileNames.add(prologueFileName);
  }

  public abstract void translate(ModuleEntry M, String N) throws IOException;

  public void translate(Set modules) throws IOException {
    translate(new SourceURLModule(getClass().getClassLoader().getResource(prologueFileName)), prologueFileName);
    Iterator MS = modules.iterator();
    while (MS.hasNext()) {
      ModuleEntry M = (ModuleEntry) MS.next();
      if (M instanceof SourceFileModule) {
        translate(M, ((SourceFileModule) M).getClassName());
      } else {
        translate(M, M.getName());
      }
    }
  }
}
