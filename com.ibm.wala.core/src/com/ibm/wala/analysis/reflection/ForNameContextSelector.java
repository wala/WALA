/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.StringStuff;

/**
 * A {@link ContextSelector} to intercept calls to Class.forName() when the parameter is a string constant
 * 
 * @author pistoia
 */
class ForNameContextSelector implements ContextSelector {

  private final IMethod forNameMethod;

  public ForNameContextSelector(final IClassHierarchy cha) {
    this.forNameMethod = cha.resolveMethod(ForNameContextInterpreter.FOR_NAME_REF);
    assert forNameMethod != null;
  }

  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    return false;
  }

  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    return false;
  }

  /**
   * If the {@link CallSiteReference} invokes Class.forName(s) and s is a string constant, return a
   * {@link JavaTypeContext} representing the type named by s, if we can resolve it in the {@link IClassHierarchy}.
   * 
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#getCalleeTarget(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.callgraph.propagation.InstanceKey)
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    if (callee.equals(forNameMethod)) {
      IR ir = caller.getIR();
      SymbolTable symbolTable = ir.getSymbolTable();
      SSAAbstractInvokeInstruction[] invokeInstructions = caller.getIR().getCalls(site);
      if (invokeInstructions.length != 1) {
        return null;
      }
      int use = invokeInstructions[0].getUse(0);
      if (symbolTable.isStringConstant(use)) {
        String className = StringStuff.deployment2CanonicalTypeString(symbolTable.getStringValue(use));
        TypeReference t = TypeReference.findOrCreate(caller.getMethod().getDeclaringClass().getClassLoader().getReference(),
            className);
        IClass klass = caller.getClassHierarchy().lookupClass(t);
        if (klass != null) {
          return new JavaTypeContext(new PointType(klass));
        }
      }
    }
    return null;
  }

  /**
   * This object may understand a dispatch to Class.forName(s) when s is a string constant.
   */
  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    if (targetMethod.equals(forNameMethod)) {
      IR ir = caller.getIR();
      SymbolTable symbolTable = ir.getSymbolTable();
      SSAAbstractInvokeInstruction[] invokeInstructions = caller.getIR().getCalls(site);
      if (invokeInstructions.length != 1) {
        return false;
      }
      int use = invokeInstructions[0].getUse(0);
      if (symbolTable.isStringConstant(use)) {
        return true;
      }
    }
    return false;
  }
}