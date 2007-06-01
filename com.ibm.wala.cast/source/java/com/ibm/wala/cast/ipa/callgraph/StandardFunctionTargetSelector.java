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

import com.ibm.wala.cast.types.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.*;

public class StandardFunctionTargetSelector implements MethodTargetSelector {  
  private final IClassHierarchy cha;
  private final MethodTargetSelector base;

  public StandardFunctionTargetSelector(IClassHierarchy cha, MethodTargetSelector base) {
    this.cha = cha;
    this.base = base;
  }

  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    if (receiver != null) {
      ClassLoaderReference loader = receiver.getClassLoader().getReference();
      TypeReference functionTypeRef =
	TypeReference.findOrCreate(loader, AstTypeReference.functionTypeName);

      if (cha.isSubclassOf(receiver, cha.lookupClass(functionTypeRef))) {
	return receiver.getMethod(AstMethodReference.fnSelector);
      }
    }

    return base.getCalleeTarget(caller, site, receiver);
  }

  public boolean mightReturnSyntheticMethod(CGNode caller, CallSiteReference site) {
    return true;
  }

  public boolean mightReturnSyntheticMethod(MethodReference declaredTarget) {
    return true;
  }
}
