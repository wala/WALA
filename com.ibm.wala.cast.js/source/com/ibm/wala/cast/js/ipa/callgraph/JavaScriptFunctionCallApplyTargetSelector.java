package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.Map;

import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummarizedFunction;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummary;
import com.ibm.wala.cast.js.loader.JSCallSiteReference;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.ssa.JSInstructionFactory;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.strings.Atom;

/**
 * Generate IR to model Function.call() and Function.apply()
 * 
 * @see <a
 *      href="https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/Apply">MDN
 *      Function.apply() docs</a>
 * @see <a
 *      href="https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/Call">MDN
 *      Function.call() docs</a>
 * 
 * @author manu
 * 
 */
public class JavaScriptFunctionCallApplyTargetSelector implements MethodTargetSelector {

  private final IClassHierarchy cha;
  private final MethodTargetSelector base;

  public JavaScriptFunctionCallApplyTargetSelector(IClassHierarchy cha, MethodTargetSelector base) {
    this.cha = cha;
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
    if (cha.isSubclassOf(receiver, cha.lookupClass(JavaScriptTypes.CodeBody))) {
      // TODO better way to do these tests
      String s = receiver.toString();
      if (s.equals("function Lprologue.js/functionCall")) {
        return getFunctionCallTarget(caller, site, receiver);
      } else if (s.equals("function Lprologue.js/functionApply")) {
        return getFunctionApplyTarget(caller, site, receiver);
      }
    }
    return base.getCalleeTarget(caller, site, receiver);
  }

  private IMethod getFunctionApplyTarget(CGNode caller, CallSiteReference site, IClass receiver) {
//    System.err.println("TODO: handle Function.apply()");
    return base.getCalleeTarget(caller, site, receiver);
  }

  private static final boolean SEPARATE_SYNTHETIC_METHOD_PER_SITE = true;

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
   * @return
   */
  private IMethod getFunctionCallTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    int nargs = getNumberOfArgsPassed(caller, site);
    Object key = getKey(nargs, caller, site);
    if (callModels.containsKey(key)) {
      return callModels.get(key);
    }
    JSInstructionFactory insts = (JSInstructionFactory) receiver.getClassLoader().getInstructionFactory();
    MethodReference ref = genSyntheticMethodRef(receiver, nargs, key);
    JavaScriptSummary S = new JavaScriptSummary(ref, nargs);

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
    S.addStatement(insts.Invoke(2, resultVal, params, resultVal + 1, cs));
    S.getNextProgramCounter();

    S.addStatement(insts.ReturnInstruction(resultVal, false));
    S.getNextProgramCounter();

    JavaScriptSummarizedFunction t = new JavaScriptSummarizedFunction(ref, S, receiver);
    callModels.put(key, t);
    return t;
  }

  private MethodReference genSyntheticMethodRef(IClass receiver, int nargs, Object key) {
    Atom atom = null;
    if (key instanceof Pair) {
      Pair p = (Pair) key;
      atom = Atom.findOrCreateUnicodeAtom("call_" + p.fst + "_" + p.snd);
    } else {
      atom = Atom.findOrCreateUnicodeAtom("call" + nargs);
    }
    Descriptor desc = Descriptor.findOrCreateUTF8(JavaScriptLoader.JS, "()LRoot;");
    MethodReference ref = MethodReference.findOrCreate(receiver.getReference(), atom, desc);
    return ref;
  }

  private Object getKey(int nargs, CGNode caller, CallSiteReference site) {
    if (SEPARATE_SYNTHETIC_METHOD_PER_SITE) {
      return Pair.make(caller.getGraphNodeId(), site.getProgramCounter());
    } else {
      return nargs;
    }
  }

  private int getNumberOfArgsPassed(CGNode caller, CallSiteReference site) {
    IR callerIR = caller.getIR();
    SSAAbstractInvokeInstruction callStmts[] = callerIR.getCalls(site);
    assert callStmts.length == 1;
    int nargs = callStmts[0].getNumberOfParameters();
    return nargs;
  }

}
