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

import com.ibm.wala.cast.ipa.callgraph.AstContextInsensitiveSSAContextInterpreter;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummarizedFunction;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummary;
import com.ibm.wala.cast.js.loader.JSCallSiteReference;
import com.ibm.wala.cast.js.ssa.JSInstructionFactory;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;

/**
 * TODO cache generated IRs
 * 
 * @see <a href="https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/Apply">MDN Function.apply() docs</a>
 */
public class JavaScriptFunctionApplyContextInterpreter extends AstContextInsensitiveSSAContextInterpreter {

  private static final TypeName APPLY_TYPE_NAME = TypeName.findOrCreate("Lprologue.js/Function_prototype_apply");

  public JavaScriptFunctionApplyContextInterpreter(AnalysisOptions options, IAnalysisCacheView cache) {
    super(options, cache);
  }

  @Override
  public boolean understands(CGNode node) {
    return node.getMethod().getDeclaringClass().getName().equals(APPLY_TYPE_NAME);
  }

  @Override
  public IR getIR(CGNode node) {
    assert understands(node);
    @SuppressWarnings("unchecked")
    ContextItem.Value<Boolean> isNonNullArray = (ContextItem.Value<Boolean>) node.getContext().get(JavaScriptFunctionApplyContextSelector.APPLY_NON_NULL_ARGS);
    // isNonNullArray can be null if, e.g., due to recursion bounding we have no
    // information on the arguments parameter
    if (isNonNullArray == null || isNonNullArray.getValue()) {
      return makeIRForArgList(node);
    } else {
      return makeIRForNoArgList(node);
    }
  }

  private static IR makeIRForArgList(CGNode node) {
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

    int numParamsToPass = 10;
    int[] paramsToPassToInvoked = new int[numParamsToPass + 1];
    // pass the 'this' argument first
    paramsToPassToInvoked[0] = 3;

//    int curValNum = passArbitraryPropertyValAsParams(insts, nargs, S, paramsToPassToInvoked);
    int curValNum = passActualPropertyValsAsParams(insts, nargs, S, paramsToPassToInvoked);
    
    CallSiteReference cs = new JSCallSiteReference(S.getNextProgramCounter());

    // function being invoked is in v2
    int resultVal = curValNum++;
    int excVal = curValNum++;
    S.addStatement(insts.Invoke(S.getNumberOfStatements(), 2, resultVal, paramsToPassToInvoked, excVal, cs));
    S.getNextProgramCounter();

    S.addStatement(insts.ReturnInstruction(S.getNumberOfStatements(), resultVal, false));
    S.getNextProgramCounter();

    JavaScriptSummarizedFunction t = new JavaScriptSummarizedFunction(ref, S, declaringClass);
    return t.makeIR(node.getContext(), null);
  }

  @SuppressWarnings("unused")
  private static int passArbitraryPropertyValAsParams(JSInstructionFactory insts, int nargs, JavaScriptSummary S, int[] paramsToPassToInvoked) {
    // read an arbitrary property name via EachElementGet
    int curValNum = nargs + 2;
    int eachElementGetResult = curValNum++;
    int nullPredVn = curValNum++;
    S.addConstant(nullPredVn, new ConstantValue(null));
    S.addStatement(insts.EachElementGetInstruction(S.getNumberOfStatements(), eachElementGetResult, 4, nullPredVn));
    S.getNextProgramCounter();
    // read value from the arbitrary property name
    int propertyReadResult = curValNum++;
    S.addStatement(insts.PropertyRead(S.getNumberOfStatements(), propertyReadResult, 4, eachElementGetResult));
    S.getNextProgramCounter();
    for (int i = 1; i < paramsToPassToInvoked.length; i++) {
      paramsToPassToInvoked[i] = propertyReadResult;
    }
    return curValNum;
  }
  
  private static int passActualPropertyValsAsParams(JSInstructionFactory insts, int nargs, JavaScriptSummary S, int[] paramsToPassToInvoked) {
    // read an arbitrary property name via EachElementGet
    int nullVn = nargs + 2;
    S.addConstant(nullVn, new ConstantValue(null));
    int curValNum = nargs + 3;
    for (int i = 1; i < paramsToPassToInvoked.length; i++) {
      // create a String constant for i-1
      final int constVN = curValNum++;
      // the commented line is correct, but it doesn't work because
      // of our broken handling of int constants as properties.
      // TODO fix property handling, and then fix this
      S.addConstant(constVN, new ConstantValue(Integer.toString(i-1)));
      //S.addConstant(constVN, new ConstantValue(i-1));
      int propertyReadResult = curValNum++;
      // 4 is position of arguments array
      S.addStatement(insts.PropertyWrite(S.getNumberOfStatements(), 4, constVN, nullVn));
      S.getNextProgramCounter();
      
      S.addStatement(insts.PropertyRead(S.getNumberOfStatements(), propertyReadResult, 4, constVN));
      S.getNextProgramCounter();
     
      paramsToPassToInvoked[i] = propertyReadResult;
    }
    return curValNum;
  }

  private static IR makeIRForNoArgList(CGNode node) {
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
    S.addStatement(insts.Invoke(S.getNumberOfStatements(), 2, resultVal, params, resultVal + 1, cs));
    S.getNextProgramCounter();

    S.addStatement(insts.ReturnInstruction(S.getNumberOfStatements(), resultVal, false));
    S.getNextProgramCounter();

    JavaScriptSummarizedFunction t = new JavaScriptSummarizedFunction(ref, S, declaringClass);
    return t.makeIR(node.getContext(), null);
  }

  @Override
  public DefUse getDU(CGNode node) {
    assert understands(node);
    return new DefUse(getIR(node));
  }

}
