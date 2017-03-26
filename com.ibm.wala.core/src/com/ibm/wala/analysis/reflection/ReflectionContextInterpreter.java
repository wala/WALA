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

import java.util.Iterator;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.FieldReference;

/**
 * {@link SSAContextInterpreter} to handle all reflection procession.
 */
public class ReflectionContextInterpreter {

  public static SSAContextInterpreter createReflectionContextInterpreter(IClassHierarchy cha, AnalysisOptions options,
      IAnalysisCacheView iAnalysisCacheView) {
    
    if (options == null) {
      throw new IllegalArgumentException("null options");
    }
    
    // start with a dummy interpreter that understands nothing
    SSAContextInterpreter result = new SSAContextInterpreter() {
      @Override
      public boolean understands(CGNode node) {
        return false;
      }

      @Override
      public boolean recordFactoryType(CGNode node, IClass klass) {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public int getNumberOfStatements(CGNode node) {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public IR getIR(CGNode node) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public IRView getIRView(CGNode node) {
        return getIR(node);
      }

      @Override
      public DefUse getDU(CGNode node) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode n) {
        // TODO Auto-generated method stub
        return null;
      }
    };

    if (options.getReflectionOptions().getNumFlowToCastIterations() > 0) {
      // need the factory bypass interpreter
      result = new DelegatingSSAContextInterpreter(new FactoryBypassInterpreter(options, iAnalysisCacheView), result);
    }
    if (!options.getReflectionOptions().isIgnoreStringConstants()) {
      result = new DelegatingSSAContextInterpreter(new GetClassContextInterpeter(), new DelegatingSSAContextInterpreter(
          new DelegatingSSAContextInterpreter(new ClassFactoryContextInterpreter(), new ClassNewInstanceContextInterpreter(cha)),
          result));
    }
    if (!options.getReflectionOptions().isIgnoreMethodInvoke()) {
      result = new DelegatingSSAContextInterpreter(new ReflectiveInvocationInterpreter(), new DelegatingSSAContextInterpreter(
          new JavaLangClassContextInterpreter(), result));
    }
    // if NEITHER string constants NOR method invocations are ignored
    if (!options.getReflectionOptions().isIgnoreStringConstants() && !options.getReflectionOptions().isIgnoreMethodInvoke()) {
      result = new DelegatingSSAContextInterpreter(new GetMethodContextInterpreter(),result);
    }
    return result;
  }

}
