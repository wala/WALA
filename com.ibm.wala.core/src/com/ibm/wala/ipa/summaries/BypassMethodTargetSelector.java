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
package com.ibm.wala.ipa.summaries;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyMethodTargetSelector;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Trace;

/**
 * 
 * "Non-standard" bypass rules to use during call graph construction.
 * 
 * Normally, the method bypass rules replace the IMethod that is resolved by
 * other means, via the getBypass() method. However, the bypass rules can be
 * invoked even before resolving the target of a call, by checking the intercept
 * rules.
 * 
 * @author sfink
 */
public class BypassMethodTargetSelector implements MethodTargetSelector {

  static final boolean DEBUG = false;

  /**
   * Method summaries collected for methods. Mapping Object -> MethodSummary
   * where Object is either a
   * <ul>
   * <li>MethodReference
   * <li>TypeReference
   * <li>Atom (package name)
   * </ul>
   */
  private final Map<MethodReference, MethodSummary> methodSummaries;

  /**
   * Set of Atoms representing package names whose methods should be treated as
   * no-ops
   */
  private final Set<Atom> ignoredPackages;

  /**
   * Governing class hierarchy.
   */
  private final ClassHierarchy cha;

  /**
   * target selector to use for non-bypassed calls
   */
  private final MethodTargetSelector parent;

  /**
   * Mapping from MethodReference -> SyntheticMethod We may call
   * syntheticMethod.put(m,null) .. in which case we use containsKey() to check
   * for having already considered m.
   */
  private HashMap<MethodReference, SummarizedMethod> syntheticMethods = HashMapFactory.make();

  /**
   * @param parent
   * @param methodSummaries
   * @param ignoredPackages
   * @param cha
   */
  public BypassMethodTargetSelector(MethodTargetSelector parent, Map<MethodReference, MethodSummary> methodSummaries, Set<Atom> ignoredPackages, ClassHierarchy cha) {
    this.methodSummaries = methodSummaries;
    this.ignoredPackages = ignoredPackages;
    this.parent = parent;
    this.cha = cha;
  }

  /**
   * Check to see if a particular call site should be bypassed, before checking
   * normal resolution of the receiver.
   * @throws IllegalArgumentException  if site is null
   */
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass dispatchType) {

    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    MethodReference ref = site.getDeclaredTarget();
    IMethod resolved = null;

    if (ClassHierarchyMethodTargetSelector.feasibleChaResolution(cha, site, dispatchType)) {
      if (site.isVirtual() || site.isInterface()) {
        if (dispatchType != null) {
          resolved = cha.resolveMethod(dispatchType, ref.getSelector());
        }
      } else /* (site.isStatic() || site.isSpecial()) */{
        IClass computedDispatchType = cha.lookupClass(site.getDeclaredTarget().getDeclaringClass());
        if (computedDispatchType != null) {
          resolved = cha.resolveMethod(computedDispatchType, ref.getSelector());
        }
      }
    }

    IMethod target = (resolved == null) ? findOrCreateSyntheticMethod(ref, site.isStatic()) : getBypassInternal(resolved, site
        .isStatic());

    if (DEBUG) Trace.println("target is initially " + target);

    if (target != null) {
      return target;
    } else {
      if (canIgnore(site.getDeclaredTarget())) {
	if (DEBUG) Trace.println("ignoring " + site);
        return null;
      }
      target = parent.getCalleeTarget(caller, site, dispatchType);

      if (DEBUG) Trace.println("target becomes " + target);

      if (target != null) {
        IMethod bypassTarget = getBypassInternal(target, site.isStatic());

	if (DEBUG) Trace.println("bypassTarget is " + target);

        return (bypassTarget == null) ? target : bypassTarget;
      } else
        return target;
    }
  }



  /**
   * Lookup bypass rules based on a resolved method
   * 
   * Method getBypass.
   * 
   * @param m
   * @return Object
   */
  private SyntheticMethod getBypassInternal(IMethod m, boolean isStatic) {
    if (DEBUG) {
      Trace.println("MethodBypass.getBypass? " + m);
    }
    return findOrCreateSyntheticMethod(m, isStatic);
  }

  /**
   * @param m
   *          a method reference
   * @return a SyntheticMethod corresponding to m; or null if none is available.
   */
  private SyntheticMethod findOrCreateSyntheticMethod(MethodReference m, boolean isStatic) {
    if (syntheticMethods.containsKey(m)) {
      return syntheticMethods.get(m);
    } else {
      MethodSummary summ = null;
      if (canIgnore(m)) {
        TypeReference T = m.getDeclaringClass();
        IClass C = cha.lookupClass(T);
        if (C == null) {
          // did not load class; don't try to create a synthetic method
          syntheticMethods.put(m, null);
          return null;
        }
        summ = generateNoOp(m, isStatic);
      } else {
        summ = findSummary(m);
      }
      if (summ != null) {
        TypeReference T = m.getDeclaringClass();
        IClass C = cha.lookupClass(T);
        if (C == null) {
          syntheticMethods.put(m, null);
          return null;
        }
        SummarizedMethod n = new SummarizedMethod(m, summ, C);
        syntheticMethods.put(m, n);
        return n;
      } else {
        syntheticMethods.put(m, null);
        return null;
      }
    }
  }

  /**
   * @param m
   *          a method reference
   * @return a SyntheticMethod corresponding to m; or null if none is available.
   */
  private SyntheticMethod findOrCreateSyntheticMethod(IMethod m, boolean isStatic) {
    MethodReference ref = m.getReference();
    if (syntheticMethods.containsKey(ref)) {
      return syntheticMethods.get(ref);
    } else {
      MethodSummary summ = null;
      if (canIgnore(ref)) {
        summ = generateNoOp(ref, isStatic);
      } else {
        summ = findSummary(ref);
      }
      if (summ != null) {
        SummarizedMethod n = new SummarizedMethod(ref, summ, m.getDeclaringClass());
        syntheticMethods.put(ref, n);
        return n;
      } else {
        syntheticMethods.put(ref, null);
        return null;
      }
    }
  }

  private MethodSummary generateNoOp(MethodReference m, boolean isStatic) {
    return new NoOpSummary(m, isStatic);
  }

  private static class NoOpSummary extends MethodSummary {

    public NoOpSummary(MethodReference method, boolean isStatic) {
      super(method);
      setStatic(isStatic);
    }

    /*
     * @see com.ibm.wala.ipa.summaries.MethodSummary#getStatements()
     */
    @Override
    public SSAInstruction[] getStatements() {
      if (getReturnType().equals(TypeReference.Void)) {
        return NO_STATEMENTS;
      } else {
        int nullValue = getNumberOfParameters() + 1;
        SSAInstruction[] result = new SSAInstruction[1];
        result[0] = new SSAReturnInstruction(nullValue, getReturnType().isPrimitiveType());
        return result;
      }
    }

  }

  /**
   * @param m
   * @return true iff we can treat m as a no-op method
   */
  private boolean canIgnore(MemberReference m) {
    TypeReference T = m.getDeclaringClass();
    TypeName n = T.getName();
    Atom p = n.getPackage();
    return (ignoredPackages.contains(p));
  }

  private MethodSummary findSummary(MemberReference m) {
    MethodSummary result = methodSummaries.get(m);
    if (result != null) {
      if (DEBUG) {
        Trace.println("findSummary succeeded: " + m);
      }
      return result;
    }

    // try the class instead.
    TypeReference t = m.getDeclaringClass();
    result = methodSummaries.get(t);
    if (result != null) {
      if (DEBUG) {
        Trace.println("findSummary succeeded: " + t);
      }
      return result;
    }
    if (t.isArrayType()) {
      return null;
    }

    // finally try the package.
    // Atom p = extractPackage(t);
    Atom p = t.getName().getPackage();
    result = methodSummaries.get(p);
    if (result != null) {
      if (DEBUG) {
        Trace.println("findSummary succeeded: " + p);
      }
      return result;
    } else {
      if (DEBUG) {
        Trace.println("findSummary failed: " + m);
      }
      return result;
    }
  }

  protected ClassHierarchy getClassHierarchy() {
    return cha;
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.MethodTargetSelector#mightReturnSyntheticMethod(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference)
   */
  public boolean mightReturnSyntheticMethod(CGNode caller, CallSiteReference site) {
    if (parent.mightReturnSyntheticMethod(caller, site)) {
      return true;
    } else {
      IMethod resolved = cha.resolveMethod(site.getDeclaredTarget());
      if (resolved == null) {
        return true;
      } else {
        return findOrCreateSyntheticMethod(resolved.getReference(), resolved.isStatic()) != null;
      }
    }
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.MethodTargetSelector#mightReturnSyntheticMethod(com.ibm.wala.types.MethodReference)
   */
  public boolean mightReturnSyntheticMethod(MethodReference declaredTarget) {
    if (parent.mightReturnSyntheticMethod(declaredTarget)) {
      return true;
    } else {
      IMethod resolved = cha.resolveMethod(declaredTarget);
      if (resolved == null) {
        return true;
      } else {
        return findOrCreateSyntheticMethod(resolved.getReference(), resolved.isStatic()) != null;
      }
    }
  }
}
