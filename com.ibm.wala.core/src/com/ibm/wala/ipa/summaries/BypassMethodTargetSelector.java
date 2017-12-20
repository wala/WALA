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
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyMethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.strings.Atom;

/**
 * "Non-standard" bypass rules to use during call graph construction.
 * 
 * Normally, the method bypass rules replace the IMethod that is resolved by other means, via the getBypass() method. However, the
 * bypass rules can be invoked even before resolving the target of a call, by checking the intercept rules.
 * 
 * @author sfink
 */
public class BypassMethodTargetSelector implements MethodTargetSelector {

  static final boolean DEBUG = false;

  /**
   * Method summaries collected for methods. Mapping Object -&gt; MethodSummary where Object is either a
   * <ul>
   * <li>MethodReference
   * <li>TypeReference
   * <li>Atom (package name)
   * </ul>
   */
  private final Map<MethodReference, MethodSummary> methodSummaries;

  /**
   * Set of Atoms representing package names whose methods should be treated as no-ops
   */
  private final Set<Atom> ignoredPackages;

  /**
   * Governing class hierarchy.
   */
  protected final IClassHierarchy cha;

  /**
   * target selector to use for non-bypassed calls
   */
  protected final MethodTargetSelector parent;

  /**
   * for checking method target resolution via CHA
   */
  private final ClassHierarchyMethodTargetSelector chaMethodTargetSelector;

  /**
   * Mapping from MethodReference -&gt; SyntheticMethod We may call syntheticMethod.put(m,null) .. in which case we use containsKey()
   * to check for having already considered m.
   */
  final private HashMap<MethodReference, SummarizedMethod> syntheticMethods = HashMapFactory.make();

  /**
   * @param parent
   * @param methodSummaries
   * @param ignoredPackages
   * @param cha
   */
  public BypassMethodTargetSelector(MethodTargetSelector parent, Map<MethodReference, MethodSummary> methodSummaries,
      Set<Atom> ignoredPackages, IClassHierarchy cha) {
    this.methodSummaries = methodSummaries;
    this.ignoredPackages = ignoredPackages;
    this.parent = parent;
    this.cha = cha;
    this.chaMethodTargetSelector = new ClassHierarchyMethodTargetSelector(cha);
  }

  /**
   * Check to see if a particular call site should be bypassed, before checking normal resolution of the receiver.
   * 
   * @throws IllegalArgumentException if site is null
   */
  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass dispatchType) {

    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    // first, see if we'd like to bypass the CHA-based target for the site
    MethodReference ref = site.getDeclaredTarget();    
    IMethod chaTarget = chaMethodTargetSelector.getCalleeTarget(caller, site, dispatchType);
    IMethod target = (chaTarget == null) ? findOrCreateSyntheticMethod(ref, site.isStatic()) : findOrCreateSyntheticMethod(chaTarget,
        site.isStatic());


    if (DEBUG) {
      System.err.println("target is initially " + target);
    }

    if (target != null) {
      return target;
    } else {
      // didn't bypass the CHA target; check if we should bypass the parent target
      if (canIgnore(site.getDeclaredTarget())) {
        // we want to generate a NoOpSummary for this method.
        return findOrCreateSyntheticMethod(site.getDeclaredTarget(), site.isStatic());
      }

      // not using if (instanceof ClassHierarchyMethodTargetSelector) because
      // we want to make sure that getCalleeTarget() is still called if 
      // parent is a subclass of ClassHierarchyMethodTargetSelector
      if (parent.getClass() == ClassHierarchyMethodTargetSelector.class) {
        // already checked this case and decided not to bypass
        return chaTarget;
      }
      target = parent.getCalleeTarget(caller, site, dispatchType);

      if (DEBUG) {
        System.err.println("target becomes " + target);
      }

      if (target != null) {
        IMethod bypassTarget = findOrCreateSyntheticMethod(target, site.isStatic());

        if (DEBUG)
          System.err.println("bypassTarget is " + target);

        return (bypassTarget == null) ? target : bypassTarget;
      } else
        return target;
    }
  }

  /**
   * @param m a method reference
   * @return a SyntheticMethod corresponding to m; or null if none is available.
   */
  protected SyntheticMethod findOrCreateSyntheticMethod(MethodReference m, boolean isStatic) {
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
   * @param m a method reference
   * @return a SyntheticMethod corresponding to m; or null if none is available.
   */
  protected SyntheticMethod findOrCreateSyntheticMethod(IMethod m, boolean isStatic) {
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

  /**
   * Generate a {@link MethodSummary} which is the "standard" representation of a method 
   * that does nothing.
   */
  public static MethodSummary generateStandardNoOp(Language l, MethodReference m, boolean isStatic) {
    return new NoOpSummary(l, m, isStatic);
  }
  
  /**
   * Generate a {@link MethodSummary} which is the "standard" representation of a method 
   * that does nothing.  Subclasses may override this method to implement alternative semantics
   * concerning what "do nothing" means.
   */
  public MethodSummary generateNoOp(MethodReference m, boolean isStatic) {
    Language l = cha.resolveMethod(m).getDeclaringClass().getClassLoader().getLanguage();
    return new NoOpSummary(l, m, isStatic);
  }
  

  private static class NoOpSummary extends MethodSummary {

    private final Language l;
    
    public NoOpSummary(Language l, MethodReference method, boolean isStatic) {
      super(method);
      setStatic(isStatic);
      this.l = l;
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
        SSAInstructionFactory insts = l.instructionFactory();
        result[0] = insts.ReturnInstruction(0, nullValue, getReturnType().isPrimitiveType());
        return result;
      }
    }

  }

  /**
   * @param m
   * @return true iff we can treat m as a no-op method
   */
  protected boolean canIgnore(MemberReference m) {
    TypeReference T = m.getDeclaringClass();
    TypeName n = T.getName();
    Atom p = n.getPackage();
    return (ignoredPackages.contains(p));
  }

  private MethodSummary findSummary(MemberReference m) {
    return methodSummaries.get(m);
  }

  protected IClassHierarchy getClassHierarchy() {
    return cha;
  }  
}
