/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.cast.ipa.callgraph.AstCallGraph.AstCGNode;
import com.ibm.wala.cast.ipa.callgraph.ScopeMappingKeysContextSelector.ScopeMappingContext;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;

public class LexicalScopingSSAContextInterpreter extends AstContextInsensitiveSSAContextInterpreter implements SSAContextInterpreter {
  
  public LexicalScopingSSAContextInterpreter(AnalysisOptions options, AnalysisCache cache) {
    super(options, cache);
  }

  public boolean understands(IMethod method, Context context) {
    assert !(context instanceof ScopeMappingContext) || method instanceof AstMethod;
    return method instanceof AstMethod 
        && ( (context instanceof ScopeMappingContext)
                           ||
             !((AstMethod)method).lexicalInfo().getAllExposedUses().isEmpty() );
  }

  public IR getIR(CGNode node) {
    if (node instanceof AstCGNode) {
      IR ir = ((AstCGNode)node).getLexicallyMutatedIR();
      if (ir != null) {
        return ir;
      }
    }
    
    return super.getIR(node);
  }
  
  public DefUse getDU(CGNode node) {
    if (node instanceof AstCGNode) {
      DefUse du = ((AstCGNode)node).getLexicallyMutatedDU();
      if (du != null) {
        return du;
      }
    }

    return super.getDU(node);
  }
  
}
