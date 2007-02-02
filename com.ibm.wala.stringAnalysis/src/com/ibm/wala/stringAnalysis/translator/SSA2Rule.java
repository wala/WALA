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
package com.ibm.wala.stringAnalysis.translator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.stringAnalysis.grammar.*;
import com.ibm.wala.stringAnalysis.ssa.*;
import com.ibm.wala.stringAnalysis.util.*;
import com.ibm.wala.types.*;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;

public abstract class SSA2Rule implements ISSA2Rule {
  protected String symbolSeparator = ":";
  private boolean approximateMembers;

  static protected class ConstantSymbol extends Symbol {
    public ConstantSymbol(String name) {
      super(name);
    }
  }

  static protected ConstantSymbol NULL_SYMBOL = new ConstantSymbol("#null");
  static protected ConstantSymbol UNDEFINED_SYMBOL = new ConstantSymbol("#undefined");
  static protected ConstantSymbol NAN_SYMBOL = new ConstantSymbol("#NaN");
  static protected ConstantSymbol DEFAULT_PARAMETER_VALUE_SYMBOL = NULL_SYMBOL;

  public SSA2Rule(boolean approximateMembers) {
    this.approximateMembers = approximateMembers;
  }
  
  public SSA2Rule() {
    this(true);
  }
  
  public ISymbol getDefaultParameterValueSymbol() {
    return DEFAULT_PARAMETER_VALUE_SYMBOL;
  }

  public Set<ISymbol> analyzeValueSymbol(int v, SSAInstruction instruction, TranslationContext ctx) {
    PointerKey pkey = null;
    if (ctx.getCGNode() != null) {
      pkey = ctx.getCGBuilder().getPointerKeyForLocal(ctx.getCGNode(), v);
    }
    Set syms = new HashSet();
    if (pkey != null) {
      OrdinalSet ikeys = ctx.getCGBuilder().getPointerAnalysis().getPointsToSet(pkey);
      for (Iterator i = ikeys.iterator(); i.hasNext(); ) {
        InstanceKey ikey = (InstanceKey) i.next();
        ISymbol s = getInstanceKeySymbol(ikey, ctx);
        if (s == null) {
          syms.clear();
          return syms;
        }
        else {
          syms.add(s);
        }
      }
    }
    return syms;
  }

  public ISymbol getValueSymbol(int v, SSAInstruction instruction, TranslationContext ctx) {
    if (v < 0) {
      return null;
    }
    Value val = ctx.getIR().getSymbolTable().getValue(v);
    return getValueSymbol(v, val, ctx);
  }

  public ISymbol getValueSymbol(int v, Value val, TranslationContext ctx) {
    if (val instanceof ConstantValue) {
      ConstantValue cval = (ConstantValue) val;
      Object obj = cval.getValue();
      ISymbol s = getObjectSymbol(obj);
      if (s != null) {
        return s;
      }
    }
    PropagationCallGraphBuilder cgbuilder = ctx.getCGBuilder();
    String names[] = ctx.getLocalNameTable().getLocalNames(v);
    String vstr = "";
    if (names.length==1) {
      vstr = names[0];
    }
    return new CDVariable(vstr, v, cgbuilder.getCallGraph(), ctx.getCGNode(), ctx.getCallSiteReference());
  }

  public ISymbol getInstanceKeySymbol(InstanceKey ikey, TranslationContext ctx) {
    if (ikey instanceof ConstantKey) {
      ConstantKey ckey = (ConstantKey) ikey;
      ISymbol s = getObjectSymbol(ckey.getValue());
      return s;
    }
    else if (ikey instanceof AllocationSiteKey) {
      /*
            AllocationSiteKey akey = (AllocationSiteKey) ikey;
            NewSiteReference sref = akey.getSite();
            SSANewInstruction newInstruction = ctx.getIR().getNew(sref);
            return getValueSymbol(newInstruction.getDef(0), newInstruction, ctx);
       */
      return null;
    }
    else {
      return null;
    }
  }

  protected ISymbol getObjectSymbol(Object obj) {
    if (obj == null) {
      return NULL_SYMBOL;
    }
    else if (obj instanceof String) {
      return new StringSymbol((String)obj);
    }
    else if (obj instanceof Number) {
      Number num = (Number) obj;
      return new NumberSymbol(num.toString());
    }
    else {
      Trace.println("unsupported type of the constant: " + obj.getClass());
      return null;
    }
  }

  protected ISymbol getSymbol(String s) {
    return new Symbol(s);
  }

  protected List getValueSymbols(int v[], SSAInstruction instruction, TranslationContext ctx) {
    List l = new ArrayList();
    for (int i = 0; i < v.length; i++) {
      l.add(getValueSymbol(v[i], instruction, ctx));
    }
    return l;
  }

  protected MemberSymbol getMemberSymbol(int objv, int memv, SSAInstruction instruction, IR ir, TranslationContext ctx) {
    return new MemberSymbol(
      getValueSymbol(objv, instruction, ctx),
      getValueSymbol(memv, instruction, ctx));
  }

  protected MemberVariable getMemberVariable(int objv, int memv, SSAInstruction instruction, TranslationContext ctx) {
    IVariable s1 = (IVariable) getValueSymbol(objv, instruction, ctx);
    ISymbol   s2 = (ISymbol) getValueSymbol(memv, instruction, ctx);
    return new MemberVariable(s1, s2);
  }

  protected MemberVariable getMemberVariable(int objv, SSAFieldAccessInstruction instruction, TranslationContext ctx) {
    IVariable s1 = (IVariable) getValueSymbol(objv, instruction, ctx);
    return new MemberVariable(s1, new Symbol(instruction.getDeclaredField().getName().toString()));
  }

  protected InvocationSymbol getInvocationSymbol(ISymbol f, ISymbol recv, int params[], SSAInstruction instruction, TranslationContext ctx) {
    InvocationSymbol isym = new InvocationSymbol(ctx.getIR(), instruction, f, recv,
      getValueSymbols(params, instruction, ctx));
    return isym;
  }

  protected String getOperatorName(BinaryOpInstruction.IOperator opCode) {
    return opCode.toString();
  }

  private InvocationSymbol getOpSymbol(BinaryOpInstruction.IOperator opCode, int params[], SSAInstruction instruction, TranslationContext ctx){
    ISymbol op = new Symbol("op(" +  getOperatorName(opCode) + ")");
    InvocationSymbol isym = new InvocationSymbol(ctx.getIR(), instruction, op, null, getValueSymbols(params, instruction, ctx));
    return isym;
  }

  public IProductionRule createRule(IR ir, SSAInstruction instruction, IVariable left, ISymbol right[]) {
    return new GRule(ir, instruction, left, right);
  }

  protected IProductionRule createRule(IR ir, SSAInstruction instruction, IVariable left, ISymbol right) {
    return createRule(ir, instruction, left, new ISymbol[]{right});
  }

  protected class BaseTranslatingProcessor
  implements SAProcessingInstructionVisitor.Processor 
  {
    protected final TranslationContext context;
    protected final Collection rules;

    protected BaseTranslatingProcessor(TranslationContext ctx, Collection rules) {
      this.context = ctx;
      this.rules = rules;
    }

    public void onUnsupportedInstruction(SSAInstruction instruction) {
      Assertions._assert(
        (instruction instanceof SSAReturnInstruction)
        || (instruction instanceof SSAGotoInstruction)
        || (instruction instanceof SSAGetCaughtExceptionInstruction));
    }

    public void onSSAAbstractInvokeInstruction(SSAAbstractInvokeInstruction instruction) {
      translateInvokeInstruction(instruction, context, rules);
    }

    public void onSSABinaryOpInstruction(SSABinaryOpInstruction instruction) {
      translateBinaryOpInstruction(instruction, context, rules);
    }

    public void onSSAAbstractUnaryInstruction(SSAAbstractUnaryInstruction instruction) {
      translateUnaryInstruction(instruction, context, rules);
    }

    public void onSSANewInstruction(SSANewInstruction instruction) {
      translateNewInstruction(instruction, context, rules);
    }

    public void onSSAPhiInstruction(SSAPhiInstruction instruction) {
      translatePhiInstruction(instruction, context, rules);
    }

    public void onSSAGetInstruction(SSAGetInstruction instruction) {
      translateGet(instruction, context, rules);
    }

    public void onSSAPutInstruction(SSAPutInstruction instruction) {
      translatePut(instruction, context, rules);
    }

    public void onSSAConditionalBranchInstruction(SSAConditionalBranchInstruction instruction) {
      // TODO:
    }

    public void onSSAReturnInstruction(SSAReturnInstruction instruction) {
      //System.out.println("TODO: " + instruction + " : " + instruction.getClass());
      // TODO:
    }

    public void onAstLexicalRead(AstLexicalRead instruction) {
      translateAstLexicalRead(instruction, context, rules);
    }

    public void onAstLexicalWrite(AstLexicalWrite instruction) {
      translateAstLexicalWrite(instruction, context, rules);
    }
  };

  protected SAProcessingInstructionVisitor 
  createTranslatorVisitor(TranslationContext ctx, Collection rules) 
  {
    return new SAProcessingInstructionVisitor(
      new BaseTranslatingProcessor(ctx, rules));
  }

  public Collection<IProductionRule> translate(SSAInstruction instruction, TranslationContext ctx) {
    final Collection<IProductionRule> rules = new HashSet<IProductionRule>();
    SSAInstructionProcessor.eachInstruction(
      new SSAInstruction[]{instruction},
      createTranslatorVisitor(ctx, rules));
    return rules;
  }

  protected void translatePhiInstruction(SSAPhiInstruction instruction, TranslationContext ctx, Collection rules) {
    int nuses = instruction.getNumberOfUses();
    for( int i = 0; i < nuses; i++ ){
      IVariable left = (IVariable) getValueSymbol(instruction.getDef(0), instruction, ctx);
      ISymbol right = getValueSymbol(instruction.getUse(i), instruction, ctx);
      rules.add(createRule(ctx.getIR(), instruction, left, right));
    }
  }

  protected void translateNewInstruction(SSANewInstruction instruction, TranslationContext ctx, Collection rules) {
    /** TODO
     * Do nothing with NewInstruction in order to avoid an unnecessary
     * production rule "vXX -> []."
     * It should be confirmed if this is a "correct" workaround.
     */
//  IVariable left = (IVariable) getValueSymbol(instruction.getDef(0), instruction, ctx);
//  ISymbol right[] = new Symbol[0];
//  rules.add(createRule(ctx.getIR(), instruction, left, right));
  }

  protected void translatePut(SSAPutInstruction instruction, TranslationContext ctx, Collection rules) {
    IVariable left1 = getMemberVariable(instruction.getRef(), instruction, ctx);
    ISymbol right1 = getValueSymbol(instruction.getUse(1), instruction, ctx); // obtain propWrite#value
    rules.add(createRule(ctx.getIR(), instruction, left1, right1));

    if (approximateMembers) {
      IVariable left2 = (IVariable) getValueSymbol(instruction.getRef(), instruction, ctx);
      ISymbol right2 = right1;
      rules.add(createRule(ctx.getIR(), instruction, left2, right2));
    }
  }

  protected void translateGet(SSAGetInstruction instruction, TranslationContext ctx, Collection rules) {
    if (instruction.hasDef()) {
      IVariable left1 = (IVariable) getValueSymbol(instruction.getDef(0), instruction, ctx);
      IVariable right1 = getMemberVariable(instruction.getRef(), instruction, ctx);
      rules.add(createRule(ctx.getIR(), instruction, left1, right1));

      if (approximateMembers) {
        IVariable left2 = (IVariable) getValueSymbol(instruction.getDef(0), instruction, ctx);
        Set rights2 = analyzeValueSymbol(instruction.getDef(0), instruction, ctx);
        if (rights2.isEmpty()) {
          ISymbol right2 = getValueSymbol(instruction.getRef(), instruction, ctx);
          rules.add(createRule(ctx.getIR(), instruction, left2, right2));
        }
        else {
          for (Iterator i = rights2.iterator(); i.hasNext(); ) {
            ISymbol right2 = (ISymbol) i.next();
            rules.add(createRule(ctx.getIR(), instruction, left2, right2));
          }
        }
      }
    } else {
      System.err.println("no lhs: (" + instruction.getClass() + ") "
        + instruction.toString(ctx.getIR().getSymbolTable(),null));
    }
  }

  protected void translateReflectivePut(AbstractReflectivePut instruction, TranslationContext ctx, Collection rules) {
    IVariable left1 = getMemberVariable(instruction.getObjectRef(), instruction.getMemberRef(), instruction, ctx);
    ISymbol right1 = getValueSymbol(instruction.getUse(2), instruction, ctx); // obtain propWrite#value
    rules.add(createRule(ctx.getIR(), instruction, left1, right1));

    if (approximateMembers) {
      IVariable left2 = (IVariable) getValueSymbol(instruction.getObjectRef(), instruction, ctx);
      ISymbol right2 = right1;
      rules.add(createRule(ctx.getIR(), instruction, left2, right2));
    }
  }

  protected void translateReflectiveGet(AbstractReflectiveGet instruction, TranslationContext ctx, Collection rules) {
    if (instruction.hasDef()) {
      IVariable left1 = (IVariable) getValueSymbol(instruction.getDef(0), instruction, ctx);
      IVariable right1 = getMemberVariable(instruction.getObjectRef(), instruction.getMemberRef(), instruction, ctx);
      rules.add(createRule(ctx.getIR(), instruction, left1, right1));

      if (approximateMembers) {
        IVariable left2 = (IVariable) getValueSymbol(instruction.getDef(0), instruction, ctx);
        Set rights2 = analyzeValueSymbol(instruction.getDef(0), instruction, ctx);
        if (rights2.isEmpty()) {
          ISymbol right2 = getValueSymbol(instruction.getObjectRef(), instruction, ctx);
          rules.add(createRule(ctx.getIR(), instruction, left2, right2));
        }
        else {
          for (Iterator i = rights2.iterator(); i.hasNext(); ) {
            ISymbol right2 = (ISymbol) i.next();
            rules.add(createRule(ctx.getIR(), instruction, left2, right2));
          }
        }
      }
    } else {
      System.err.println("no lhs: (" + instruction.getClass() + ") "
        + instruction.toString(ctx.getIR().getSymbolTable(),null));
    }
  }

  protected void translateUnaryInstruction(SSAAbstractUnaryInstruction instruction, TranslationContext ctx, Collection rules) {
    IVariable left = (IVariable) getValueSymbol(instruction.getDef(0), instruction, ctx);
    int v = instruction.getUse(0);
    ISymbol right = getValueSymbol(v, instruction, ctx);
    rules.add(createRule(ctx.getIR(), instruction, left, right));
  }

  protected void translateBinaryOpInstruction(SSABinaryOpInstruction instruction, TranslationContext ctx, Collection rules) {
    IVariable left = (IVariable) getValueSymbol(instruction.getDef(0), instruction, ctx);
    int params[] = new int[instruction.getNumberOfUses()];
    for( int i = 0; i < params.length; i++ ){
      params[i] = instruction.getUse(i);
    }
    ISymbol right = getOpSymbol(instruction.getOperator(), params, instruction, ctx);
    rules.add(createRule(ctx.getIR(), instruction, left, right));
  }
  
  abstract protected void translateInvokeInstruction(SSAAbstractInvokeInstruction instruction, TranslationContext ctx, Collection rules);

  protected void translateAstLexicalRead(AstLexicalRead instruction, TranslationContext ctx, Collection rules) {
    AstLexicalAccess.Access accesses[] = instruction.getAccesses();
    for (int i = 0; i < accesses.length; i++) {
      IVariable left = (IVariable) getValueSymbol(accesses[i].valueNumber, instruction, ctx);
      // TODO: the lexical variable is identified by its name and definer's name at this moment.
      String name = accesses[i].variableName;
      if (accesses[i].variableDefiner != null) {
        name = name + "@" + accesses[i].variableDefiner;
      }
      ISymbol right = new LexicalVariable(name);
      rules.add(createRule(ctx.getIR(), instruction, left, right));
    }
  }

  protected void translateAstLexicalWrite(AstLexicalWrite instruction, TranslationContext ctx, Collection rules) {
    AstLexicalAccess.Access accesses[] = instruction.getAccesses();
    for (int i = 0; i < accesses.length; i++) {
      String name = accesses[i].variableName;
      if (accesses[i].variableDefiner != null) {
        name = name + "@" + accesses[i].variableDefiner;
      }
      IVariable left = new LexicalVariable(name);
      ISymbol right = getValueSymbol(accesses[i].valueNumber, instruction, ctx);
      rules.add(createRule(ctx.getIR(), instruction, left, right));
    }
  }
  
  public GR postTranslate(GR gr) {
    return gr;
  }
}
