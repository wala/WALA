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
package com.ibm.wala.j2ee;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.reflection.JavaTypeContext;
import com.ibm.wala.analysis.typeInference.ConeType;
import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.summaries.SyntheticIR;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * Logic to interpret dynacache commands in context
 * 
 * @author sfink
 */
public class CommandInterpreter implements SSAContextInterpreter {

  private static final boolean DEBUG = false;

  private static final Atom PerformExecuteAtom = Atom.findOrCreateAsciiAtom("performExecute");
  private final static Descriptor PerformExecuteDesc = Descriptor.findOrCreateUTF8("()V");

  /**
   * A cache of synthetic method implementations, indexed by Context
   */
  private final Map<Context, SpecializedExecuteMethod> syntheticMethodCache = HashMapFactory.make();

  /**
   * Governing class hierarchy
   */
  private final ClassHierarchy cha;

  /**
   * Keep track of analysis warnings
   */
  private WarningSet warnings;

  /**
   * @param cha
   *          governing class hierarchy
   * @param warnings
   *          object to track analysis warnings
   */
  public CommandInterpreter(ClassHierarchy cha,  WarningSet warnings) {
    this.cha = cha;
    this.warnings = warnings;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.cfa.CFAContextInterpreter#getIR(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.callgraph.Context,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public IR getIR(CGNode node, WarningSet warnings) {
    SpecializedExecuteMethod m = findOrCreateSpecializedMethod(node);
    return m.getIR(warnings);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.cfa.CFAContextInterpreter#getNumberOfStatements(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.callgraph.Context,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public int getNumberOfStatements(CGNode node, WarningSet warnings) {
    SpecializedExecuteMethod m = findOrCreateSpecializedMethod(node);
    return m.calls.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.rta.RTAContextInterpreter#understands(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.callgraph.Context)
   */
  public boolean understands(CGNode node) {

    if (!(node.getContext() instanceof JavaTypeContext)) {
      return false;
    }
    return node.getMethod().getReference().equals(J2EEContextSelector.ExecuteMethod);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.rta.RTAContextInterpreter#getNewSites(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.callgraph.Context,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public Iterator<NewSiteReference> iterateNewSites(CGNode node, WarningSet warnings) {
    return EmptyIterator.instance();
  }

  /**
   * Evaluate a new-instance method in a given Context
   */
  public Iterator<SSAInstruction> getInvokeStatements(CGNode node) {
    SpecializedExecuteMethod m = findOrCreateSpecializedMethod(node);
    return m.getInvokeStatements().iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.rta.RTAContextInterpreter#getCallSites(com.ibm.wala.classLoader.IMethod,
   *      com.ibm.detox.ipa.callgraph.Context,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public Iterator<CallSiteReference> iterateCallSites(CGNode node, WarningSet warnings) {
    final Iterator<SSAInstruction> I = getInvokeStatements(node);
    return new Iterator<CallSiteReference>() {
      public boolean hasNext() {
        return I.hasNext();
      }

      public CallSiteReference next() {
        SSAInvokeInstruction s = (SSAInvokeInstruction) I.next();
        return s.getCallSite();
      }

      public void remove() {
        Assertions.UNREACHABLE();
      }
    };
  }
  protected class SpecializedExecuteMethod extends SyntheticMethod {

    /**
     * List of synthetic invoke instructions we model for this specialized
     * instance.
     */
    private ArrayList<SSAInstruction> calls = new ArrayList<SSAInstruction>();

    int nextLocal = 2;

    private SpecializedExecuteMethod(IMethod execute, final TypeAbstraction T) {
      super(execute, execute.getDeclaringClass(), false, false);
      if (T instanceof PointType) {
        addStatementsForConcreteType(T.getType().getReference());
      } else if (T instanceof ConeType) {
        if (((ConeType) T).isInterface()) {
          Set<IClass> implementors = cha.getImplementors(T.getType().getReference());
          if (implementors.isEmpty()) {
            if (DEBUG) {
              Trace.println("Found no implementors of type " + T);
            }
            warnings.add(NoSubtypesWarning.create(T));
          }

          addStatementsForSetOfTypes(implementors.iterator());
        } else {
          Collection<IClass> subclasses = cha.computeSubClasses(T.getType().getReference());
          if (subclasses.isEmpty()) {
            if (DEBUG) {
              Trace.println("Found no subclasses of type " + T);
            }
            warnings.add(NoSubtypesWarning.create(T));
          }
          addStatementsForSetOfTypes(subclasses.iterator());
        }
      } else {
        Assertions.UNREACHABLE("Unexpected type " + T.getClass());
      }
    }

    /**
     * Set up a method summary which allocates and returns an instance of
     * concrete type T.
     * 
     * @param T
     */
    private void addStatementsForConcreteType(final TypeReference T) {
      if (DEBUG) {
        Trace.println("addStatementsForConcreteType: " + T);
      }
      MethodReference performExecute = MethodReference.findOrCreate(T, PerformExecuteAtom, PerformExecuteDesc);
      CallSiteReference site = CallSiteReference.make(calls.size(), performExecute, IInvokeInstruction.Dispatch.VIRTUAL);
      int[] params = new int[1];
      // value number 1 is the receiver.
      params[0] = 1;
      int exc = nextLocal++;
      SSAInvokeInstruction s = new SSAInvokeInstruction(params, exc, site);
      calls.add(s);
    }

    private void addStatementsForSetOfTypes(Iterator<IClass> it) {
      for (; it.hasNext();) {
        IClass klass = (IClass) it.next();
        TypeReference T = klass.getReference();
        addStatementsForConcreteType(T);
      }
    }

    /**
     * @return List of InvokeInstruction
     */
    public List<SSAInstruction> getInvokeStatements() {
      return calls;
    }

    /**
     * Two specialized methods can be different, even if they represent the same
     * source method. So, revert to object identity for testing equality. TODO:
     * this is non-optimal; could try to re-use specialized methods that have
     * the same context.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
      return this == obj;
    }

    /**
     * Two specialized methods can be different, even if they represent the same
     * source method. So, revert to object identity for testing equality. TODO:
     * this is non-optimal; could try to re-use specialized methods that have
     * the same context.
     */
    public int hashCode() {
      // TODO: change this to avoid non-determinism!
      return System.identityHashCode(this);
    }

    public String toString() {
      return super.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.classLoader.IMethod#getStatements(com.ibm.wala.util.warnings.WarningSet)
     */
    public SSAInstruction[] getStatements(WarningSet warnings) {
      SSAInstruction[] result = new SSAInstruction[calls.size()];
      int i = 0;
      for (Iterator<SSAInstruction> it = calls.iterator(); it.hasNext();) {
        result[i++] = it.next();
      }
      return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.classLoader.IMethod#getIR(com.ibm.wala.util.warnings.WarningSet)
     */
    public IR getIR(WarningSet warnings) {
      SSAInstruction[] instrs = getStatements(warnings);
      return new SyntheticIR(this, Everywhere.EVERYWHERE, new InducedCFG(instrs, this, Everywhere.EVERYWHERE), instrs, SSAOptions.defaultOptions(), null, warnings);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.xta.XTAContextInterpreter#iterateFieldsRead(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public Iterator<IField> iterateFieldsRead(CGNode node, WarningSet warnings) {
    return EmptyIterator.instance();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.xta.XTAContextInterpreter#iterateFieldsWritten(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public Iterator<IField> iterateFieldsWritten(CGNode node, WarningSet warnings) {
    return EmptyIterator.instance();
  }

  /**
   * @param node
   * @return a synthetic method representing the node
   */
  private SpecializedExecuteMethod findOrCreateSpecializedMethod(CGNode node) {
    if (Assertions.verifyAssertions) {
      if (!(node.getContext() instanceof JavaTypeContext))
        Assertions._assert(false, "unexpected context: " + node);
    }
    SpecializedExecuteMethod m = syntheticMethodCache.get(node.getContext());
    if (m == null) {
      TypeAbstraction T = (TypeAbstraction) ((JavaTypeContext) node.getContext()).get(ContextKey.RECEIVER);
      m = new SpecializedExecuteMethod(node.getMethod(), T);
      syntheticMethodCache.put(node.getContext(), m);
    }
    return m;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.xta.XTAContextInterpreter#getCaughtExceptions(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public Set<Object> getCaughtExceptions(CGNode node, WarningSet warnings) {
    return Collections.emptySet();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.xta.XTAContextInterpreter#hasObjectArrayLoad(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public boolean hasObjectArrayLoad(CGNode node, WarningSet warnings) {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.xta.XTAContextInterpreter#hasObjectArrayStore(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public boolean hasObjectArrayStore(CGNode node, WarningSet warnings) {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.xta.XTAContextInterpreter#iterateCastTypes(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.util.warnings.WarningSet)
   */
  public Iterator<IClass> iterateCastTypes(CGNode node, WarningSet warnings) {
    return EmptyIterator.instance();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.rta.RTAContextInterpreter#recordFactoryType(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.IClass)
   */
  public boolean recordFactoryType(CGNode node, IClass klass) {
    // this class does not observe factories
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.rta.RTAContextInterpreter#setWarnings(com.ibm.wala.util.warnings.WarningSet)
   */
  public void setWarnings(WarningSet newWarnings) {
    this.warnings = newWarnings;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.cfg.CFGProvider#getCFG(com.ibm.wala.ipa.callgraph.CGNode)
   */
  public ControlFlowGraph getCFG(CGNode N, WarningSet warnings) {
    return getIR(N, warnings).getControlFlowGraph();
  }
 /**
   * @author sfink
   *
   * A waring when we fail to find subtypes for a command method
   */
  private static class NoSubtypesWarning extends Warning {

    final TypeAbstraction T;
    NoSubtypesWarning(TypeAbstraction T) {
      super(Warning.SEVERE);
      this.T = T;
    }
    public String getMsg() {
      return getClass().toString() + " : " + T;
    }
    public static NoSubtypesWarning create(TypeAbstraction T) {
      return new NoSubtypesWarning(T);
    }
  }
  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getDU(com.ibm.wala.ipa.callgraph.CGNode, com.ibm.wala.util.warnings.WarningSet)
   */
  public DefUse getDU(CGNode node, WarningSet warnings) {
    return new DefUse(getIR(node,warnings));
  }
}
