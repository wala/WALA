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
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceFileModule;

public abstract class TranslatorBase {

  public abstract void translate(ModuleEntry M, String N) throws IOException;

  public void translate(List modules) throws IOException {
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
