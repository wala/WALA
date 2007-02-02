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
package com.ibm.wala.stringAnalysis.js.translator;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.js.loader.JSCallSiteReference;
import com.ibm.wala.cast.js.ssa.*;
import com.ibm.wala.cast.types.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.impl.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.stringAnalysis.grammar.*;
import com.ibm.wala.stringAnalysis.js.ssa.*;
import com.ibm.wala.stringAnalysis.ssa.*;
import com.ibm.wala.stringAnalysis.translator.*;
import com.ibm.wala.stringAnalysis.util.*;
import com.ibm.wala.types.*;

import java.util.*;

public class JSSSA2Rule extends SSA2Rule {
  public JSSSA2Rule(boolean approximateMembers) {
    super(approximateMembers);
  }
  
  public JSSSA2Rule() {
    super();
  }
  
  protected class JSTranslatingProcessor extends BaseTranslatingProcessor implements SAJSProcessingInstructionVisitor.Processor
  {
    public JSTranslatingProcessor(TranslationContext ctx, Collection rules) {
      super(ctx, rules);
    }

    public void onJavaScriptPropertyRead(JavaScriptPropertyRead instruction) {
      translateReflectiveGet(instruction, context, rules);
    }

    public void onJavaScriptPropertyWrite(JavaScriptPropertyWrite instruction) {
      translateReflectivePut(instruction, context, rules);
    }
  };

  static final List<String> domProperties = new ArrayList<String>();
  {
    domProperties.add("target");
    domProperties.add("parentNode");
    domProperties.add("previousSibling");
    domProperties.add("nextSibling");
    domProperties.add("firstChild");
    domProperties.add("lastChild");
    domProperties.add("childNodes");
    domProperties.add("attributes");
    domProperties.add("nodeName");
    domProperties.add("nodeValue");
  }
  
  protected void translateGet(SSAGetInstruction instruction, TranslationContext ctx, Collection rules) {
    String fieldName = instruction.getDeclaredField().getName().toString();
    System.err.println("field name: " + fieldName);
    if (domProperties.contains(fieldName)) {
      IVariable recv = (IVariable) getValueSymbol(instruction.getRef(), instruction, ctx);
      IVariable left = (IVariable) getValueSymbol(instruction.getDef(0), instruction, ctx);
      InvocationSymbol invoke = getInvocationSymbol(new StringSymbol(fieldName), recv, new int[]{instruction.getRef()}, instruction, ctx);
      IProductionRule rule = createRule(ctx.getIR(), instruction, left, invoke);
      rules.add(rule);
    }
    else {
      super.translateGet(instruction, ctx, rules);
    }
  }

  protected void translateInvokeInstruction(SSAAbstractInvokeInstruction instruction, TranslationContext ctx, Collection rules) {
    IVariable left = (IVariable) getValueSymbol(instruction.getDef(0), instruction, ctx);
    int nuses = instruction.getNumberOfUses();
    if (nuses <= 0) {
      if (instruction.getDeclaredTarget().getDeclaringClass().equals(FakeRootClass.FAKE_ROOT_CLASS)) {
	return;
      } else {
	Assertions._assert(nuses > 0, 
          "bad " + instruction.toString() + " for " + ctx.getIR() );
      }
    }
    ISymbol f = null;
    if (instruction instanceof JavaScriptInvoke) {
      JavaScriptInvoke jsInvoke = (JavaScriptInvoke) instruction;
      f = getValueSymbol(jsInvoke.getFunction(), instruction, ctx);
    }
    ISymbol recv = getValueSymbol(instruction.getReceiver(), instruction, ctx);
    int params[] = new int[instruction.getNumberOfParameters()-1];
    for( int i = 0; i < params.length; i++ ){
      params[i] = instruction.getUse(i+1);
    }
    ISymbol right = getInvocationSymbol(f, recv, params, instruction, ctx);
    rules.add(createRule(ctx.getIR(), instruction, left, right));
  }

  protected SAProcessingInstructionVisitor 
  createTranslatorVisitor(TranslationContext ctx, Collection rules) 
  {
    return 
    new SAJSProcessingInstructionVisitor(
      new JSTranslatingProcessor(ctx, rules));
  }
}




