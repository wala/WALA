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
package com.ibm.wala.cast.ir.ssa;


import com.ibm.wala.cast.loader.*;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cfg.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.ssa.SSACFG.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.*;

import java.util.*;

public class AstIRFactory implements IRFactory {
  private final boolean keepIR;
  private final Map keptIRs;

  AstIRFactory(boolean keepIR) {
    this.keepIR = keepIR;
    this.keptIRs = (keepIR)? new HashMap(): null;
  }

  public ControlFlowGraph makeCFG(final IMethod method, 
	  final Context context,
	  final ClassHierarchy cha, 
	  final WarningSet warnings)
  {
    return ((AstMethod)method).getControlFlowGraph();
  }

  public class AstIR extends IR {
    private final SSA2LocalMap localMap;

    private void setCatchInstructions(SSACFG ssacfg, AbstractCFG oldcfg) {
      for (int i = 0; i < oldcfg.getNumberOfNodes(); i++)
	if (oldcfg.isCatchBlock(i)) {
	  ExceptionHandlerBasicBlock B =
	    (ExceptionHandlerBasicBlock) ssacfg.getNode(i);
	  B.setCatchInstruction((SSAGetCaughtExceptionInstruction)
	     getInstructions()[B.getFirstInstructionIndex()]);
	}
    }

    private void setupCatchTypes(SSACFG cfg, TypeReference[][] catchTypes) {
      for (int i = 0; i < catchTypes.length; i++) {
	if (catchTypes[i] != null) {
	  ExceptionHandlerBasicBlock bb = 
	    (ExceptionHandlerBasicBlock) cfg.getNode(i);
	  for (int j = 0; j < catchTypes[i].length; j++) {
	    bb.addCaughtExceptionType(catchTypes[i][j]);
	  }
	}
      }
    }
      
    protected SSA2LocalMap getLocalMap() {
      return localMap;
    }
	  
    protected String instructionPosition(int instructionIndex) {
      Position pos = 
	((AstMethod)getMethod()).getSourcePosition(instructionIndex);
      if (pos == null) {
        return "";
      } else {
	return pos.toString();
      }
    }

    private AstIR(AstMethod method, 
		  SSAInstruction[] instructions, 
		  SymbolTable symbolTable, 
		  SSACFG cfg,
		  SSAOptions options) 
    {
      super(method, instructions, symbolTable, cfg, options);
    
      setCatchInstructions(getControlFlowGraph(), method.cfg);

      localMap = SSAConversion.convert(method, this, options);
	  
      setupCatchTypes(getControlFlowGraph(), method.catchTypes);

      setupLocationMap();
    }
  }
    
  public IR makeIR(final IMethod method, 
	  final Context context,
	  final ClassHierarchy cha, 
	  final SSAOptions options, 
	  final WarningSet warnings)
  {    
    Assertions._assert(method instanceof AstMethod, method.toString());
    if (keepIR) {
      if (keptIRs.containsKey(method)) {
	return (IR) keptIRs.get(method);
      }
    }

    AbstractCFG oldCfg = ((AstMethod)method).cfg;
    SSAInstruction[] instrs = (SSAInstruction[])oldCfg.getInstructions();

    IR newIR = 
      new AstIR((AstMethod)method, 
	     instrs,
	     ((AstMethod)method).symtab,
	     new SSACFG(method, oldCfg, instrs, warnings),
	     options);

    if (keepIR) {
      keptIRs.put(method, newIR);
    }

    return newIR;
  }
    
  public static IRFactory makeDefaultFactory(final boolean keepAstIRs) {
    return new DefaultIRFactory() {
      private final AstIRFactory astFactory = new AstIRFactory(keepAstIRs);

      public IR makeIR(IMethod method, 
		       Context context,
		       ClassHierarchy cha, 
		       SSAOptions options, 
		       WarningSet warnings) 
      {
	if (method instanceof AstMethod) {
	  return astFactory.makeIR(method, context, cha, options, warnings);
	} else {
	  return super.makeIR(method, context, cha, options, warnings);
	}
      }

      public ControlFlowGraph makeCFG(IMethod method, 
		       Context context,
		       ClassHierarchy cha, 
		       WarningSet warnings) 
      {
	if (method instanceof AstMethod) {
	  return astFactory.makeCFG(method, context, cha, warnings);
	} else {
	  return super.makeCFG(method, context, cha, warnings);
	}
      }
    };
  }
}
