/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.summaries;

import java.util.WeakHashMap;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeDynamicInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

public class LambdaMethodTargetSelector implements MethodTargetSelector {

  private final WeakHashMap<BootstrapMethod, SummarizedMethod> summaries = new WeakHashMap<>();

  private final MethodTargetSelector base;
  
  public LambdaMethodTargetSelector(MethodTargetSelector base) {
    this.base = base;
  }

  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    if (! site.getDeclaredTarget().getName().equals(MethodReference.clinitName) && 
        caller.getClassHierarchy().lookupClass(TypeReference.LambdaMetaFactory) != null &&
        caller.getClassHierarchy().lookupClass(TypeReference.LambdaMetaFactory).equals(
            caller.getClassHierarchy().lookupClass(site.getDeclaredTarget().getDeclaringClass()))) 
    {
      SSAInvokeDynamicInstruction invoke = (SSAInvokeDynamicInstruction)caller.getIR().getCalls(site)[0];
      
      if (!summaries.containsKey(invoke.getBootstrap())) {
        String cls = caller.getMethod().getDeclaringClass().getName().toString().replace("/", "$").substring(1);
        int bootstrapIndex = invoke.getBootstrap().getIndexInClassFile();
        MethodReference ref = 
            MethodReference.findOrCreate(
                site.getDeclaredTarget().getDeclaringClass(), 
                Atom.findOrCreateUnicodeAtom(site.getDeclaredTarget().getName().toString() +"$" + cls + "$" + bootstrapIndex),
                site.getDeclaredTarget().getDescriptor());
        
        MethodSummary summary = new MethodSummary(ref);
        
        if (site.isStatic()) {
          summary.setStatic(true);
        }
        
        int index = 0;
        int v = site.getDeclaredTarget().getNumberOfParameters() + 2;
        IClass lambda = LambdaSummaryClass.findOrCreate(caller, invoke);
        SSAInstructionFactory insts = Language.JAVA.instructionFactory();
        summary.addStatement(insts.NewInstruction(index, v, NewSiteReference.make(index, lambda.getReference())));
        index++;
        for(int i = 0; i < site.getDeclaredTarget().getNumberOfParameters(); i++) {
          summary.addStatement(
              insts.PutInstruction(index++, v, i+1, lambda.getField(Atom.findOrCreateUnicodeAtom("c" + i)).getReference()));
        }
        summary.addStatement(insts.ReturnInstruction(index++, v, false));
        
        summaries.put(invoke.getBootstrap(), new SummarizedMethod(ref, summary, caller.getClassHierarchy().lookupClass(site.getDeclaredTarget().getDeclaringClass())));
      }
      
      return summaries.get(invoke.getBootstrap());
      
    } else {
      return base.getCalleeTarget(caller, site, receiver);
    }
  }

}
