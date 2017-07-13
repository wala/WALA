/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.cast.ipa.callgraph.AstSSAPropagationCallGraphBuilder.AstPointerAnalysisImpl.AstImplicitPointsToSetVisitor;
import com.ibm.wala.cast.util.TargetLanguageSelector;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointsToMap;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.PropagationSystem;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.strings.Atom;

public abstract class CrossLanguageSSAPropagationCallGraphBuilder extends AstSSAPropagationCallGraphBuilder {

  private final TargetLanguageSelector<ConstraintVisitor, CGNode> visitors;

  private final TargetLanguageSelector<InterestingVisitor, Integer> interesting;

  protected abstract TargetLanguageSelector<ConstraintVisitor, CGNode> makeMainVisitorSelector();

  protected abstract TargetLanguageSelector<InterestingVisitor, Integer> makeInterestingVisitorSelector();

  protected abstract TargetLanguageSelector<AstImplicitPointsToSetVisitor, LocalPointerKey> makeImplicitVisitorSelector(
      CrossLanguagePointerAnalysisImpl analysis);

  protected abstract TargetLanguageSelector<AbstractRootMethod, CrossLanguageCallGraph> makeRootNodeSelector();

  protected CrossLanguageSSAPropagationCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache,
      PointerKeyFactory pointerKeyFactory) {
    super(cha, options, cache, pointerKeyFactory);
    visitors = makeMainVisitorSelector();
    interesting = makeInterestingVisitorSelector();
  }

  @Override
  protected ExplicitCallGraph createEmptyCallGraph(IClassHierarchy cha, AnalysisOptions options) {
    return new CrossLanguageCallGraph(makeRootNodeSelector(), cha, options, getAnalysisCache());
  }

  protected static Atom getLanguage(CGNode node) {
    return node.getMethod().getReference().getDeclaringClass().getClassLoader().getLanguage();
  }

  @Override
  protected InterestingVisitor makeInterestingVisitor(CGNode node, int vn) {
    return interesting.get(getLanguage(node), new Integer(vn));
  }

  @Override
  public ConstraintVisitor makeVisitor(CGNode node) {
    return visitors.get(getLanguage(node), node);
  }

  @Override
  protected PropagationSystem makeSystem(AnalysisOptions options) {
    return new PropagationSystem(callGraph, pointerKeyFactory, instanceKeyFactory) {
      @Override
      public PointerAnalysis<InstanceKey> makePointerAnalysis(PropagationCallGraphBuilder builder) {
        assert builder == CrossLanguageSSAPropagationCallGraphBuilder.this;
        return new CrossLanguagePointerAnalysisImpl(CrossLanguageSSAPropagationCallGraphBuilder.this, cg, pointsToMap,
            instanceKeys, pointerKeyFactory, instanceKeyFactory);
      }
    };
  }

  protected static class CrossLanguagePointerAnalysisImpl extends AstPointerAnalysisImpl {
    private final TargetLanguageSelector<AstImplicitPointsToSetVisitor, LocalPointerKey> implicitVisitors;

    protected CrossLanguagePointerAnalysisImpl(CrossLanguageSSAPropagationCallGraphBuilder builder, CallGraph cg,
        PointsToMap pointsToMap, MutableMapping<InstanceKey> instanceKeys, PointerKeyFactory pointerKeys,
        InstanceKeyFactory iKeyFactory) {
      super(builder, cg, pointsToMap, instanceKeys, pointerKeys, iKeyFactory);
      this.implicitVisitors = builder.makeImplicitVisitorSelector(this);
    }

    @Override
    protected ImplicitPointsToSetVisitor makeImplicitPointsToVisitor(LocalPointerKey lpk) {
      return implicitVisitors.get(getLanguage(lpk.getNode()), lpk);
    }
  }

  @Override
  protected void customInit() {
    for (CGNode root : callGraph) {
      markDiscovered(root);
    }
  }

}
