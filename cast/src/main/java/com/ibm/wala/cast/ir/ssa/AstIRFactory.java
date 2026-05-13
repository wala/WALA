/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.cast.ir.ssa.SSAConversion.SSAInformation;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AstIRFactory<T extends IMethod> implements IRFactory<T> {

  public ControlFlowGraph<?, ?> makeCFG(final IMethod method) {
    return ((AstMethod) method).getControlFlowGraph();
  }

  public static class AstDefaultIRFactory<T extends IMethod> extends DefaultIRFactory {
    private final AstIRFactory<T> astFactory;

    public AstDefaultIRFactory() {
      this(new AstIRFactory<>());
    }

    public AstDefaultIRFactory(AstIRFactory<T> astFactory) {
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
    public ControlFlowGraph<?, ?> makeCFG(IMethod method, Context context) {
      if (method instanceof AstMethod) {
        return astFactory.makeCFG(method);
      } else {
        return super.makeCFG(method, context);
      }
    }
  }

  public static class AstIR extends IR {
    private final LexicalInformation lexicalInfo;

    private final SSAConversion.SSAInformation localMap;

    public LexicalInformation lexicalInfo() {
      return lexicalInfo;
    }

    private void setCatchInstructions(SSACFG ssacfg, AbstractCFG<?, ?> oldCfg) {
      for (int i = 0; i < oldCfg.getNumberOfNodes(); i++)
        if (oldCfg.isCatchBlock(i)) {
          ExceptionHandlerBasicBlock B = (ExceptionHandlerBasicBlock) ssacfg.getNode(i);
          B.setCatchInstruction(
              (SSAGetCaughtExceptionInstruction) getInstructions()[B.getFirstInstructionIndex()]);
          getInstructions()[B.getFirstInstructionIndex()] = null;
        } else {
          assert !(ssacfg.getNode(i) instanceof ExceptionHandlerBasicBlock);
        }
    }

    private static void setupCatchTypes(
        SSACFG cfg, Map<IBasicBlock<SSAInstruction>, Set<TypeReference>> map) {
      for (Entry<IBasicBlock<SSAInstruction>, Set<TypeReference>> e : map.entrySet()) {
        if (e.getKey().getNumber() != -1) {
          ExceptionHandlerBasicBlock bb =
              (ExceptionHandlerBasicBlock) cfg.getNode(e.getKey().getNumber());
          e.getValue().forEach(bb::addCaughtExceptionType);
        }
      }
    }

    @Override
    public SSAInformation getLocalMap() {
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
      return (AstMethod) super.getMethod();
    }

    private AstIR(
        AstMethod method,
        SSAInstruction[] instructions,
        SymbolTable symbolTable,
        SSACFG cfg,
        SSAOptions options) {
      super(method, instructions, symbolTable, cfg, options);

      lexicalInfo = method.cloneLexicalInfo();

      setCatchInstructions(getControlFlowGraph(), method.cfg());

      localMap = SSAConversion.convert(method, this, options);

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

    AbstractCFG<?, ?> oldCfg = ((AstMethod) method).cfg();
    SSAInstruction[] oldInstrs = (SSAInstruction[]) oldCfg.getInstructions();
    SSAInstruction[] instrs = oldInstrs.clone();

    return new AstIR(
        (AstMethod) method,
        instrs,
        ((AstMethod) method).symbolTable().copy(),
        new SSACFG(method, oldCfg, instrs),
        options);
  }

  public static IRFactory<IMethod> makeDefaultFactory() {
    return new AstDefaultIRFactory<>();
  }

  @Override
  public boolean contextIsIrrelevant(IMethod method) {
    return true;
  }
}
