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

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ContainerContextSelector;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.warnings.ResolutionFailure;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * This class provides Instance Key call backs where each instance is in the
 * same equivalence class as all other instances allocated at the same site. 
 *
 * @author sfink
 */
public class AllocationSiteInstanceKeys implements InstanceKeyFactory {

  /**
   * Governing call graph contruction options
   */
  private final AnalysisOptions options;

  /**
   * An object to track analysis warnings
   */
  private final WarningSet warnings;

  /**
   * Governing class hierarchy
   */
  private final ClassHierarchy cha;

  private final ClassBasedInstanceKeys classBased;

  /**
   * @param options
   *          Governing call graph contruction options
   * @param warnings
   *          An object to track analysis warnings
   */
  public AllocationSiteInstanceKeys(AnalysisOptions options, ClassHierarchy cha, WarningSet warnings) {
    this.options = options;
    this.cha = cha;
    this.warnings = warnings;
    this.classBased = new ClassBasedInstanceKeys(options, cha, warnings);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.detox.ipa.underConstruction.DataflowCallGraphBuilder#getInstanceKeyForAllocation(com.ibm.detox.ipa.callgraph.CGNode,
   *      com.ibm.wala.ssa.NewInstruction)
   */
  public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
    IClass type = options.getClassTargetSelector().getAllocatedTarget(node, allocation);
    if (type == null) {
      warnings.add(ResolutionFailure.create(node,allocation));
      return null;
    }

    // disallow recursion in contexts.
    if (node.getContext() instanceof ReceiverInstanceContext) {
      IMethod m = node.getMethod();
      CGNode n = ContainerContextSelector.findNodeRecursiveMatchingContext(m, node.getContext());
      if (n != null) {
        return new NormalAllocationSiteKey(n, allocation, type);
      }
    }

    InstanceKey key = new NormalAllocationSiteKey(node, allocation, type);

    return key;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.detox.ipa.underConstruction.DataflowCallGraphBuilder#getInstanceKeyForMultiNewArray(com.ibm.detox.ipa.callgraph.CGNode,
   *      com.ibm.wala.ssa.NewInstruction, int)
   */
  public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
    IClass type = options.getClassTargetSelector().getAllocatedTarget(node, allocation);
    if (type == null) {
      warnings.add(ResolutionFailure.create(node,allocation));
      return null;
    }
    InstanceKey key = new MultiNewArrayAllocationSiteKey(node, allocation, type, dim);

    return key;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.detox.ipa.underConstruction.DataflowCallGraphBuilder#getInstanceKeyForStringConstant(java.lang.String)
   */
  public InstanceKey getInstanceKeyForConstant(Object S) {
    if (options.getUseConstantSpecificKeys())
      return new ConstantKey(S, cha.lookupClass(options.getConstantType(S)));
    else
      return new ConcreteTypeKey(cha.lookupClass(options.getConstantType(S)));
  }

  public String getStringConstantForInstanceKey(InstanceKey I) {
    if (I instanceof StringConstantKey) {
      return ((StringConstantKey) I).getString();
    } else {
      return null;
    }
  }

  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter pei, TypeReference type) {
    return classBased.getInstanceKeyForPEI(node, pei, type);
  }

  public InstanceKey getInstanceKeyForClassObject(TypeReference type) {
    return classBased.getInstanceKeyForClassObject(type);
  }

}
