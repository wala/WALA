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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.Atom;

/**
 * "Non-standard" bypass rules to use during call graph construction.
 * 
 * Normally, the method bypass rules replace the IMethod that is resolved by other means, via the getBypass() method. However, the
 * bypass rules can be invoked even before resolving the target of a call, by checking the intercept rules.
 */
public class MethodBypass {

  static final boolean DEBUG = false;

  /**
   * Method summaries collected for methods. Mapping Object -&gt; MethodSummary where Object is either a
   * <ul>
   * <li>MethodReference
   * <li>TypeReference
   * <li>Atom (package name)
   * </ul>
   */
  private final Map methodSummaries;

  /**
   * Set of TypeReferences which are marked "allocatable"
   */
  private final Set allocatable;

  /**
   * Governing class hierarchy.
   */
  private final IClassHierarchy cha;

  /**
   * Mapping from MethodReference -&gt; SyntheticMethod
   */
  final private HashMap<MethodReference, SummarizedMethod> syntheticMethods = HashMapFactory.make();

  /**
   * Set of method references that have been considered already.
   */
  final private HashSet<MethodReference> considered = HashSetFactory.make();

  public MethodBypass(Map methodSummaries, Set allocatable, IClassHierarchy cha) {
    this.methodSummaries = methodSummaries;
    this.allocatable = allocatable;
    this.cha = cha;
  }

  /**
   * Lookup bypass rules based on a method reference only.
   * 
   * Method getBypass.
   * 
   * @param m
   * @return Object
   */
  private SyntheticMethod getBypass(MethodReference m) {
    if (DEBUG) {
      System.err.println(("MethodBypass.getBypass? " + m));
    }
    SyntheticMethod result = findOrCreateSyntheticMethod(m);
    if (result != null) {
      return result;
    }
    // first lookup failed ... try resolving target via CHA and try again.
    m = resolveTarget(m);
    return findOrCreateSyntheticMethod(m);
  }

  /**
   * @param m a method reference
   * @return a SyntheticMethod corresponding to m; or null if none is available.
   */
  private SyntheticMethod findOrCreateSyntheticMethod(MethodReference m) {
    if (considered.contains(m)) {
      return syntheticMethods.get(m);
    } else {
      considered.add(m);
      MethodSummary summ = findSummary(m);
      if (summ != null) {
        TypeReference T = m.getDeclaringClass();
        IClass c = cha.lookupClass(T);
        assert c != null : "null class for " + T;
        SummarizedMethod n = new SummarizedMethod(m, summ, c);
        syntheticMethods.put(m, n);
        return n;
      }
      return null;
    }
  }

  private MethodSummary findSummary(MemberReference m) {
    MethodSummary result = (MethodSummary) methodSummaries.get(m);
    if (result != null) {
      if (DEBUG) {
        System.err.println(("findSummary succeeded: " + m));
      }
      return result;
    }

    // try the class instead.
    TypeReference t = m.getDeclaringClass();
    result = (MethodSummary) methodSummaries.get(t);
    if (result != null) {
      if (DEBUG) {
        System.err.println(("findSummary succeeded: " + t));
      }
      return result;
    }
    if (t.isArrayType())
      return null;

    // finally try the package.
    Atom p = extractPackage(t);
    result = (MethodSummary) methodSummaries.get(p);
    if (result != null) {
      if (DEBUG) {
        System.err.println(("findSummary succeeded: " + p));
      }
      return result;
    } else {
      if (DEBUG) {
        System.err.println(("findSummary failed: " + m));
      }
      return result;
    }
  }

  /**
   * Method getBypass. check to see if a call to the receiver 'target' should be redirected to a different receiver.
   * 
   * @param target
   * @return Object
   * @throws IllegalArgumentException if target is null
   */
  public SyntheticMethod getBypass(IMethod target) {
    if (target == null) {
      throw new IllegalArgumentException("target is null");
    }
    return getBypass(target.getReference());
  }

  /**
   * Method extractPackage.
   * 
   * @param type
   * @return Atom that represents the package name, or null if this is the unnamed package.
   */
  private static Atom extractPackage(TypeReference type) {
    String s = type.getName().toString();
    int index = s.lastIndexOf('/');
    if (index == -1) {
      return null;
    } else {
      s = s.substring(0, index);
      return Atom.findOrCreateAsciiAtom(s);
    }
  }

  protected IClassHierarchy getClassHierarchy() {
    return cha;
  }

  protected MethodReference resolveTarget(MethodReference target) {
    IMethod m = getClassHierarchy().resolveMethod(target);
    if (m != null) {
      if (DEBUG) {
        System.err.println(("resolveTarget: resolved to " + m));
      }
      target = m.getReference();
    }
    return target;
  }

  /**
   * Are we allowed to allocate (for analysis purposes) an instance of a given type? By default, the answer is yes iff T is not
   * abstract. However, subclasses and summaries can override this to allow "special" abstract classes to be allocatable as well.
   * 
   * @throws IllegalArgumentException if klass is null
   * 
   */
  public boolean isAllocatable(IClass klass) {
    if (klass == null) {
      throw new IllegalArgumentException("klass is null");
    }
    if (!klass.isAbstract() && !klass.isInterface()) {
      return true;
    } else {
      return allocatable.contains(klass.getReference());
    }
  }
}
