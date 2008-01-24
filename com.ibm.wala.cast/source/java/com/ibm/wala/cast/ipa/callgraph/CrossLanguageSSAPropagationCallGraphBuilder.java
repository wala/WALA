package com.ibm.wala.cast.ipa.callgraph;

import java.util.Iterator;

import com.ibm.wala.cast.ipa.callgraph.AstSSAPropagationCallGraphBuilder.AstPointerAnalysisImpl.AstImplicitPointsToSetVisitor;
import com.ibm.wala.cast.util.TargetLanguageSelector;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerFlowGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerFlowGraphFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointsToMap;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.PropagationSystem;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.strings.Atom;

public abstract class CrossLanguageSSAPropagationCallGraphBuilder extends AstSSAPropagationCallGraphBuilder {

  public interface PointerVisitorFactory {
    PointerFlowGraph.InstructionVisitor make(PointerAnalysis pa, CallGraph cg, Graph<PointerKey> delegate, CGNode node, IR ir,
        BasicBlock bb);
  }

  private final TargetLanguageSelector<ConstraintVisitor, ExplicitCallGraph.ExplicitNode> visitors;

  private final TargetLanguageSelector<InterestingVisitor, Integer> interesting;

  protected abstract TargetLanguageSelector<ConstraintVisitor, ExplicitCallGraph.ExplicitNode> makeMainVisitorSelector();

  protected abstract TargetLanguageSelector<InterestingVisitor, Integer> makeInterestingVisitorSelector();

  protected abstract TargetLanguageSelector<PointerVisitorFactory, CGNode> makePointerVisitorSelector(
      CrossLanguagePointerFlowGraph analysis);

  protected abstract TargetLanguageSelector<AstImplicitPointsToSetVisitor, LocalPointerKey> makeImplicitVisitorSelector(
      CrossLanguagePointerAnalysisImpl analysis);

  protected abstract TargetLanguageSelector<AbstractRootMethod, CrossLanguageCallGraph> makeRootNodeSelector();

  protected CrossLanguageSSAPropagationCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache,
      PointerKeyFactory pointerKeyFactory) {
    super(cha, options, cache, pointerKeyFactory);
    visitors = makeMainVisitorSelector();
    interesting = makeInterestingVisitorSelector();
  }

  protected ExplicitCallGraph createEmptyCallGraph(IClassHierarchy cha, AnalysisOptions options) {
    return new CrossLanguageCallGraph(makeRootNodeSelector(), cha, options, getAnalysisCache());
  }

  protected static Atom getLanguage(CGNode node) {
    return node.getMethod().getReference().getDeclaringClass().getClassLoader().getLanguage();
  }

  protected InterestingVisitor makeInterestingVisitor(CGNode node, int vn) {
    return interesting.get(getLanguage(node), new Integer(vn));
  }

  protected ConstraintVisitor makeVisitor(ExplicitCallGraph.ExplicitNode node) {
    return visitors.get(getLanguage(node), node);
  }

  protected static class CrossLanguagePointerFlowGraph extends AstPointerFlowGraph {
    private final TargetLanguageSelector<PointerVisitorFactory, CGNode> pointerVisitors;

    protected CrossLanguagePointerFlowGraph(CrossLanguageSSAPropagationCallGraphBuilder builder, PointerAnalysis pa, CallGraph cg) {
      super(pa, cg);
      this.pointerVisitors = builder.makePointerVisitorSelector(this);
    }

    protected InstructionVisitor makeInstructionVisitor(CGNode node, IR ir, BasicBlock bb) {
      return pointerVisitors.get(getLanguage(node), node).make(pa, cg, delegate, node, ir, bb);
    }
  }

  public PointerFlowGraphFactory getPointerFlowGraphFactory() {
    return new PointerFlowGraphFactory() {
      public PointerFlowGraph make(PointerAnalysis pa, CallGraph cg) {
        return new CrossLanguagePointerFlowGraph(CrossLanguageSSAPropagationCallGraphBuilder.this, pa, cg);
      }
    };
  }

  protected PropagationSystem makeSystem(AnalysisOptions options) {
    return new PropagationSystem(callGraph, pointerKeyFactory, instanceKeyFactory, options.getSupportRefinement()) {
      public PointerAnalysis makePointerAnalysis(PropagationCallGraphBuilder builder) {
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

    protected ImplicitPointsToSetVisitor makeImplicitPointsToVisitor(LocalPointerKey lpk) {
      return implicitVisitors.get(getLanguage(lpk.getNode()), lpk);
    }
  }

  protected void customInit() {
    for (Iterator roots = ((CrossLanguageCallGraph) callGraph).getLanguageRoots(); roots.hasNext();) {
      markDiscovered((CGNode) roots.next());
    }
  }

}
