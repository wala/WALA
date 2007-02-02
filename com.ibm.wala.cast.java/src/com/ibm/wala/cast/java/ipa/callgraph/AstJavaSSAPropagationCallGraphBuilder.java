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
package com.ibm.wala.cast.java.ipa.callgraph;

import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.cast.ipa.callgraph.AstSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.java.analysis.typeInference.AstJavaTypeInference;
import com.ibm.wala.cast.java.ssa.AstJavaInstructionVisitor;
import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.util.warnings.WarningSet;

public class AstJavaSSAPropagationCallGraphBuilder extends AstSSAPropagationCallGraphBuilder {

  protected
    AstJavaSSAPropagationCallGraphBuilder(ClassHierarchy cha, 
					  WarningSet warnings,
					  AnalysisOptions options,
					  PointerKeyFactory pointerKeyFactory)
  {
    super(cha, warnings, options, pointerKeyFactory);
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // language specialization interface
  //
  /////////////////////////////////////////////////////////////////////////////

  protected boolean useObjectCatalog() {
    return false;
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // top-level node constraint generation
  //
  /////////////////////////////////////////////////////////////////////////////

  protected TypeInference makeTypeInference(IR ir, ClassHierarchy cha) {
    TypeInference ti = new AstJavaTypeInference(ir, cha, false);
    ti.solve();

    if (DEBUG_TYPE_INFERENCE) {
      Trace.println("IR of " + ir.getMethod());
      Trace.println( ir );
      Trace.println("TypeInference of " + ir.getMethod());
      for(int i = 0; i < ir.getSymbolTable().getMaxValueNumber(); i++) {
	if (ti.isUndefined(i)) {
	  Trace.println("  value " + i + " is undefined");
	} else {
	  Trace.println("  value " + i + " has type " + ti.getType(i));
	}
      }
    }

    return ti;
  }

  protected class AstJavaInterestingVisitor
    extends AstInterestingVisitor 
      implements AstJavaInstructionVisitor 
  {
    protected AstJavaInterestingVisitor(int vn) {
      super(vn);
    }

    public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {
      bingo = true;
    }
  }

  protected InterestingVisitor makeInterestingVisitor(int vn) {
    return new AstJavaInterestingVisitor(vn);
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // specialized pointer analysis
  //
  /////////////////////////////////////////////////////////////////////////////

  protected class AstJavaPointerFlowGraph extends AstPointerFlowGraph {
    
    protected class AstJavaPointerFlowVisitor
      extends AstPointerFlowVisitor 
      implements AstJavaInstructionVisitor
    {
      protected AstJavaPointerFlowVisitor(CGNode node, IR ir, BasicBlock bb) {
	super(node, ir, bb);
      }

      public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {
	  
      }
    }

    protected AstJavaPointerFlowGraph(PointerAnalysis pa, CallGraph cg) {
      super(pa,cg);
    }

    protected InstructionVisitor makeInstructionVisitor(CGNode node, IR ir, BasicBlock bb) {
      return new AstJavaPointerFlowVisitor(node,ir, bb);
    }
  }

  public PointerFlowGraphFactory getPointerFlowGraphFactory() {
    return new PointerFlowGraphFactory() {
      public PointerFlowGraph make(PointerAnalysis pa, CallGraph cg) {
	return new AstJavaPointerFlowGraph(pa, cg);
      }
    };
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // IR visitor specialization for AST-based Java
  //
  /////////////////////////////////////////////////////////////////////////////
  
  protected class AstJavaConstraintVisitor 
    extends AstConstraintVisitor 
    implements AstJavaInstructionVisitor 
  {

    public AstJavaConstraintVisitor(ExplicitCallGraph.ExplicitNode node, IR ir, ExplicitCallGraph callGraph, DefUse du) {
      super(node, ir, callGraph, du);
    }
    
    public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {
      visitInvokeInternal(instruction);
    }
  }

  protected ConstraintVisitor makeVisitor(ExplicitCallGraph.ExplicitNode node, 
					  IR ir, 
					  DefUse du,
					  ExplicitCallGraph callGraph)
  {
    return new AstJavaConstraintVisitor(node, ir, callGraph, du);
  }
}
