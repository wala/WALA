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

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;

public class CAstRhinoTranslator implements TranslatorToCAst {

  private final CAstImpl Ast = new CAstImpl();

  private final SourceModule M;
  
   public CAstRhinoTranslator(SourceModule M) {
    this.M = M;
   }

  public CAstEntity translateToCAst() throws IOException {
    String N;
    if (M instanceof SourceFileModule) {
      N = ((SourceFileModule) M).getClassName();
    } else {
      N = M.getName();
    }

    return
      new PropertyReadExpander(Ast).rewrite(
          new RhinoToAstTranslator(Ast, M, N).translate());
    }
}
