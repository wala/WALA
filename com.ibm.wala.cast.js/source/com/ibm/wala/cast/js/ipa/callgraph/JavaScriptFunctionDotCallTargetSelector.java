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
package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.Map;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummarizedFunction;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummary;
import com.ibm.wala.cast.js.loader.JSCallSiteReference;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.ssa.JSInstructionFactory;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.strings.Atom;

/**
 * Generate IR to model Function.call()
 * 
 * @see <a
 *      href="https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/Call">MDN
 *      Function.call() docs</a>
 * 
 * @author manu
 * 
 */
public class JavaScriptFunctionDotCallTargetSelector implements MethodTargetSelector {
  /*
   * Call graph imprecision often leads to spurious invocations of Function.prototype.call; two common
   * patterns are invocations of "new" on Function.prototype.call (which in reality would lead to a
   * type error), and self-applications of Function.prototype.call.
   * 
   * While neither of these situations is a priori impossible, they are most likely due to analysis
   * imprecision. If this flag is set to true, we emit a warning when seeing them. 
   */
  public static boolean WARN_ABOUT_IMPRECISE_CALLGRAPH = true;
  
  public static final boolean DEBUG_SYNTHETIC_CALL_METHODS = false;

  private static final TypeName CALL_TYPE_NAME = TypeName.findOrCreate("Lprologue.js/Function_prototype_call");
  private final MethodTargetSelector base;

  public JavaScriptFunctionDotCallTargetSelector(MethodTargetSelector base) {
    this.base = base;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.ibm.wala.ipa.callgraph.MethodTargetSelector#getCalleeTarget(com.ibm
   * .wala.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference,
   * com.ibm.wala.classLoader.IClass)
   */
  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    IMethod method = receiver.getMethod(AstMethodReference.fnSelector);
    if (method != null) {
      TypeName tn = method.getReference().getDeclaringClass().getName();
      if (tn.equals(CALL_TYPE_NAME)) {
        /* invoking Function.prototype.call as a constructor results in a TypeError
         * see ECMA-262 5.1, 15: "None of the built-in functions described in this clause that 
         *   are not constructors shall implement the [[Construct]] internal method unless otherwise 
         *   specified" */
        if(!site.getDeclaredTarget().equals(JavaScriptMethods.ctorReference)) {
          IMethod target = getFunctionCallTarget(caller, site, receiver);
          if(target != null)
            return target;
        }
        // if we get here, we either saw an invocation of "call" as a constructor, or an invocation 
        // without receiver object; in either case, this is likely due to bad call graph info
        if(WARN_ABOUT_IMPRECISE_CALLGRAPH)
          warnAboutImpreciseCallGraph(caller, site);
      }
    }
    return base.getCalleeTarget(caller, site, receiver);
  }

  protected void warnAboutImpreciseCallGraph(CGNode caller, CallSiteReference site) {
    IntIterator indices = caller.getIR().getCallInstructionIndices(site).intIterator();
    IMethod callerMethod = caller.getMethod();
    Position pos = null;
    if(indices.hasNext() && callerMethod instanceof AstMethod) {
      pos = ((AstMethod)callerMethod).getSourcePosition(indices.next());
    }
    System.err.println("Detected improbable call to Function.prototype.call " +
        (pos == null ? "in function " + caller : "at position " + pos) +
        "; this is likely caused by call graph imprecision.");
  }
  
  private static final boolean SEPARATE_SYNTHETIC_METHOD_PER_SITE = false;

  /**
   * cache synthetic method for each arity of Function.call() invocation
   */
  private final Map<Object, JavaScriptSummarizedFunction> callModels = HashMapFactory.make();

  /**
   * generate a synthetic method modeling the invocation of Function.call() at
   * the site
   * 
   * @param caller
   * @param site
   * @param receiver
   */
  private IMethod getFunctionCallTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    int nargs = getNumberOfArgsPassed(caller, site);
    if(nargs < 2)
      return null;
    String key = getKey(nargs, caller, site);
    if (callModels.containsKey(key)) {
      return callModels.get(key);
    }
    JSInstructionFactory insts = (JSInstructionFactory) receiver.getClassLoader().getInstructionFactory();
    MethodReference ref = genSyntheticMethodRef(receiver, key);
    JavaScriptSummary S = new JavaScriptSummary(ref, nargs);
    
    if(WARN_ABOUT_IMPRECISE_CALLGRAPH && caller.getMethod().getName().toString().contains(SYNTHETIC_CALL_METHOD_PREFIX))
      warnAboutImpreciseCallGraph(caller, site);
    
    // print information about where the method was created if desired 
    if(DEBUG_SYNTHETIC_CALL_METHODS) {
      IMethod method = caller.getMethod();
      if(method instanceof AstMethod) {
        int line = ((AstMethod)method).getLineNumber(caller.getIR().getCallInstructionIndices(site).intIterator().next());
        System.err.println("creating " + ref.getName() + " at line " + line + " in " + caller);
      } else {
        System.err.println("creating " + ref.getName() + " in " + method.getName());
      }
    }

    // generate invocation instruction for the real method being invoked
    int resultVal = nargs + 2;
    CallSiteReference cs = new JSCallSiteReference(S.getNextProgramCounter());
    int[] params = new int[nargs - 2];
    for (int i = 0; i < params.length; i++) {
      // add 3 to skip v1 (which points to Function.call() itself) and v2 (the
      // real function being invoked)
      params[i] = i + 3;
    }
    // function being invoked is in v2
    S.addStatement(insts.Invoke(S.getNumberOfStatements(), 2, resultVal, params, resultVal + 1, cs));
    S.getNextProgramCounter();

    S.addStatement(insts.ReturnInstruction(S.getNumberOfStatements(), resultVal, false));
    S.getNextProgramCounter();

    JavaScriptSummarizedFunction t = new JavaScriptSummarizedFunction(ref, S, receiver);
    callModels.put(key, t);
    return t;
  }

  public static final String SYNTHETIC_CALL_METHOD_PREFIX = "$$ call_";

  private static MethodReference genSyntheticMethodRef(IClass receiver, String key) {
    Atom atom = Atom.findOrCreateUnicodeAtom(SYNTHETIC_CALL_METHOD_PREFIX + key);
    Descriptor desc = Descriptor.findOrCreateUTF8(JavaScriptLoader.JS, "()LRoot;");
    MethodReference ref = MethodReference.findOrCreate(receiver.getReference(), atom, desc);
    return ref;
  }

  private static String getKey(int nargs, CGNode caller, CallSiteReference site) {
    if (SEPARATE_SYNTHETIC_METHOD_PER_SITE) {
      return CAstCallGraphUtil.getShortName(caller) + "_" + caller.getGraphNodeId() + "_" + site.getProgramCounter();
    } else {
      return ""+nargs;
    }
  }

  private static int getNumberOfArgsPassed(CGNode caller, CallSiteReference site) {
    IR callerIR = caller.getIR();
    SSAAbstractInvokeInstruction callStmts[] = callerIR.getCalls(site);
    assert callStmts.length == 1;
    int nargs = callStmts[0].getNumberOfParameters();
    return nargs;
  }

}
