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
package com.ibm.wala.analysis.reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.typeInference.ConeType;
import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.analysis.typeInference.SetType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.CodeScanner;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ipa.summaries.SyntheticIR;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.Warnings;

/**
 * Logic to interpret "factory" methods in context.
 */
public class FactoryBypassInterpreter extends AbstractReflectionInterpreter {

  /**
   * A Map from CallerSiteContext -&gt; Set &lt;TypeReference&gt;represents the types a factory method might create in a particular context
   */
  private final Map<Context, Set<TypeReference>> map = HashMapFactory.make();

  /**
   * A cache of synthetic method implementations, indexed by Context
   */
  private final Map<Context, SpecializedFactoryMethod> syntheticMethodCache = HashMapFactory.make();

  /**
   * @param options governing analysis options
   */
  public FactoryBypassInterpreter(AnalysisOptions options, IAnalysisCacheView iAnalysisCacheView) {
    this.options = options;
    this.cache = iAnalysisCacheView;
  }

  @Override
  public IR getIR(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (DEBUG) {
      System.err.println("generating IR for " + node);
    }
    SpecializedFactoryMethod m = findOrCreateSpecializedFactoryMethod(node);
    return cache.getIR(m, node.getContext());
  }

  @Override
  public IRView getIRView(CGNode node) {
    return getIR(node);
  }

  private Set<TypeReference> getTypesForContext(Context context) {
    // first try user spec
    // XMLReflectionReader spec = (XMLReflectionReader) userSpec;
    // if (spec != null && context instanceof CallerSiteContext) {
    // CallerSiteContext site = (CallerSiteContext) context;
    // MemberReference m = site.getCaller().getMethod().getReference();
    // ReflectionSummary summary = spec.getSummary(m);
    // if (summary != null) {
    // Set<TypeReference> types = summary.getTypesForProgramLocation(site.getCallSite().getProgramCounter());
    // if (types != null) {
    // return types;
    // }
    // }
    // }

    Set<TypeReference> types = map.get(context);
    return types;
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getNumberOfStatements(com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  public int getNumberOfStatements(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    SpecializedFactoryMethod m = findOrCreateSpecializedFactoryMethod(node);
    return m.allInstructions.size();
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.rta.RTAContextInterpreter#understands(com.ibm.wala.classLoader.IMethod,
   * com.ibm.wala.ipa.callgraph.Context)
   */
  @Override
  public boolean understands(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (node.getMethod().isSynthetic()) {
      SyntheticMethod s = (SyntheticMethod) node.getMethod();
      if (s.isFactoryMethod()) {
        return getTypesForContext(node.getContext()) != null;
      }
    }
    return false;

  }

  @Override
  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    SpecializedFactoryMethod m = findOrCreateSpecializedFactoryMethod(node);
    HashSet<NewSiteReference> result = HashSetFactory.make(5);
    for (SSAInstruction ssaInstruction : m.getAllocationStatements()) {
      SSANewInstruction s = (SSANewInstruction) ssaInstruction;
      result.add(s.getNewSite());
    }
    return result.iterator();
  }

  public Iterator<SSAInstruction> getInvokeStatements(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    SpecializedFactoryMethod m = findOrCreateSpecializedFactoryMethod(node);
    return m.getInvokeStatements().iterator();
  }

  @Override
  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    final Iterator<SSAInstruction> I = getInvokeStatements(node);
    return new Iterator<CallSiteReference>() {
      @Override
      public boolean hasNext() {
        return I.hasNext();
      }

      @Override
      public CallSiteReference next() {
        SSAInvokeInstruction s = (SSAInvokeInstruction) I.next();
        return s.getCallSite();
      }

      @Override
      public void remove() {
        Assertions.UNREACHABLE();
      }
    };
  }

  public boolean recordType(IClassHierarchy cha, Context context, TypeReference type) {
    Set<TypeReference> types = map.get(context);
    if (types == null) {
      types = HashSetFactory.make(2);
      map.put(context, types);
    }
    if (types.contains(type)) {
      return false;
    } else {
      types.add(type);
      // update any extant synthetic method
      SpecializedFactoryMethod m = syntheticMethodCache.get(context);
      if (m != null) {
        TypeAbstraction T = typeRef2TypeAbstraction(cha, type);
        m.addStatementsForTypeAbstraction(T);
        cache.invalidate(m, context);
      }
      return true;
    }
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#recordFactoryType(com.ibm.wala.ipa.callgraph.CGNode,
   * com.ibm.wala.classLoader.IClass)
   */
  @Override
  public boolean recordFactoryType(CGNode node, IClass klass) {
    if (klass == null) {
      throw new IllegalArgumentException("klass is null");
    }
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    return recordType(node.getMethod().getClassHierarchy(), node.getContext(), klass.getReference());
  }

  @Override
  public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    SpecializedFactoryMethod m = findOrCreateSpecializedFactoryMethod(node);
    try {
      return CodeScanner.getFieldsRead(m).iterator();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  @Override
  public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    SpecializedFactoryMethod m = findOrCreateSpecializedFactoryMethod(node);
    try {
      return CodeScanner.getFieldsWritten(m).iterator();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  private SpecializedFactoryMethod findOrCreateSpecializedFactoryMethod(CGNode node) {
    SpecializedFactoryMethod m = syntheticMethodCache.get(node.getContext());
    if (m == null) {
      Set<TypeReference> types = getTypesForContext(node.getContext());
      m = new SpecializedFactoryMethod((SummarizedMethod) node.getMethod(), node.getContext(), types);
      syntheticMethodCache.put(node.getContext(), m);
    }
    return m;
  }

  public Set getCaughtExceptions(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    SpecializedFactoryMethod m = findOrCreateSpecializedFactoryMethod(node);
    try {
      return CodeScanner.getCaughtExceptions(m);
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public boolean hasObjectArrayLoad(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    SpecializedFactoryMethod m = findOrCreateSpecializedFactoryMethod(node);
    try {
      return CodeScanner.hasObjectArrayLoad(m);
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return false;
    }
  }

  public boolean hasObjectArrayStore(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    SpecializedFactoryMethod m = findOrCreateSpecializedFactoryMethod(node);
    try {
      return CodeScanner.hasObjectArrayStore(m);
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return false;
    }
  }

  public Iterator<TypeReference> iterateCastTypes(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    SpecializedFactoryMethod m = findOrCreateSpecializedFactoryMethod(node);
    try {
      return CodeScanner.iterateCastTypes(m);
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getCFG(com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode N) {
    return getIR(N).getControlFlowGraph();
  }

  @Override
  public DefUse getDU(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    // SpecializedFactoryMethod m = findOrCreateSpecializedFactoryMethod(node);
    return cache.getDefUse(getIR(node));
  }

  protected class SpecializedFactoryMethod extends SpecializedMethod {

    /**
     * List of synthetic invoke instructions we model for this specialized instance.
     */
    final private ArrayList<SSAInstruction> calls = new ArrayList<>();

    /**
     * The method being modelled
     */
    private final IMethod method;

    /**
     * Context being modelled
     */
    private final Context context;

    /**
     * next free local value number;
     */
    private int nextLocal;

    /**
     * value number for integer constant 1
     */
    private int valueNumberForConstantOne = -1;

    private final SSAInstructionFactory insts = declaringClass.getClassLoader().getInstructionFactory();

    private void initValueNumberForConstantOne() {
      if (valueNumberForConstantOne == -1) {
        valueNumberForConstantOne = nextLocal++;
      }
    }

    protected SpecializedFactoryMethod(final SummarizedMethod m, Context context, final Set<TypeReference> S) {
      super(m, m.getDeclaringClass(), m.isStatic(), true);

      this.context = context;
      if (DEBUG) {
        System.err.println(("Create SpecializedFactoryMethod " + m + S));
      }

      this.method = m;
      assert S != null;
      assert m.getDeclaringClass() != null : "null declaring class for " + m;

      // add original statements from the method summary
      nextLocal = addOriginalStatements(m);

      for (TypeReference type : S) {
        TypeAbstraction T = typeRef2TypeAbstraction(m.getClassHierarchy(), type);
        addStatementsForTypeAbstraction(T);
      }
    }

    protected void addStatementsForTypeAbstraction(TypeAbstraction T) {

      if (DEBUG) {
        System.err.println(("adding " + T + " to " + method));
      }
      T = interceptType(T);
      if (T == null) {
        return;
      }
      if ((T instanceof PointType) || (T instanceof ConeType)) {
        TypeReference ref = T.getType().getReference();
        NewSiteReference site = NewSiteReference.make(0, ref);

        if (DEBUG) {
          IClass klass = options.getClassTargetSelector().getAllocatedTarget(null, site);
          System.err.println(("Selected allocated target: " + klass + " for " + T));
        }
        if (T instanceof PointType) {
          if (!typesAllocated.contains(ref)) {
            addStatementsForConcreteType(ref);
          }
        } else if (T instanceof ConeType) {
          if (DEBUG) {
            System.err.println(("Cone clause for " + T));
          }
          if (((ConeType) T).isInterface()) {
            Set<IClass> implementors = T.getType().getClassHierarchy().getImplementors(ref);
            if (DEBUG) {
              System.err.println(("Implementors for " + T + " " + implementors));
            }
            if (implementors.isEmpty()) {
              if (DEBUG) {
                System.err.println(("Found no implementors of type " + T));
              }
              Warnings.add(NoSubtypesWarning.create(T));
            }
            if (implementors.size() > CONE_BOUND) {
              Warnings.add(ManySubtypesWarning.create(T, implementors.size()));
            }

            addStatementsForSetOfTypes(implementors.iterator());
          } else {
            Collection<IClass> subclasses = T.getType().getClassHierarchy().computeSubClasses(ref);
            if (DEBUG) {
              System.err.println(("Subclasses for " + T + " " + subclasses));
            }
            if (subclasses.isEmpty()) {
              if (DEBUG) {
                System.err.println(("Found no subclasses of type " + T));
              }
              Warnings.add(NoSubtypesWarning.create(T));
            }
            if (subclasses.size() > CONE_BOUND) {
              Warnings.add(ManySubtypesWarning.create(T, subclasses.size()));
            }
            addStatementsForSetOfTypes(subclasses.iterator());
          }
        } else {
          Assertions.UNREACHABLE("Unexpected type " + T.getClass());
        }
      } else if (T instanceof SetType) {
        // This code has clearly bitrotted, since iteratePoints() returns an Iterator<TypeReference>
        // and we need an Iterator<IClass>. Commenting out for now. --MS
        Assertions.UNREACHABLE();
        // addStatementsForSetOfTypes(((SetType) T).iteratePoints());
      } else {
        Assertions.UNREACHABLE("Unexpected type " + T.getClass());
      }
    }

    private TypeAbstraction interceptType(TypeAbstraction T) {
      TypeReference type = T.getType().getReference();
      if (type.equals(TypeReference.JavaIoSerializable)) {
        Warnings.add(IgnoreSerializableWarning.create());
        return null;
      } else {
        return T;
      }
    }

    /**
     * Set up a method summary which allocates and returns an instance of concrete type T.
     * 
     * @param T
     */
    private void addStatementsForConcreteType(final TypeReference T) {
      int alloc = addStatementsForConcreteSimpleType(T);
      if (alloc == -1) {
        return;
      }
      if (T.isArrayType()) {
        MethodReference init = MethodReference.findOrCreate(T, MethodReference.initAtom, MethodReference.defaultInitDesc);
        CallSiteReference site = CallSiteReference.make(getCallSiteForType(T), init, IInvokeInstruction.Dispatch.SPECIAL);
        int[] params = new int[1];
        params[0] = alloc;
        int exc = getExceptionsForType(T);
        SSAInvokeInstruction s = insts.InvokeInstruction(allInstructions.size(), params, exc, site, null);
        calls.add(s);
        allInstructions.add(s);
      }
    }

    private int addOriginalStatements(SummarizedMethod m) {
      SSAInstruction[] original = m.getStatements(options.getSSAOptions());
      // local value number 1 is "this", so the next free value number is 2
      int nextLocal = 2;
      for (SSAInstruction s : original) {
        allInstructions.add(s);
        if (s instanceof SSAInvokeInstruction) {
          calls.add(s);
        }
        if (s instanceof SSANewInstruction) {
          allocations.add(s);
        }
        for (int j = 0; j < s.getNumberOfDefs(); j++) {
          int def = s.getDef(j);
          if (def >= nextLocal) {
            nextLocal = def + 1;
          }
        }
        for (int j = 0; j < s.getNumberOfUses(); j++) {
          int use = s.getUse(j);
          if (use >= nextLocal) {
            nextLocal = use + 1;
          }
        }
      }
      return nextLocal;
    }

    private void addStatementsForSetOfTypes(Iterator<IClass> it) {
      if (!it.hasNext()) { // Uh. No types. Hope the caller reported a warning.
        SSAReturnInstruction r = insts.ReturnInstruction(allInstructions.size(), nextLocal, false);
        allInstructions.add(r);
      }

      for (IClass klass : Iterator2Iterable.make(it)) {
        TypeReference T = klass.getReference();
        if (klass.isAbstract() || klass.isInterface() || typesAllocated.contains(T)) {
          continue;
        }
        typesAllocated.add(T);
        int i = getLocalForType(T);
        NewSiteReference ref = NewSiteReference.make(getNewSiteForType(T), T);
        SSANewInstruction a = null;
        if (T.isArrayType()) {
          int[] sizes = new int[((ArrayClass)klass).getDimensionality()];
          initValueNumberForConstantOne();
          Arrays.fill(sizes, valueNumberForConstantOne);
          a = insts.NewInstruction(allInstructions.size(), i, ref, sizes);

        } else {
          a = insts.NewInstruction(allInstructions.size(), i, ref);
        }
        allocations.add(a);
        allInstructions.add(a);
        SSAReturnInstruction r = insts.ReturnInstruction(allInstructions.size(), i, false);
        allInstructions.add(r);
        MethodReference init = MethodReference.findOrCreate(T, MethodReference.initAtom, MethodReference.defaultInitDesc);
        CallSiteReference site = CallSiteReference.make(getCallSiteForType(T), init, IInvokeInstruction.Dispatch.SPECIAL);
        int[] params = new int[1];
        params[0] = i;
        SSAInvokeInstruction s = insts.InvokeInstruction(allInstructions.size(), params, getExceptionsForType(T), site, null);
        calls.add(s);
        allInstructions.add(s);
      }
    }

    public List<SSAInstruction> getAllocationStatements() {
      return allocations;
    }

    public List<SSAInstruction> getInvokeStatements() {
      return calls;
    }

    /**
     * Two specialized methods can be different, even if they represent the same source method. So, revert to object identity for
     * testing equality. TODO: this is non-optimal; could try to re-use specialized methods that have the same context.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      return this == obj;
    }

    @Override
    public int hashCode() { // TODO: change this to avoid non-determinism!
      return System.identityHashCode(this);
    }

    @Override
    public String toString() {
      return super.toString();
    }

    @Override
    public SSAInstruction[] getStatements() {
      SSAInstruction[] result = new SSAInstruction[allInstructions.size()];
      int i = 0;
      for (SSAInstruction ssaInstruction : allInstructions) {
        result[i++] = ssaInstruction;
      }
      return result;
    }

    @Override
    public IClass getDeclaringClass() {
      assert method.getDeclaringClass() != null : "null declaring class for original method " + method;
      return method.getDeclaringClass();
    }

    @Override
    public int getNumberOfParameters() {
      return method.getNumberOfParameters();
    }

    @Override
    public TypeReference getParameterType(int i) {
      return method.getParameterType(i);
    }

    /*
     * @see com.ibm.wala.classLoader.IMethod#getIR(com.ibm.wala.util.WarningSet)
     */
    @Override
    public IR makeIR(Context C, SSAOptions options) {
      SSAInstruction[] instrs = getStatements();
      Map<Integer, ConstantValue> constants = null;
      if (valueNumberForConstantOne > -1) {
        constants = HashMapFactory.make(1);
        constants.put(new Integer(valueNumberForConstantOne), new ConstantValue(new Integer(1)));
      }

      return new SyntheticIR(this, context, new InducedCFG(instrs, this, context), instrs, options, constants);
    }
  }
}
