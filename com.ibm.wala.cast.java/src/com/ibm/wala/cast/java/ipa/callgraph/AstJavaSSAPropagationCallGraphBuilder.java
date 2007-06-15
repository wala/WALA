/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.java.ipa.callgraph;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.cast.ipa.callgraph.AstSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.java.analysis.typeInference.AstJavaTypeInference;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl.JavaClass;
import com.ibm.wala.cast.java.ssa.AstJavaInstructionVisitor;
import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.fixpoint.IntSetVariable;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerFlowGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerFlowGraphFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.warnings.WarningSet;

public class AstJavaSSAPropagationCallGraphBuilder extends AstSSAPropagationCallGraphBuilder {

  protected AstJavaSSAPropagationCallGraphBuilder(IClassHierarchy cha, WarningSet warnings, AnalysisOptions options,
      PointerKeyFactory pointerKeyFactory) {
    super(cha, warnings, options, pointerKeyFactory);
  }

  // ///////////////////////////////////////////////////////////////////////////
  //
  // language specialization interface
  //
  // ///////////////////////////////////////////////////////////////////////////

  protected boolean useObjectCatalog() {
    return false;
  }

  // ///////////////////////////////////////////////////////////////////////////
  //
  // enclosing object pointer flow support
  //
  // ///////////////////////////////////////////////////////////////////////////

  public static class EnclosingObjectReferenceKey extends AbstractFieldPointerKey {
    private final IClass outer;

    private EnclosingObjectReferenceKey(InstanceKey inner, IClass outer) {
      super(inner);
      this.outer = outer;
    }

    public int hashCode() {
      return getInstanceKey().hashCode() * outer.hashCode();
    }

    public boolean equals(Object o) {
      return (o instanceof EnclosingObjectReferenceKey) && ((EnclosingObjectReferenceKey) o).outer.equals(outer)
          && ((EnclosingObjectReferenceKey) o).getInstanceKey().equals(getInstanceKey());
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  //
  // top-level node constraint generation
  //
  // ///////////////////////////////////////////////////////////////////////////

  protected TypeInference makeTypeInference(IR ir, IClassHierarchy cha) {
    TypeInference ti = new AstJavaTypeInference(ir, cha, false);
    ti.solve();

    if (DEBUG_TYPE_INFERENCE) {
      Trace.println("IR of " + ir.getMethod());
      Trace.println(ir);
      Trace.println("TypeInference of " + ir.getMethod());
      for (int i = 0; i < ir.getSymbolTable().getMaxValueNumber(); i++) {
        if (ti.isUndefined(i)) {
          Trace.println("  value " + i + " is undefined");
        } else {
          Trace.println("  value " + i + " has type " + ti.getType(i));
        }
      }
    }

    return ti;
  }

  protected class AstJavaInterestingVisitor extends AstInterestingVisitor implements AstJavaInstructionVisitor {
    protected AstJavaInterestingVisitor(int vn) {
      super(vn);
    }

    public void visitEnclosingObjectReference(EnclosingObjectReference inst) {
      Assertions.UNREACHABLE();
    }

    public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {
      bingo = true;
    }
  }

  protected InterestingVisitor makeInterestingVisitor(CGNode node, int vn) {
    return new AstJavaInterestingVisitor(vn);
  }

  // ///////////////////////////////////////////////////////////////////////////
  //
  // specialized pointer analysis
  //
  // ///////////////////////////////////////////////////////////////////////////

  protected static class AstJavaPointerFlowGraph extends AstPointerFlowGraph {

    protected static class AstJavaPointerFlowVisitor extends AstPointerFlowVisitor implements AstJavaInstructionVisitor {
      protected AstJavaPointerFlowVisitor(PointerAnalysis pa, CallGraph cg, Graph<PointerKey> delegate, CGNode node, IR ir, BasicBlock bb) {
        super(pa, cg, delegate, node, ir, bb);
      }

      public void visitEnclosingObjectReference(EnclosingObjectReference x) {

      }

      public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {

      }
    }

    protected AstJavaPointerFlowGraph(PointerAnalysis pa, CallGraph cg) {
      super(pa, cg);
    }

    protected InstructionVisitor makeInstructionVisitor(CGNode node, IR ir, BasicBlock bb) {
      return new AstJavaPointerFlowVisitor(pa, cg, delegate, node, ir, bb);
    }
  }

  public PointerFlowGraphFactory getPointerFlowGraphFactory() {
    return new PointerFlowGraphFactory() {
      public PointerFlowGraph make(PointerAnalysis pa, CallGraph cg) {
        return new AstJavaPointerFlowGraph(pa, cg);
      }
    };
  }

  // ///////////////////////////////////////////////////////////////////////////
  //
  // IR visitor specialization for AST-based Java
  //
  // ///////////////////////////////////////////////////////////////////////////

  protected static class AstJavaConstraintVisitor extends AstConstraintVisitor implements AstJavaInstructionVisitor {

    public AstJavaConstraintVisitor(
			     AstSSAPropagationCallGraphBuilder builder,
			     ExplicitCallGraph.ExplicitNode node) 
    {
      super(builder, node);
    }

    private void handleEnclosingObject(final PointerKey lvalKey, final IClass cls, final PointerKey objKey) {
      SymbolTable symtab = ir.getSymbolTable();
      int objVal;
      if (objKey instanceof LocalPointerKey) {
        objVal = ((LocalPointerKey) objKey).getValueNumber();
      } else {
        objVal = 0;
      }

      if (objVal > 0 && contentsAreInvariant(symtab, du, objVal)) {
        system.recordImplicitPointsToSet(objKey);

        InstanceKey[] objs = getInvariantContents(objVal);

        for (int i = 0; i < objs.length; i++) {
          PointerKey enclosing = new EnclosingObjectReferenceKey(objs[i], cls);
          system.newConstraint(lvalKey, assignOperator, enclosing);
        }

      } else {
        system.newSideEffect(new UnaryOperator() {
          public byte evaluate(IVariable lhs, IVariable rhs) {
            IntSetVariable tv = (IntSetVariable) rhs;
            if (tv.getValue() != null) {
              tv.getValue().foreach(new IntSetAction() {
                public void act(int ptr) {
                  InstanceKey iKey = system.getInstanceKey(ptr);
                  PointerKey enclosing = new EnclosingObjectReferenceKey(iKey, cls);
                  system.newConstraint(lvalKey, assignOperator, enclosing);
                }
              });
            }
            return NOT_CHANGED;
          }

          public int hashCode() {
            return System.identityHashCode(this);
          }

          public boolean equals(Object o) {
            return o == this;
          }

          public String toString() {
            return "enclosing objects of " + objKey;
          }
        }, objKey);
      }
    }

    public void visitEnclosingObjectReference(EnclosingObjectReference inst) {
      PointerKey lvalKey = getPointerKeyForLocal(inst.getDef());
      PointerKey objKey = getPointerKeyForLocal(1);
      IClass cls = getClassHierarchy().lookupClass(inst.getEnclosingType());
      handleEnclosingObject(lvalKey, cls, objKey);
    }

    public void visitNew(SSANewInstruction instruction) {
      super.visitNew(instruction);
      InstanceKey iKey = getInstanceKeyForAllocation(instruction.getNewSite());

      if (iKey != null) {
        IClass klass = iKey.getConcreteType();

        if (klass instanceof JavaClass) {
          IClass enclosingClass = ((JavaClass) klass).getEnclosingClass();
          if (enclosingClass != null) {
            IClass currentCls = node.getMethod().getDeclaringClass();
            PointerKey objKey = getPointerKeyForLocal(1);
            boolean needIndirection = false;

            Trace.println("class is " + klass + ", enclosing is " + enclosingClass + ", method is " + node.getMethod());

            if (node.getMethod().isSynthetic()) {
              return;
            }

            while (!getClassHierarchy().isSubclassOf(currentCls, enclosingClass)) {
              Assertions._assert(currentCls instanceof JavaClass);
              currentCls = ((JavaClass) currentCls).getEnclosingClass();
              needIndirection = true;
            }

            while (enclosingClass != null) {
              PointerKey x = new EnclosingObjectReferenceKey(iKey, enclosingClass);
              if (needIndirection) {
                handleEnclosingObject(x, currentCls, objKey);
                Trace.println("at " + instruction + ": adding " + iKey + ", " + enclosingClass + " <-- " + objKey + ", "
                    + currentCls);
              } else {
                system.newConstraint(x, assignOperator, objKey);
                Trace.println("at " + instruction + ": adding " + iKey + ", " + enclosingClass + " <-- " + objKey);
              }

              if (enclosingClass instanceof JavaClass) {
                needIndirection = true;
                enclosingClass = ((JavaClass) enclosingClass).getEnclosingClass();
                currentCls = ((JavaClass) currentCls).getEnclosingClass();
              } else {
                break;
              }
            }
          }
        }
      }
    }

    public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {
      visitInvokeInternal(instruction);
    }
  }

  protected ConstraintVisitor makeVisitor(ExplicitCallGraph.ExplicitNode node) {
    return new AstJavaConstraintVisitor(this, node);
  }
}
