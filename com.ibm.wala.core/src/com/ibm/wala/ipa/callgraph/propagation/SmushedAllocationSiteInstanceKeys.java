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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ContainerContextSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;

/**
 * This class provides instance keys where for a given type T in a CGNode N, there is one "abstract allocation site" instance for
 * all T allocations in node N.
 */
public class SmushedAllocationSiteInstanceKeys implements InstanceKeyFactory {

  /**
   * Governing call graph construction options
   */
  private final AnalysisOptions options;

  /**
   * Governing class hierarchy
   */
  private final IClassHierarchy cha;

  private final ClassBasedInstanceKeys classBased;

  /**
   * @param options Governing call graph construction options
   */
  public SmushedAllocationSiteInstanceKeys(AnalysisOptions options, IClassHierarchy cha) {
    this.options = options;
    this.cha = cha;
    this.classBased = new ClassBasedInstanceKeys(options, cha);
  }

  @Override
  public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
    IClass type = options.getClassTargetSelector().getAllocatedTarget(node, allocation);
    if (type == null) {
      return null;
    }

    // disallow recursion in contexts.
    if (node.getContext() instanceof ReceiverInstanceContext) {
      IMethod m = node.getMethod();
      CGNode n = ContainerContextSelector.findNodeRecursiveMatchingContext(m, node.getContext());
      if (n != null) {
        return new SmushedAllocationSiteInNode(n, type);
      }
    }

    InstanceKey key = new SmushedAllocationSiteInNode(node, type);

    return key;
  }

  @Override
  public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
    ArrayClass type = (ArrayClass) options.getClassTargetSelector().getAllocatedTarget(node, allocation);
    if (type == null) {
      return null;
    }
    InstanceKey key = new MultiNewArrayInNode(node, allocation, type, dim);

    return key;
  }

  @Override
  public <T> InstanceKey getInstanceKeyForConstant(TypeReference type, T S) {
    if (options.getUseConstantSpecificKeys())
      return new ConstantKey<>(S, cha.lookupClass(type));
    else
      return new ConcreteTypeKey(cha.lookupClass(type));
  }

  @Override
  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter pei, TypeReference type) {
    return classBased.getInstanceKeyForPEI(node, pei, type);
  }

  @Override
  public InstanceKey getInstanceKeyForMetadataObject(Object obj, TypeReference objType) {
    return classBased.getInstanceKeyForMetadataObject(obj, objType);
  }

}
