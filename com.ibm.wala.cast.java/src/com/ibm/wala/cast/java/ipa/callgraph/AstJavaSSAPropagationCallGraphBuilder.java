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
import com.ibm.wala.cast.ipa.callgraph.GlobalObjectKey;
import com.ibm.wala.cast.java.analysis.typeInference.AstJavaTypeInference;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl.JavaClass;
import com.ibm.wala.cast.java.ssa.AstJavaInstructionVisitor;
import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.cast.java.ssa.AstJavaNewEnclosingInstruction;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.fixpoint.IntSetVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

public class AstJavaSSAPropagationCallGraphBuilder extends AstSSAPropagationCallGraphBuilder {

  protected AstJavaSSAPropagationCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache,
      PointerKeyFactory pointerKeyFactory) {
    super(cha, options, cache, pointerKeyFactory);
  }

  // ///////////////////////////////////////////////////////////////////////////
  //
  // language specialization interface
  //
  // ///////////////////////////////////////////////////////////////////////////

  @Override
  protected boolean useObjectCatalog() {
    return false;
  }

  @Override
  protected AbstractFieldPointerKey fieldKeyForUnknownWrites(AbstractFieldPointerKey fieldKey) {
    assert false;
    return null;
  }

  // ///////////////////////////////////////////////////////////////////////////
  //
  // enclosing object pointer flow support
  //
  // ///////////////////////////////////////////////////////////////////////////

  public static class EnclosingObjectReferenceKey extends AbstractFieldPointerKey {
    private final IClass outer;

    public EnclosingObjectReferenceKey(InstanceKey inner, IClass outer) {
      super(inner);
      this.outer = outer;
    }

    @Override
    public int hashCode() {
      return getInstanceKey().hashCode() * outer.hashCode();
    }

    @Override
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

  protected TypeInference makeTypeInference(IR ir) {
    TypeInference ti = new AstJavaTypeInference(ir, false);

    if (DEBUG_TYPE_INFERENCE) {
      System.err.println(("IR of " + ir.getMethod()));
      System.err.println(ir);
      System.err.println(("TypeInference of " + ir.getMethod()));
      for (int i = 0; i <= ir.getSymbolTable().getMaxValueNumber(); i++) {
        if (ti.isUndefined(i)) {
          System.err.println(("  value " + i + " is undefined"));
        } else {
          System.err.println(("  value " + i + " has type " + ti.getType(i)));
        }
      }
    }

    return ti;
  }

  protected class AstJavaInterestingVisitor extends AstInterestingVisitor implements AstJavaInstructionVisitor {
    protected AstJavaInterestingVisitor(int vn) {
      super(vn);
    }

    @Override
    public void visitEnclosingObjectReference(EnclosingObjectReference inst) {
      Assertions.UNREACHABLE();
    }

    @Override
    public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {
      bingo = true;
    }
  }

  @Override
  protected InterestingVisitor makeInterestingVisitor(CGNode node, int vn) {
    return new AstJavaInterestingVisitor(vn);
  }

  // ///////////////////////////////////////////////////////////////////////////
  //
  // specialized pointer analysis
  //
  // ///////////////////////////////////////////////////////////////////////////

  protected static class AstJavaConstraintVisitor extends AstConstraintVisitor implements AstJavaInstructionVisitor {

    public AstJavaConstraintVisitor(
			     AstSSAPropagationCallGraphBuilder builder,
			     CGNode node) 
    {
      super(builder, node);
    }

    /**
     * For each of objKey's instance keys ik, adds the constraint lvalKey = EORK(ik,cls),
     * where EORK(ik,cls) will be made equivalent to the actual enclosing class by
     * the handleNew() function below.
     * @param lvalKey
     * @param cls
     * @param objKey
     */
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

        for (InstanceKey obj : objs) {
          PointerKey enclosing = new EnclosingObjectReferenceKey(obj, cls);
          system.newConstraint(lvalKey, assignOperator, enclosing);
        }

      } else {
        system.newSideEffect(new UnaryOperator<PointsToSetVariable>() {
          @Override
          public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
            IntSetVariable<?> tv = rhs;
            if (tv.getValue() != null) {
              tv.getValue().foreach(ptr -> {
                InstanceKey iKey = system.getInstanceKey(ptr);
                PointerKey enclosing = new EnclosingObjectReferenceKey(iKey, cls);
                system.newConstraint(lvalKey, assignOperator, enclosing);
              });
            }
            return NOT_CHANGED;
          }

          @Override
          public int hashCode() {
            return System.identityHashCode(this);
          }

          @Override
          public boolean equals(Object o) {
            return o == this;
          }

          @Override
          public String toString() {
            return "enclosing objects of " + objKey;
          }
        }, objKey);
      }
    }

    @Override
    public void visitEnclosingObjectReference(EnclosingObjectReference inst) {
      PointerKey lvalKey = getPointerKeyForLocal(inst.getDef());
      PointerKey objKey = getPointerKeyForLocal(1);
      IClass cls = getClassHierarchy().lookupClass(inst.getEnclosingType());
      handleEnclosingObject(lvalKey, cls, objKey);
    }

    @Override
    public void visitNew(SSANewInstruction instruction) {
      super.visitNew(instruction);
      InstanceKey iKey = getInstanceKeyForAllocation(instruction.getNewSite());

      if (iKey != null) {
        IClass klass = iKey.getConcreteType();

        // in the case of a AstJavaNewEnclosingInstruction (a new instruction like outer.new Bla()),
        // we may need to record the instance keys if the pointer key outer is invariant (and thus implicit) 
        InstanceKey enclosingInvariantKeys[] = null;
       
        if (klass instanceof JavaClass) {
          IClass enclosingClass = ((JavaClass) klass).getEnclosingClass(); // the immediate enclosing class.
          if (enclosingClass != null) {
            IClass currentCls = node.getMethod().getDeclaringClass();
            PointerKey objKey;
            
            if ( instruction instanceof AstJavaNewEnclosingInstruction ) {
              

              int enclosingVal = ((AstJavaNewEnclosingInstruction) instruction).getEnclosing();
              SymbolTable symtab = ir.getSymbolTable();

              // pk 'outer' is invariant, which means it's implicit, so can't add a constraint with the pointer key.
              // we should just add constraints directly to the instance keys (below) 
              if ( contentsAreInvariant(symtab, du, enclosingVal) )
                enclosingInvariantKeys = getInvariantContents(enclosingVal);
              
              // what happens if objKey is implicit but the contents aren't invariant?! (it this possible?) big trouble!
              
              objKey = getPointerKeyForLocal(enclosingVal);
            }
            else
              objKey = getPointerKeyForLocal(1);

            System.err.println(("class is " + klass + ", enclosing is " + enclosingClass + ", method is " + node.getMethod()));

            if (node.getMethod().isSynthetic()) {
              return;
            }

            currentCls = enclosingClass;
            
            PointerKey x = new EnclosingObjectReferenceKey(iKey, currentCls);
            if ( enclosingInvariantKeys != null )
              for ( InstanceKey obj: enclosingInvariantKeys )
                system.newConstraint(x, obj);
            else
              system.newConstraint(x, assignOperator, objKey);

            // If the immediate inclosing class is not a top-level class, we must make EORKs for all enclosing classes up to the top level.
            // for instance, if we have "D d = c.new D()", and c is of type A$B$C, methods in D may reference variables and functions from
            // A, B, and C. Therefore we must also make the links from EORK(allocsite of d,enc class B) and EORK(allocsite of d,en class A).
            // We do this by getting the enclosing class of C and making a link from EORK(d,B) -> EORK(c,B), etc.
            currentCls = ((JavaClass) currentCls).getEnclosingClass();
            while (currentCls != null) { 
              x = new EnclosingObjectReferenceKey(iKey, currentCls); // make EORK(d,B), EORK(d,A), etc.
              handleEnclosingObject(x, currentCls, objKey);
              // objKey is the pointer key representing the immediate inner class.
              // handleEnclosingObject finds x's instance keys and for each one "ik" links x to EORK(ik,currentCls)
              // thus, for currentCls=B, it will find the allocation site of c and make a link from EORK(d,B) to EORK(c,B)
              
              currentCls = ((JavaClass) currentCls).getEnclosingClass();
            }
            
          }
        }
      }
    }

    @Override
    public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {
      visitInvokeInternal(instruction, new DefaultInvariantComputer());
    }
  }

  @Override
  public ConstraintVisitor makeVisitor(CGNode node) {
    return new AstJavaConstraintVisitor(this, node);
  }

  @Override
  protected boolean sameMethod(CGNode opNode, String definingMethod) {
    MethodReference reference = opNode.getMethod().getReference();
    String selector = reference.getSelector().toString();
    String containingClass = reference.getDeclaringClass().getName().toString();
    return definingMethod.equals(containingClass + "/" + selector);
  }

  @Override
  public GlobalObjectKey getGlobalObject(Atom language) {
    Assertions.UNREACHABLE();
    return null;
  }
}
