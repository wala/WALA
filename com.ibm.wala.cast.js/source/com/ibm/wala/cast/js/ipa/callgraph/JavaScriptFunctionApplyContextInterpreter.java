package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.cast.ipa.callgraph.AstContextInsensitiveSSAContextInterpreter;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptFunctionApplyContextSelector.ApplyContext;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummarizedFunction;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummary;
import com.ibm.wala.cast.js.loader.JSCallSiteReference;
import com.ibm.wala.cast.js.ssa.JSInstructionFactory;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;

public class JavaScriptFunctionApplyContextInterpreter extends AstContextInsensitiveSSAContextInterpreter {

  public JavaScriptFunctionApplyContextInterpreter(AnalysisOptions options, AnalysisCache cache) {
    super(options, cache);
  }

  @Override
  public boolean understands(IMethod method, Context context) {
    return context instanceof ApplyContext;
  }

  @Override
  public IR getIR(CGNode node) {
    if (understands(node.getMethod(), node.getContext())) {
      ApplyContext applyContext = (ApplyContext) node.getContext();
      InstanceKey argsList = applyContext.getArgsListKey();
      if (argsList == null) {
        return makeIRForNoArgList(node);
      }
      assert false : "asking for IR for " + node;
    }
    return super.getIR(node);
  }

  private IR makeIRForNoArgList(CGNode node) {
    // kind of a hack; re-use the summarized function infrastructure
    MethodReference ref = node.getMethod().getReference();
    IClass declaringClass = node.getMethod().getDeclaringClass();
    JSInstructionFactory insts = (JSInstructionFactory) declaringClass.getClassLoader().getInstructionFactory();
    // nargs needs to match that of Function.apply(), even though no argsList
    // argument was passed in this case
    int nargs = 4;
    JavaScriptSummary S = new JavaScriptSummary(ref, nargs);

    // generate invocation instruction for the real method being invoked
    int resultVal = nargs + 2;
    CallSiteReference cs = new JSCallSiteReference(S.getNextProgramCounter());
    int[] params = new int[1];
    params[0] = 3;
    // function being invoked is in v2
    S.addStatement(insts.Invoke(2, resultVal, params, resultVal + 1, cs));
    S.getNextProgramCounter();

    S.addStatement(insts.ReturnInstruction(resultVal, false));
    S.getNextProgramCounter();

    JavaScriptSummarizedFunction t = new JavaScriptSummarizedFunction(ref, S, declaringClass);
    return t.makeIR(node.getContext(), null);
  }

  @Override
  public DefUse getDU(CGNode node) {
    if (understands(node.getMethod(), node.getContext())) {
      return new DefUse(getIR(node));
    }
    return super.getDU(node);
  }

}
