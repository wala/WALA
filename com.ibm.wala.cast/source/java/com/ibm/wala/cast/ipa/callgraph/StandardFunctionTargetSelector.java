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
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.types.AstTypeReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;

public class StandardFunctionTargetSelector implements MethodTargetSelector {  
  private final IClassHierarchy cha;
  private final MethodTargetSelector base;

  public StandardFunctionTargetSelector(IClassHierarchy cha, MethodTargetSelector base) {
    assert cha != null;
    this.cha = cha;
    this.base = base;
  }

  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
   ClassLoaderReference loader =
      (site.isStatic() || receiver==null)? 
	site.getDeclaredTarget().getDeclaringClass().getClassLoader(): 
	receiver.getClassLoader().getReference();

    TypeReference functionTypeRef =
      TypeReference.findOrCreate(loader, AstTypeReference.functionTypeName);

    IClass declarer = 
      site.isStatic()?
	cha.lookupClass(site.getDeclaredTarget().getDeclaringClass()):
	receiver;

    if (declarer == null) {
      System.err.println(("cannot find declarer for " +
      site + ", " + receiver + " in " + caller));
    }

    IClass fun = cha.lookupClass(functionTypeRef);

    if (fun == null) {
      System.err.println(("cannot find function " + functionTypeRef + " for " +
      site + ", " + receiver + " in " + caller));
    }

    if (fun != null && declarer != null && cha.isSubclassOf(declarer, fun)) {
      return declarer.getMethod(AstMethodReference.fnSelector);

    } else {
      return base.getCalleeTarget(caller, site, receiver);
    }
  }

  public boolean mightReturnSyntheticMethod() {
    return true;
  }
}
