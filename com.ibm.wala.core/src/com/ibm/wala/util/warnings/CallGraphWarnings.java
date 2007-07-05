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
package com.ibm.wala.util.warnings;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.UnresolvedReflectionWarning;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.IteratorUtil;

/**
 * 
 * Support to walk a call graph and generate warnings.
 * 
 * @author sfink
 */
public class CallGraphWarnings {

  private static final TypeReference[] ignoredTypeArray = {
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/Object"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/xerces/dom3/UserDataHandler"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/w3c/dom/events/EventListener"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/w3c/dom/ls/DOMBuilderFilter"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/xerces/dom/PSVIAttrNSImpl"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/xerces/dom/PSVIElementNSImpl"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/xerces/dom/NodeIteratorImpl"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/xml/serializer/SerializerTrace"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/xml/sax/DocumentHandler"),
      TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/xerces/dom/RangeImpl") };

  /**
   * Set of TypeReferences representing types which we do not report unreachable
   * warnings for.
   */
  final private static Set<TypeReference> ignoredTypes = HashSetFactory.make();
  static {
    for (int i = 0; i < ignoredTypeArray.length; i++) {
      ignoredTypes.add(ignoredTypeArray[i]);
    }
  }

  /**
   * Set of Atoms representing package names whose methods should be treated as
   * no-ops
   */
  final private static Set<Atom> ignoredPackages = HashSetFactory.make();

  /**
   * Register a package names whose methods should be treated as no-ops; don't
   * report unreachable warnings for these
   */
  public static synchronized void ignorePackage(Atom p) {
    ignoredPackages.add(p);
  }

  /**
   * @param m
   * @return true iff we can ignore unreachable warnings for m
   */
  private static boolean canIgnore(MemberReference m) {
    TypeReference T = m.getDeclaringClass();
    TypeName n = T.getName();
    Atom p = n.getPackage();
    return (ignoredPackages.contains(p) || ignoredTypes.contains(m.getDeclaringClass()));
  }

  /**
   * @param cg
   *          Call Graph
   * @return set of warnings inferred from the call graph.
   * @throws IllegalArgumentException  if cg is null
   */
  public static WarningSet getWarnings(CallGraph cg) {
    if (cg == null) {
      throw new IllegalArgumentException("cg is null");
    }
    WarningSet result = new WarningSet();
    if (cg.getNumberOfNodes() == 1) {
      result.add(NoEntrypointsFailure.INSTANCE);
    } else {
      for (Iterator it = cg.iterator(); it.hasNext();) {
        CGNode n = (CGNode) it.next();
        addWarningsForNode(result, n);
      }
    }
    return result;
  }
  
  /**
   * @author sfink
   */
  private static class NoEntrypointsFailure extends Warning {

    final private static NoEntrypointsFailure INSTANCE = new NoEntrypointsFailure();
    NoEntrypointsFailure() {
      super(Warning.SEVERE);
    }
    @Override
    public String getMsg() {
      return getClass().toString();
    }
  }

  private static void addWarningsForNode(WarningSet warnings, CGNode n) {
    IMethod m = n.getMethod();
    if (m == null) {
      return;
    }
    if (m.isSynthetic()) {
      SyntheticMethod s = (SyntheticMethod) m;
      if (s.hasPoison()) {
        warnings.add(new PoisonWarning(s.getPoisonLevel(),n));
      }
      if (s.isFactoryMethod()) {
        int count = IteratorUtil.count(n.iterateNewSites());
        if (count == 1) {
          warnings.add(new UnresolvedReflectionWarning(n));
        }
      }
    }
    if (m.isNative()) {
      warnings.add(new NativeWarning(n));
    }
    //  check that every call site in node has at least one successor
    for (Iterator it = n.iterateCallSites(); it.hasNext();) {
      CallSiteReference site = (CallSiteReference) it.next();
      Iterator targets = n.getPossibleTargets(site).iterator();

      if (!targets.hasNext()) {
        if (!canIgnore(site.getDeclaredTarget())) {
          warnings.add(new NoCalleeWarning(n, site));
        }
      }
    }
  }
  
  /**
   * @author sfink
   *
   */
  private static class PoisonWarning extends MethodWarning {

    final String poison;
    public PoisonWarning(byte level, CGNode node) {
      super(level, node.getMethod().getReference());
      poison = ((SyntheticMethod)node.getMethod()).getPoison();
    }

    @Override
    public String getMsg() {
      return getClass() + " " + poison;
    }
    
  }

  /**
   * @author sfink
   * 
   * A warning generated by reaching an unmodelled native method
   */
  private static class NativeWarning extends MethodWarning {
    /**
     * @param node
     */
    public NativeWarning(CGNode node) {
      super(SEVERE, node.getMethod().getReference());
    }

    /*
     * @see com.ibm.wala.util.Warning#getMsg()
     */
    @Override
    public String getMsg() {
      return "Native method " + getMethod();
    }
  }

  /**
   * A warning generated by reaching an call site with no callees found.
   */
  private static class NoCalleeWarning extends MethodWarning {

    final private CallSiteReference site;

    public NoCalleeWarning(CGNode node, CallSiteReference site) {
      super(node.getMethod().getReference());
      this.site = site;
      ClassLoaderReference cl = node.getMethod().getDeclaringClass().getClassLoader().getReference();
      if (cl.equals(ClassLoaderReference.Primordial)) {
        setLevel(CLIENT_MILD);
      } else if (cl.equals(ClassLoaderReference.Extension)) {
        setLevel(CLIENT_MODERATE);
      } else {
        setLevel(CLIENT_SEVERE);
      }
    }

    /*
     * @see com.ibm.wala.util.Warning#getMsg()
     */
    @Override
    public String getMsg() {
      return "No callee for " + site + " in node " + getMethod();
    }

    /*
     * @see com.ibm.wala.util.warnings.Warning#severityString()
     */
    @Override
    protected String severityString() {
      return "Unreachable call";
    }
  }
}
