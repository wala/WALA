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
package com.ibm.wala.cast.ipa.callgraph;

import java.util.Iterator;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ContextInsensitiveSSAInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.warnings.WarningSet;

public class AstContextInsensitiveSSAContextInterpreter
  extends ContextInsensitiveSSAInterpreter 
{

  public AstContextInsensitiveSSAContextInterpreter(AnalysisOptions options) {
    super(options);
  }

  public boolean understands(IMethod method, Context context) {
    return method instanceof AstMethod;
  }

  public Iterator<NewSiteReference> iterateNewSites(CGNode N) {
    IR ir = getIR(N, new WarningSet());
    if (ir == null) {
      return EmptyIterator.instance();
    } else {
      return ir.iterateNewSites();
    }
  }

  public Iterator<CallSiteReference> iterateCallSites(CGNode N) {
    IR ir = getIR(N, new WarningSet());
    if (ir == null) {
      return EmptyIterator.instance();
    } else {
      return ir.iterateCallSites();
    }
  }

}
