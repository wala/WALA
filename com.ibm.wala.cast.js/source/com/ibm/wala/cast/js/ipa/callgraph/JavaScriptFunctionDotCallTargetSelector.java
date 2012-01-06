package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.Map;

import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummarizedFunction;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummary;
import com.ibm.wala.cast.js.loader.JSCallSiteReference;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.ssa.JSInstructionFactory;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position; 

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
  // whether to warn about what looks like constructor invocations of Function.prototype.call
  // while not impossible, such invocations always result in a TypeError and thus are likely due to imprecise call graph information
  public static final boolean WARN_ABOUT_NEW_CALL = true;

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
<<<<<<< HEAD
    IMethod method = receiver.getMethod(AstMethodReference.fnSelector);
    if (method != null) {
      String s = method.getReference().getDeclaringClass().getName().toString();
      if (s.equals("Lprologue.js/functionCall")) {
        return getFunctionCallTarget(caller, site, receiver);
=======
    if (cha.isSubclassOf(receiver, cha.lookupClass(JavaScriptTypes.CodeBody))) {
      // TODO better way to do this test?
      String s = receiver.toString();
      if (s.equals("function Lprologue.js/functionCall")) {
        /* invoking Function.prototype.call as a constructor results in a TypeError
         * see ECMA-262 5.1, 15: "None of the built-in functions described in this clause that 
         *   are not constructors shall implement the [[Construct]] internal method unless otherwise 
         *   specified" */
        if(!site.getDeclaredTarget().equals(JavaScriptMethods.ctorReference)) {
          return getFunctionCallTarget(caller, site, receiver);
        } else {
          // TODO: we know that this invocation would lead to a type error; how do we express this as a call target?
          if(WARN_ABOUT_NEW_CALL) {
            IntIterator indices = caller.getIR().getCallInstructionIndices(site).intIterator();
            IMethod callerMethod = caller.getMethod();
            Position pos = null;
            if(indices.hasNext() && callerMethod instanceof AstMethod) {
              pos = ((AstMethod)callerMethod).getSourcePosition(indices.next());
            }
            System.err.println("Detected constructor call to Function.prototype.call " +
                (pos == null ? "" : "at position " + pos) +
                "; this is likely caused by call graph imprecision.");
          }
        }
>>>>>>> Improved target selector for Function.prototype.call to handle cases
      }
    }
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
    if(nargs < 2)
      Assertions.UNREACHABLE("Call to Function.prototype.call without receiver; this shouldn't be possible.");
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

  public static final String SYNTHETIC_CALL_METHOD_PREFIX = "$$ call_";

  private MethodReference genSyntheticMethodRef(IClass receiver, int nargs, Object key) {
    Atom atom = null;
    if (key instanceof Pair) {
      Pair p = (Pair) key;
      atom = Atom.findOrCreateUnicodeAtom(SYNTHETIC_CALL_METHOD_PREFIX + p.fst + "_" + p.snd);
    } else {
      atom = Atom.findOrCreateUnicodeAtom(SYNTHETIC_CALL_METHOD_PREFIX + nargs);
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
