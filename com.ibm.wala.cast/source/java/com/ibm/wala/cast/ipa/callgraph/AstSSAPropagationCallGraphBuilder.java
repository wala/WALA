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
package com.ibm.wala.cast.ipa.callgraph;

import java.io.UTFDataFormatException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.analysis.reflection.ReflectionContextInterpreter;
import com.ibm.wala.cast.ipa.callgraph.AstCallGraph.AstCGNode;
import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys.ScopeMappingInstanceKey;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.ssa.AstInstructionVisitor;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.fixpoint.IntSetVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysisImpl;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointsToMap;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.PropagationSystem;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.modref.ArrayLengthKey;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.strings.Atom;

public abstract class AstSSAPropagationCallGraphBuilder extends SSAPropagationCallGraphBuilder {

  public static final boolean DEBUG_TYPE_INFERENCE = false;

  public static final boolean DEBUG_PROPERTIES = false;

  // /////////////////////////////////////////////////////////////////////////
  //
  // language specialization interface
  //
  // /////////////////////////////////////////////////////////////////////////

  /**
   * should we maintain an object catalog for each instance key, storing the
   * names of all known properties of the instance key? required to handle
   * {@link EachElementGetInstruction}s.
   * 
   * @see AstConstraintVisitor#visitPut(SSAPutInstruction)
   * @see AstConstraintVisitor#visitEachElementGet(EachElementGetInstruction)
   */
  protected abstract boolean useObjectCatalog();

  /**
   * each language can specify whether a particular field name should be stored
   * in object catalogs or not. By default, always return false.
   */
  @SuppressWarnings("unused")
  protected boolean isUncataloguedField(IClass type, String fieldName) {
    return false;
  }

  // /////////////////////////////////////////////////////////////////////////
  //
  // overall control
  //
  // /////////////////////////////////////////////////////////////////////////

  
  public abstract GlobalObjectKey getGlobalObject(Atom language);

  protected AstSSAPropagationCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache,
      PointerKeyFactory pointerKeyFactory) {
    super(cha, options, cache, pointerKeyFactory);
  }

  public SSAContextInterpreter makeDefaultContextInterpreters(SSAContextInterpreter appContextInterpreter, AnalysisOptions options,
      IClassHierarchy cha) {
    SSAContextInterpreter c = new DefaultSSAInterpreter(options, getAnalysisCache());
    c = new DelegatingSSAContextInterpreter(new AstContextInsensitiveSSAContextInterpreter(options, getAnalysisCache()), c);

    c = new DelegatingSSAContextInterpreter(ReflectionContextInterpreter.createReflectionContextInterpreter(cha, options,
        getAnalysisCache()), c);

    if (appContextInterpreter == null)
      return c;
    else
      return new DelegatingSSAContextInterpreter(appContextInterpreter, c);
  }

  // /////////////////////////////////////////////////////////////////////////
  //
  // specialized pointer analysis
  //
  // /////////////////////////////////////////////////////////////////////////

  @Override
  protected PropagationSystem makeSystem(AnalysisOptions options) {
    return new PropagationSystem(callGraph, pointerKeyFactory, instanceKeyFactory) {
      @Override
      public PointerAnalysis<InstanceKey> makePointerAnalysis(PropagationCallGraphBuilder builder) {
        return new AstPointerAnalysisImpl(builder, cg, pointsToMap, instanceKeys, pointerKeyFactory, instanceKeyFactory);
      }
    };
  }

  public static class AstPointerAnalysisImpl extends PointerAnalysisImpl {

    public AstPointerAnalysisImpl(PropagationCallGraphBuilder builder, CallGraph cg, PointsToMap pointsToMap,
        MutableMapping<InstanceKey> instanceKeys, PointerKeyFactory pointerKeys, InstanceKeyFactory iKeyFactory) {
      super(builder, cg, pointsToMap, instanceKeys, pointerKeys, iKeyFactory);
    }

    @Override
    protected HeapModel makeHeapModel() {
      class Model extends HModel implements AstHeapModel {
        @Override
        public PointerKey getPointerKeyForArrayLength(InstanceKey I) {
          return new ArrayLengthKey(I);
        }

        @Override
        public Iterator<PointerKey> getPointerKeysForReflectedFieldRead(InstanceKey I, InstanceKey F) {
          return ((AstPointerKeyFactory)pointerKeys).getPointerKeysForReflectedFieldRead(I, F);
        }

        @Override
        public Iterator<PointerKey> getPointerKeysForReflectedFieldWrite(InstanceKey I, InstanceKey F) {
          return ((AstPointerKeyFactory)pointerKeys).getPointerKeysForReflectedFieldWrite(I, F);
        }

        @Override
        public PointerKey getPointerKeyForObjectCatalog(InstanceKey I) {
          return ((AstPointerKeyFactory)pointerKeys).getPointerKeyForObjectCatalog(I);
        }
      }
      
      return new Model();
    }


    @Override
    protected ImplicitPointsToSetVisitor makeImplicitPointsToVisitor(LocalPointerKey lpk) {
      return new AstImplicitPointsToSetVisitor(this, lpk);
    }

    public static class AstImplicitPointsToSetVisitor extends ImplicitPointsToSetVisitor implements AstInstructionVisitor {
      public AstImplicitPointsToSetVisitor(AstPointerAnalysisImpl analysis, LocalPointerKey lpk) {
        super(analysis, lpk);
      }

      @Override
      public void visitAstLexicalRead(AstLexicalRead instruction) {

      }

      @Override
      public void visitAstLexicalWrite(AstLexicalWrite instruction) {

      }

      @Override
      public void visitAstGlobalRead(AstGlobalRead instruction) {
        pointsToSet = analysis.computeImplicitPointsToSetAtGet(node, instruction.getDeclaredField(), -1, true);
      }

      @Override
      public void visitAstGlobalWrite(AstGlobalWrite instruction) {

      }

      @Override
      public void visitAssert(AstAssertInstruction instruction) {

      }

      @Override
      public void visitEachElementGet(EachElementGetInstruction inst) {

      }

      @Override
      public void visitEachElementHasNext(EachElementHasNextInstruction inst) {

      }

      @Override
      public void visitIsDefined(AstIsDefinedInstruction inst) {

      }

      @Override
      public void visitEcho(AstEchoInstruction inst) {

      }
    }
  }

  // /////////////////////////////////////////////////////////////////////////
  //
  // top-level node constraint generation
  //
  // /////////////////////////////////////////////////////////////////////////

  @Override
  protected ExplicitCallGraph createEmptyCallGraph(IClassHierarchy cha, AnalysisOptions options) {
    return new AstCallGraph(cha, options, getAnalysisCache());
  }

  public static class AstInterestingVisitor extends InterestingVisitor implements AstInstructionVisitor {

    public AstInterestingVisitor(int vn) {
      super(vn);
    }

    @Override
    public void visitAstLexicalRead(AstLexicalRead instruction) {
      bingo = true;
    }

    @Override
    public void visitAstLexicalWrite(AstLexicalWrite instruction) {
      bingo = true;
    }

    @Override
    public void visitAstGlobalRead(AstGlobalRead instruction) {
      bingo = true;
    }

    @Override
    public void visitAstGlobalWrite(AstGlobalWrite instruction) {
      bingo = true;
    }

    @Override
    public void visitAssert(AstAssertInstruction instruction) {
      bingo = true;
    }

    @Override
    public void visitEachElementGet(EachElementGetInstruction inst) {
      bingo = true;
    }

    @Override
    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {

    }

    @Override
    public void visitIsDefined(AstIsDefinedInstruction inst) {

    }

    @Override
    public void visitEcho(AstEchoInstruction inst) {

    }
  }

  @Override
  protected InterestingVisitor makeInterestingVisitor(CGNode node, int vn) {
    return new AstInterestingVisitor(vn);
  }

  @Override
  public boolean hasNoInterestingUses(CGNode node, int vn, DefUse du) {
    if (node.getMethod() instanceof AstMethod) {
      // uses in nested functions are interesting
      IntSet uses = ((AstIRFactory.AstIR) node.getIR()).lexicalInfo().getAllExposedUses();
      if (uses.contains(vn)) {
        return false;
      }
    }

    return super.hasNoInterestingUses(node, vn, du);
  }

  // /////////////////////////////////////////////////////////////////////////
  //
  // IR visitor specialization for Ast-specific IR types
  //
  // /////////////////////////////////////////////////////////////////////////

  @Override
  public ConstraintVisitor makeVisitor(CGNode node) {
    return new AstConstraintVisitor(this, node);
  }

  protected static class AstConstraintVisitor extends ConstraintVisitor implements AstInstructionVisitor {

     public AstConstraintVisitor(AstSSAPropagationCallGraphBuilder builder, CGNode node) {
      super(builder, node);
    }

    @Override
    protected AstSSAPropagationCallGraphBuilder getBuilder() {
      return (AstSSAPropagationCallGraphBuilder) builder;
    }

    public PointerKey getPointerKeyForObjectCatalog(InstanceKey I) {
      return ((AstPointerKeyFactory) getBuilder().getPointerKeyFactory()).getPointerKeyForObjectCatalog(I);
    }

    public Iterator<PointerKey> getPointerKeysForReflectedFieldRead(InstanceKey I, InstanceKey F) {
      return ((AstPointerKeyFactory) getBuilder().getPointerKeyFactory()).getPointerKeysForReflectedFieldRead(I, F);
    }

    public Iterator<PointerKey> getPointerKeysForReflectedFieldWrite(InstanceKey I, InstanceKey F) {
      return ((AstPointerKeyFactory) getBuilder().getPointerKeyFactory()).getPointerKeysForReflectedFieldWrite(I, F);
    }

    private static void visitLexical(final LexicalOperator op) {
      op.doLexicalPointerKeys();
      // I have no idea what the code below does, but commenting it out doesn't
      // break any regression tests. --MS
      // if (! checkLexicalInstruction(instruction)) {
      // system.newSideEffect(op, getPointerKeyForLocal(1));
      // }
    }



    @Override
    public void visitAstLexicalRead(AstLexicalRead instruction) {
      visitLexical(new LexicalOperator((AstCGNode) node, instruction.getAccesses(), true) {
        @Override
        protected void action(PointerKey lexicalKey, int vn) {
          PointerKey lval = getPointerKeyForLocal(vn);
          if (lexicalKey instanceof LocalPointerKey) {
            CGNode lnode = ((LocalPointerKey) lexicalKey).getNode();
            int lvn = ((LocalPointerKey) lexicalKey).getValueNumber();
            IRView lir = getBuilder().getCFAContextInterpreter().getIRView(lnode);
            SymbolTable lsymtab = lir.getSymbolTable();
            DefUse ldu = getBuilder().getCFAContextInterpreter().getDU(lnode);
            // DefUse ldu = getAnalysisCache().getDefUse(lir);
            if (contentsAreInvariant(lsymtab, ldu, lvn)) {
              InstanceKey[] ik = getInvariantContents(lsymtab, ldu, lnode, lvn);
              system.recordImplicitPointsToSet(lexicalKey);
              for (InstanceKey element : ik) {
                system.findOrCreateIndexForInstanceKey(element);
                system.newConstraint(lval, element);
              }

              return;
            }
          }

          system.newConstraint(lval, assignOperator, lexicalKey);
        }
      });
    }

    @Override
    public void visitAstLexicalWrite(AstLexicalWrite instruction) {
      visitLexical(new LexicalOperator((AstCGNode) node, instruction.getAccesses(), false) {
        @Override
        protected void action(PointerKey lexicalKey, int vn) {
          PointerKey rval = getPointerKeyForLocal(vn);
          if (contentsAreInvariant(symbolTable, du, vn)) {
            InstanceKey[] ik = getInvariantContents(vn);
            system.recordImplicitPointsToSet(rval);
            for (InstanceKey element : ik) {
              system.findOrCreateIndexForInstanceKey(element);
              system.newConstraint(lexicalKey, element);
            }
          } else {
            system.newConstraint(lexicalKey, assignOperator, rval);
          }
        }
      });
    }

    @Override
    public void visitAstGlobalRead(AstGlobalRead instruction) {
      visitGetInternal(instruction.getDef(), -1, true, instruction.getDeclaredField());
    }

    @Override
    public void visitAstGlobalWrite(AstGlobalWrite instruction) {
      visitPutInternal(instruction.getVal(), -1, true, instruction.getDeclaredField());
    }

    @Override
    public void visitPut(SSAPutInstruction inst) {
      super.visitPut(inst);

      if (inst.isStatic() || !getBuilder().useObjectCatalog())
        return;

      // update the object catalog corresponding to the base pointer, adding the
      // name of the field as a property

      SymbolTable symtab = ir.getSymbolTable();

      int objVn = inst.getRef();
      String fieldName = null;
      try {
        fieldName = inst.getDeclaredField().getName().toUnicodeString();
      } catch (UTFDataFormatException e) {
        Assertions.UNREACHABLE();
      }

      final PointerKey objKey = getPointerKeyForLocal(objVn);

      final InstanceKey[] fieldNameKeys = new InstanceKey[] { getInstanceKeyForConstant(fieldName) };
      assert fieldNameKeys.length == 1;

      if (contentsAreInvariant(symtab, du, objVn)) {
        system.recordImplicitPointsToSet(objKey);
        final InstanceKey[] objKeys = getInvariantContents(objVn);

        for (int i = 0; i < objKeys.length; i++) {
          if (!getBuilder().isUncataloguedField(objKeys[i].getConcreteType(), fieldName)) {
            PointerKey objCatalog = getPointerKeyForObjectCatalog(objKeys[i]);
            if (objCatalog != null) {
              system.newConstraint(objCatalog, fieldNameKeys[0]);
            }
          }
        }

      } else {
        final String hack = fieldName;
        system.newSideEffect(new UnaryOperator<PointsToSetVariable>() {
          @Override
          public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
            final IntSetVariable<?> objects = rhs;
            if (objects.getValue() != null) {
              objects.getValue().foreach(optr -> {
                InstanceKey object = system.getInstanceKey(optr);
                if (!getBuilder().isUncataloguedField(object.getConcreteType(), hack)) {
                  PointerKey cat = getPointerKeyForObjectCatalog(object);
                  if (cat != null) {
                    system.newConstraint(cat, fieldNameKeys[0]);
                  }
                }
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
            return "field name record: " + objKey;
          }
        }, objKey);
      }
    }

    @Override
    public void visitAssert(AstAssertInstruction instruction) {

    }

    @Override
    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {

    }

    @Override
    public void visitEachElementGet(EachElementGetInstruction inst) {
      int lval = inst.getDef(0);
      final PointerKey lk = getPointerKeyForLocal(lval);

      int rval = inst.getUse(0);
      final PointerKey rk = getPointerKeyForLocal(rval);

      if (contentsAreInvariant(symbolTable, du, rval)) {
        InstanceKey objects[] = getInvariantContents(rval);
        for (InstanceKey object : objects) {
          PointerKey catalog = getPointerKeyForObjectCatalog(object);
          system.newConstraint(lk, assignOperator, catalog);
        }
      }

      else {
        system.newSideEffect(new UnaryOperator<PointsToSetVariable>() {
          @Override
          public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
            final IntSetVariable<?> objects = rhs;
            if (objects.getValue() != null) {
              objects.getValue().foreach(optr -> {
                InstanceKey object = system.getInstanceKey(optr);
                PointerKey objCatalog = getPointerKeyForObjectCatalog(object);
                if (objCatalog != null) {
                  system.newConstraint(lk, assignOperator, objCatalog);
                }
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
            return "get catalog op" + rk;
          }
        }, rk);
      }
    }

    @Override
    public void visitIsDefined(AstIsDefinedInstruction inst) {

    }

    @Override
    public void visitEcho(AstEchoInstruction inst) {

    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // lexical scoping handling
    //
    // /////////////////////////////////////////////////////////////////////////

    private abstract class LexicalOperator extends UnaryOperator<PointsToSetVariable> {
      /**
       * node in which lexical accesses are performed
       */
      private final AstCGNode node;

      /**
       * the lexical accesses to be handled
       */
      private final Access[] accesses;

      /**
       * are all the lexical accesses loads? if false, they are all stores
       */
      private final boolean isLoad;

      private LexicalOperator(AstCGNode node, Access[] accesses, boolean isLoad) {
        this.node = node;
        this.isLoad = isLoad;
        this.accesses = accesses;
      }

      /**
       * perform the necessary {@link #action(PointerKey, int)}s for the
       * accesses. For each access, we determine the possible {@link CGNode}s
       * corresponding to its definer (see
       * {@link AstConstraintVisitor#getLexicalDefiners(CGNode, String)}). Handle
       * using
       * {@link AstConstraintVisitor#handleRootLexicalReference(String, String, CGNode)}
       * .
       */
      private void doLexicalPointerKeys() {
        for (Access accesse : accesses) {
          final String name = accesse.variableName;
          final String definer = accesse.variableDefiner;
          final int vn = accesse.valueNumber;

          if (AstTranslator.DEBUG_LEXICAL)
            System.err.println(("looking up lexical parent " + definer));

          Set<CGNode> creators = getLexicalDefiners(node, Pair.make(name, definer));
          
          System.err.println("definers " + creators.size());
          
          for (CGNode n : creators) {
            PointerKey funargKey = handleRootLexicalReference(name, definer, n);
            action(funargKey, vn);
          }
        }
      }

      @Override
      public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
        doLexicalPointerKeys();
        return NOT_CHANGED;
      }

      abstract protected void action(PointerKey lexicalKey, int vn);

      @Override
      public String toString() {
        return "lexical op";
      }

      @Override
      public boolean equals(Object o) {
        if (!(o instanceof LexicalOperator)) {
          return false;
        } else {
          LexicalOperator other = (LexicalOperator) o;

          if (isLoad != other.isLoad) {
            return false;

          } else if (!node.equals(other.node)) {
            return false;

          } else if (accesses.length != other.accesses.length) {
            return false;

          } else {
            for (int i = 0; i < accesses.length; i++) {
              if (!accesses[i].equals(other.accesses[i])) {
                return false;
              }
            }

            for (int i = 0; i < accesses.length; i++) {
              if (!accesses[i].equals(other.accesses[i])) {
                return false;
              }
            }

            return true;
          }
        }
      }

      @Override
      public int hashCode() {
        return node.hashCode() * accesses[0].hashCode() * accesses.length;
      }
    }

    private Set<CGNode> getLexicalDefiners(final CGNode opNode, final Pair<String, String> definer) {
      if (definer == null) {
        return Collections.singleton(getBuilder().getCallGraph().getFakeRootNode());
      } else if (getBuilder().sameMethod(opNode, definer.snd)) {
        // lexical access to a variable declared in opNode itself
        return Collections.singleton(opNode);
      } else {
        final Set<CGNode> result = HashSetFactory.make();
        PointerKey F = getBuilder().getPointerKeyForLocal(opNode, 1);

        IRView ir = getBuilder().getCFAContextInterpreter().getIRView(opNode);
        SymbolTable symtab = ir.getSymbolTable();
        DefUse du = getBuilder().getCFAContextInterpreter().getDU(opNode);
        if (contentsAreInvariant(symtab, du, 1)) {
          system.recordImplicitPointsToSet(F);
          final InstanceKey[] functionKeys = getInvariantContents(symtab, du, opNode, 1);
          for (InstanceKey functionKey : functionKeys) {
            system.findOrCreateIndexForInstanceKey(functionKey);
            ScopeMappingInstanceKey K = (ScopeMappingInstanceKey) functionKey;
            Iterator<CGNode> x = K.getFunargNodes(definer);
            while (x.hasNext()) {
              result.add(x.next());
            }
          }
        } else {
          PointsToSetVariable FV = system.findOrCreatePointsToSet(F);
          if (FV.getValue() != null) {
            FV.getValue().foreach(ptr -> {
              InstanceKey iKey = system.getInstanceKey(ptr);
              if (iKey instanceof ScopeMappingInstanceKey) {
                ScopeMappingInstanceKey K = (ScopeMappingInstanceKey) iKey;
                Iterator<CGNode> x = K.getFunargNodes(definer);
                while (x.hasNext()) {
                  result.add(x.next());
                }
              } else {
                Assertions.UNREACHABLE("unexpected instance key " + iKey);
              }
            });
          }
        }

        return result;
      }
    }

    private Set<PointerKey> discoveredUpwardFunargs = HashSetFactory.make();

    /**
     * add constraints that assign the final value of name in definingNode to
     * the upward funarg (lhs), modeling adding of the state to the closure
     */
    private void addUpwardFunargConstraints(PointerKey lhs, String name, String definer, CGNode definingNode) {
      discoveredUpwardFunargs.add(lhs);

      LexicalInformation LI = ((AstIRFactory.AstIR) definingNode.getIR()).lexicalInfo();
      Pair<String, String>[] names = LI.getExposedNames();
      for (int i = 0; i < names.length; i++) {
        if (name.equals(names[i].fst) && definer.equals(names[i].snd)) {
          int vn = LI.getExitExposedUses()[i];
          if (vn > 0) {
            IRView ir = getBuilder().getCFAContextInterpreter().getIRView(definingNode);
            DefUse du = getBuilder().getCFAContextInterpreter().getDU(definingNode);
            SymbolTable st = ir.getSymbolTable();

            PointerKey rhs = getBuilder().getPointerKeyForLocal(definingNode, vn);

            if (contentsAreInvariant(st, du, vn)) {
              system.recordImplicitPointsToSet(rhs);
              final InstanceKey[] objs = getInvariantContents(st, du, definingNode, vn);
              for (InstanceKey obj : objs) {
                system.findOrCreateIndexForInstanceKey(obj);
                system.newConstraint(lhs, obj);
              }
            } else {
              system.newConstraint(lhs, assignOperator, rhs);
            }
          }

          return;
        }
      }

      Assertions.UNREACHABLE();
    }

    /**
     * handle a lexical reference where we found no parent call graph node
     * defining the name; it's either a global or an upward funarg
     */
    private PointerKey handleRootLexicalReference(String name, String definer, final CGNode definingNode) {
      // global variable
      if (definer == null) {
        return new AstGlobalPointerKey(name);

        // upward funarg
      } else {
        class UpwardFunargPointerKey extends AstGlobalPointerKey {
          UpwardFunargPointerKey(String name) {
            super(name);
          }

          public CGNode getDefiningNode() {
            return definingNode;
          }

          @Override
          public boolean equals(Object x) {
            return (x instanceof UpwardFunargPointerKey)
                && super.equals(x)
                && (definingNode == null ? definingNode == ((UpwardFunargPointerKey) x).getDefiningNode() : definingNode
                    .equals(((UpwardFunargPointerKey) x).getDefiningNode()));
          }

          @Override
          public int hashCode() {
            return super.hashCode() * ((definingNode == null) ? 17 : definingNode.hashCode());
          }

          @Override
          public String toString() {
            return "[upward:" + getName() + ":" + definingNode + "]";
          }
        }

        PointerKey result = new UpwardFunargPointerKey(name);

        if (!discoveredUpwardFunargs.contains(result) && definingNode != null) {
          addUpwardFunargConstraints(result, name, definer, definingNode);
        }

        return result;
      }
    }

    

    // /////////////////////////////////////////////////////////////////////////
    //
    // property manipulation handling
    //
    // /////////////////////////////////////////////////////////////////////////

    protected interface ReflectedFieldAction {
      void action(AbstractFieldPointerKey fieldKey);

      void dump(AbstractFieldPointerKey fieldKey, boolean constObj, boolean constProp);
    }

    private void newFieldOperation(CGNode opNode, final int objVn, final int fieldsVn, final boolean isLoadOperation,
        final ReflectedFieldAction action) {
      IRView ir = getBuilder().getCFAContextInterpreter().getIRView(opNode);
      SymbolTable symtab = ir.getSymbolTable();
      DefUse du = getBuilder().getCFAContextInterpreter().getDU(opNode);
      PointerKey objKey = getBuilder().getPointerKeyForLocal(opNode, objVn);
      final PointerKey fieldKey = getBuilder().getPointerKeyForLocal(opNode, fieldsVn);

      // log field access
      if (DEBUG_PROPERTIES) {
        if (isLoadOperation)
          System.err.print(("adding read of " + objKey + "." + fieldKey + ":"));
        else
          System.err.print(("adding write of " + objKey + "." + fieldKey + ":"));

        if (contentsAreInvariant(symtab, du, objVn)) {
          System.err.print(" constant obj:");
          InstanceKey[] x = getInvariantContents(symtab, du, opNode, objVn);
          for (InstanceKey element : x) {
            System.err.print((element.toString() + " "));
          }
        } else {
          System.err.print((" obj:" + system.findOrCreatePointsToSet(objKey)));
        }

        if (contentsAreInvariant(symtab, du, fieldsVn)) {
          System.err.print(" constant prop:");
          InstanceKey[] x = getInvariantContents(symtab, du, opNode, fieldsVn);
          for (InstanceKey element : x) {
            System.err.print((element.toString() + " "));
          }
        } else {
          System.err.print((" props:" + system.findOrCreatePointsToSet(fieldKey)));
        }

        System.err.print("\n");
      }

      // make sure instance keys get mapped for PointerAnalysisImpl
      if (contentsAreInvariant(symtab, du, objVn)) {
        InstanceKey[] x = getInvariantContents(symtab, du, opNode, objVn);
        for (InstanceKey element : x) {
          system.findOrCreateIndexForInstanceKey(element);
        }
      }
      if (contentsAreInvariant(symtab, du, fieldsVn)) {
        InstanceKey[] x = getInvariantContents(symtab, du, opNode, fieldsVn);
        for (InstanceKey element : x) {
          system.findOrCreateIndexForInstanceKey(element);
        }
      }

      // process field access
      if (contentsAreInvariant(symtab, du, objVn)) {
        system.recordImplicitPointsToSet(objKey);
        final InstanceKey[] objKeys = getInvariantContents(symtab, du, opNode, objVn);

        if (contentsAreInvariant(symtab, du, fieldsVn)) {
          system.recordImplicitPointsToSet(fieldKey);
          InstanceKey[] fieldsKeys = getInvariantContents(symtab, du, opNode, fieldsVn);

          newFieldOperationObjectAndFieldConstant(isLoadOperation, action, objKeys, fieldsKeys);

        } else {
          newFieldOperationOnlyObjectConstant(isLoadOperation, action, fieldKey, objKeys);
        }

      } else {
        if (contentsAreInvariant(symtab, du, fieldsVn)) {
          system.recordImplicitPointsToSet(fieldKey);
          final InstanceKey[] fieldsKeys = getInvariantContents(symtab, du, opNode, fieldsVn);

          newFieldOperationOnlyFieldConstant(isLoadOperation, action, objKey, fieldsKeys);

        } else {
          newFieldFullOperation(isLoadOperation, action, objKey, fieldKey);
        }
      }

      if (DEBUG_PROPERTIES) {
        System.err.println("finished\n");
      }
    }

    protected void newFieldOperationFieldConstant(CGNode opNode, final boolean isLoadOperation, final ReflectedFieldAction action,
        final int objVn, final InstanceKey[] fieldsKeys) {
      IRView ir = getBuilder().getCFAContextInterpreter().getIRView(opNode);
      SymbolTable symtab = ir.getSymbolTable();
      DefUse du = getBuilder().getCFAContextInterpreter().getDU(opNode);
      PointerKey objKey = getBuilder().getPointerKeyForLocal(opNode, objVn);

      if (contentsAreInvariant(symtab, du, objVn)) {
        system.recordImplicitPointsToSet(objKey);
        InstanceKey[] objectKeys = getInvariantContents(symtab, du, opNode, objVn);

        newFieldOperationObjectAndFieldConstant(isLoadOperation, action, objectKeys, fieldsKeys);

      } else {
        newFieldOperationOnlyFieldConstant(isLoadOperation, action, objKey, fieldsKeys);
      }

    }

    protected void newFieldFullOperation(final boolean isLoadOperation, final ReflectedFieldAction action, PointerKey objKey,
        final PointerKey fieldKey) {
      system.newSideEffect(new AbstractOperator<PointsToSetVariable>() {
        private final MutableIntSet doneReceiver = IntSetUtil.make();
        private final MutableIntSet doneField = IntSetUtil.make();

        @Override
        public byte evaluate(PointsToSetVariable lhs, final PointsToSetVariable[] rhs) {
          final IntSetVariable<?> receivers = rhs[0];
          final IntSetVariable<?> fields = rhs[1];
          if (receivers.getValue() != null && fields.getValue() != null) {
            receivers.getValue().foreach(rptr -> {
              final InstanceKey receiver = system.getInstanceKey(rptr);

              if (!isLoadOperation) {
                PointerKey cat = getPointerKeyForObjectCatalog(receiver);
                if (cat != null) {
                  system.newConstraint(cat, assignOperator, fieldKey);
                }
              }

              fields.getValue().foreach(fptr -> {
                if (!doneField.contains(fptr) || !doneReceiver.contains(rptr)) {
                  InstanceKey field = system.getInstanceKey(fptr);
                  for (PointerKey pkey : Iterator2Iterable.make(isLoadOperation ? getPointerKeysForReflectedFieldRead(receiver, field)
                      : getPointerKeysForReflectedFieldWrite(receiver, field))) {
                    AbstractFieldPointerKey key = (AbstractFieldPointerKey) pkey;
                    if (DEBUG_PROPERTIES)
                      action.dump(key, false, false);
                    action.action(key);
                  }
                }
              });
            });
            doneReceiver.addAll(receivers.getValue());
            doneField.addAll(fields.getValue());
          }

          return NOT_CHANGED;
        }

        @Override
        public String toString() {
          return "field op";
        }

        @Override
        public boolean equals(Object o) {
          return o == this;
        }

        @Override
        public int hashCode() {
          return System.identityHashCode(this);
        }
      }, objKey, fieldKey);
    }

    protected void newFieldOperationOnlyFieldConstant(final boolean isLoadOperation, final ReflectedFieldAction action,
        final PointerKey objKey, final InstanceKey[] fieldsKeys) {
      system.newSideEffect(new UnaryOperator<PointsToSetVariable>() {
        @Override
        public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
          final IntSetVariable<?> objects = rhs;
          if (objects.getValue() != null) {
            objects.getValue().foreach(optr -> {
              InstanceKey object = system.getInstanceKey(optr);
              PointerKey objCatalog = getPointerKeyForObjectCatalog(object);
              for (InstanceKey fieldsKey : fieldsKeys) {
                if (isLoadOperation) {
                  for (PointerKey pkey : Iterator2Iterable.make(getPointerKeysForReflectedFieldRead(object, fieldsKey))) {
                    AbstractFieldPointerKey key = (AbstractFieldPointerKey) pkey;
                    if (DEBUG_PROPERTIES)
                      action.dump(key, true, false);
                    action.action(key);
                  }
                } else {
                  if (objCatalog != null) {
                    system.newConstraint(objCatalog, fieldsKey);
                  }
                  for (PointerKey pkey : Iterator2Iterable.make(getPointerKeysForReflectedFieldWrite(object, fieldsKey))) {
                    AbstractFieldPointerKey key = (AbstractFieldPointerKey) pkey;
                    if (DEBUG_PROPERTIES)
                      action.dump(key, true, false);
                    action.action(key);
                  }
                }
              }
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
          return "field op" + objKey;
        }
      }, objKey);
    }

    protected void newFieldOperationOnlyObjectConstant(final boolean isLoadOperation, final ReflectedFieldAction action,
        final PointerKey fieldKey, final InstanceKey[] objKeys) {
      if (!isLoadOperation) {
        for (InstanceKey objKey : objKeys) {
          PointerKey objCatalog = getPointerKeyForObjectCatalog(objKey);
          if (objCatalog != null) {
            system.newConstraint(objCatalog, assignOperator, fieldKey);
          }
        }
      }

      system.newSideEffect(new UnaryOperator<PointsToSetVariable>() {
        @Override
        public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
          final IntSetVariable<?> fields = rhs;
          if (fields.getValue() != null) {
            fields.getValue().foreach(fptr -> {
              InstanceKey field = system.getInstanceKey(fptr);
              for (InstanceKey objKey : objKeys) {
                for (PointerKey pkey : Iterator2Iterable.make(isLoadOperation ? getPointerKeysForReflectedFieldRead(objKey, field)
                    : getPointerKeysForReflectedFieldWrite(objKey, field))) {
                  AbstractFieldPointerKey key = (AbstractFieldPointerKey) pkey;
                  if (DEBUG_PROPERTIES)
                    action.dump(key, false, true);
                  action.action(key);
                }
              }
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
          return "field op" + fieldKey;
        }
      }, fieldKey);
    }

    protected void newFieldOperationObjectAndFieldConstant(final boolean isLoadOperation, final ReflectedFieldAction action,
        final InstanceKey[] objKeys, InstanceKey[] fieldsKeys) {
      for (InstanceKey objKey : objKeys) {
        PointerKey objCatalog = getPointerKeyForObjectCatalog(objKey);
        for (InstanceKey fieldsKey : fieldsKeys) {
          if (isLoadOperation) {
            for (PointerKey pkey : Iterator2Iterable.make(getPointerKeysForReflectedFieldRead(objKey, fieldsKey))) {
              AbstractFieldPointerKey key = (AbstractFieldPointerKey) pkey;
              if (DEBUG_PROPERTIES)
                action.dump(key, true, true);
              action.action(key);
            }
          } else {
            if (objCatalog != null) {
              system.newConstraint(objCatalog, fieldsKey);
            }
            for (PointerKey pkey : Iterator2Iterable.make(getPointerKeysForReflectedFieldWrite(objKey, fieldsKey))) {
              AbstractFieldPointerKey key = (AbstractFieldPointerKey) pkey;
              if (DEBUG_PROPERTIES)
                action.dump(key, true, true);
              action.action(key);
            }
          }
        }
      }
    }

    public void newFieldWrite(CGNode opNode, int objVn, int fieldsVn, int rhsVn) {
      IRView ir = getBuilder().getCFAContextInterpreter().getIRView(opNode);
      SymbolTable symtab = ir.getSymbolTable();
      DefUse du = getBuilder().getCFAContextInterpreter().getDU(opNode);
      PointerKey rhsKey = getBuilder().getPointerKeyForLocal(opNode, rhsVn);
      if (contentsAreInvariant(symtab, du, rhsVn)) {
        system.recordImplicitPointsToSet(rhsKey);
        newFieldWrite(opNode, objVn, fieldsVn, getInvariantContents(symtab, du, opNode, rhsVn));
      } else {
        newFieldWrite(opNode, objVn, fieldsVn, rhsKey);
      }
    }

    private final class ConstantWriter implements ReflectedFieldAction {
      private final InstanceKey[] rhsFixedValues;

      private ConstantWriter(InstanceKey[] rhsFixedValues) {
        this.rhsFixedValues = rhsFixedValues;
      }

      @Override
      public void dump(AbstractFieldPointerKey fieldKey, boolean constObj, boolean constProp) {
        System.err.println(("writing fixed rvals to " + fieldKey + " " + constObj + ", " + constProp));
        for (InstanceKey rhsFixedValue : rhsFixedValues) {
          System.err.println(("writing " + rhsFixedValue));
        }
      }

      @Override
      public void action(AbstractFieldPointerKey fieldKey) {
        if (!representsNullType(fieldKey.getInstanceKey())) {
          for (InstanceKey rhsFixedValue : rhsFixedValues) {
            system.findOrCreateIndexForInstanceKey(rhsFixedValue);
            system.newConstraint(fieldKey, rhsFixedValue);
          }
        }
      }
    }

    public void newFieldWrite(CGNode opNode, int objVn, int fieldsVn, final InstanceKey[] rhsFixedValues) {
      newFieldOperation(opNode, objVn, fieldsVn, false, new ConstantWriter(rhsFixedValues));
    }

    public void newFieldWrite(CGNode opNode, int objVn, InstanceKey[] fieldKeys, final InstanceKey[] rhsValues) {
      newFieldOperationFieldConstant(opNode, false, new ConstantWriter(rhsValues), objVn, fieldKeys);
    }

    private final class NormalWriter implements ReflectedFieldAction {
      private final PointerKey rhs;

      private NormalWriter(PointerKey rhs) {
        this.rhs = rhs;
      }

      @Override
      public void dump(AbstractFieldPointerKey fieldKey, boolean constObj, boolean constProp) {
        System.err.println(("write " + rhs + " to " + fieldKey + " " + constObj + ", " + constProp));
      }

      @Override
      public void action(AbstractFieldPointerKey fieldKey) {
        if (!representsNullType(fieldKey.getInstanceKey())) {
          system.newConstraint(fieldKey, assignOperator, rhs);
        }
      }
    }

    public void newFieldWrite(CGNode opNode, int objVn, int fieldsVn, final PointerKey rhs) {
      newFieldOperation(opNode, objVn, fieldsVn, false, new NormalWriter(rhs));
    }

    public void newFieldWrite(CGNode opNode, int objVn, InstanceKey[] fieldKeys, final PointerKey rhs) {
      newFieldOperationFieldConstant(opNode, false, new NormalWriter(rhs), objVn, fieldKeys);
    }

    protected void newFieldRead(CGNode opNode, int objVn, int fieldsVn, int lhsVn) {
      newFieldRead(opNode, objVn, fieldsVn, getBuilder().getPointerKeyForLocal(opNode, lhsVn));
    }

    protected void newFieldRead(CGNode opNode, int objVn, int fieldsVn, final PointerKey lhs) {
      newFieldOperation(opNode, objVn, fieldsVn, true, new ReflectedFieldAction() {
        @Override
        public void dump(AbstractFieldPointerKey fieldKey, boolean constObj, boolean constProp) {
          System.err.println(("read " + lhs + " from " + fieldKey + " " + constObj + ", " + constProp));
        }

        @Override
        public void action(AbstractFieldPointerKey fieldKey) {
          if (!representsNullType(fieldKey.getInstanceKey())) {
            system.newConstraint(lhs, assignOperator, fieldKey);
            AbstractFieldPointerKey unknown = getBuilder().fieldKeyForUnknownWrites(fieldKey);
            if (unknown != null) {
              system.newConstraint(lhs, assignOperator, unknown);            
            }
          }
        }
      });
    }
  }

  /**
   * If the given fieldKey represents a concrete field, return the corresponding field key that
   * represents all writes to unknown fields that could potentially alias fieldKey
   */
  protected abstract AbstractFieldPointerKey fieldKeyForUnknownWrites(AbstractFieldPointerKey fieldKey);
  
  /**
   * 
   * Is definingMethod the same as the method represented by opNode?  We need this since the names for 
   * methods in some languages don't map in the straightforward way to the CGNode 
   */
  protected abstract boolean sameMethod(final CGNode opNode, final String definingMethod);

}
