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
/**
 * translator from Callgraph to GR. 
 */
package com.ibm.wala.stringAnalysis.translator;

import java.util.*;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.*;
import com.ibm.wala.stringAnalysis.grammar.*;
import com.ibm.wala.stringAnalysis.util.SAUtil;

public class CG2GR implements ICG2Grammar {
  private IIR2Grammar ir2gr;
  private ICalleeResolver calleeResolver;

  static private class DefaultFunctionNameResolver implements IFunctionNameResolver {
    public Set resolve(PropagationCallGraphBuilder builder, CallGraph cg, CGNode node, GR gr, InvocationSymbol invoke) {
      return SAUtil.set(new ISymbol[]{invoke});
    }
  }

  private static ICalleeResolver defaultCalleeResolver = new FunctionNameCalleeResolver(new DefaultFunctionNameResolver());

  public CG2GR(IIR2Grammar ir2gr, ICalleeResolver calleeResolver) {
    this.ir2gr = ir2gr;
    this.calleeResolver = calleeResolver;
  }

  public CG2GR(IIR2Grammar ir2gr) {
    this(ir2gr, defaultCalleeResolver);
  }

  public ISimplify translate(PropagationCallGraphBuilder callGraphBuilder) {
    CallGraph cg = callGraphBuilder.getCallGraph();
    CalleeMap node2gr = new CalleeMap();
    Collection entryPoints = cg.getEntrypointNodes();
    if (entryPoints.isEmpty()) {
      entryPoints.add(cg.getFakeRootNode());
    }
    for (Iterator i = entryPoints.iterator(); i.hasNext(); ) {
      CGNode entryPoint = (CGNode) i.next();
      createGRMap(callGraphBuilder, null, entryPoint, null, null, node2gr, new HashSet());
    }
    replaceInvocationWithGR(callGraphBuilder, cg, node2gr);
    IRegularlyControlledGrammar ggr = null;
    for (Iterator i = cg.getEntrypointNodes().iterator(); i.hasNext(); ) {
      CGNode entryPoint = (CGNode) i.next();
      GR gr = (GR) node2gr.get(new CalleeNode(cg,null,entryPoint,null,null));
      if (ggr == null) {
        ggr = gr;
      }
      else {
        ggr = ControlledGrammars.createUnion(ggr, gr);
      }
      Trace.println("-- CG2GR#translate: ggr =");
      Trace.println(SAUtil.prettyFormat(ggr));
    }
    Assertions._assert(ggr != null);
    return new GR(null, null, null, ggr.getAutomaton(), ggr.getFails(), ggr.getRuleMap());
  }

  private void createGRMap(PropagationCallGraphBuilder cgbuilder, CGNode caller, CGNode callee, CallSiteReference callSite, InstanceKey ikey, CalleeMap node2gr, Set history) {
    CallGraph cg = cgbuilder.getCallGraph();
    IR ir = SAUtil.Domo.getIR(cg, callee);
    CalleeNode cnode = new CalleeNode(cg, caller, callee, callSite, ikey);
    if (history.contains(cnode)) {
      return ;
    }
    history.add(cnode);
    GR gr = (GR) ir2gr.translate(new TranslationContext(ir, callee, callSite, cgbuilder));
    node2gr.put(cnode, gr);
    Trace.println("-- CG2GR#createGRMap: cnode = " + cnode + ", gr =");
    Trace.println(SAUtil.prettyFormat(gr));
    // TODO: can I use ir.iterateCallSites()?
    for (Iterator i = ir.iterateAllInstructions(); i.hasNext(); ) {
      SSAInstruction instruction = (SSAInstruction) i.next();
      if (!(instruction instanceof SSAAbstractInvokeInstruction)) {
        continue;
      }
      SSAAbstractInvokeInstruction invoke = (SSAAbstractInvokeInstruction) instruction;
      if (invoke.getInvocationCode() == IInvokeInstruction.Dispatch.STATIC) {
        continue;
      }
      PointerKey pkey = cgbuilder.getPointerKeyForLocal(cnode.callee, invoke.getReceiver());
      OrdinalSet ikeys = cgbuilder.getPointerAnalysis().getPointsToSet(pkey);
      for (Iterator iterIKey = ikeys.iterator(); iterIKey.hasNext(); ) {
        InstanceKey nextIKey = (InstanceKey) iterIKey.next();
        CGNode nextNode = cgbuilder.getTargetForCall(cnode.callee, invoke.getCallSite(), nextIKey);
        if (nextNode == null) continue; // TODO: should remove this code?
        Assertions._assert(nextNode != null, "no target for " + nextIKey + " at " + invoke + " in " + callee);
        Trace.println("getTargetForCall in createGRMap(): #node=" + cg.getNumber(nextNode) + ": " + nextNode);
        Trace.println("                                 : #ikey=" + nextIKey);
        Trace.println("                                 : #site=" + invoke.getCallSite());
        createGRMap(cgbuilder, cnode.callee, nextNode, invoke.getCallSite(), nextIKey, node2gr, history);
      }
    }
  }

  private void replaceInvocationWithGR(PropagationCallGraphBuilder builder, CallGraph cg, final CalleeMap node2gr){
    for (Iterator i = node2gr.getCalleeNodes().iterator(); i.hasNext(); ) {
      CalleeNode cnode = (CalleeNode) i.next();
      GR gr = (GR) node2gr.get(cnode);
      Trace.println("-- CG2GR#replaceInvocationWithGR:");
      Trace.println("processing " + gr.getIR().getMethod());
      Trace.println("-- CG2GR#replaceInvocationWithGR: gr =" + SAUtil.prettyFormat(gr));
      for (Iterator k = gr.getRuleMap().keySet().iterator(); k.hasNext(); ) {
        ISymbol label = (ISymbol) k.next();
        GRule rule = (GRule) gr.getRule(label);
        if (rule.getSSAInstruction() instanceof SSAAbstractInvokeInstruction) {
          replaceInvokeInstruction(builder, cg, cnode.callee, gr, label, rule, node2gr);
        }
      }
      Trace.println("-- CG2GR#replaceInvocationWithGR: gr =" + SAUtil.prettyFormat(gr));
    }
  }

  private Set getCallTargets(CalleeMap node2gr, CGNode caller, CallSiteReference callSite) {
    Set s = new HashSet();
    for (Iterator i = node2gr.getCalleeNodes().iterator(); i.hasNext(); ) {
      CalleeNode cnode = (CalleeNode) i.next();
      if ( ((cnode.caller==null) ? (caller==null) : cnode.caller.equals(caller))
          && ((cnode.callSite==null) ? (callSite==null) : cnode.callSite.equals(callSite)) ) {
        s.add(node2gr.get(cnode));
      }
    }
    return s;
  }

  protected Set resolveCallees(PropagationCallGraphBuilder builder, CallGraph cg, CGNode node, GR gr, GRule rule, CalleeMap node2gr) {
    return calleeResolver.resolve(builder, cg, node, gr, rule, node2gr);
  }

  private void replaceInvokeInstruction(PropagationCallGraphBuilder builder, CallGraph cg, CGNode node, GR gr, ISymbol label, GRule rule, final CalleeMap node2gr) {
    SSAAbstractInvokeInstruction invoke = (SSAAbstractInvokeInstruction) rule.getSSAInstruction();
    Trace.println("instruction: " + invoke + "[" + invoke.getClass() + "]");
    Set callees = getCallTargets(node2gr, node, invoke.getCallSite());
    if (callees.isEmpty()) {
      Trace.println("callees are not found for: " + rule);
      Set grammars = resolveCallees(builder, cg, node, gr, rule, node2gr);
      GInvocationRule crule = new GInvocationRule(grammars, rule);
      Trace.println("created composite rule for: " + rule);
      Trace.println(SAUtil.prettyFormat(crule));
      gr.getRuleMap().put(label, crule);
    }
    else{
      GInvocationRule crule = createCompositeRuleFromGR(builder, gr, rule, callees);
      Trace.println("created composite rule for: " + rule);
      Trace.println(SAUtil.prettyFormat(crule));
      gr.getRuleMap().put(label, crule);
    }
  }

  private GInvocationRule createCompositeRuleFromGR(PropagationCallGraphBuilder builder, final GR caller, final GRule rule, Set callees) {
    InvocationSymbol invokeSymbol = (InvocationSymbol) rule.getRight(0);
    final GInvocationRule crule = new GInvocationRule(callees, rule);
    for (Iterator i = callees.iterator(); i.hasNext(); ) {
      GR callee = (GR) i.next();

      // calculate production rules for function parameters.
      List params = callee.getParameterVariables();
      if (!params.isEmpty()) {
        Trace.println("invoke: " + rule);
        Trace.println("        " + rule.getSSAInstruction().getClass());
        Trace.println("callee: " + callee.getIR().getMethod() + " : #params=" + params.size());
        ListIterator iparams = params.listIterator();
        {
          int idx = iparams.nextIndex();
          IVariable lhs0 = (IVariable) iparams.next(); // 0th parameter of IR represents 'this'.
          IVariable rhs0 = (IVariable) invokeSymbol.getReceiver();
          if (rhs0 != null) {
            Trace.println("  add alias rule (for receiver): " + lhs0 + " <-> " + rhs0);
            crule.addAliasRule(callee.getIR(), null, idx, lhs0, rhs0);
          }
        }
        while (iparams.hasNext()) {
          int idx = iparams.nextIndex();
          IVariable lhs = (IVariable) iparams.next();
          ISymbol rhs = null;
          if (idx-1 < invokeSymbol.getParameters().size()) {
            rhs = invokeSymbol.getParameter(idx-1);
          }
          else {
            rhs = ir2gr.getBB2Grammar().getSSA2Rule().getDefaultParameterValueSymbol();
          }
          Trace.println("  add alias rule: " + lhs + " <-> " + rhs);
          crule.addAliasRule(callee.getIR(), null, idx, lhs, rhs);
        }
      }

      // calculate production rules for user defined function calls.
      final IVariable lhs = rule.getLeft();
      Set rhss = callee.getReturnSymbols();
      for (Iterator irhss = rhss.iterator(); irhss.hasNext(); ) {
        ISymbol rhs = (ISymbol) irhss.next();
        Trace.println("  add alias rule: " + lhs + " <-> " + rhs);
        crule.addAliasRule(caller.getIR(), rule.getSSAInstruction(), -1, lhs, rhs);
      }
    }
    Trace.println("created composite rule: ");
    Trace.println(SAUtil.prettyFormat(crule));
    return crule;
  }

  public IIR2Grammar getIR2Grammar() {
    return ir2gr;
  }

  /*
    static public List getIR(
            CallGraphBuilder builder,
            Pattern signature, Pattern context) {
        CallGraph cg = builder.getCallGraph();
        List irs = new ArrayList();
        for (Iterator x = cg.iterateNodes(); x.hasNext();) {
            CGNode N = (CGNode) x.next();
            IMethod m = N.getMethod();
            Context c = N.getContext();
            if (signature.matcher(m.getSignature()).matches()
                    && context.matcher(c.toString()).matches()) {
                IR ir = ((SSAContextInterpreter) cg.getInterpreter(N)).getIR(N,
                        builder.getWarnings());
                irs.add(ir);
            }
        }
        return irs;
    }
   */
}
