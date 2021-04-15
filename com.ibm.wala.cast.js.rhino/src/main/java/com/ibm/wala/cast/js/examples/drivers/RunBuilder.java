/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.examples.drivers;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.ir.translator.RewritingTranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.translator.RhinoToAstTranslator;
import com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil.CGBuilderType;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import java.io.IOException;

public class RunBuilder {

  public static void main(String[] args)
      throws IOException, WalaException, IllegalArgumentException, CancelException {

    class CAstRhinoNewTranslator extends RewritingTranslatorToCAst {
      public CAstRhinoNewTranslator(ModuleEntry m, boolean replicateForDoLoops) {
        super(
            m, new RhinoToAstTranslator(new CAstImpl(), m, m.getName(), replicateForDoLoops, true));
      }
    }

    com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(
        new CAstRhinoTranslatorFactory() {
          @Override
          public TranslatorToCAst make(CAst ast, ModuleEntry M) {
            return new CAstRhinoNewTranslator(M, false);
          }
        });

    JSCFABuilder builder =
        JSCallGraphBuilderUtil.makeScriptCGBuilder(
            args[0],
            args[1],
            CGBuilderType.ZERO_ONE_CFA_WITHOUT_CORRELATION_TRACKING,
            RunBuilder.class.getClassLoader());

    // builder.setContextSelector(new CPAContextSelector(builder.getContextSelector()));

    CallGraph CG = builder.makeCallGraph(builder.getOptions());

    System.err.println(CG.getClassHierarchy());

    CAstCallGraphUtil.AVOID_DUMP = false;
    CAstCallGraphUtil.dumpCG(builder.getCFAContextInterpreter(), builder.getPointerAnalysis(), CG);
  }
}
