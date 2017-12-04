/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.exceptionanalysis;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.analysis.arraybounds.ArrayOutOfBoundsAnalysis;
import com.ibm.wala.analysis.nullpointer.IntraproceduralNullPointerAnalysis;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionMatcher;
import com.ibm.wala.ipa.cfg.exceptionpruning.filter.DummyFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural.IgnoreExceptionsInterFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural.InterproceduralExceptionFilter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstruction.Visitor;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.InvertedGraph;

/**
 * 
 * This class analyzes the exceptional control flow. Use
 * {@link ExceptionAnalysis2EdgeFilter} to remove infeasible edges.
 * 
 * In a first step an intraprocedural analysis is performed, to collect the
 * thrown exceptions and collect the exceptions caught, per invoke instruction.
 * The results of the intraprocedural analysis are used for a GenKill data flow
 * analysis on the call graph. (Each node generates intraprocedural thrown
 * exceptions and along invoke edges, caught exceptions are removed.)
 * 
 * Notice: Only exceptions, which are part of the analysis scope are considered.
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class ExceptionAnalysis {
  private BitVectorSolver<CGNode> solver;
  private Exception2BitvectorTransformer transformer;
  private InterproceduralExceptionFilter<SSAInstruction> filter;
  private ClassHierarchy cha;
  private CGIntraproceduralExceptionAnalysis intraResult;
  private CallGraph cg;
  private boolean isSolved = false;

  public ExceptionAnalysis(CallGraph callgraph, PointerAnalysis<InstanceKey> pointerAnalysis, ClassHierarchy cha) {
    this(callgraph, pointerAnalysis, cha, null);
  }

  /**
   * @param callgraph
   * @param pointerAnalysis
   * @param cha
   * @param filter
   *          a filter to include results of other analysis (like
   *          {@link ArrayOutOfBoundsAnalysis} or
   *          {@link IntraproceduralNullPointerAnalysis}) or to ignore
   *          exceptions completely.
   */
  public ExceptionAnalysis(CallGraph callgraph, PointerAnalysis<InstanceKey> pointerAnalysis, ClassHierarchy cha,
      InterproceduralExceptionFilter<SSAInstruction> filter) {
    this.cha = cha;
    this.cg = callgraph;
    if (filter == null) {
      this.filter = new IgnoreExceptionsInterFilter<>(new DummyFilter<SSAInstruction>());
    } else {
      this.filter = filter;
    }

    intraResult = new CGIntraproceduralExceptionAnalysis(callgraph, pointerAnalysis, cha, this.filter);
    transformer = new Exception2BitvectorTransformer(intraResult.getExceptions());
    ExceptionTransferFunctionProvider transferFunctionProvider = new ExceptionTransferFunctionProvider(intraResult, callgraph,
        transformer);

    Graph<CGNode> graph = new InvertedGraph<>(callgraph);
    BitVectorFramework<CGNode, TypeReference> problem = new BitVectorFramework<>(graph, transferFunctionProvider,
        transformer.getValues());

    solver = new InitializedBitVectorSolver(problem);
    solver.initForFirstSolve();
  }

  public void solve() {
    try {
      solver.solve(null);
    } catch (CancelException e) {
      throw new RuntimeException("Internal Error: Got Cancel Exception, " + "but didn't use Progressmonitor!", e);
    }
    this.isSolved = true;
  }

  public void solve(IProgressMonitor monitor) throws CancelException {
    solver.solve(monitor);
    this.isSolved = true;
  }

  public boolean catchesException(CGNode node, ISSABasicBlock throwBlock, ISSABasicBlock catchBlock) {
    if (!isSolved) {
      throw new IllegalStateException("You need to use .solve() first!");
    }

    if (node.getIR().getControlFlowGraph().getExceptionalSuccessors(throwBlock).contains(catchBlock) && catchBlock.isCatchBlock()) {
      SSAInstruction instruction = IntraproceduralExceptionAnalysis.getThrowingInstruction(throwBlock);
      assert instruction != null;
      Iterator<TypeReference> caughtExceptions = catchBlock.getCaughtExceptionTypes();
      Set<TypeReference> thrownExceptions = this.getExceptions(node, instruction);
      boolean isCaught = false;
      while (caughtExceptions.hasNext() && !isCaught) {
        TypeReference caughtException = caughtExceptions.next();
        for (TypeReference thrownException : thrownExceptions) {
          isCaught |= cha.isAssignableFrom(cha.lookupClass(caughtException), cha.lookupClass(thrownException));
          if (isCaught)
            break;
        }
      }
      return isCaught;
    } else {
      return false;
    }
  }

  /**
   * @param node
   * @param block
   * @return if the block has uncaught exceptions
   */
  public boolean hasUncaughtExceptions(CGNode node, ISSABasicBlock block) {
    if (!isSolved) {
      throw new IllegalStateException("You need to use .solve() first!");
    }

    SSAInstruction instruction = IntraproceduralExceptionAnalysis.getThrowingInstruction(block);
    if (instruction != null) {
      Set<TypeReference> exceptions = this.getExceptions(node, instruction);

      boolean allCaught = true;
      for (TypeReference thrownException : exceptions) {
        boolean isCaught = false;
        for (ISSABasicBlock catchBlock : node.getIR().getControlFlowGraph().getExceptionalSuccessors(block)) {
          Iterator<TypeReference> caughtExceptions = catchBlock.getCaughtExceptionTypes();
          while (caughtExceptions.hasNext() && !isCaught) {
            TypeReference caughtException = caughtExceptions.next();
            isCaught |= cha.isAssignableFrom(cha.lookupClass(caughtException), cha.lookupClass(thrownException));
            if (isCaught)
              break;
          }
          if (isCaught)
            break;
        }
        allCaught &= isCaught;
        if (!allCaught)
          break;
      }

      return !allCaught;
    } else {
      return false;
    }
  }

  /**
   * Returns all exceptions, which may be raised by this instruction. This
   * includes exceptions from throw and invoke statements.
   * 
   * @param node
   * @param instruction
   * @return all exceptions, which may be raised by this instruction
   */
  public Set<TypeReference> getExceptions(final CGNode node, SSAInstruction instruction) {
    if (!isSolved) {
      throw new IllegalStateException("You need to use .solve() first!");
    }

    final Set<TypeReference> thrown = intraResult.getAnalysis(node).collectThrownExceptions(instruction);

    instruction.visit(new Visitor() {
      @Override
      public void visitInvoke(SSAInvokeInstruction instruction) {
        CallSiteReference site = instruction.getCallSite();
        Set<CGNode> targets = cg.getPossibleTargets(node, site);
        for (CGNode target : targets) {
          thrown.addAll(getCGNodeExceptions(target));
        }
      }
    });

    Set<TypeReference> result = thrown;
    if (filter != null) {
      ExceptionFilter<SSAInstruction> nodeFilter = filter.getFilter(node);
      result = ExceptionMatcher.retainedExceptions(thrown, nodeFilter.filteredExceptions(instruction), cha);
    }
    return result;
  }

  /**
   * 
   * @param node
   * @return all exceptions, which might be thrown by the method represented
   *         through the call graph node.
   */
  public Set<TypeReference> getCGNodeExceptions(CGNode node) {
    if (!isSolved) {
      throw new IllegalStateException("You need to use .solve() first!");
    }

    BitVectorVariable nodeResult = solver.getOut(node);
    if (nodeResult != null) {
      return transformer.computeExceptions(nodeResult);
    } else {
      return null;
    }
  }

  /**
   * @return the used filter
   */
  public InterproceduralExceptionFilter<SSAInstruction> getFilter() {
    if (!isSolved) {
      throw new IllegalStateException("You need to use .solve() first!");
    }

    return filter;
  }
}
