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

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

public class AstIRFactory implements IRFactory {

  public ControlFlowGraph makeCFG(final IMethod method, final Context context) {
    return ((AstMethod) method).getControlFlowGraph();
  }

  public class AstIR extends IR {
    private final LexicalInformation lexicalInfo;
    
    private final SSA2LocalMap localMap;

    public LexicalInformation lexicalInfo() {
      return lexicalInfo;
    }
    
    private void setCatchInstructions(SSACFG ssacfg, AbstractCFG oldcfg) {
      for (int i = 0; i < oldcfg.getNumberOfNodes(); i++)
        if (oldcfg.isCatchBlock(i)) {
          ExceptionHandlerBasicBlock B = (ExceptionHandlerBasicBlock) ssacfg.getNode(i);
          B.setCatchInstruction((SSAGetCaughtExceptionInstruction) getInstructions()[B.getFirstInstructionIndex()]);
          getInstructions()[B.getFirstInstructionIndex()] = null;
        }
    }

    private void setupCatchTypes(SSACFG cfg, TypeReference[][] catchTypes) {
      for (int i = 0; i < catchTypes.length; i++) {
        if (catchTypes[i] != null) {
          ExceptionHandlerBasicBlock bb = (ExceptionHandlerBasicBlock) cfg.getNode(i);
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
      Position pos = ((AstMethod) getMethod()).getSourcePosition(instructionIndex);
      if (pos == null) {
        return "";
      } else {
        return pos.toString();
      }
    }

    private AstIR(AstMethod method, SSAInstruction[] instructions, SymbolTable symbolTable, SSACFG cfg, SSAOptions options) {
      super(method, instructions, symbolTable, cfg, options);

      lexicalInfo = method.cloneLexicalInfo();
      
      localMap = SSAConversion.convert(method, this, options);

      setCatchInstructions(getControlFlowGraph(), method.cfg());

      setupCatchTypes(getControlFlowGraph(), method.catchTypes());

      setupLocationMap();
    }
  }

  public IR makeIR(final IMethod method, final Context context, final SSAOptions options) {
    Assertions._assert(method instanceof AstMethod, method.toString());
//    Pair<IMethod,Context> key = Pair.make(method, context);

    AbstractCFG oldCfg = ((AstMethod) method).cfg();
    SSAInstruction[] oldInstrs = (SSAInstruction[]) oldCfg.getInstructions();
    SSAInstruction[] instrs = new SSAInstruction[ oldInstrs.length ];
    System.arraycopy(oldInstrs, 0, instrs, 0, instrs.length);
    
    IR newIR = new AstIR((AstMethod) method, instrs, ((AstMethod) method).symbolTable(), new SSACFG(method, oldCfg, instrs),
        options);

    return newIR;
  }

  public static IRFactory<IMethod> makeDefaultFactory() {
    return new DefaultIRFactory() {
      private final AstIRFactory astFactory = new AstIRFactory();

      public IR makeIR(IMethod method, Context context, SSAOptions options) {
        if (method instanceof AstMethod) {
          return astFactory.makeIR(method, context, options);
        } else {
          return super.makeIR(method, context, options);
        }
      }

      public ControlFlowGraph makeCFG(IMethod method, Context context) {
        if (method instanceof AstMethod) {
          return astFactory.makeCFG(method, context);
        } else {
          return super.makeCFG(method, context);
        }
      }
    };
  }

  public boolean contextIsIrrelevant(IMethod method) {
    return true;
  }
}
