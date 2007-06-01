package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.cast.ipa.callgraph.AstSSAPropagationCallGraphBuilder.AstPointerAnalysisImpl.*;
import com.ibm.wala.cast.util.*;
import com.ibm.wala.cfg.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.ssa.SSACFG.*;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.graph.*;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.warnings.*;

import java.util.*;

public abstract class CrossLanguageSSAPropagationCallGraphBuilder
    extends AstSSAPropagationCallGraphBuilder
{

  public interface PointerVisitorFactory {
    PointerFlowGraph.InstructionVisitor make (PointerAnalysis pa, 
	  CallGraph cg,
	  Graph<PointerKey> delegate, 
	  CGNode node, 
	  IR ir, 
	  BasicBlock bb);
  }

  private final TargetLanguageSelector<ConstraintVisitor, ExplicitCallGraph.ExplicitNode> 
    visitors;

  private final TargetLanguageSelector<InterestingVisitor, Integer> 
    interesting;

  protected abstract TargetLanguageSelector<ConstraintVisitor, ExplicitCallGraph.ExplicitNode>
    makeMainVisitorSelector();

  protected abstract TargetLanguageSelector<InterestingVisitor, Integer>
    makeInterestingVisitorSelector();

  protected abstract TargetLanguageSelector<PointerVisitorFactory, CGNode> 
    makePointerVisitorSelector(CrossLanguagePointerFlowGraph analysis);

  protected abstract TargetLanguageSelector<AstImplicitPointsToSetVisitor, LocalPointerKey> 
    makeImplicitVisitorSelector(CrossLanguagePointerAnalysisImpl analysis);

  protected abstract TargetLanguageSelector<AbstractRootMethod, CrossLanguageCallGraph>
    makeRootNodeSelector();

  protected CrossLanguageSSAPropagationCallGraphBuilder(
		    IClassHierarchy cha, 
		    WarningSet warnings, 
		    AnalysisOptions options,
		    PointerKeyFactory pointerKeyFactory) 
  {
    super(cha, warnings, options, pointerKeyFactory);
    visitors = makeMainVisitorSelector();
    interesting = makeInterestingVisitorSelector();
  }

  protected ExplicitCallGraph createEmptyCallGraph(IClassHierarchy cha, AnalysisOptions options) {
      return new CrossLanguageCallGraph(makeRootNodeSelector(),
					cha,
					options);
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

  protected static class CrossLanguagePointerFlowGraph 
      extends AstPointerFlowGraph 
  {
    private final TargetLanguageSelector<PointerVisitorFactory, CGNode>
      pointerVisitors;

    protected CrossLanguagePointerFlowGraph(
		   CrossLanguageSSAPropagationCallGraphBuilder builder,
		   PointerAnalysis pa, 
		   CallGraph cg) 
    {
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
        return new CrossLanguagePointerFlowGraph(
		       CrossLanguageSSAPropagationCallGraphBuilder.this,
		       pa,
		       cg);
      }
    };
  }

  protected PropagationSystem makeSystem(AnalysisOptions options) {
    return new PropagationSystem(callGraph, 
				 pointerKeyFactory,
				 instanceKeyFactory, 
				 options.getSupportRefinement(), 
				 getWarnings()) {
      public PointerAnalysis
	makePointerAnalysis(PropagationCallGraphBuilder builder) 
      {
	assert builder == CrossLanguageSSAPropagationCallGraphBuilder.this;
	return new CrossLanguagePointerAnalysisImpl(
		   CrossLanguageSSAPropagationCallGraphBuilder.this,
		   cg,
		   pointsToMap,
		   instanceKeys, 
		   pointerKeyFactory,
		   instanceKeyFactory);
      }
    };
  }

  protected static class CrossLanguagePointerAnalysisImpl
      extends AstPointerAnalysisImpl
  {
    private final TargetLanguageSelector<AstImplicitPointsToSetVisitor, LocalPointerKey>
      implicitVisitors;

    protected CrossLanguagePointerAnalysisImpl(
			   CrossLanguageSSAPropagationCallGraphBuilder builder, 
			   CallGraph cg, 
			   PointsToMap pointsToMap,
			   MutableMapping<InstanceKey> instanceKeys, 
			   PointerKeyFactory pointerKeys, 
			   InstanceKeyFactory iKeyFactory)
    {
      super(builder,
	    cg, 
	    pointsToMap, 
	    instanceKeys, 
	    pointerKeys, 
	    iKeyFactory);
      this.implicitVisitors = builder.makeImplicitVisitorSelector(this);
    }

    protected ImplicitPointsToSetVisitor makeImplicitPointsToVisitor(LocalPointerKey lpk) {
      return implicitVisitors.get(getLanguage(lpk.getNode()), lpk);
    }
  }

  protected void customInit() {
    for(Iterator roots = 
	  ((CrossLanguageCallGraph)callGraph).getLanguageRoots();
	roots.hasNext(); )
    {
      markDiscovered( (CGNode) roots.next() );
    }
  }

}

