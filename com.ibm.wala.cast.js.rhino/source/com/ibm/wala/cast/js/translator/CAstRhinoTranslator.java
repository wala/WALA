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

import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.cast.ir.translator.RewritingTranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.CopyKey;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.RewriteContext;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;

public class CAstRhinoTranslator extends RewritingTranslatorToCAst implements TranslatorToCAst {
  private final List<CAstRewriterFactory> rewriters = new LinkedList<CAstRewriterFactory>();
    
  private static String getName(SourceModule M) {
    if (M instanceof SourceFileModule) {
      return ((SourceFileModule) M).getClassName();
    } else {
      return M.getName();
    }
  }
  
  public CAstRhinoTranslator(SourceModule M, boolean replicateForDoLoops) {
    super(M, new RhinoToAstTranslator(new CAstImpl(), M, getName(M), replicateForDoLoops));
   }

  @Override
  public <C extends RewriteContext<K>, K extends CopyKey<K>> void addRewriter(CAstRewriterFactory<C, K> factory, boolean prepend) {
    if(prepend)
      rewriters.add(0, factory);
    else
      rewriters.add(factory);
  }  

}
