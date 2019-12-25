/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.analysis.reflection.ReflectionContextInterpreter;
import com.ibm.wala.analysis.reflection.ReflectionContextSelector;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.ReflectionHandler;
import com.ibm.wala.ssa.SSAOptions;

/**
 * Basic interface for options that control call graph generation.
 *
 * <p>TODO: This class should be refactored into an abstract base class and language-specific
 * subclasses.
 */
public class AnalysisOptions {

  /** An object that represents the analysis scope */
  private AnalysisScope analysisScope;

  /** An object that identifies the entrypoints for the call graph */
  private Iterable<? extends Entrypoint> entrypoints;

  /** Policy that determines types allocated at new statements. */
  private ClassTargetSelector classTargetSelector;

  /** Policy that determines methods called at call sites. */
  private MethodTargetSelector methodTargetSelector;

  /**
   * A tuning parameter; how may new equations must be added before doing a new topological sort?
   */
  private int minEquationsForTopSort = 100;

  /**
   * A tuning parameter; by what percentage must the number of equations grow before we perform a
   * topological sort?
   */
  private double topologicalGrowthFactor = 0.5;

  /**
   * A tuning parameter: how many evaluations are allowed to take place between topological
   * re-orderings. The idea is that many evaluations may be a sign of a bad ordering, even when few
   * new equations are being added.
   */
  private int maxEvalBetweenTopo = 1000000000;

  /** options for handling reflection during call graph construction */
  public static enum ReflectionOptions {
    FULL("full", Integer.MAX_VALUE, false, false, false),
    APPLICATION_GET_METHOD("application_get_method", Integer.MAX_VALUE, false, false, true),
    NO_FLOW_TO_CASTS("no_flow_to_casts", 0, false, false, false),
    NO_FLOW_TO_CASTS_APPLICATION_GET_METHOD(
        "no_flow_to_casts_application_get_method", 0, false, false, true),
    NO_METHOD_INVOKE("no_method_invoke", Integer.MAX_VALUE, true, false, false),
    NO_FLOW_TO_CASTS_NO_METHOD_INVOKE("no_flow_to_casts_no_method_invoke", 0, true, false, false),
    ONE_FLOW_TO_CASTS_NO_METHOD_INVOKE("one_flow_to_casts_no_method_invoke", 1, true, false, false),
    ONE_FLOW_TO_CASTS_APPLICATION_GET_METHOD(
        "one_flow_to_casts_application_get_method", 1, false, false, true),
    MULTI_FLOW_TO_CASTS_APPLICATION_GET_METHOD(
        "multi_flow_to_casts_application_get_method", 100, false, false, true),
    NO_STRING_CONSTANTS("no_string_constants", Integer.MAX_VALUE, false, true, false),
    STRING_ONLY("string_constants", 0, true, false, true),
    NONE("none", 0, true, true, true);

    private final String name;

    /** how many times should flows from newInstance() calls to casts be analyzed? */
    private final int numFlowToCastIterations;

    /** should calls to Method.invoke() be ignored? */
    private final boolean ignoreMethodInvoke;

    /** should calls to Class.getMethod() be modeled only for application classes? */
    private final boolean applicationClassesOnly;

    /** should calls to reflective methods with String constant arguments be ignored? */
    private final boolean ignoreStringConstants;

    private ReflectionOptions(
        String name,
        int numFlowToCastIterations,
        boolean ignoreMethodInvoke,
        boolean ignoreInterpretCalls,
        boolean applicationClassesOnly) {
      this.name = name;
      this.numFlowToCastIterations = numFlowToCastIterations;
      this.ignoreMethodInvoke = ignoreMethodInvoke;
      this.ignoreStringConstants = ignoreInterpretCalls;
      this.applicationClassesOnly = applicationClassesOnly;
    }

    public String getName() {
      return name;
    }

    public int getNumFlowToCastIterations() {
      return numFlowToCastIterations;
    }

    public boolean isIgnoreMethodInvoke() {
      return ignoreMethodInvoke;
    }

    public boolean isIgnoreStringConstants() {
      return ignoreStringConstants;
    }

    public boolean isApplicationClassesOnly() {
      return applicationClassesOnly;
    }
  }

  /**
   * Should call graph construction attempt to handle reflection via detection of flows to casts,
   * analysis of string constant parameters to reflective methods, etc.?
   *
   * @see ReflectionHandler
   * @see ReflectionContextInterpreter
   * @see ReflectionContextSelector
   */
  private ReflectionOptions reflectionOptions = ReflectionOptions.FULL;

  /** Should call graph construction handle possible invocations of static initializer methods? */
  private boolean handleStaticInit = true;

  /** Options governing SSA construction */
  private SSAOptions ssaOptions = new SSAOptions();

  /**
   * Use distinct instance keys for distinct string constants?
   *
   * <p>TODO: Probably, this option should moved somewhere into the creation of instance keys.
   * However, those factories are created within the various builders right now, and this is the
   * most convenient place for an engine user to set an option which the creation of instance keys
   * later picks up.
   */
  private boolean useConstantSpecificKeys = false;

  /**
   * Should analysis of lexical scoping consider call stacks?
   *
   * <p>TODO: this option does not apply to all languages. We could have a separation into core
   * engine options and language-specific options.
   *
   * <p>(be careful with multithreaded languages, as threading can break the stack discipline this
   * option may assume)
   */
  private boolean useStacksForLexicalScoping = false;

  /**
   * Should global variables be considered lexically-scoped from the root node?
   *
   * <p>TODO: this option does not apply to all languages. We could have a separation into core
   * engine options and language-specific options.
   *
   * <p>(be careful with multithreaded languages, as threading can break the stack discipline this
   * option may assume)
   */
  private boolean useLexicalScopingForGlobals = false;

  /**
   * Should analysis try to understand the results of string constants flowing to a + operator? Note
   * that this option does not apply to Java bytecode analysis, since the + operators have been
   * compiled away for that. It is used for the Java CAst front end.
   */
  private boolean traceStringConstants = false;

  /**
   * This numerical value indicates the maximum number of nodes that any {@link CallGraph} build
   * with this {@link AnalysisOptions} object is allowed to have. During {@link CallGraph}
   * construction, once {@code maxNumberOfNodes} {@link CGNode} objects have been added to the
   * {@link CallGraph}, no more {@link CGNode} objects will be added. By default, {@code
   * maxNumberOfNodes} is set to {@code -1}, which indicates that no restrictions are in place. See
   * also {@link ExplicitCallGraph}.
   */
  private long maxNumberOfNodes = -1;

  /** Should call graph construction handle arrays of zero-length differently? */
  private boolean handleZeroLengthArray = true;

  // SJF: I'm not sure these factories and caches belong here.
  // TODO: figure out how to clean this up.

  public AnalysisOptions() {}

  public AnalysisOptions(AnalysisScope scope, Iterable<? extends Entrypoint> e) {
    this.analysisScope = scope;
    this.entrypoints = e;
  }

  public AnalysisScope getAnalysisScope() {
    return analysisScope;
  }

  public void setAnalysisScope(AnalysisScope analysisScope) {
    this.analysisScope = analysisScope;
  }

  /** TODO: this really should go away. The entrypoints don't belong here. */
  public Iterable<? extends Entrypoint> getEntrypoints() {
    return entrypoints;
  }

  public void setEntrypoints(Iterable<? extends Entrypoint> entrypoints) {
    this.entrypoints = entrypoints;
  }

  public long getMaxNumberOfNodes() {
    return maxNumberOfNodes;
  }

  public void setMaxNumberOfNodes(long maxNumberOfNodes) {
    this.maxNumberOfNodes = maxNumberOfNodes;
  }

  /** @return Policy that determines methods called at call sites. */
  public MethodTargetSelector getMethodTargetSelector() {
    return methodTargetSelector;
  }

  /** @return Policy that determines types allocated at new statements. */
  public ClassTargetSelector getClassTargetSelector() {
    return classTargetSelector;
  }

  /**
   * install a method target selector
   *
   * @param x an object which controls the policy for selecting the target at a call site
   */
  public void setSelector(MethodTargetSelector x) {
    methodTargetSelector = x;
  }

  /**
   * install a class target selector
   *
   * @param x an object which controls the policy for selecting the allocated object at a new site
   */
  public void setSelector(ClassTargetSelector x) {
    classTargetSelector = x;
  }

  /**
   * @return the mininum number of equations that the pointer analysis system must contain before
   *     the solver will try to topologically sore
   */
  public int getMinEquationsForTopSort() {
    return minEquationsForTopSort;
  }

  /**
   * @param i the mininum number of equations that the pointer analysis system must contain before
   *     the solver will try to topologically sore
   */
  public void setMinEquationsForTopSort(int i) {
    minEquationsForTopSort = i;
  }

  /**
   * @return the maximum number of evaluations that the pointer analysis solver will perform before
   *     topologically resorting the system
   */
  public int getMaxEvalBetweenTopo() {
    return maxEvalBetweenTopo;
  }

  /** @return a fraction x s.t. the solver will resort the system when it grows by a factor of x */
  public double getTopologicalGrowthFactor() {
    return topologicalGrowthFactor;
  }

  /**
   * @param i the maximum number of evaluations that the pointer analysis solver will perform before
   *     topologically resorting the system
   */
  public void setMaxEvalBetweenTopo(int i) {
    maxEvalBetweenTopo = i;
  }

  /** @param d a fraction x s.t. the solver will resort the system when it grows by a factor of x */
  public void setTopologicalGrowthFactor(double d) {
    topologicalGrowthFactor = d;
  }

  /** @return options governing SSA construction */
  public SSAOptions getSSAOptions() {
    return ssaOptions;
  }

  /** @param ssaOptions options governing SSA construction */
  public void setSSAOptions(SSAOptions ssaOptions) {
    this.ssaOptions = ssaOptions;
  }

  /** Use distinct instance keys for distinct string constants? */
  public boolean getUseConstantSpecificKeys() {
    return useConstantSpecificKeys;
  }

  /** Use distinct instance keys for distinct string constants? */
  public void setUseConstantSpecificKeys(boolean useConstantSpecificKeys) {
    this.useConstantSpecificKeys = useConstantSpecificKeys;
  }

  /** Should analysis of lexical scoping consider call stacks? */
  public boolean getUseStacksForLexicalScoping() {
    return useStacksForLexicalScoping;
  }

  /** Should analysis of lexical scoping consider call stacks? */
  public void setUseStacksForLexicalScoping(boolean v) {
    useStacksForLexicalScoping = v;
  }

  /** Should global variables be considered lexically-scoped from the root node? */
  public boolean getUseLexicalScopingForGlobals() {
    return useLexicalScopingForGlobals;
  }

  /** Should global variables be considered lexically-scoped from the root node? */
  public void setUseLexicalScopingForGlobals(boolean v) {
    useLexicalScopingForGlobals = v;
  }

  /**
   * Should analysis try to understand the results of string constants flowing to a + operator? Note
   * that this option does not apply to Java bytecode analysis, since the + operators have been
   * compiled away for that. It is used for the Java CAst front end.
   */
  public void setTraceStringConstants(boolean v) {
    traceStringConstants = v;
  }

  /**
   * Should analysis try to understand the results of string constants flowing to a + operator? Note
   * that this option does not apply to Java bytecode analysis, since the + operators have been
   * compiled away for that. It is used for the Java CAst front end.
   */
  public boolean getTraceStringConstants() {
    return traceStringConstants;
  }

  /**
   * Should call graph construction attempt to handle reflection via detection of flows to casts,
   * analysis of string constant parameters to reflective methods, etc.?
   *
   * @see ReflectionHandler
   * @see ReflectionContextInterpreter
   * @see ReflectionContextSelector
   */
  public ReflectionOptions getReflectionOptions() {
    return reflectionOptions;
  }

  /**
   * Should call graph construction attempt to handle reflection via detection of flows to casts,
   * analysis of string constant parameters to reflective methods, etc.?
   *
   * @see ReflectionHandler
   * @see ReflectionContextInterpreter
   * @see ReflectionContextSelector
   */
  public void setReflectionOptions(ReflectionOptions reflectionOptions) {
    this.reflectionOptions = reflectionOptions;
  }

  /** Should call graph construction handle possible invocations of static initializer methods? */
  public boolean getHandleStaticInit() {
    return handleStaticInit;
  }

  /** Should call graph construction handle possible invocations of static initializer methods? */
  public void setHandleStaticInit(boolean handleStaticInit) {
    this.handleStaticInit = handleStaticInit;
  }

  /** Should call graph construction handle arrays of zero-length differently? */
  public boolean getHandleZeroLengthArray() {
    return handleZeroLengthArray;
  }

  /** Should call graph construction handle arrays of zero-length differently? */
  public void setHandleZeroLengthArray(boolean handleZeroLengthArray) {
    this.handleZeroLengthArray = handleZeroLengthArray;
  }
}
