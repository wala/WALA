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
package com.ibm.wala.ipa.callgraph.propagation;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;

/**
 * 
 * This abstract base class provides the general algorithm for a call graph
 * builder that relies on propagation through an iterative dataflow solver
 * 
 * TODO: This implementation currently keeps all points to sets live ... even
 * those for local variables that do not span interprocedural boundaries. This
 * may be too space-inefficient .. we can consider recomputing local sets on
 * demand.
 * 
 * @author sfink
 * @author adonovan
 */
public abstract class PropagationCallGraphBuilder implements CallGraphBuilder {
  private final static boolean DEBUG_ALL = false;

  final static boolean DEBUG_ASSIGN = DEBUG_ALL | false;

  private final static boolean DEBUG_ARRAY_LOAD = DEBUG_ALL | false;

  private final static boolean DEBUG_ARRAY_STORE = DEBUG_ALL | false;

  private final static boolean DEBUG_FILTER = DEBUG_ALL | false;

  final protected static boolean DEBUG_GENERAL = DEBUG_ALL | false;

  private final static boolean DEBUG_GET = DEBUG_ALL | false;

  private final static boolean DEBUG_PUT = DEBUG_ALL | false;

  private final static boolean DEBUG_ENTRYPOINTS = DEBUG_ALL | false;

  // private final static boolean DEBUG = DEBUG_ENTRYPOINTS | DEBUG_GENERAL |
  // DEBUG_ASSIGN | DEBUG_ARRAY_LOAD | DEBUG_ARRAY_STORE
  // | DEBUG_GET | DEBUG_PUT | DEBUG_FILTER;

  final static String DEBUG_METHOD_SUBSTRING = null;

  final static boolean DEBUG_TRACK_INSTANCE = false;

  final static int DEBUG_INSTANCE_KEY = 7900;

  /**
   * Meta-data regarding how pointers are modelled
   */
  protected final PointerKeyFactory pointerKeyFactory;

  /**
   * The object that represents the java.lang.Throwable class
   */
  final private IClass JAVA_LANG_OBJECT;

  /**
   * The object that represents the java.lang.Throwable class
   */
  final private IClass JAVA_LANG_THROWABLE;

  /**
   * A singleton set holding the java.lang.Throwable TypeReference
   */
  public final static Set<TypeReference> THROWABLE_SET = Collections.singleton(TypeReference.JavaLangThrowable);

  /**
   * Governing class hierarchy
   */
  final protected IClassHierarchy cha;

  /**
   * Special rules for bypassing Java calls
   */
  final protected AnalysisOptions options;

  /**
   * Set of nodes that have already been traversed for constraints
   */
  final private Set<CGNode> alreadyVisited = HashSetFactory.make();

  /**
   * At any given time, the set of nodes that have been discovered but not yet
   * processed for constraints
   */
  private Set<CGNode> discoveredNodes = HashSetFactory.make();

  /**
   * Set of calls (CallSiteReferences) that are created by entrypoints
   */
  final protected Set<CallSiteReference> entrypointCallSites = HashSetFactory.make();

  /**
   * The system of constraints used to build this graph
   */
  protected PropagationSystem system;

  /**
   * Algorithm used to solve the system of constraints
   */
  private IPointsToSolver solver;

  /**
   * The call graph under construction
   */
  protected final ExplicitCallGraph callGraph;

  /**
   * Singleton operator for assignments
   */
  protected final static AssignOperator assignOperator = new AssignOperator();

  /**
   * singleton operator for filter
   */
  public final FilterOperator filterOperator = new FilterOperator();

  /**
   * singleton operator for inverse filter
   */
  protected final InverseFilterOperator inverseFilterOperator = new InverseFilterOperator();

  /**
   * An object which interprets methods in context
   */
  private SSAContextInterpreter contextInterpreter;

  /**
   * A context selector which may use information derived from the
   * propagation-based dataflow.
   */
  protected ContextSelector contextSelector;

  /**
   * An object that abstracts how to model instances in the heap.
   */
  protected InstanceKeyFactory instanceKeyFactory;

  /**
   * Map: MethodReference -> Integer, caching the number of implementors in the
   * class hierarchy for a method reference
   */
  final private Map<MethodReference, Integer> cachedBoundMap = HashMapFactory.make();

  /**
   * Algorithmic choice for bounding number of evaluations of dispatch operators
   */
  private byte dispatchBoundHeuristic;

  /**
   * Algorithmic choice: should the GetfieldOperator and PutfieldOperator cache
   * its previous history to reduce work?
   */
  final private boolean rememberGetPutHistory = true;

  /**
   * @param cha
   *          governing class hierarchy
   * @param warnings
   *          an object to track analysis warnings
   * @param options
   *          governing call graph construction options
   * @param pointerKeyFactory
   *          factory which embodies pointer abstraction policy
   */
  protected PropagationCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options,
      PointerKeyFactory pointerKeyFactory) {
    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    this.cha = cha;
    this.options = options;
    if (Assertions.verifyAssertions) {
      // we need pointer keys to handle reflection
      Assertions._assert(pointerKeyFactory != null);
    }
    this.pointerKeyFactory = pointerKeyFactory;
    this.dispatchBoundHeuristic = options.getDispatchBoundHeuristic();
    callGraph = createEmptyCallGraph(cha, options);
    callGraph.init();
    callGraph.setInterpreter(contextInterpreter);
    JAVA_LANG_OBJECT = cha.lookupClass(TypeReference.JavaLangObject);
    JAVA_LANG_THROWABLE = cha.lookupClass(TypeReference.JavaLangThrowable);
  }

  protected ExplicitCallGraph createEmptyCallGraph(IClassHierarchy cha, AnalysisOptions options) {
    return new ExplicitCallGraph(cha, options);
  }

  protected byte getDefaultDispatchBoundHeuristic() {
    return AnalysisOptions.NO_DISPATCH_BOUND;
  }

  /**
   * @param klass
   * @return true iff the klass represents java.lang.Object
   */
  protected boolean isJavaLangObject(IClass klass) {
    return (klass.getReference().equals(TypeReference.JavaLangObject));
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.CallGraphBuilder#makeCallGraph(com.ibm.wala.ipa.callgraph.AnalysisOptions)
   */
  public CallGraph makeCallGraph(AnalysisOptions options) {
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    system = makeSystem(options);

    if (DEBUG_GENERAL) {
      Trace.println("Enter makeCallGraph!");
    }

    if (dispatchBoundHeuristic == AnalysisOptions.UNSPECIFIED) {
      dispatchBoundHeuristic = getDefaultDispatchBoundHeuristic();
    }

    if (DEBUG_GENERAL) {
      Trace.println("Initialized call graph");
    }

    system.setMinEquationsForTopSort(options.getMinEquationsForTopSort());
    system.setTopologicalGrowthFactor(options.getTopologicalGrowthFactor());
    system.setMaxEvalBetweenTopo(options.getMaxEvalBetweenTopo());

    discoveredNodes = HashSetFactory.make();
    discoveredNodes.add(callGraph.getFakeRootNode());

    // Set up the initially reachable methods and classes
    for (Iterator it = options.getEntrypoints().iterator(); it.hasNext();) {
      Entrypoint E = (Entrypoint) it.next();
      if (DEBUG_ENTRYPOINTS) {
        Trace.println("Entrypoint: " + E);
      }
      SSAAbstractInvokeInstruction call = E.addCall((AbstractRootMethod) callGraph.getFakeRootNode().getMethod());

      if (call == null) {
        Warnings.add(EntrypointResolutionWarning.create(E));
      } else {
        entrypointCallSites.add(call.getCallSite());
      }
    }

    customInit();

    solver = makeSolver();
    solver.solve();

    return callGraph;
  }

  protected PropagationSystem makeSystem(AnalysisOptions options) {
    return new PropagationSystem(callGraph, pointerKeyFactory, instanceKeyFactory, options.getSupportRefinement());
  }

  protected abstract IPointsToSolver makeSolver();

  /**
   * @author sfink
   * 
   * A warning for when we fail to resolve a call to an entrypoint
   */
  private static class EntrypointResolutionWarning extends Warning {

    final Entrypoint entrypoint;

    EntrypointResolutionWarning(Entrypoint entrypoint) {
      super(Warning.SEVERE);
      this.entrypoint = entrypoint;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + entrypoint;
    }

    public static EntrypointResolutionWarning create(Entrypoint entrypoint) {
      return new EntrypointResolutionWarning(entrypoint);
    }
  }

  protected void customInit() {
  }

  /**
   * Add constraints a node.
   * 
   * @return true iff any new constraints are added.
   */
  protected abstract boolean addConstraintsFromNode(CGNode n);

  /**
   * Add constraints from newly discovered nodes. Note: the act of adding
   * constraints may discover new nodes, so this routine is iterative.
   * 
   * @return true iff any new constraints are added.
   */
  protected boolean addConstraintsFromNewNodes() {
    boolean result = false;
    while (!discoveredNodes.isEmpty()) {
      Iterator<CGNode> it = discoveredNodes.iterator();
      discoveredNodes = HashSetFactory.make();
      while (it.hasNext()) {
        CGNode n = it.next();
        result |= addConstraintsFromNode(n);
      }
    }
    return result;
  }

  /**
   * @param node
   * @param valueNumber
   * @return the PointerKey that acts as a representative for the class of
   *         pointers that includes the local variable identified by the value
   *         number parameter.
   */
  public PointerKey getPointerKeyForLocal(CGNode node, int valueNumber) {
    return pointerKeyFactory.getPointerKeyForLocal(node, valueNumber);
  }

  /**
   * @param node
   * @param valueNumber
   * @return the PointerKey that acts as a representative for the class of
   *         pointers that includes the local variable identified by the value
   *         number parameter.
   */
  public FilteredPointerKey getFilteredPointerKeyForLocal(CGNode node, int valueNumber, FilteredPointerKey.TypeFilter filter) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(filter != null);
    }
    return pointerKeyFactory.getFilteredPointerKeyForLocal(node, valueNumber, filter);
  }

  public FilteredPointerKey getFilteredPointerKeyForLocal(CGNode node, int valueNumber, IClass filter) {
    return getFilteredPointerKeyForLocal(node, valueNumber, new FilteredPointerKey.SingleClassFilter(filter));
  }

  public FilteredPointerKey getFilteredPointerKeyForLocal(CGNode node, int valueNumber, InstanceKey filter) {
    return getFilteredPointerKeyForLocal(node, valueNumber, new FilteredPointerKey.SingleInstanceFilter(filter));
  }

  /**
   * @param node
   * @return the PointerKey that acts as a representative for the class of
   *         pointers that includes the return value for a node
   */
  public PointerKey getPointerKeyForReturnValue(CGNode node) {
    return pointerKeyFactory.getPointerKeyForReturnValue(node);
  }

  /**
   * @param node
   * @return the PointerKey that acts as a representative for the class of
   *         pointers that includes the exceptional return value
   */
  public PointerKey getPointerKeyForExceptionalReturnValue(CGNode node) {
    return pointerKeyFactory.getPointerKeyForExceptionalReturnValue(node);
  }

  /**
   * @return the PointerKey that acts as a representative for the class of
   *         pointers that includes the contents of the static field
   */
  public PointerKey getPointerKeyForStaticField(IField f) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(f != null, "null FieldReference");
    }
    return pointerKeyFactory.getPointerKeyForStaticField(f);
  }

  /**
   * @return the PointerKey that acts as a representation for the class of
   *         pointers that includes the given instance field. null if there's
   *         some problem.
   * @throws IllegalArgumentException
   *           if I is null
   * @throws IllegalArgumentException
   *           if field is null
   */
  public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField field) {
    if (field == null) {
      throw new IllegalArgumentException("field is null");
    }
    if (I == null) {
      throw new IllegalArgumentException("I is null");
    }
    IClass t = field.getDeclaringClass();
    IClass C = I.getConcreteType();
    if (!(C instanceof SyntheticClass)) {
      if (!getClassHierarchy().isSubclassOf(C, t)) {
        return null;
      }
    }

    return pointerKeyFactory.getPointerKeyForInstanceField(I, field);
  }

  /**
   * TODO: expand this API to differentiate between different array indices
   * 
   * @param I
   *          an InstanceKey representing an abstract array
   * @return the PointerKey that acts as a representation for the class of
   *         pointers that includes the given array contents, or null if none
   *         found.
   * @throws IllegalArgumentException
   *           if I is null
   */
  public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
    if (I == null) {
      throw new IllegalArgumentException("I is null");
    }
    if (Assertions.verifyAssertions) {
      IClass C = I.getConcreteType();
      if (!C.isArrayClass()) {
        Assertions._assert(false, "illegal arguments: " + I);
      }
    }
    return pointerKeyFactory.getPointerKeyForArrayContents(I);
  }

  /**
   * Handle assign of a particular exception instance into an exception variable
   * 
   * @param exceptionVar
   *          points-to set for a variable representing a caught exception
   * @param catchClasses
   *          set of TypeReferences that the exceptionVar may catch
   * @param e
   *          a particular exception instance
   */
  protected void assignInstanceToCatch(PointerKey exceptionVar, Set catchClasses, InstanceKey e) {

    if (catches(catchClasses, e.getConcreteType(), cha)) {
      system.newConstraint(exceptionVar, e);
    }
  }

  /**
   * Generate a set of constraints to represent assignment to an exception
   * variable in a catch clause. Note that we use FilterOperator to filter out
   * types that the exception handler doesn't catch.
   * 
   * @param exceptionVar
   *          points-to set for a variable representing a caught exception
   * @param catchClasses
   *          set of TypeReferences that the exceptionVar may catch
   * @param e
   *          points-to-set representing a thrown exception that might be
   *          caught.
   */
  protected void addAssignmentsForCatchPointerKey(PointerKey exceptionVar, Set catchClasses, PointerKey e) {
    if (DEBUG_GENERAL) {
      Trace.guardedPrintln("addAssignmentsForCatch: " + catchClasses, DEBUG_METHOD_SUBSTRING);
    }
    // this is tricky ... we want to filter based on a number of classes ... so
    // we can't
    // just used a FilteredPointerKey for the exceptionVar. Instead, we create a
    // new
    // "typed local" for each catch class, and coalesce the results using
    // assignment
    for (Iterator it2 = catchClasses.iterator(); it2.hasNext();) {
      TypeReference T = (TypeReference) it2.next();
      IClass C = cha.lookupClass(T);
      if (C == null) {
        Warnings.add(ExceptionLookupFailure.create(T));
      } else {
        if (C.getReference().equals(TypeReference.JavaLangThrowable)) {
          system.newConstraint(exceptionVar, assignOperator, e);
        } else {
          FilteredPointerKey typedException = TypedPointerKey.make(exceptionVar, C);
          system.newConstraint(typedException, filterOperator, e);
          system.newConstraint(exceptionVar, assignOperator, typedException);
          // System.err.println("TE " + typedException);
        }
      }
    }
  }

  /**
   * @author sfink
   * 
   * A warning for when we fail to resolve a call to an entrypoint
   */
  private static class ExceptionLookupFailure extends Warning {

    final TypeReference t;

    ExceptionLookupFailure(TypeReference t) {
      super(Warning.SEVERE);
      this.t = t;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + t;
    }

    public static ExceptionLookupFailure create(TypeReference t) {
      return new ExceptionLookupFailure(t);
    }
  }

  /**
   * @author sfink
   * 
   * A pointer key that delegates to an untyped variant, but adds a type filter
   */
  public final static class TypedPointerKey implements FilteredPointerKey {

    private final IClass type;

    private final PointerKey base;

    static TypedPointerKey make(PointerKey base, IClass type) {
      if (Assertions.verifyAssertions) {
        Assertions._assert(type != null);
      }
      return new TypedPointerKey(base, type);
    }

    private TypedPointerKey(PointerKey base, IClass type) {
      this.type = type;
      this.base = base;
      if (Assertions.verifyAssertions) {
        Assertions._assert(type != null);
        Assertions._assert(!(type instanceof FilteredPointerKey));
      }
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey#getTypeFilter()
     */
    public TypeFilter getTypeFilter() {
      return new SingleClassFilter(type);
    }

    @Override
    public boolean equals(Object obj) {
      // instanceof is OK because this class is final
      if (obj instanceof TypedPointerKey) {
        TypedPointerKey other = (TypedPointerKey) obj;
        return type.equals(other.type) && base.equals(other.base);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return 67931 * base.hashCode() + type.hashCode();
    }

    @Override
    public String toString() {
      return "{ " + base + " type: " + type + "}";
    }

    public PointerKey getBase() {
      return base;
    }
  }

  /**
   * @param catchClasses
   *          Set of TypeReference
   * @param klass
   *          an Exception Class
   * @return true iff klass is a subclass of some element of the Set
   * @throws IllegalArgumentException
   *           if catchClasses is null
   */
  public static boolean catches(Set catchClasses, IClass klass, IClassHierarchy cha) {
    if (catchClasses == null) {
      throw new IllegalArgumentException("catchClasses is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(catchClasses.size() > 0);
      // Assertions._assert(cha.isSubclassOf(klass,
      // TypeReference.JavaLangThrowable));
    }
    // quick shortcut
    if (catchClasses == THROWABLE_SET) {
      return true;
    }
    for (Iterator it = catchClasses.iterator(); it.hasNext();) {
      TypeReference T = (TypeReference) it.next();
      IClass C = cha.lookupClass(T);
      if (Assertions.verifyAssertions) {
        if (C == null) {
          // an unresolved catch type ... we should have raised a warning for
          // this earlier!
          continue;
        }
      }
      if (cha.isAssignableFrom(C, klass)) {
        return true;
      }
    }
    return false;
  }

  public static boolean representsNullType(InstanceKey key) {
    IClass cls  = key.getConcreteType();
    Language L = cls.getClassLoader().getLanguage();
    return L.isNullType( cls.getReference() );
  }

  /**
   * The FilterOperator is a filtered set-union. i.e. the LHS is `unioned' with
   * the RHS, but filtered by the set associated with this operator instance.
   * The filter is the set of InstanceKeys corresponding to the target type of
   * this cast. This is still monotonic.
   * 
   * LHS U= (RHS n k)
   * 
   * 
   * Unary op: <lhs>:= Cast_k( <rhs>)
   * 
   * (Again, technically a binary op -- see note for Assign)
   * 
   * TODO: these need to be canonicalized.
   * 
   */
  public class FilterOperator extends UnaryOperator implements IPointerOperator {

    protected FilterOperator() {
    }

    /*
     * @see com.ibm.wala.dataflow.UnaryOperator#evaluate(com.ibm.wala.dataflow.IVariable,
     *      com.ibm.wala.dataflow.IVariable)
     */
    @Override
    public byte evaluate(IVariable lhs, IVariable rhs) {

      PointsToSetVariable L = (PointsToSetVariable) lhs;
      PointsToSetVariable R = (PointsToSetVariable) rhs;
      FilteredPointerKey pk = (FilteredPointerKey) L.getPointerKey();

      // String Sx = "EVAL Filter " + L.getPointerKey() + " " +
      // R.getPointerKey();
      // System.err.println(Sx);

      boolean debug = false;
      if (DEBUG_FILTER) {
        String S = "EVAL Filter " + L.getPointerKey() + " " + R.getPointerKey();
        S += "\nEVAL      " + lhs + " " + rhs;
        debug = Trace.guardedPrintln(S, DEBUG_METHOD_SUBSTRING);
      }
      if (R.size() == 0) {
        return NOT_CHANGED;
      }

      boolean changed = false;
      FilteredPointerKey.TypeFilter filter = pk.getTypeFilter();
      changed = filter.addFiltered(system, L, R);

      // SJF: Do NOT propagate malleables through filters!
      // IntSet malleable = getMalleableInstances();
      // if (malleable != null) {
      // changed |= L.addAllInIntersection(R, malleable);
      // }

      if (DEBUG_FILTER) {
        if (debug) {
          Trace.println("RESULT " + L + (changed ? " (changed)" : ""));
        }
      }

      if (PropagationCallGraphBuilder.DEBUG_TRACK_INSTANCE) {
        if (changed) {
          if (L.contains(PropagationCallGraphBuilder.DEBUG_INSTANCE_KEY)
              && R.contains(PropagationCallGraphBuilder.DEBUG_INSTANCE_KEY)) {
            System.err.println("Filter: FLOW FROM " + R.getPointerKey() + " TO " + L.getPointerKey());
            Trace.println("Filter: FLOW FROM " + R.getPointerKey() + " TO " + L.getPointerKey());
            Trace.println("   filter: " + filter);
            InstanceKey I = system.getInstanceKey(DEBUG_INSTANCE_KEY);
            Trace.println("   I type: " + I.getConcreteType());
          }
        }
      }

      return changed ? CHANGED : NOT_CHANGED;
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.propagation.IPointerOperator#isComplex()
     */
    public boolean isComplex() {
      return false;
    }

    @Override
    public String toString() {
      return "Filter ";
    }

    @Override
    public boolean equals(Object obj) {
      // these objects are canonicalized for the duration of a
      // solve
      return this == obj;
    }

    @Override
    public int hashCode() {
      return 88651;
    }

  }

  public IClassHierarchy getClassHierarchy() {
    return cha;
  }

  public AnalysisOptions getOptions() {
    return options;
  }

  public IClass getJavaLangObject() {
    return JAVA_LANG_OBJECT;
  }

  public IClass getJavaLangThrowable() {
    return JAVA_LANG_THROWABLE;
  }

  public ExplicitCallGraph getCallGraph() {
    return callGraph;
  }

  /**
   * Subclasses must register the context interpreter before building a call
   * graph.
   */
  public void setContextInterpreter(SSAContextInterpreter interpreter) {
    contextInterpreter = interpreter;
    callGraph.setInterpreter(interpreter);
  }

  /*
   * @see com.ibm.detox.ipa.callgraph.CallGraphBuilder#getPointerAnalysis()
   */
  public PointerAnalysis getPointerAnalysis() {
    return system.extractPointerAnalysis(this);
  }

  public PointerFlowGraphFactory getPointerFlowGraphFactory() {
    return new PointerFlowGraphFactory();
  }

  public PropagationSystem getPropagationSystem() {
    return system;
  }

  public PointerKeyFactory getPointerKeyFactory() {
    return pointerKeyFactory;
  }

  public RTAContextInterpreter getContextInterpreter() {
    return contextInterpreter;
  }

  /**
   * A constant which constrains computation in getBoundOnNumberOfTargets
   */
  protected static final int CUTOFF = 10;

  /**
   * @param caller
   *          the caller node
   * @return the maximum number of nodes this call might resolve to, or -1 if
   *         there is no known bound
   */
  protected int getBoundOnNumberOfTargets(CGNode caller, CallSiteReference site) {
    switch (dispatchBoundHeuristic) {
    case AnalysisOptions.NO_DISPATCH_BOUND:
      return -1;
    case AnalysisOptions.SIMPLE_DISPATCH_BOUND:
      return getSimpleBoundOnNumberOfTargets(caller, site);
    case AnalysisOptions.CHA_DISPATCH_BOUND:
      return getBoundOnNumberOfTargetsFromIClassHierarchy(caller, site);
    default:
      Assertions.UNREACHABLE();
      return -1;
    }
  }

  /**
   * @param caller
   *          the caller node
   * @return the maximum number of nodes this call might resolve to, or -1 if
   *         there is no known bound
   */
  private int getSimpleBoundOnNumberOfTargets(CGNode caller, CallSiteReference site) {
    if (site.isInterface()) {
      return -1;
    }
    if (site.isVirtual()) {
      IClass klass = getClassHierarchy().lookupClass(site.getDeclaredTarget().getDeclaringClass());
      if (klass == null) {
        return -1;
      }
      IMethod targetMethod = klass.getMethod(site.getDeclaredTarget().getSelector());
      if (targetMethod.isPrivate() || targetMethod.isFinal()) {
        // there is at most one target method ... check how many contexts are
        // possible.
        targetMethod = options.getMethodTargetSelector().getCalleeTarget(caller, site, targetMethod.getDeclaringClass());
        if (targetMethod == null) {
          // uh oh .. hope someone will raise a warning somewhere.
          return -1;
        }
        return contextSelector.getBoundOnNumberOfTargets(caller, site, targetMethod);
      } else {
        return -1;
      }
    } else {
      // there is at most one target method ... check how many contexts are
      // possible.
      IMethod targetMethod = options.getMethodTargetSelector().getCalleeTarget(caller, site, null);
      if (targetMethod == null) {
        // uh oh .. hope someone will raise a warning somewhere.
        return -1;
      }
      return contextSelector.getBoundOnNumberOfTargets(caller, site, targetMethod);
    }
  }

  /**
   * @param caller
   *          the caller node
   * @return the maximum number of nodes this call might resolve to, or -1 if
   *         there is no known bound
   */
  private int getBoundOnNumberOfTargetsFromIClassHierarchy(CGNode caller, CallSiteReference site) {
    if (site.isDispatch()) {
      if (hasManyImplementors(site.getDeclaredTarget())) {
        return -1;
      }
      int nImplementations = findOrCreateBoundFromIClassHierarchy(site.getDeclaredTarget());
      if (nImplementations > CUTOFF) {
        return -1;
      }
      Iterator possibleTargets = getClassHierarchy().getPossibleTargets(site.getDeclaredTarget()).iterator();
      int result = 0;
      for (Iterator it = possibleTargets; it.hasNext();) {
        IMethod t = (IMethod) it.next();
        int b = contextSelector.getBoundOnNumberOfTargets(caller, site, t);
        if (Assertions.verifyAssertions) {
          if (b == 0) {
            Assertions._assert(false, contextSelector.getClass().toString());
          }
        }
        if (b == -1) {
          return -1;
        } else {
          result += b;
        }
        if (result > CUTOFF) {
          // give up
          return -1;
        }
      }
      if (result == 0) {
        result = -1;
      }
      return result;
    } else {
      // there is at most one target method ... check how many contexts are
      // possible.
      IMethod targetMethod = options.getMethodTargetSelector().getCalleeTarget(caller, site, null);
      if (targetMethod == null) {
        // uh oh .. hope someone will raise a warning somewhere.
        return -1;
      }
      return contextSelector.getBoundOnNumberOfTargets(caller, site, targetMethod);
    }
  }

  /**
   * @param m
   * @return true if we know a priori that there will be many implementors of m
   */
  private boolean hasManyImplementors(MemberReference m) {
    if (m.getDeclaringClass().equals(TypeReference.JavaLangObject)) {
      return true;
    }
    return false;
  }

  /**
   * TODO: optimize this by taking an IMethod rather than a MethodReference, to
   * avoid more class hierarchy lookups
   * 
   * @param m
   * @return the number of implementations of this method in the class hierarchy
   */
  protected int findOrCreateBoundFromIClassHierarchy(MethodReference m) {
    Integer I = cachedBoundMap.get(m);
    if (I == null) {
      int i = 0;
      for (Iterator it = getClassHierarchy().getPossibleTargets(m).iterator(); it.hasNext();) {
        i++;
        it.next();
      }
      I = new Integer(i);
      cachedBoundMap.put(m, I);
    }
    return I.intValue();
  }

  /**
   * @param caller
   *          the caller node
   * @param iKey
   *          an abstraction of the receiver of the call (or null if not
   *          applicable)
   * @return the CGNode to which this particular call should dispatch.
   */
  public CGNode getTargetForCall(CGNode caller, CallSiteReference site, InstanceKey iKey) {
    IClass recv = (iKey != null) ? iKey.getConcreteType() : null;
    IMethod targetMethod = options.getMethodTargetSelector().getCalleeTarget(caller, site, recv);

    // this most likely indicates an exclusion at work; the target selector
    // should have issued a warning
    if (targetMethod == null || targetMethod.isAbstract()) {
      return null;
    }

    Context targetContext = contextSelector.getCalleeTarget(caller, site, targetMethod, iKey);

    return getCallGraph().findOrCreateNode(targetMethod, targetContext);
  }

  /**
   * @return the context selector for this call graph builder
   */
  public ContextSelector getContextSelector() {
    return contextSelector;
  }

  public void setContextSelector(ContextSelector selector) {
    contextSelector = selector;
  }

  public InstanceKeyFactory getInstanceKeys() {
    return instanceKeyFactory;
  }

  public void setInstanceKeys(InstanceKeyFactory keys) {
    this.instanceKeyFactory = keys;
  }

  /**
   * @return the InstanceKey that acts as a representative for the class of
   *         objects that includes objects allocated at the given new
   *         instruction in the given node
   */
  public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
    return instanceKeyFactory.getInstanceKeyForAllocation(node, allocation);
  }

  /**
   * @param dim
   *          the dimension of the array whose instance we would like to model.
   *          dim == 0 represents the first dimension, e.g., the [Object;
   *          instances in [[Object; e.g., the [[Object; instances in [[[Object;
   *          dim == 1 represents the second dimension, e.g., the [Object
   *          instances in [[[Object;
   * @return the InstanceKey that acts as a representative for the class of
   *         array contents objects that includes objects allocated at the given
   *         new instruction in the given node
   */
  public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
    return instanceKeyFactory.getInstanceKeyForMultiNewArray(node, allocation, dim);
  }

  public InstanceKey getInstanceKeyForConstant(TypeReference type, Object S) {
    return instanceKeyFactory.getInstanceKeyForConstant(type, S);
  }

  public String getStringConstantForInstanceKey(InstanceKey I) {
    return instanceKeyFactory.getStringConstantForInstanceKey(I);
  }

  public InstanceKey getInstanceKeyForClassObject(TypeReference type) {
    return instanceKeyFactory.getInstanceKeyForClassObject(type);
  }

  public boolean haveAlreadyVisited(CGNode node) {
    return alreadyVisited.contains(node);
  }

  /**
   * @param node
   */
  protected void markAlreadyVisited(CGNode node) {
    alreadyVisited.add(node);
  }

  /**
   * record that we've discovered a node
   * 
   * @param node
   */
  public void markDiscovered(CGNode node) {
    discoveredNodes.add(node);
  }

  protected void markChanged(CGNode node) {
    alreadyVisited.remove(node);
    discoveredNodes.add(node);
  }

  protected boolean wasChanged(CGNode node) {
    return discoveredNodes.contains(node) && !alreadyVisited.contains(node);
  }

  /**
   * Binary op: <dummy>:= ArrayLoad( &lt;arrayref>) Side effect: Creates new
   * equations.
   */
  public final class ArrayLoadOperator extends UnarySideEffect implements IPointerOperator {
    protected final MutableIntSet priorInstances = rememberGetPutHistory ? IntSetUtil.make() : null;

    @Override
    public String toString() {
      return "ArrayLoad";
    }

    public ArrayLoadOperator(PointsToSetVariable def) {
      super(def);
      system.registerFixedSet(def, this);
    }

    @Override
    public byte evaluate(IVariable rhs) {
      boolean debug = false;
      if (DEBUG_ARRAY_LOAD) {
        PointsToSetVariable ref = (PointsToSetVariable) rhs;
        PointsToSetVariable def = getFixedSet();
        String S = "EVAL ArrayLoad " + ref.getPointerKey() + " " + def.getPointerKey();
        debug = Trace.guardedPrintln(S, DEBUG_METHOD_SUBSTRING);
        if (debug) {
          Trace.println("EVAL ArrayLoad " + def + " " + rhs);
          if (priorInstances != null) {
            Trace.println("prior instances: " + priorInstances + " " + priorInstances.getClass());
          }
        }
      }

      PointsToSetVariable ref = (PointsToSetVariable) rhs;
      if (ref.size() == 0) {
        return NOT_CHANGED;
      }
      final PointerKey object = ref.getPointerKey();

      PointsToSetVariable def = getFixedSet();
      final PointerKey dVal = def.getPointerKey();

      final boolean finalDebug = debug;
      final MutableBoolean sideEffect = new MutableBoolean();
      IntSetAction action = new IntSetAction() {
        public void act(int i) {
          InstanceKey I = system.getInstanceKey(i);
          if (!I.getConcreteType().isArrayClass()) {
            return;
          }
          TypeReference C = I.getConcreteType().getReference().getArrayElementType();
          if (C.isPrimitiveType()) {
            return;
          }
          PointerKey p = getPointerKeyForArrayContents(I);
          if (p == null) {
            return;
          }

          if (DEBUG_ARRAY_LOAD && finalDebug) {
            Trace.println("ArrayLoad add assign: " + dVal + " " + p);
          }
          sideEffect.b |= system.newFieldRead(dVal, assignOperator, p, object);
        }
      };
      if (priorInstances != null) {
        ref.getValue().foreachExcluding(priorInstances, action);
        priorInstances.addAll(ref.getValue());
      } else {
        ref.getValue().foreach(action);
      }
      byte sideEffectMask = sideEffect.b ? (byte) SIDE_EFFECT_MASK : 0;
      return (byte) (NOT_CHANGED | sideEffectMask);
    }

    @Override
    public int hashCode() {
      return 9871 + super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return super.equals(o);
    }

    @Override
    protected boolean isLoadOperator() {
      return true;
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.propagation.IPointerOperator#isComplex()
     */
    public boolean isComplex() {
      return true;
    }
  }

  /**
   * Binary op: <dummy>:= ArrayStore( &lt;arrayref>) Side effect: Creates new
   * equations.
   */
  public final class ArrayStoreOperator extends UnarySideEffect implements IPointerOperator {
    @Override
    public String toString() {
      return "ArrayStore";
    }

    public ArrayStoreOperator(PointsToSetVariable val) {
      super(val);
      system.registerFixedSet(val, this);
    }

    @Override
    public byte evaluate(IVariable rhs) {
      boolean debug = false;
      if (DEBUG_ARRAY_STORE) {
        PointsToSetVariable ref = (PointsToSetVariable) rhs;
        PointsToSetVariable val = getFixedSet();
        String S = "EVAL ArrayStore " + ref.getPointerKey() + " " + val.getPointerKey();
        debug = Trace.guardedPrintln(S, DEBUG_METHOD_SUBSTRING);
        if (debug) {
          Trace.println("EVAL ArrayStore " + rhs + " " + getFixedSet());
        }
      }

      PointsToSetVariable ref = (PointsToSetVariable) rhs;
      if (ref.size() == 0) {
        return NOT_CHANGED;
      }
      PointerKey object = ref.getPointerKey();

      PointsToSetVariable val = getFixedSet();
      PointerKey pVal = val.getPointerKey();

      List<InstanceKey> instances = system.getInstances(ref.getValue());
      boolean sideEffect = false;
      for (Iterator<InstanceKey> it = instances.iterator(); it.hasNext();) {
        InstanceKey I = it.next();
        if (!I.getConcreteType().isArrayClass()) {
          continue;
        }
        TypeReference C = I.getConcreteType().getReference().getArrayElementType();
        if (C.isPrimitiveType()) {
          continue;
        }
        IClass contents = getClassHierarchy().lookupClass(C);
        if (Assertions.verifyAssertions) {
          if (contents == null) {
            Assertions._assert(false, "null type for " + C + " " + I.getConcreteType());
          }
        }
        PointerKey p = getPointerKeyForArrayContents(I);
        if (DEBUG_ARRAY_STORE && debug) {
          Trace.println("ArrayStore add filtered-assign: " + p + " " + pVal);
        }

        // note that the following is idempotent
        if (isJavaLangObject(contents)) {
          sideEffect |= system.newFieldWrite(p, assignOperator, pVal, object);
        } else {
          sideEffect |= system.newFieldWrite(p, filterOperator, pVal, object);
        }
      }
      byte sideEffectMask = sideEffect ? (byte) SIDE_EFFECT_MASK : 0;
      return (byte) (NOT_CHANGED | sideEffectMask);
    }

    @Override
    public int hashCode() {
      return 9859 + super.hashCode();
    }

    public boolean isComplex() {
      return true;
    }

    @Override
    public boolean equals(Object o) {
      return super.equals(o);
    }

    @Override
    protected boolean isLoadOperator() {
      return false;
    }
  }

  /**
   * Binary op: <dummy>:= GetField( <ref>) Side effect: Creates new equations.
   */
  public class GetFieldOperator extends UnarySideEffect implements IPointerOperator {
    private final IField field;

    protected final MutableIntSet priorInstances = rememberGetPutHistory ? IntSetUtil.make() : null;

    public GetFieldOperator(IField field, PointsToSetVariable def) {
      super(def);
      this.field = field;
      system.registerFixedSet(def, this);
    }

    @Override
    public String toString() {
      return "GetField " + getField() + "," + getFixedSet().getPointerKey();
    }

    @Override
    public byte evaluate(IVariable rhs) {
      if (DEBUG_GET) {
        String S = "EVAL GetField " + getField() + " " + getFixedSet().getPointerKey() + " "
            + ((PointsToSetVariable) rhs).getPointerKey() + getFixedSet() + " " + rhs;
        Trace.guardedPrintln(S, DEBUG_METHOD_SUBSTRING);
      }

      PointsToSetVariable ref = (PointsToSetVariable) rhs;
      if (ref.size() == 0) {
        return NOT_CHANGED;
      }
      final PointerKey object = ref.getPointerKey();
      PointsToSetVariable def = getFixedSet();
      final PointerKey dVal = def.getPointerKey();

      IntSet value = filterInstances(ref.getValue());
      if (DEBUG_GET) {
        Trace.println("filtered value: " + value + " " + value.getClass());
        if (priorInstances != null) {
          Trace.println("prior instances: " + priorInstances + " " + priorInstances.getClass());
        }
      }
      final MutableBoolean sideEffect = new MutableBoolean();
      IntSetAction action = new IntSetAction() {
        public void act(int i) {
          InstanceKey I = system.getInstanceKey(i);
	  if (! representsNullType(I)) {
	    PointerKey p = getPointerKeyForInstanceField(I, getField());

	    if (p != null) {
	      if (DEBUG_GET) {
		String S = "Getfield add constraint " + dVal + " " + p;
		Trace.guardedPrintln(S, DEBUG_METHOD_SUBSTRING);
	      }
	      sideEffect.b |= system.newFieldRead(dVal, assignOperator, p, object);
	    }
	  }
	}
      };
      if (priorInstances != null) {
        // temp for performance debugging
        // MutableSparseIntSet temp = new MutableSparseIntSet();
        // temp.addAll(value);
        // BitVectorIntSet b = new BitVectorIntSet();
        // b.addAll(priorInstances);
        // temp.removeAll(b);
        // if (temp.size() == 317) {
        //        
        // System.err.println("SIZE GG" + temp.size());
        // }
        value.foreachExcluding(priorInstances, action);
        priorInstances.addAll(value);
      } else {
        value.foreach(action);
      }
      byte sideEffectMask = sideEffect.b ? (byte) SIDE_EFFECT_MASK : 0;
      return (byte) (NOT_CHANGED | sideEffectMask);
    }

    /**
     * Subclasses can override as needed
     * 
     * @param value
     * @return an IntSet
     */
    protected IntSet filterInstances(IntSet value) {
      return value;
    }

    @Override
    public int hashCode() {
      return 9857 * getField().hashCode() + getFixedSet().hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof GetFieldOperator) {
        GetFieldOperator other = (GetFieldOperator) o;
        return getField().equals(other.getField()) && getFixedSet().equals(other.getFixedSet());
      } else {
        return false;
      }
    }

    /**
     * @return Returns the field.
     */
    protected IField getField() {
      return field;
    }

    @Override
    protected boolean isLoadOperator() {
      return true;
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.propagation.IPointerOperator#isComplex()
     */
    public boolean isComplex() {
      return true;
    }
  }

  /**
   * Operator that represents a putfield
   */
  public class PutFieldOperator extends UnarySideEffect implements IPointerOperator {
    private final IField field;

    protected final MutableIntSet priorInstances = rememberGetPutHistory ? IntSetUtil.make() : null;

    @Override
    public String toString() {
      return "PutField" + getField();
    }

    public PutFieldOperator(IField field, PointsToSetVariable val) {
      super(val);
      this.field = field;
      system.registerFixedSet(val, this);
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.propagation.IPointerOperator#isComplex()
     */
    public boolean isComplex() {
      return true;
    }

    @Override
    public byte evaluate(IVariable rhs) {
      if (DEBUG_PUT) {
        String S = "EVAL PutField " + getField() + " " + (getFixedSet()).getPointerKey() + " "
            + ((PointsToSetVariable) rhs).getPointerKey() + getFixedSet() + " " + rhs;
        Trace.guardedPrintln(S, DEBUG_METHOD_SUBSTRING);
      }

      PointsToSetVariable ref = (PointsToSetVariable) rhs;
      if (ref.size() == 0) {
        return NOT_CHANGED;
      }
      final PointerKey object = ref.getPointerKey();

      PointsToSetVariable val = getFixedSet();
      final PointerKey pVal = val.getPointerKey();
      IntSet value = ref.getValue();
      value = filterInstances(value);
      final UnaryOperator assign = getPutAssignmentOperator();
      if (assign == null) {
        Assertions.UNREACHABLE();
      }
      final MutableBoolean sideEffect = new MutableBoolean();
      IntSetAction action = new IntSetAction() {
        public void act(int i) {
          InstanceKey I = system.getInstanceKey(i);
	  if (! representsNullType(I)) {
	    if (DEBUG_PUT) {
	      String S = "Putfield consider instance " + I;
	      Trace.guardedPrintln(S, DEBUG_METHOD_SUBSTRING);
	    }
	    PointerKey p = getPointerKeyForInstanceField(I, getField());
	    if (DEBUG_PUT) {
	      String S = "Putfield add constraint " + p + " " + pVal;
	      Trace.guardedPrintln(S, DEBUG_METHOD_SUBSTRING);
	    }
	    sideEffect.b |= system.newFieldWrite(p, assign, pVal, object);
	  }
	}
      };
      if (priorInstances != null) {
        value.foreachExcluding(priorInstances, action);
        priorInstances.addAll(value);
      } else {
        value.foreach(action);
      }
      byte sideEffectMask = sideEffect.b ? (byte) SIDE_EFFECT_MASK : 0;
      return (byte) (NOT_CHANGED | sideEffectMask);
    }

    /**
     * Subclasses can override as needed
     * 
     * @param value
     * @return an IntSet
     */
    protected IntSet filterInstances(IntSet value) {
      return value;
    }

    @Override
    public int hashCode() {
      return 9857 * getField().hashCode() + getFixedSet().hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o.getClass().equals(getClass())) {
        PutFieldOperator other = (PutFieldOperator) o;
        return getField().equals(other.getField()) && getFixedSet().equals(other.getFixedSet());
      } else {
        return false;
      }
    }

    /**
     * subclasses (e.g. XTA) can override this to enforce a filtered assignment.
     * returns null if there's a problem.
     */
    public UnaryOperator getPutAssignmentOperator() {
      return assignOperator;
    }

    /**
     * @return Returns the field.
     */
    protected IField getField() {
      return field;
    }

    @Override
    protected boolean isLoadOperator() {
      return false;
    }
  }

  /**
   * Update the points-to-set for a field to include a particular instance key.
   */
  public final class InstancePutFieldOperator extends UnaryOperator implements IPointerOperator {
    final private IField field;

    final private InstanceKey instance;

    protected final MutableIntSet priorInstances = rememberGetPutHistory ? IntSetUtil.make() : null;

    @Override
    public String toString() {
      return "InstancePutField" + field;
    }

    public InstancePutFieldOperator(IField field, InstanceKey instance) {
      this.field = field;
      this.instance = instance;
    }

    /**
     * Simply add the instance to each relevant points-to set.
     */
    @Override
    public byte evaluate(IVariable dummyLHS, IVariable var) {
      PointsToSetVariable ref = (PointsToSetVariable) var;
      if (ref.size() == 0) {
        return NOT_CHANGED;
      }
      IntSet value = ref.getValue();
      final MutableBoolean sideEffect = new MutableBoolean();
      IntSetAction action = new IntSetAction() {
        public void act(int i) {
          InstanceKey I = system.getInstanceKey(i);
	  if (! representsNullType(I)) {
	    PointerKey p = getPointerKeyForInstanceField(I, field);
	    sideEffect.b |= system.newConstraint(p, instance);
	  }
	}
      };
      if (priorInstances != null) {
        value.foreachExcluding(priorInstances, action);
        priorInstances.addAll(value);
      } else {
        value.foreach(action);
      }
      byte sideEffectMask = sideEffect.b ? (byte) SIDE_EFFECT_MASK : 0;
      return (byte) (NOT_CHANGED | sideEffectMask);
    }

    @Override
    public int hashCode() {
      return field.hashCode() + 9839 * instance.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof InstancePutFieldOperator) {
        InstancePutFieldOperator other = (InstancePutFieldOperator) o;
        return field.equals(other.field) && instance.equals(other.instance);
      } else {
        return false;
      }
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.propagation.IPointerOperator#isComplex()
     */
    public boolean isComplex() {
      return true;
    }
  }

  /**
   * Update the points-to-set for an array contents to include a particular
   * instance key.
   */
  public final class InstanceArrayStoreOperator extends UnaryOperator implements IPointerOperator {
    final private InstanceKey instance;

    protected final MutableIntSet priorInstances = rememberGetPutHistory ? IntSetUtil.make() : null;

    @Override
    public String toString() {
      return "InstanceArrayStore ";
    }

    public InstanceArrayStoreOperator(InstanceKey instance) {
      this.instance = instance;
    }

    /**
     * Simply add the instance to each relevant points-to set.
     */
    @Override
    public byte evaluate(IVariable dummyLHS, IVariable var) {
      PointsToSetVariable arrayref = (PointsToSetVariable) var;
      if (arrayref.size() == 0) {
        return NOT_CHANGED;
      }
      IntSet value = arrayref.getValue();
      final MutableBoolean sideEffect = new MutableBoolean();
      IntSetAction action = new IntSetAction() {
        public void act(int i) {
          InstanceKey I = system.getInstanceKey(i);
          if (!I.getConcreteType().isArrayClass()) {
            return;
          }
          TypeReference C = I.getConcreteType().getReference().getArrayElementType();
          if (C.isPrimitiveType()) {
            return;
          }
          IClass contents = getClassHierarchy().lookupClass(C);
          if (Assertions.verifyAssertions) {
            if (contents == null) {
              Assertions._assert(false, "null type for " + C + " " + I.getConcreteType());
            }
          }
          PointerKey p = getPointerKeyForArrayContents(I);
          if (contents.isInterface()) {
            if (getClassHierarchy().implementsInterface(instance.getConcreteType(), contents.getReference())) {
              sideEffect.b |= system.newConstraint(p, instance);
            }
          } else {
            if (getClassHierarchy().isSubclassOf(instance.getConcreteType(), contents)) {
              sideEffect.b |= system.newConstraint(p, instance);
            }
          }
        }
      };
      if (priorInstances != null) {
        value.foreachExcluding(priorInstances, action);
        priorInstances.addAll(value);
      } else {
        value.foreach(action);
      }
      byte sideEffectMask = sideEffect.b ? (byte) SIDE_EFFECT_MASK : 0;
      return (byte) (NOT_CHANGED | sideEffectMask);
    }

    @Override
    public int hashCode() {
      return 9839 * instance.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof InstanceArrayStoreOperator) {
        InstanceArrayStoreOperator other = (InstanceArrayStoreOperator) o;
        return instance.equals(other.instance);
      } else {
        return false;
      }
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.propagation.IPointerOperator#isComplex()
     */
    public boolean isComplex() {
      return true;
    }
  }

  protected MutableIntSet getMutableInstanceKeysForClass(IClass klass) {
    return system.cloneInstanceKeysForClass(klass);
  }

  protected IntSet getInstanceKeysForClass(IClass klass) {
    return system.getInstanceKeysForClass(klass);
  }

  /**
   * @param klass
   *          a class
   * @return an int set which represents the subset of S that correspond to
   *         subtypes of klass
   */
  protected IntSet filterForClass(IntSet S, IClass klass) {
    MutableIntSet filter = null;
    if (klass.getReference().equals(TypeReference.JavaLangObject)) {
      return S;
    } else {
      filter = getMutableInstanceKeysForClass(klass);

      boolean debug = false;
      if (DEBUG_FILTER) {
        String s = "klass     " + klass;
        debug = Trace.guardedPrintln(s, DEBUG_METHOD_SUBSTRING);
        if (debug) {
          Trace.println("initial filter    " + filter);
        }
      }
      filter.intersectWith(S);

      if (DEBUG_FILTER && debug) {
        Trace.println("final filter    " + filter);
      }
    }
    return filter;
  }

  protected class InverseFilterOperator extends FilterOperator {
    public InverseFilterOperator() {
      super();
    }

    @Override
    public String toString() {
      return "InverseFilter";
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.propagation.IPointerOperator#isComplex()
     */
    @Override
    public boolean isComplex() {
      return false;
    }

    /*
     * simply check if rhs contains a malleable.
     * 
     * @see com.ibm.wala.dataflow.UnaryOperator#evaluate(com.ibm.wala.dataflow.IVariable,
     *      com.ibm.wala.dataflow.IVariable)
     */
    @Override
    public byte evaluate(IVariable lhs, IVariable rhs) {

      PointsToSetVariable L = (PointsToSetVariable) lhs;
      PointsToSetVariable R = (PointsToSetVariable) rhs;
      FilteredPointerKey pk = (FilteredPointerKey) L.getPointerKey();
      FilteredPointerKey.TypeFilter filter = pk.getTypeFilter();

      boolean debug = false;
      if (DEBUG_FILTER) {
        String S = "EVAL InverseFilter/" + filter + " " + L.getPointerKey() + " " + R.getPointerKey();
        S += "\nEVAL      " + lhs + " " + rhs;
        debug = Trace.guardedPrintln(S, DEBUG_METHOD_SUBSTRING);
      }
      if (R.size() == 0) {
        return NOT_CHANGED;
      }

      boolean changed = filter.addInverseFiltered(system, L, R);

      if (DEBUG_FILTER) {
        if (debug) {
          Trace.println("RESULT " + L + (changed ? " (changed)" : ""));
        }
      }
      return changed ? CHANGED : NOT_CHANGED;
    }
  }


  protected IPointsToSolver getSolver() {
    return solver;
  }

  /**
   * Add constraints when the interpretation of a node changes (e.g. reflection)
   */
  public void addConstraintsFromChangedNode(CGNode node) {
    unconditionallyAddConstraintsFromNode(node);
  }

  protected abstract boolean unconditionallyAddConstraintsFromNode(CGNode node);

  protected static class MutableBoolean {
    // a horrendous hack since we don't have closures
    boolean b = false;
  };

}
