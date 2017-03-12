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

import java.util.Map;
import java.util.Map.Entry;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.ExceptionHandlerBasicBlock;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAIndirectionData;
import com.ibm.wala.ssa.SSAIndirectionData.Name;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;

public class AstIRFactory<T extends IMethod> implements IRFactory<T> {

  public ControlFlowGraph makeCFG(final IMethod method, final Context context) {
    return ((AstMethod) method).getControlFlowGraph();
  }

  public static class AstDefaultIRFactory extends DefaultIRFactory {
    private final AstIRFactory astFactory;

    public AstDefaultIRFactory() {
      this(new AstIRFactory());
    }
    
    public AstDefaultIRFactory(AstIRFactory astFactory) {
      this.astFactory = astFactory;
    }

    @Override
    public IR makeIR(IMethod method, Context context, SSAOptions options) {
      if (method instanceof AstMethod) {
        return astFactory.makeIR(method, context, options);
      } else {
        return super.makeIR(method, context, options);
      }
    }

    @Override
    public ControlFlowGraph makeCFG(IMethod method, Context context) {
      if (method instanceof AstMethod) {
        return astFactory.makeCFG(method, context);
      } else {
        return super.makeCFG(method, context);
      }
    }
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

    private void setupCatchTypes(SSACFG cfg, Map<IBasicBlock<SSAInstruction>, TypeReference[]> map) {
      for(Entry<IBasicBlock<SSAInstruction>, TypeReference[]> e : map.entrySet()) {
        if (e.getKey().getNumber() != -1) {
          ExceptionHandlerBasicBlock bb = (ExceptionHandlerBasicBlock) cfg.getNode(e.getKey().getNumber());
          for (int j = 0; j < e.getValue().length; j++) {
            bb.addCaughtExceptionType(e.getValue()[j]);
          }
        }
      }
    }

    @Override
    protected SSA2LocalMap getLocalMap() {
      return localMap;
    }

    @Override
    protected String instructionPosition(int instructionIndex) {
      Position pos = getMethod().getSourcePosition(instructionIndex);
      if (pos == null) {
        return "";
      } else {
        return pos.toString();
      }
    }

    @Override
    public AstMethod getMethod() {
      return (AstMethod)super.getMethod();
    }

    private AstIR(AstMethod method, SSAInstruction[] instructions, SymbolTable symbolTable, SSACFG cfg, SSAOptions options) {
      super(method, instructions, symbolTable, cfg, options);

      lexicalInfo = method.cloneLexicalInfo();
      
      localMap = SSAConversion.convert(method, this, options);

      setCatchInstructions(getControlFlowGraph(), method.cfg());

      setupCatchTypes(getControlFlowGraph(), method.catchTypes());

      setupLocationMap();
    }

    @Override
    protected SSAIndirectionData<Name> getIndirectionData() {
      // TODO Auto-generated method stub
      return null;
    }
  }

  @Override
  public IR makeIR(final IMethod method, final Context context, final SSAOptions options) {
    assert method instanceof AstMethod : method.toString();
  
    AbstractCFG oldCfg = ((AstMethod) method).cfg();
    SSAInstruction[] oldInstrs = (SSAInstruction[]) oldCfg.getInstructions();
    SSAInstruction[] instrs = new SSAInstruction[ oldInstrs.length ];
    System.arraycopy(oldInstrs, 0, instrs, 0, instrs.length);
    
    IR newIR = new AstIR((AstMethod) method, instrs, ((AstMethod) method).symbolTable().copy(), new SSACFG(method, oldCfg, instrs),
        options);

    return newIR;
  }

  public static IRFactory<IMethod> makeDefaultFactory() {
    return new AstDefaultIRFactory();
  }

  @Override
  public boolean contextIsIrrelevant(IMethod method) {
    return true;
  }
}
