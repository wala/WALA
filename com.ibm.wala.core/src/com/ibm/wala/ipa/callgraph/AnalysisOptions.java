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
package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ssa.SSAOptions;

/**
 * 
 * Basic interface for options that control call graph generation.
 * 
 * @author sfink
 */
public class AnalysisOptions {

  /**
   * An object that represents the analysis scope
   */
  private AnalysisScope analysisScope;

  /**
   * An object that identifies the entrypoints for the call graph
   */
  private Iterable<Entrypoint> entrypoints;

  /**
   * An object which represents the user specification for reflection
   */
  private ReflectionSpecification reflection;

  /**
   * Policy that determines types allocated at new statements.
   */
  private ClassTargetSelector classTargetSelector;

  /**
   * Policy that determines methods called at call sites.
   */
  private MethodTargetSelector methodTargetSelector;

  /**
   * A tuning parameter; how may new equations must be added before doing a new
   * topological sort?
   */
  private int minEquationsForTopSort = 100;

  /**
   * A tuning parameter; by what percentage must the number of equations grow
   * before we perform a topological sort?
   */
  private double topologicalGrowthFactor = 0.5;

  /**
   * A tuning parameter: how many evaluations are allowed to take place between
   * topological re-orderings. The idea is that many evaluations may be a sign
   * of a bad ordering, even when few new equations are being added.
   */
  private int maxEvalBetweenTopo = 1000000000;

  /**
   * Should pointer analysis retain information to support iterative refinement?
   * 
   * Maybe this flag can go away with the new post-mortem pointer-flow graph?
   */
  private boolean supportRefinement = false;

  /**
   * Should we use the pretransitive solver for pointer analysis? Not yet ready
   * for prime-time.
   */
  private boolean usePreTransitiveSolver = false;

  /**
   * Options governing SSA construction
   */
  private SSAOptions ssaOptions = new SSAOptions();

  /**
   * Use distinct instance keys for distinct string constants?
   * 
   * TODO: Probably, this option should moved somewhere into the creation of
   * instance keys. However, those factories are created within the various
   * builders right now, and this is the most convenient place for an engine
   * user to set an option which the creation of instance keys later picks up.
   */
  private boolean useConstantSpecificKeys = false;

  /**
   * Should analysis of lexical scoping consider call stacks?
   * 
   * TODO: this option does not apply to all languages. We could have a
   * separation into core engine options and language-specific options.
   * 
   * (be careful with multithreaded languages, as threading can break the stack
   * discipline this option may assume)
   */
  private boolean useStacksForLexicalScoping = false;

  /**
   * Should global variables be considered lexically-scoped from the root node?
   * 
   * TODO: this option does not apply to all languages. We could have a
   * separation into core engine options and language-specific options.
   * 
   * (be careful with multithreaded languages, as threading can break the stack
   * discipline this option may assume)
   */
  private boolean useLexicalScopingForGlobals = false;

  /**
   *  Should analysis try to understand the results of string constants
   * flowing to a + operator?  Note that this option does not apply to
   * Java bytecode analysis, since the + operators have been compiled away 
   * for that.  It is used for the Java CAst front end.
   */
  private boolean traceStringConstants = false;
  
  /**
   * This numerical value indicates the maximum number of nodes that any {@link CallGraph}
   * build with this {@link AnalysisOptions} object is allowed to have.  During {@link CallGraph}
   * construction, once <code>maxNumberOfNodes</code> {@link CGNode} objects have been added
   * to the {@link CallGraph}, no more {@link CGNode} objects will be added.
   * By default, <code>maxNumberOfNodes</code> is set to <code>-1</code>, which indicates
   * that no restrictions are in place.  See also {@link ExplicitCallGraph}.
   */
  private long maxNumberOfNodes = -1;

  // SJF: I'm not sure these factories and caches belong here.
  // TODO: figure out how to clean this up.


  public AnalysisOptions() {
  }

  public AnalysisOptions(AnalysisScope scope,  Iterable<Entrypoint> e) {
    this.analysisScope = scope;
    this.entrypoints = e;
  }

  public AnalysisScope getAnalysisScope() {
    return analysisScope;
  }

  public void setAnalysisScope(AnalysisScope analysisScope) {
    this.analysisScope = analysisScope;
  }

  public Iterable<Entrypoint> getEntrypoints() {
    return entrypoints;
  }

  public void setEntrypoints(Iterable<Entrypoint> entrypoints) {
    this.entrypoints = entrypoints;
  }
  
  public long getMaxNumberOfNodes() {
    return maxNumberOfNodes;
  }
  
  public void setMaxNumberOfNodes(long maxNumberOfNodes) {
    this.maxNumberOfNodes = maxNumberOfNodes;
  }

  /**
   * @return An object which represents the user specification for reflection
   */
  public ReflectionSpecification getReflectionSpec() {
    return reflection;
  }

  /**
   * @param specification
   *          An object which represents the user specification for reflection
   */
  public void setReflectionSpec(ReflectionSpecification specification) {
    reflection = specification;
  }

  /**
   * @return Policy that determines methods called at call sites.
   */
  public MethodTargetSelector getMethodTargetSelector() {
    return methodTargetSelector;
  }

  /**
   * @return Policy that determines types allocated at new statements.
   */
  public ClassTargetSelector getClassTargetSelector() {
    return classTargetSelector;
  }

  /**
   * install a method target selector
   * 
   * @param x
   *          an object which controls the policy for selecting the target at a
   *          call site
   */
  public void setSelector(MethodTargetSelector x) {
    methodTargetSelector = x;
  }

  /**
   * install a class target selector
   * 
   * @param x
   *          an object which controls the policy for selecting the allocated
   *          object at a new site
   */
  public void setSelector(ClassTargetSelector x) {
    classTargetSelector = x;
  }

  /**
   * @return the mininum number of equations that the pointer analysis system
   *         must contain before the solver will try to topologically sore
   */
  public int getMinEquationsForTopSort() {
    return minEquationsForTopSort;
  }

  /**
   * @param i
   *          the mininum number of equations that the pointer analysis system
   *          must contain before the solver will try to topologically sore
   */
  public void setMinEquationsForTopSort(int i) {
    minEquationsForTopSort = i;
  }

  /**
   * @return the maximum number of evaluations that the pointer analysis solver
   *         will perform before topologically resorting the system
   */
  public int getMaxEvalBetweenTopo() {
    return maxEvalBetweenTopo;
  }

  /**
   * @return a fraction x s.t. the solver will resort the system when it grows
   *         by a factor of x
   */
  public double getTopologicalGrowthFactor() {
    return topologicalGrowthFactor;
  }

  /**
   * @param i
   *          the maximum number of evaluations that the pointer analysis solver
   *          will perform before topologically resorting the system
   */
  public void setMaxEvalBetweenTopo(int i) {
    maxEvalBetweenTopo = i;
  }

  /**
   * @param d
   *          a fraction x s.t. the solver will resort the system when it grows
   *          by a factor of x
   */
  public void setTopologicalGrowthFactor(double d) {
    topologicalGrowthFactor = d;
  }

  /**
   * @returns whether the call graph builder should support Julian's iterative
   *          refinement
   */
  public boolean getSupportRefinement() {
    return supportRefinement;
  }

  /**
   * @param supportRefinement
   *          whether the call graph builder should contruct a pointer-flow
   *          graph as it solves
   */
  public void setSupportRefinement(boolean supportRefinement) {
    this.supportRefinement = supportRefinement;
  }

  /**
   * @return options governing SSA construction
   */
  public SSAOptions getSSAOptions() {
    return ssaOptions;
  }

  /**
   * @param ssaOptions
   *          options governing SSA construction
   */
  public void setSSAOptions(SSAOptions ssaOptions) {
    this.ssaOptions = ssaOptions;
  }

  public boolean getUseConstantSpecificKeys() {
    return useConstantSpecificKeys;
  }

  public void setUseConstantSpecificKeys(boolean useConstantSpecificKeys) {
    this.useConstantSpecificKeys = useConstantSpecificKeys;
  }

  public boolean getUseStacksForLexicalScoping() {
    return useStacksForLexicalScoping;
  }

  public void setUseStacksForLexicalScoping(boolean v) {
    useStacksForLexicalScoping = v;
  }

  public boolean getUseLexicalScopingForGlobals() {
    return useLexicalScopingForGlobals;
  }

  public void setTraceStringConstants(boolean v) {
    traceStringConstants = v;
  }

  public boolean getTraceStringConstants() {
    return traceStringConstants;
  }

  public void setUseLexicalScopingForGlobals(boolean v) {
    useLexicalScopingForGlobals = v;
  }

  /**
   * @return true iff we should solve with the sub-transitive solver
   */
  public boolean usePreTransitiveSolver() {
    return usePreTransitiveSolver;
  }

  public void setUsePreTransitiveSolver(boolean b) {
    usePreTransitiveSolver = b;
  }
}
