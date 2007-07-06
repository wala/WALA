/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

abstract public class ReflectiveSubtypesEntrypoint extends SubtypesEntrypoint {

  public ReflectiveSubtypesEntrypoint(MethodReference method, IClassHierarchy cha) {
    super(method, cha);
  }

  public ReflectiveSubtypesEntrypoint(IMethod method, IClassHierarchy cha) {
    super(method, cha);
  }

  abstract protected boolean useReflectiveMachinery(TypeReference type);

  @Override
  protected int makeArgument(AbstractRootMethod m, int i) {
    if (useReflectiveMachinery(method.getParameterType(i))) {
      int fakeString = m.addAllocation(TypeReference.JavaLangString).getDef(0);
      CallSiteReference fn = CallSiteReference.make(0, MethodReference.JavaLangClassForName, IInvokeInstruction.Dispatch.STATIC);
      int lv1 = m.addInvocation(new int[] { fakeString }, fn).getDef(0);
      CallSiteReference ni = CallSiteReference.make(0, MethodReference.JavaLangClassNewInstance, IInvokeInstruction.Dispatch.VIRTUAL);
      return m.addInvocation(new int[] { lv1 }, ni).getDef(0);
    } else {
      return super.makeArgument(m, i);
    }
  }
}
