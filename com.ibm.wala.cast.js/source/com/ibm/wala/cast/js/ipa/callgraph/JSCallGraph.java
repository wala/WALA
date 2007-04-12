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
package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.cast.ipa.callgraph.*;
import com.ibm.wala.cast.js.cfg.*;
import com.ibm.wala.cast.js.ssa.*;
import com.ibm.wala.cast.js.types.*;
import com.ibm.wala.cfg.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.warnings.WarningSet;

public class JSCallGraph extends AstCallGraph {

  public JSCallGraph(ClassHierarchy cha, AnalysisOptions options, WarningSet warnings) {
    super(cha, options, warnings);
  }

  public class JSFakeRoot extends ScriptFakeRoot {

    public JSFakeRoot(ClassHierarchy cha, AnalysisOptions options) {
      super(cha, options);
    }

    public InducedCFG makeControlFlowGraph() {
      return new JSInducedCFG(getStatements(new WarningSet()), this, Everywhere.EVERYWHERE);
    }

    public SSANewInstruction addAllocation(TypeReference T, WarningSet warnings) {
      if (cha.isSubclassOf(cha.lookupClass(T), cha.lookupClass(JavaScriptTypes.Root))) {
        int instance = nextLocal++;
        NewSiteReference ref = NewSiteReference.make(statements.size(), T);
        SSANewInstruction result = new JavaScriptNewInstruction(instance, ref);
        statements.add(result);
        return result;
      } else {
        return super.addAllocation(T, warnings);
      }
    }

    public SSAAbstractInvokeInstruction addDirectCall(int function, int[] params, CallSiteReference site) {
      CallSiteReference newSite = CallSiteReference.make(statements.size(), site.getDeclaredTarget(), site.getInvocationCode());

      JavaScriptInvoke s = new JavaScriptInvoke(function, nextLocal++, params, nextLocal++, newSite);
      statements.add(s);

      return s;
    }
  }

  protected CGNode makeFakeRootNode() {
    return findOrCreateNode(new JSFakeRoot(cha, options), Everywhere.EVERYWHERE);
  }
}
