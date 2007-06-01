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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.fixedpoint.impl.AbstractOperator;
import com.ibm.wala.fixedpoint.impl.AbstractStatement;
import com.ibm.wala.fixedpoint.impl.DefaultFixedPointSolver;
import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixedpoint.impl.UnaryStatement;
import com.ibm.wala.fixedpoint.impl.Worklist;
import com.ibm.wala.fixpoint.IFixedPointSystem;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.fixpoint.IntSetVariable;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder.FilterOperator;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyWarning;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.ReferenceCleanser;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.debug.VerboseAction;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.heapTrace.HeapTracer;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * System of constraints that define propagation for call graph construction
 * 
 * @author sfink
 */
public class PropagationSystem extends DefaultFixedPointSolver {

  private final static boolean DEBUG = false;

  private final static boolean DEBUG_MEMORY = false;

  private static int DEBUG_MEM_COUNTER = 0;

  private final static int DEBUG_MEM_INTERVAL = 5;

  /**
   * object that tracks points-to sets
   */
  protected final PointsToMap pointsToMap = new PointsToMap();

  /**
   * Implementation of the underlying dataflow graph
   */
  private final PropagationGraph flowGraph = new PropagationGraph();

  /**
   * bijection from InstanceKey <=>Integer
   */
  protected final MutableMapping<InstanceKey> instanceKeys = new MutableMapping<InstanceKey>();

  /**
   * An object to track analysis warnings
   */
  private final WarningSet warnings;

  /**
   * A mapping from IClass -> MutableSharedBitVectorIntSet The range represents
   * the instance keys that correspond to a given class. This mapping is used to
   * filter sets based on declared types; e.g., in cast constraints
   */
  private Map<IClass, MutableIntSet> class2InstanceKey = HashMapFactory.make();

  /**
   * An abstraction of the pointer analysis result
   */
  private PointerAnalysis pointerAnalysis;

  /**
   * Meta-data regarding how pointers are modelled.
   */
  private final PointerKeyFactory pointerKeyFactory;

  /**
   * Meta-data regarding how instances are modelled.
   */
  private final InstanceKeyFactory instanceKeyFactory;

  /**
   * When doing unification, we must also updated the fixed sets in unary side
   * effects.
   * 
   * This maintains a map from PointsToSetVariable -> Set<UnarySideEffect>
   */
  final private Map<PointsToSetVariable,Set<UnarySideEffect>> fixedSetMap = HashMapFactory.make();

  /**
   * Governing call graph;
   */
  protected final CallGraph cg;

  private int verboseInterval = DEFAULT_VERBOSE_INTERVAL;

  private int periodicMaintainInterval = DEFAULT_PERIODIC_MAINTENANCE_INTERVAL;

  /**
   * @param pointerKeyFactory
   * @param instanceKeyFactory
   * @param refinement
   * @param warnings
   */
  public PropagationSystem(CallGraph cg, PointerKeyFactory pointerKeyFactory, InstanceKeyFactory instanceKeyFactory, boolean refinement,
      WarningSet warnings) {
    this.cg = cg;
    this.pointerKeyFactory = pointerKeyFactory;
    this.instanceKeyFactory = instanceKeyFactory;
    this.warnings = warnings;
  }

  /**
   * @return an object which encapsulates the pointer analysis result
   */
  public PointerAnalysis makePointerAnalysis(PropagationCallGraphBuilder builder) {
    return new PointerAnalysisImpl(builder, cg, pointsToMap, instanceKeys, pointerKeyFactory, instanceKeyFactory);
  }

  protected void registerFixedSet(PointsToSetVariable p, UnarySideEffect s) {
    Set<UnarySideEffect> set = MapUtil.findOrCreateSet(fixedSetMap, p);
    set.add(s);
  }

  protected void updateSideEffects(PointsToSetVariable p, PointsToSetVariable rep) {
    Set<UnarySideEffect> set = fixedSetMap.get(p);
    if (set != null) {
      for (Iterator it = set.iterator(); it.hasNext();) {
        UnarySideEffect s = (UnarySideEffect) it.next();
        s.replaceFixedSet(rep);
      }
      Set<UnarySideEffect> s2 = MapUtil.findOrCreateSet(fixedSetMap, rep);
      s2.addAll(set);
      fixedSetMap.remove(p);
    }
  }

  /**
   * Keep this method private .. this returns the actual backing set for the
   * class, which we do not want to expose to clients.
   */
  private MutableIntSet findOrCreateSparseSetForClass(IClass klass) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(klass.getReference() != TypeReference.JavaLangObject);
    }
    MutableIntSet result = class2InstanceKey.get(klass);
    if (result == null) {
      result = IntSetUtil.getDefaultIntSetFactory().make();
      class2InstanceKey.put(klass, result);
    }
    return result;
  }

  /**
   * @return a set of integers representing the instance keys that correspond to
   *         a given class. This method creates a new set, which the caller may
   *         bash at will.
   */
  MutableIntSet cloneInstanceKeysForClass(IClass klass) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(klass.getReference() != TypeReference.JavaLangObject);
    }
    MutableIntSet set = class2InstanceKey.get(klass);
    if (set == null) {
      return IntSetUtil.getDefaultIntSetFactory().make();
    } else {
      // return a copy.
      return IntSetUtil.getDefaultIntSetFactory().makeCopy(set);
    }
  }

  /**
   * @return a set of integers representing the instance keys that correspond to
   *         a given class, or null if there are none.
   * @throws IllegalArgumentException  if klass is null
   */
  public IntSet getInstanceKeysForClass(IClass klass) {
    if (klass == null) {
      throw new IllegalArgumentException("klass is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(klass.getReference() != TypeReference.JavaLangObject);
    }
    return class2InstanceKey.get(klass);
  }

  /**
   * @return the instance key numbered with index i
   */
  public InstanceKey getInstanceKey(int i) {
    return instanceKeys.getMappedObject(i);
  }

  /**
   * TODO: optimize; this may be inefficient;
   * 
   * @return an List of instance keys corresponding to the integers in a set
   */
  List<InstanceKey> getInstances(IntSet set) {
    LinkedList<InstanceKey> result = new LinkedList<InstanceKey>();
    int i = 0;
    for (IntIterator it = set.intIterator(); it.hasNext(); i++) {
      int j = it.next();
      result.add(getInstanceKey(j));
    }
    return result;
  }

  @Override
  protected void initializeVariables() {
    // don't have to do anything; all variables initialized
    // by default to TOP (the empty set);
  }

  /**
   * record that a particular points-to-set is represented implicitly.
   * 
   * @param key
   */
  public void recordImplicitPointsToSet(PointerKey key) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(key != null);
    }
    pointsToMap.recordImplicit(key);
  }

  /**
   * If key is unifed, returns the representative
   * 
   * @param key
   * @return the dataflow variable that tracks the points-to set for key
   */
  public PointsToSetVariable findOrCreatePointsToSet(PointerKey key) {

    if (Assertions.verifyAssertions) {
      Assertions._assert(key != null);
    }
    PointsToSetVariable result = pointsToMap.getPointsToSet(key);
    if (result == null) {
      result = new PointsToSetVariable(key);
      pointsToMap.put(key, result);
    } else {
      // check that the filter for this variable remains unique
      if (!pointsToMap.isUnified(key) && key instanceof FilteredPointerKey) {
        PointerKey pk = result.getPointerKey();
        if (!(pk instanceof FilteredPointerKey)) {
          // add a filter for all future evaluations.
          // this is tricky, but the logic is OK .. any constraints that need
          // the filter will see it ...
          // CALLERS MUST BE EXTRA CAREFUL WHEN DEALING WITH UNIFICATION!
          result.setPointerKey(key);
          pk = key;
        }
        if (Assertions.verifyAssertions) {
          FilteredPointerKey fpk = (FilteredPointerKey) pk;
          if (fpk == null) {
            Assertions.UNREACHABLE("fpk is null");
          }
          if (key == null) {
            Assertions.UNREACHABLE("key is null");
          }
          if (fpk.getTypeFilter() == null) {
            Assertions.UNREACHABLE("fpk.getTypeFilter() is null");
          }
          if (!fpk.getTypeFilter().equals(((FilteredPointerKey) key).getTypeFilter())) {
	    Assertions.UNREACHABLE("Cannot use filter " + ((FilteredPointerKey) key).getTypeFilter() + " for " + key + ": previously created different filter "  + fpk.getTypeFilter());
          }
        }
      }
    }
    return result;
  }

  public int findOrCreateIndexForInstanceKey(InstanceKey key) {
    int result = instanceKeys.getMappedIndex(key);
    if (result == -1) {
      result = instanceKeys.add(key);
    }
    if (DEBUG) {
      Trace.println("getIndexForInstanceKey " + key + " " + result);
    }
    return result;
  }

  /**
   * NB: this is idempotent ... if the given constraint exists, it will not be
   * added to the system; however, this will be more expensive since it must
   * check if the constraint pre-exits.
   * 
   * @return true iff the system changes
   */
  public boolean newConstraint(PointerKey lhs, UnaryOperator op, PointerKey rhs) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(lhs != null);
      Assertions._assert(op != null);
      Assertions._assert(rhs != null);
    }
    if (DEBUG) {
      Trace.println("Add constraint A: " + lhs + " " + op + " " + rhs);
    }
    PointsToSetVariable L = findOrCreatePointsToSet(lhs);
    PointsToSetVariable R = findOrCreatePointsToSet(rhs);
    if (op instanceof FilterOperator) {
      // we do not want to revert the lhs to pre-transitive form;
      // we instead want to check in the outer loop of the pre-transitive
      // solver if the value of L changes.
      pointsToMap.recordTransitiveRoot(L.getPointerKey());
      if (Assertions.verifyAssertions) {
        if (!(L.getPointerKey() instanceof FilteredPointerKey)) {
          Assertions.UNREACHABLE("expected filtered lhs " + L.getPointerKey() + " " + L.getPointerKey().getClass() + " " + lhs
              + " " + lhs.getClass());
        }
      }
    }
    return newStatement(L, op, R, true, true);
  }

  public void newConstraint(PointerKey lhs, AbstractOperator op, PointerKey rhs) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(lhs != null);
      Assertions._assert(op != null);
      Assertions._assert(rhs != null);
    }
    if (DEBUG) {
      Trace.println("Add constraint A: " + lhs + " " + op + " " + rhs);
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(!pointsToMap.isUnified(lhs));
      Assertions._assert(!pointsToMap.isUnified(rhs));
    }
    IntSetVariable L = findOrCreatePointsToSet(lhs);
    IntSetVariable R = findOrCreatePointsToSet(rhs);
    newStatement(L, op, new IVariable[] { R }, true, true);
  }

  public void newConstraint(PointerKey lhs, AbstractOperator op, PointerKey rhs1, PointerKey rhs2) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(lhs != null);
      Assertions._assert(op != null);
      Assertions._assert(rhs1 != null);
      Assertions._assert(rhs2 != null);
    }
    if (DEBUG) {
      Trace.println("Add constraint A: " + lhs + " " + op + " " + rhs1 + ", " + rhs2);
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(!pointsToMap.isUnified(lhs));
      Assertions._assert(!pointsToMap.isUnified(rhs1));
      Assertions._assert(!pointsToMap.isUnified(rhs2));
    }
    IntSetVariable L = findOrCreatePointsToSet(lhs);
    IntSetVariable R1 = findOrCreatePointsToSet(rhs1);
    IntSetVariable R2 = findOrCreatePointsToSet(rhs2);
    newStatement(L, op, R1, R2, true, true);
  }

  /**
   * @return true iff the system changes
   */
  public boolean newFieldWrite(PointerKey lhs, UnaryOperator op, PointerKey rhs, PointerKey container) {
    return newConstraint(lhs, op, rhs);
  }

  /**
   * @return true iff the system changes
   */
  public boolean newFieldRead(PointerKey lhs, UnaryOperator op, PointerKey rhs, PointerKey container) {
    return newConstraint(lhs, op, rhs);
  }

  /**
   * @return true iff the system changes
   */
  public boolean newConstraint(PointerKey lhs, InstanceKey value) {
    if (DEBUG) {
      Trace.println("Add constraint B: " + lhs + " U= " + value);
    }
    pointsToMap.recordTransitiveRoot(lhs);

    // we don't actually add a constraint.
    // instead, we immediately add the value to the points-to set.
    // This works since the solver is monotonic with TOP = {}
    IntSetVariable L = findOrCreatePointsToSet(lhs);
    int index = findOrCreateIndexForInstanceKey(value);
    if (L.contains(index)) {
      // a no-op
      return false;
    } else {
      L.add(index);

      // also register that we have an instanceKey for the klass
      if (Assertions.verifyAssertions) {
        Assertions._assert(value.getConcreteType() != null);
      }

      if (!value.getConcreteType().getReference().equals(TypeReference.JavaLangObject)) {
        registerInstanceOfClass(value.getConcreteType(), index);
      }

      // we'd better update the worklist appropriately
      // if graphNodeId == -1, then there are no equations that use this
      // variable.
      if (L.getGraphNodeId() > -1) {
        changedVariable(L);
      }
      return true;
    }

  }

  /**
   * Record that we have a new instanceKey for a given declared type.
   * 
   * @param klass
   * @param index
   */
  private void registerInstanceOfClass(IClass klass, int index) {

    if (DEBUG) {
      Trace.println("registerInstanceOfClass " + klass + " " + index);
    }

    if (Assertions.verifyAssertions) {
      Assertions._assert(!klass.getReference().equals(TypeReference.JavaLangObject));
    }

    try {
      IClass T = klass;
      registerInstanceWithAllSuperclasses(index, T);
      registerInstanceWithAllInterfaces(klass, index);

      if (klass.isArrayClass()) {
        ArrayClass aClass = (ArrayClass) klass;
        int dim = aClass.getDimensionality();
        registerMultiDimArraysForArrayOfObjectTypes(dim, index, aClass);

        IClass elementClass = aClass.getInnermostElementClass();
        if (elementClass != null) {
          registerArrayInstanceWithAllSuperclassesOfElement(index, elementClass, dim);
          registerArrayInstanceWithAllInterfacesOfElement(index, elementClass, dim);
        }
      }
    } catch (ClassHierarchyException e) {
      warnings.add(ClassHierarchyWarning.create(e.getMessage()));
    }
  }

  private int registerMultiDimArraysForArrayOfObjectTypes(int dim, int index, ArrayClass aClass) {

    for (int i = 1; i < dim; i++) {
      TypeReference jlo = makeArray(TypeReference.JavaLangObject, i);
      IClass jloClass = null;
      jloClass = aClass.getClassLoader().lookupClass(jlo.getName(), aClass.getClassHierarchy());
      MutableIntSet set = findOrCreateSparseSetForClass(jloClass);
      set.add(index);
    }
    return dim;
  }

  private void registerArrayInstanceWithAllInterfacesOfElement(int index, IClass elementClass, int dim) {
    Collection ifaces = null;
    try {
      ifaces = (elementClass.isInterface()) ? elementClass.getAllAncestorInterfaces() : elementClass.getAllImplementedInterfaces();
    } catch (ClassHierarchyException e) {
      warnings.add(ClassHierarchyWarning.create(e.getMessage()));
      return;
    }
    for (Iterator it = ifaces.iterator(); it.hasNext();) {
      IClass I = (IClass) it.next();
      TypeReference iArrayRef = makeArray(I.getReference(), dim);
      IClass iArrayClass = null;
      iArrayClass = I.getClassLoader().lookupClass(iArrayRef.getName(), I.getClassHierarchy());
      MutableIntSet set = findOrCreateSparseSetForClass(iArrayClass);
      set.add(index);
      if (DEBUG) {
        Trace.println("dense filter for interface " + iArrayClass + " " + set);
      }
    }
  }

  private TypeReference makeArray(TypeReference element, int dim) {
    TypeReference iArrayRef = element;
    for (int i = 0; i < dim; i++) {
      iArrayRef = TypeReference.findOrCreateArrayOf(iArrayRef);
    }
    return iArrayRef;
  }

  private void registerArrayInstanceWithAllSuperclassesOfElement(int index, IClass elementClass, int dim)
      throws ClassHierarchyException {
    IClass T;
    // register the array with each supertype of the element class
    T = elementClass.getSuperclass();
    while (T != null) {
      TypeReference tArrayRef = makeArray(T.getReference(), dim);
      IClass tArrayClass = null;
      tArrayClass = T.getClassLoader().lookupClass(tArrayRef.getName(), T.getClassHierarchy());
      MutableIntSet set = findOrCreateSparseSetForClass(tArrayClass);
      set.add(index);
      if (DEBUG) {
        Trace.println("dense filter for class " + tArrayClass + " " + set);
      }
      T = T.getSuperclass();
    }
  }

  /**
   * @param klass
   * @param index
   * @throws ClassHierarchyException
   */
  private void registerInstanceWithAllInterfaces(IClass klass, int index) throws ClassHierarchyException {
    Collection ifaces = klass.getAllImplementedInterfaces();
    for (Iterator it = ifaces.iterator(); it.hasNext();) {
      IClass I = (IClass) it.next();
      MutableIntSet set = findOrCreateSparseSetForClass(I);
      set.add(index);
      if (DEBUG) {
        Trace.println("dense filter for interface " + I + " " + set);
      }
    }
  }

  /**
   * @param index
   * @param T
   * @throws ClassHierarchyException
   */
  private void registerInstanceWithAllSuperclasses(int index, IClass T) throws ClassHierarchyException {
    while (T != null && !T.getReference().equals(TypeReference.JavaLangObject)) {
      MutableIntSet set = findOrCreateSparseSetForClass(T);
      set.add(index);
      if (DEBUG) {
        Trace.println("dense filter for class " + T + " " + set);
      }
      T = T.getSuperclass();
    }
  }

  /**
   * @param op
   * @param arg0
   */
  public void newSideEffect(UnaryOperator op, PointerKey arg0) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(arg0 != null);
    }
    if (DEBUG) {
      Trace.println("add constraint D: " + op + " " + arg0);
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(!pointsToMap.isUnified(arg0));
    }
    IntSetVariable v1 = findOrCreatePointsToSet(arg0);
    newStatement(null, op, v1, true, true);
  }

  /**
   * @param op
   * @param arg0
   * @param arg1
   */
  public void newSideEffect(AbstractOperator op, PointerKey arg0, PointerKey arg1) {
    if (DEBUG) {
      Trace.println("add constraint D: " + op + " " + arg0);
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(!pointsToMap.isUnified(arg0));
      Assertions._assert(!pointsToMap.isUnified(arg1));
    }
    IntSetVariable v1 = findOrCreatePointsToSet(arg0);
    IntSetVariable v2 = findOrCreatePointsToSet(arg1);
    newStatement(null, op, v1, v2, true, true);
  }

  @Override
  protected void initializeWorkList() {
    addAllStatementsToWorkList();
  }

  /**
   * @return an object that encapsulates the pointer analysis results
   */
  public PointerAnalysis extractPointerAnalysis(PropagationCallGraphBuilder builder) {
    if (pointerAnalysis == null) {
      pointerAnalysis = makePointerAnalysis(builder);
    }
    return pointerAnalysis;
  }

  @Override
  public void performVerboseAction() {
    super.performVerboseAction();
    if (DEBUG_MEMORY) {
      DEBUG_MEM_COUNTER++;
      if (DEBUG_MEM_COUNTER % DEBUG_MEM_INTERVAL == 0) {
        DEBUG_MEM_COUNTER = 0;
        ReferenceCleanser.clearSoftCaches();

        Trace.println(flowGraph.spaceReport());

        System.err.println("Analyze leaks..");
        HeapTracer.traceHeap(Collections.singleton(this), true);
        System.err.println("done analyzing leaks");
      }
    }
    if (getFixedPointSystem() instanceof VerboseAction) {
      ((VerboseAction) getFixedPointSystem()).performVerboseAction();
    }
    AbstractStatement s = workList.takeStatement();
    System.err.println(printRHSInstances(s));
    workList.insertStatement(s);
    System.err.println("CGNodes: " + cg.getNumberOfNodes());

  }

  private String printRHSInstances(AbstractStatement s) {
    if (s instanceof UnaryStatement) {
      UnaryStatement u = (UnaryStatement) s;
      PointsToSetVariable rhs = (PointsToSetVariable) u.getRightHandSide();
      IntSet value = rhs.getValue();
      final int[] topFive = new int[5];
      value.foreach(new IntSetAction() {
        public void act(int x) {
          for (int i = 0; i < 4; i++) {
            topFive[i] = topFive[i + 1];
          }
          topFive[4] = x;
        }
      });
      StringBuffer result = new StringBuffer();
      for (int i = 0; i < 5; i++) {
        int p = topFive[i];
        if (p != 0) {
          InstanceKey ik = getInstanceKey(p);
          result.append(p).append("  ").append(ik).append("\n");
        }
      }
      return result.toString();
    } else {
      return s.getClass().toString();
    }
  }

  @Override
  public IFixedPointSystem getFixedPointSystem() {
    return flowGraph;
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.HeapModel#iteratePointerKeys()
   */
  public Iterator iteratePointerKeys() {
    return pointsToMap.iterateKeys();
  }

  /**
   * warning: this is _real_ slow; don't use it anywhere performance critical
   */
  public int getNumberOfPointerKeys() {
    return pointsToMap.getNumberOfPointerKeys();
  }

  /**
   * Use with care.
   */
  Worklist getWorklist() {
    return workList;
  }

  public Iterator<AbstractStatement> getStatementsThatUse(IVariable v) {
    return flowGraph.getStatementsThatUse(v);
  }

  public Iterator<AbstractStatement> getStatementsThatDef(IVariable v) {
    return flowGraph.getStatementsThatDef(v);
  }

  public NumberedGraph<IVariable> getAssignmentGraph() {
    return flowGraph.getAssignmentGraph();
  }

  public Graph<IVariable> getFilterAsssignmentGraph() {
    return flowGraph.getFilterAssignmentGraph();
  }

  /**
   * 
   */
  public void revertToPreTransitive() {
    pointsToMap.revertToPreTransitive();
  }

  public Iterator getTransitiveRoots() {
    return pointsToMap.getTransitiveRoots();
  }

  public boolean isTransitiveRoot(PointerKey key) {
    return pointsToMap.isTransitiveRoot(key);
  }

  @Override
  protected void periodicMaintenance() {
    super.periodicMaintenance();
    ReferenceCleanser.clearSoftCaches();
  }

  @Override
  public int getVerboseInterval() {
    return verboseInterval;
  }

  /**
   * @param verboseInterval
   *          The verboseInterval to set.
   */
  public void setVerboseInterval(int verboseInterval) {
    this.verboseInterval = verboseInterval;
  }

  @Override
  public int getPeriodicMaintainInterval() {
    return periodicMaintainInterval;
  }

  /**
   * @param periodicMaintainInteval
   */
  public void setPeriodicMaintainInterval(int periodicMaintainInteval) {
    this.periodicMaintainInterval = periodicMaintainInteval;
  }

  /**
   * Unify the points-to-sets for the variables identified by the set s
   * 
   * @param s
   *          numbers of points-to-set variables
   * @throws IllegalArgumentException  if s is null
   */
  public void unify(IntSet s) {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    // cache the variables represented
    HashSet<PointsToSetVariable> cache = HashSetFactory.make(s.size());
    for (IntIterator it = s.intIterator(); it.hasNext();) {
      int i = it.next();
      cache.add(pointsToMap.getPointsToSet(i));
    }

    // unify the variables
    pointsToMap.unify(s);
    int rep = pointsToMap.getRepresentative(s.intIterator().next());

    // clean up the equations
    updateEquationsForUnification(cache, rep);

    // special logic to clean up side effects
    updateSideEffectsForUnification(cache, rep);
  }

  /**
   * Update side effect after unification
   * 
   * @param s
   *          set of PointsToSetVariables that have been unified
   * @param rep
   *          number of the representative variable for the unified set.
   */
  private void updateSideEffectsForUnification(HashSet<PointsToSetVariable> s, int rep) {
    PointsToSetVariable pRef = pointsToMap.getPointsToSet(rep);
    for (Iterator<PointsToSetVariable> it = s.iterator(); it.hasNext();) {
      PointsToSetVariable p = it.next();
      updateSideEffects(p, pRef);
    }
  }

  /**
   * Update equation def/uses after unification
   * 
   * @param s
   *          set of PointsToSetVariables that have been unified
   * @param rep
   *          number of the representative variable for the unified set.
   */
  private void updateEquationsForUnification(HashSet<PointsToSetVariable> s, int rep) {
    PointsToSetVariable pRef = pointsToMap.getPointsToSet(rep);
    for (Iterator<PointsToSetVariable> it = s.iterator(); it.hasNext();) {
      PointsToSetVariable p = it.next();

      if (p != pRef) {
        // pRef is the representative for p.
        // be careful: cache the defs before mucking with the underlying system
        for (Iterator d = new Iterator2Collection<AbstractStatement>(getStatementsThatDef(p)).iterator(); d.hasNext();) {
          AbstractStatement as = (AbstractStatement) d.next();

          if (as instanceof AssignEquation) {
            AssignEquation assign = (AssignEquation) as;
            PointsToSetVariable rhs = (PointsToSetVariable) assign.getRightHandSide();
            int rhsRep = pointsToMap.getRepresentative(pointsToMap.getIndex(rhs.getPointerKey()));
            if (rhsRep == rep) {
              flowGraph.removeStatement(as);
            } else {
              replaceLHS(pRef, p, as);
            }
          } else {
            replaceLHS(pRef, p, as);
          }
        }
        // be careful: cache the defs before mucking with the underlying system
        for (Iterator u = new Iterator2Collection<AbstractStatement>(getStatementsThatUse(p)).iterator(); u.hasNext();) {
          AbstractStatement as = (AbstractStatement) u.next();
          if (as instanceof AssignEquation) {
            AssignEquation assign = (AssignEquation) as;
            PointsToSetVariable lhs = (PointsToSetVariable) assign.getLHS();
            int lhsRep = pointsToMap.getRepresentative(pointsToMap.getIndex(lhs.getPointerKey()));
            if (lhsRep == rep) {
              flowGraph.removeStatement(as);
            } else {
              replaceRHS(pRef, p, as);
            }
          } else {
            replaceRHS(pRef, p, as);
          }
        }
        if (flowGraph.getNumberOfStatementsThatDef(p) == 0 && flowGraph.getNumberOfStatementsThatUse(p) == 0) {
          flowGraph.removeVariable(p);
        }
      }
    }
  }

  /**
   * replace all occurences of p on the rhs of a statement with pRef
   * 
   * @param as
   *          a statement that uses p in it's right-hand side
   */
  private void replaceRHS(PointsToSetVariable pRef, PointsToSetVariable p, AbstractStatement as) {
    if (as instanceof UnaryStatement) {
      if (Assertions.verifyAssertions) {
        Assertions._assert(((UnaryStatement) as).getRightHandSide() == p);
      }
      newStatement(as.getLHS(), (UnaryOperator) as.getOperator(), pRef, false, false);
    } else {
      IVariable[] rhs = as.getRHS();
      IVariable[] newRHS = new IVariable[rhs.length];
      for (int i = 0; i < rhs.length; i++) {
        if (rhs[i].equals(p)) {
          newRHS[i] = pRef;
        } else {
          newRHS[i] = rhs[i];
        }
      }
      newStatement(as.getLHS(), as.getOperator(), newRHS, false, false);
    }
    flowGraph.removeStatement(as);
  }

  /**
   * replace all occurences of p on the lhs of a statement with pRef
   * 
   * @param as
   *          a statement that defs p
   */
  private void replaceLHS(PointsToSetVariable pRef, PointsToSetVariable p, AbstractStatement as) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(as.getLHS() == p);
    }
    if (as instanceof UnaryStatement) {
      newStatement(pRef, (UnaryOperator) as.getOperator(), ((UnaryStatement) as).getRightHandSide(), false, false);
    } else {
      newStatement(pRef, as.getOperator(), as.getRHS(), false, false);
    }
    flowGraph.removeStatement(as);
  }

  public boolean isUnified(PointerKey result) {
    return pointsToMap.isUnified(result);
  }

  public int getNumber(PointerKey p) {
    return pointsToMap.getIndex(p);
  }
}
