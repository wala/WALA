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
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;

/**
 * TODO cache generated IRs
 * 
 * @author manu
 *
 */
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
      boolean isNonNullArray = applyContext.isNonNullArray();
      if (isNonNullArray) {
        return makeIRForArgList(node);
      } else {
        return makeIRForNoArgList(node);
      }
    }
    return super.getIR(node);
  }

  private IR makeIRForArgList(CGNode node) {
    // we have: v1 is dummy apply method
    // v2 is function to be invoked
    // v3 is argument to be passed as 'this'
    // v4 is array containing remaining arguments
    // Ideally, we would take advantage of cases like constant arrays and
    // precisely pass arguments in the appropriate slots. Unfortunately, in the
    // pointer analysis fixed-point computation, it's possible that we will
    // process the apply() call and then process some update to the arguments
    // array, reflected only in its property values and object catalog. Perhaps
    // eventually, we could create contexts based on the catalog of the object
    // and then do a better job, but since the catalog is not passed directly as
    // a parameter to apply(), this is not so easy.
    // In the meantime, we do things imprecisely. We read an arbitrary
    // enumerable property name of the argument list (via an
    // EachElementGetInstruction), perform a dynamic read of that property, and
    // then pass the resulting values in all argument positions (except 'this').
    //
    // NOTE: we don't know how many arguments the callee will take, whether it
    // uses
    // the arguments array, etc. For now, we use an unsound hack and pass the
    // argument 10 times.
    //
    // NOTE: strictly speaking, using EachElementGet could be imprecise, as it
    // should
    // return properties inherited via the prototype chain. However, since this
    // behavior
    // is not modeled in WALA as of now, using the instruction is ok.
    MethodReference ref = node.getMethod().getReference();
    IClass declaringClass = node.getMethod().getDeclaringClass();
    JSInstructionFactory insts = (JSInstructionFactory) declaringClass.getClassLoader().getInstructionFactory();
    // nargs needs to match that of Function.apply(), even though no argsList
    // argument was passed in this case
    int nargs = 4;
    JavaScriptSummary S = new JavaScriptSummary(ref, nargs);

    // read an arbitrary property name via EachElementGet
    int curValNum = nargs + 2;
    int eachElementGetResult = curValNum++;
    S.addStatement(insts.EachElementGetInstruction(eachElementGetResult, 4));
    S.getNextProgramCounter();
    // read value from the arbitrary property name
    int propertyReadResult = curValNum++;
    S.addStatement(insts.PropertyRead(propertyReadResult, 4, eachElementGetResult));
    S.getNextProgramCounter();

    int numParamsToPass = 10;
    CallSiteReference cs = new JSCallSiteReference(S.getNextProgramCounter());
    int[] params = new int[numParamsToPass + 1];
    // pass the 'this' argument first
    params[0] = 3;
    for (int i = 1; i < params.length; i++) {
      params[i] = propertyReadResult;
    }
    // function being invoked is in v2
    int resultVal = curValNum++;
    int excVal = curValNum++;
    S.addStatement(insts.Invoke(2, resultVal, params, excVal, cs));
    S.getNextProgramCounter();

    S.addStatement(insts.ReturnInstruction(resultVal, false));
    S.getNextProgramCounter();

    JavaScriptSummarizedFunction t = new JavaScriptSummarizedFunction(ref, S, declaringClass);
    return t.makeIR(node.getContext(), null);
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
