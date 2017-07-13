/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ipa.callgraph;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.util.TargetLanguageSelector;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * A CallGraph implementation adapted to work for graphs that contain code
 * entities from multiple languages, and hence multiple specialized forms of IR.
 * The root node delegates to one of several language-specific root nodes,
 * allowing each language to use its own specialized IR constructs for entry
 * points.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class CrossLanguageCallGraph extends AstCallGraph {

  public CrossLanguageCallGraph(TargetLanguageSelector<AbstractRootMethod, CrossLanguageCallGraph> roots, IClassHierarchy cha,
      AnalysisOptions options, IAnalysisCacheView cache) {
    super(cha, options, cache);
    this.roots = roots;
  }

  private final TargetLanguageSelector<AbstractRootMethod, CrossLanguageCallGraph> roots;

  private final Set<CGNode> languageRootNodes = HashSetFactory.make();

  private final Map<Atom,IMethod> languageRoots = HashMapFactory.make();

  @SuppressWarnings("deprecation")
  public AbstractRootMethod getLanguageRoot(Atom language){
    if (!languageRoots.containsKey(language)) {
      AbstractRootMethod languageRoot = roots.get(language, this);

      CGNode languageRootNode = null;
      try {
        languageRootNode = findOrCreateNode(languageRoot, Everywhere.EVERYWHERE);
      } catch (CancelException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }

      languageRootNodes.add(languageRootNode);

      CallSiteReference site = CallSiteReference.make(1, languageRoot.getReference(), IInvokeInstruction.Dispatch.STATIC);

      CGNode fakeRootNode = getFakeRootNode();
      CrossLanguageFakeRoot fakeRootMethod = (CrossLanguageFakeRoot) fakeRootNode.getMethod();

      site = fakeRootMethod.addInvocationInternal(new int[0], site).getCallSite();

      fakeRootNode.addTarget(site, languageRootNode);

      languageRoots.put(language, languageRoot);
    }

    return (AbstractRootMethod) languageRoots.get(language);
  }

  public class CrossLanguageFakeRoot extends ScriptFakeRoot {

    public CrossLanguageFakeRoot(IClass declaringClass, IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
      super(FakeRootMethod.rootMethod, declaringClass, cha, options, cache);
    }

    public CrossLanguageFakeRoot(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
      super(FakeRootMethod.rootMethod, cha, options, cache);
    }

    public int addPhi(TypeReference type, int[] values) {
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return root.addPhi(values);
    }

    @Override
    public int addGetInstance(FieldReference ref, int object) {
      TypeReference type = ref.getDeclaringClass();
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return root.addGetInstance(ref, object);
    }

    @Override
    public int addGetStatic(FieldReference ref) {
      TypeReference type = ref.getDeclaringClass();
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return root.addGetStatic(ref);
    }

    @Override
    public int addCheckcast(TypeReference[] type, int rv, boolean isPEI) {
      Atom language = type[0].getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return root.addCheckcast(type, rv, isPEI);
    }

    @Override
    public SSANewInstruction addAllocation(TypeReference type) {
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return root.addAllocation(type);
    }

    @Override
    public SSAInvokeInstruction addInvocation(int[] params, CallSiteReference site) {
      TypeReference type = site.getDeclaredTarget().getDeclaringClass();
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return root.addInvocation(params, site);
    }

    public SSAInvokeInstruction addInvocationInternal(int[] params, CallSiteReference site) {
      return super.addInvocation(params, site);
    }

    @Override
    public AstLexicalRead addGlobalRead(TypeReference type, String name) {
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return ((AstFakeRoot) root).addGlobalRead(type, name);
    }

    @Override
    public SSAAbstractInvokeInstruction addDirectCall(int functionVn, int[] argVns, CallSiteReference callSite) {
      TypeReference type = callSite.getDeclaredTarget().getDeclaringClass();
      Atom language = type.getClassLoader().getLanguage();
      AbstractRootMethod root = getLanguageRoot(language);
      return ((ScriptFakeRoot) root).addDirectCall(functionVn, argVns, callSite);
    }
  }

  Iterator<CGNode> getLanguageRoots() {
    return languageRootNodes.iterator();
  }

  @Override
  protected CGNode makeFakeRootNode() throws CancelException {
    return findOrCreateNode(new CrossLanguageFakeRoot(cha, options, getAnalysisCache()), Everywhere.EVERYWHERE);
  }
}
