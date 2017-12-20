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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNodeFactory;
import com.ibm.wala.ipa.callgraph.propagation.ClassBasedInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.SmushedAllocationSiteInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;

/**
 * Flexible class to create {@link InstanceKey}s depending on various policies ranging from class-based (i.e. 0-CFA) to
 * allocation-site-based (0-1-CFA variants).
 */
public class ZeroXInstanceKeys implements InstanceKeyFactory {

  private final static TypeName JavaLangStringBufferName = TypeName.string2TypeName("Ljava/lang/StringBuffer");

  public final static TypeReference JavaLangStringBuffer = TypeReference.findOrCreate(ClassLoaderReference.Primordial,
      JavaLangStringBufferName);

  private final static TypeName JavaLangStringBuilderName = TypeName.string2TypeName("Ljava/lang/StringBuilder");

  public final static TypeReference JavaLangStringBuilder = TypeReference.findOrCreate(ClassLoaderReference.Primordial,
      JavaLangStringBuilderName);

  private final static TypeName JavaLangAbstractStringBuilderName = TypeName.string2TypeName("Ljava/lang/AbstractStringBuilder");

  public final static TypeReference JavaLangAbstractStringBuilder = TypeReference.findOrCreate(ClassLoaderReference.Primordial,
      JavaLangAbstractStringBuilderName);

  /**
   * The NONE policy is not allocation-site based
   */
  public static final int NONE = 0;

  /**
   * An ALLOCATIONS - based policy distinguishes instances by allocation site. Otherwise, the policy distinguishes instances by
   * type.
   */
  public static final int ALLOCATIONS = 1;

  /**
   * A policy variant where String and StringBuffers are NOT disambiguated according to allocation site.
   */
  public static final int SMUSH_STRINGS = 2;

  /**
   * A policy variant where {@link Throwable} instances are NOT disambiguated according to allocation site.
   * 
   */
  public static final int SMUSH_THROWABLES = 4;

  /**
   * A policy variant where if a type T has only primitive instance fields, then instances of type T are NOT disambiguated by
   * allocation site.
   */
  public static final int SMUSH_PRIMITIVE_HOLDERS = 8;

  /**
   * This variant counts the N, number of allocation sites of a particular type T in each method. If N &gt; SMUSH_LIMIT, then these N
   * allocation sites are NOT distinguished ... instead there is a single abstract allocation site for &lt;N,T&gt;
   * 
   * Probably the best choice in many cases.
   */
  public static final int SMUSH_MANY = 16;

  /**
   * Should we use constant-specific keys?
   */
  public static final int CONSTANT_SPECIFIC = 32;

  /**
   * When using smushing, how many sites in a node will be kept distinct before smushing?
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
  private final AllocationSiteInNodeFactory siteBased;

  /**
   * A delegate object to create "abstract allocation site" - based abstract instances
   */
  private final SmushedAllocationSiteInstanceKeys smushed;

  /**
   * The governing class hierarchy
   */
  private final IClassHierarchy cha;

  /**
   * An object which interprets nodes in context.
   */
  final private RTAContextInterpreter contextInterpreter;

  /**
   * a Map from CGNode-&gt;Set&lt;IClass&gt; that should be smushed.
   */
  protected final Map<CGNode, Set<IClass>> smushMap = HashMapFactory.make();

  public ZeroXInstanceKeys(AnalysisOptions options, IClassHierarchy cha, RTAContextInterpreter contextInterpreter, int policy) {
    if (options == null) {
      throw new IllegalArgumentException("null options");
    }
    this.policy = policy;
    if (disambiguateConstants()) {
      // this is an ugly hack. TODO: clean it all up.
      options.setUseConstantSpecificKeys(true);
    }
    classBased = new ClassBasedInstanceKeys(options, cha);
    siteBased = new AllocationSiteInNodeFactory(options, cha);
    smushed = new SmushedAllocationSiteInstanceKeys(options, cha);
    this.cha = cha;
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

  public boolean disambiguateConstants() {
    return (policy & CONSTANT_SPECIFIC) > 0;
  }

  @Override
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
   * @return true iff the node contains too many allocation sites of type c
   */
  private boolean exceedsSmushLimit(IClass c, CGNode node) {
    Set<IClass> s = smushMap.get(node);
    if (s == null) {
      Map<IClass, Integer> count = countAllocsByType(node);
      HashSet<IClass> smushees = HashSetFactory.make(5);
      for (Map.Entry<IClass, Integer> e : count.entrySet()) {
        Integer i = e.getValue();
        if (i.intValue() > SMUSH_LIMIT) {
          smushees.add(e.getKey());
        }
      }
      s = smushees.isEmpty() ? Collections.<IClass> emptySet() : smushees;
      smushMap.put(node, s);
    }
    return s.contains(c);
  }

  /**
   * @return Map: IClass -&gt; Integer, the number of allocation sites for each type.
   */
  private Map<IClass, Integer> countAllocsByType(CGNode node) {
    Map<IClass, Integer> count = HashMapFactory.make();
    for (NewSiteReference n : Iterator2Iterable.make(contextInterpreter.iterateNewSites(node))) {
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

  @Override
  public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
    if (allocationPolicy()) {
      return siteBased.getInstanceKeyForMultiNewArray(node, allocation, dim);
    } else {
      return classBased.getInstanceKeyForMultiNewArray(node, allocation, dim);
    }
  }

  @Override
  public <T> InstanceKey getInstanceKeyForConstant(TypeReference type, T S) {
    if (type == null) {
      throw new IllegalArgumentException("null type");
    }
    if (disambiguateConstants() || isReflectiveType(type)) {
      return new ConstantKey<>(S, getClassHierarchy().lookupClass(type));
    } else {
      return classBased.getInstanceKeyForConstant(type, S);
    }
  }

  private static boolean isReflectiveType(TypeReference type) {
    return type.equals(TypeReference.JavaLangReflectConstructor) || type.equals(TypeReference.JavaLangReflectMethod);
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory#getInstanceKeyForPEI(com.ibm.wala.ipa.callgraph.CGNode,
   * com.ibm.wala.classLoader.ProgramCounter, com.ibm.wala.types.TypeReference)
   */
  @Override
  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter pei, TypeReference type) {
    return classBased.getInstanceKeyForPEI(node, pei, type);
  }

  @Override
  public InstanceKey getInstanceKeyForMetadataObject(Object obj, TypeReference objType) {
    return classBased.getInstanceKeyForMetadataObject(obj, objType);
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
    return C.getReference().equals(TypeReference.JavaLangString) || C.getReference().equals(JavaLangStringBuffer)
        || C.getReference().equals(JavaLangStringBuilder) || C.getReference().equals(JavaLangAbstractStringBuilder);
  }

  public static boolean isThrowable(IClass c) {
    if (c == null) {
      throw new IllegalArgumentException("null c");
    }
    return c.getClassHierarchy().isSubclassOf(c, c.getClassHierarchy().lookupClass(TypeReference.JavaLangThrowable));
  }


  public static boolean isStackTraceElement(IClass c) {
    if (c == null) {
      throw new IllegalArgumentException("C is null");
    }
    return c.getReference().equals(TypeReference.JavaLangStackTraceElement);
  }


  private boolean allFieldsArePrimitive(IClass c) {
    if (c.isArrayClass()) {
      TypeReference t = c.getReference().getArrayElementType();
      return t.isPrimitiveType();
    } else {
      if (c.getReference().equals(TypeReference.JavaLangObject)) {
        return true;
      } else {
        for (IField f : c.getDeclaredInstanceFields()) {
          if (f.getReference().getFieldType().isReferenceType()) {
            return false;
          }
        }
        return allFieldsArePrimitive(c.getSuperclass());
      }
    }
  }

  protected IClassHierarchy getClassHierarchy() {
    return cha;
  }

  public ClassBasedInstanceKeys getClassBasedInstanceKeys() {
    return classBased;
  }
}
