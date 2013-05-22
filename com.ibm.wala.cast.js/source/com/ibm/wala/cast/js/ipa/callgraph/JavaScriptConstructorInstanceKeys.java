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

import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.NormalAllocationInNode;
import com.ibm.wala.types.TypeReference;

public class JavaScriptConstructorInstanceKeys implements InstanceKeyFactory {
  private final InstanceKeyFactory base;
  
  public JavaScriptConstructorInstanceKeys(InstanceKeyFactory base) {
    super();
    this.base = base;
  }

  public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
    if (node.getMethod() instanceof JavaScriptConstructTargetSelector.JavaScriptConstructor) {
      InstanceKey bk = base.getInstanceKeyForAllocation(node, allocation);
      return new NormalAllocationInNode(node, allocation, bk.getConcreteType());
    } else {
      return base.getInstanceKeyForAllocation(node, allocation);
    }
  }

  public InstanceKey getInstanceKeyForClassObject(TypeReference type) {
    return base.getInstanceKeyForClassObject(type);
  }

  public <T> InstanceKey getInstanceKeyForConstant(TypeReference type, T S) {
    return base.getInstanceKeyForConstant(type, S);
  }

  public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
    return base.getInstanceKeyForMultiNewArray(node, allocation, dim);
  }

  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter instr, TypeReference type) {
    return base.getInstanceKeyForPEI(node, instr, type);
  }

}
