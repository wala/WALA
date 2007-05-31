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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.ClassBasedInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.SmushedAllocationSiteInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * @author sfink
 * 
 */
public class ZeroXInstanceKeys implements InstanceKeyFactory {

  private final static TypeName JavaLangStringBufferName = TypeName.string2TypeName("Ljava/lang/StringBuffer");

  public final static TypeReference JavaLangStringBuffer = TypeReference.findOrCreate(ClassLoaderReference.Primordial,
      JavaLangStringBufferName);

  /**
   * The NONE policy is not allocation-site based
   */
  public static final int NONE = 0;

  /**
   * An ALLOCATIONS - based policy distinguishes instances by allocation site.
   * Otherwise, the policy distinguishes instances by type.
   */
  public static final int ALLOCATIONS = 1;

  /**
   * A policy variant where String and StringBuffers are NOT disambiguated
   * according to allocation site.
   */
  public static final int SMUSH_STRINGS = 2;

  /**
   * A policy variant where Throwable instances are NOT disambiguated according
   * to allocation site.
   * 
   */
  public static final int SMUSH_THROWABLES = 4;

  /**
   * A policy variant where if a type T has only primitive instance fields, then
   * instances of type T are NOT disambiguated by allocation site.
   */
  public static final int SMUSH_PRIMITIVE_HOLDERS = 8;

  /**
   * This variant counts the N, number of allocation sites of a particular type
   * T in each method. If N > SMUSH_LIMIT, then these N allocation sites are NOT
   * distinguished ... instead there is a single abstract allocation site for
   * <N,T>
   * 
   * Probably the best choice in many cases.
   */
  public static final int SMUSH_MANY = 16;

  /**
   * When using smushing, how many sites in a node will be kept distinct before
   * smushing?
   */
  private final int SMUSH_LIMIT = 25;

  /**
   * The policy choice for instance disambiguation
   */
  private final int policy;

  /**
   * A delegate object to create class-based abstract instances
   */
  private final ClassBasedInstanceKeys classBased;

  /**
   * A delegate object to create allocation site-based abstract instances
   */
  private final AllocationSiteInstanceKeys siteBased;

  /**
   * A delegate object to create "abstract allocation site" - based abstract
   * instances
   */
  private final SmushedAllocationSiteInstanceKeys smushed;

  /**
   * The governing class hierarchy
   */
  private final ClassHierarchy cha;

  /**
   * An object which interprets nodes in context.
   */
  private RTAContextInterpreter contextInterpreter;

  /**
   * a Map from CGNode->Set<IClass> that should be smushed.
   */
  Map<CGNode, Set> smushMap = new HashMap<CGNode, Set>();

  public ZeroXInstanceKeys(AnalysisOptions options, ClassHierarchy cha, RTAContextInterpreter contextInterpreter,
      WarningSet warnings, int policy) {
    classBased = new ClassBasedInstanceKeys(options, cha, warnings);
    siteBased = new AllocationSiteInstanceKeys(options, cha, warnings);
    smushed = new SmushedAllocationSiteInstanceKeys(options, cha, warnings);
    this.cha = cha;
    this.policy = policy;
    this.contextInterpreter = contextInterpreter;
  }

  /**
   * @return true iff the policy smushes some allocation sites
   */
  private boolean smushMany() {
    return (policy & SMUSH_MANY) > 0;
  }

  private boolean allocationPolicy() {
    return (policy & ALLOCATIONS) > 0;
  }

  private boolean smushStrings() {
    return (policy & SMUSH_STRINGS) > 0;
  }

  public boolean smushThrowables() {
    return (policy & SMUSH_THROWABLES) > 0;
  }

  private boolean smushPrimHolders() {
    return (policy & SMUSH_PRIMITIVE_HOLDERS) > 0;
  }

  public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
    if (allocation == null) {
      throw new IllegalArgumentException("allocation is null");
    }
    TypeReference t = allocation.getDeclaredType();
    IClass C = cha.lookupClass(t);

    if (C != null && isInteresting(C)) {
      if (smushMany()) {
        if (exceedsSmushLimit(C, node)) {
          return smushed.getInstanceKeyForAllocation(node, allocation);
        } else {
          return siteBased.getInstanceKeyForAllocation(node, allocation);
        }
      } else {
        return siteBased.getInstanceKeyForAllocation(node, allocation);
      }
    } else {
      return classBased.getInstanceKeyForAllocation(node, allocation);
    }
  }

  /**
   * side effect: populates the smush map.
   * 
   * @param c
   * @param node
   * @return true iff the node contains too many allocation sites of type c
   */
  private boolean exceedsSmushLimit(IClass c, CGNode node) {
    Set s = smushMap.get(node);
    if (s == null) {
      Map<IClass, Integer> count = countAllocsByType(node);
      HashSet<IClass> smushees = new HashSet<IClass>(5);
      for (Iterator<Map.Entry<IClass, Integer>> it = count.entrySet().iterator(); it.hasNext();) {
        Map.Entry<IClass, Integer> e = it.next();
        Integer i = e.getValue();
        if (i.intValue() > SMUSH_LIMIT) {
          smushees.add(e.getKey());
        }
      }
      s = smushees.isEmpty() ? Collections.EMPTY_SET : smushees;
      smushMap.put(node, s);
    }
    return s.contains(c);
  }

  /**
   * @param node
   * @return Map: IClass -> Integer, the number of allocation sites for each
   *         type.
   */
  private Map<IClass, Integer> countAllocsByType(CGNode node) {
    Map<IClass, Integer> count = new HashMap<IClass, Integer>();
    for (Iterator it = contextInterpreter.iterateNewSites(node); it.hasNext();) {
      NewSiteReference n = (NewSiteReference) it.next();
      IClass alloc = cha.lookupClass(n.getDeclaredType());
      if (alloc != null) {
        Integer old = count.get(alloc);
        if (old == null) {
          count.put(alloc, new Integer(1));
        } else {
          count.put(alloc, new Integer(old.intValue() + 1));
        }
      }
    }
    return count;
  }

  public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
    if (allocationPolicy()) {
      return siteBased.getInstanceKeyForMultiNewArray(node, allocation, dim);
    } else {
      return classBased.getInstanceKeyForMultiNewArray(node, allocation, dim);
    }
  }

  public InstanceKey getInstanceKeyForConstant(Object S) {
    return classBased.getInstanceKeyForConstant(S);
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory#getStringConstantForInstanceKey(com.ibm.wala.ipa.callgraph.propagation.InstanceKey)
   */
  public String getStringConstantForInstanceKey(InstanceKey I) {
    return classBased.getStringConstantForInstanceKey(I);
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory#getInstanceKeyForPEI(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.ProgramCounter,
   *      com.ibm.wala.types.TypeReference)
   */
  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter pei, TypeReference type) {
    return classBased.getInstanceKeyForPEI(node, pei, type);
  }

  public InstanceKey getInstanceKeyForClassObject(TypeReference type) {
    return classBased.getInstanceKeyForClassObject(type);
  }

  /**
   * A class is "interesting" iff we distinguish instances of the class
   */
  public boolean isInteresting(IClass C) {
    if (!allocationPolicy()) {
      return false;
    } else {
      if (smushStrings() && isStringish(C)) {
        return false;
      } else if (smushThrowables() && (isThrowable(C) || isStackTraceElement(C))) {
        return false;
      } else if (smushPrimHolders() && allFieldsArePrimitive(C)) {
        return false;
      }
      return true;
    }
  }

  public static boolean isStringish(IClass C) {
    if (C == null) {
      throw new IllegalArgumentException("C is null");
    }
    return C.getReference().equals(TypeReference.JavaLangString) || C.getReference().equals(JavaLangStringBuffer);
  }

  public boolean isThrowable(IClass C) {
    return cha.isSubclassOf(C, cha.lookupClass(TypeReference.JavaLangThrowable));
  }

  public boolean isStackTraceElement(IClass C) {
    if (C == null) {
      throw new IllegalArgumentException("C is null");
    }
    return C.getReference().equals(TypeReference.JavaLangStackTraceElement);
  }

  private boolean allFieldsArePrimitive(IClass C) {
    if (C.isArrayClass()) {
      TypeReference t = C.getReference().getArrayElementType();
      return t.isPrimitiveType();
    } else {
      if (C.getReference().equals(TypeReference.JavaLangObject)) {
        return true;
      } else {
        for (Iterator<IField> it = C.getDeclaredInstanceFields().iterator(); it.hasNext();) {
          IField f = it.next();
          if (f.getReference().getFieldType().isReferenceType()) {
            return false;
          }
        }
        try {
          return allFieldsArePrimitive(C.getSuperclass());
        } catch (ClassHierarchyException e) {
          Assertions.UNREACHABLE();
          return false;
        }
      }
    }
  }

  /**
   * @return Returns the cha.
   */
  protected ClassHierarchy getClassHierarchy() {
    return cha;
  }
}
