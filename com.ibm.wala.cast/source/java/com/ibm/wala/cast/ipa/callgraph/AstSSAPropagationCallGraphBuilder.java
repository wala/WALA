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
import com.ibm.wala.cast.ir.ssa.AbstractLexicalInvoke;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstIRFactory.AstIR;
import com.ibm.wala.cast.ir.ssa.AstInstructionVisitor;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.cast.ir.ssa.SSAConversion;
import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.fixedpoint.impl.AbstractOperator;
import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.fixpoint.IntSetVariable;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysisImpl;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointsToMap;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.functions.Function;
import com.ibm.wala.util.graph.traverse.NumberedDFSDiscoverTimeIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableMapping;

public abstract class AstSSAPropagationCallGraphBuilder extends SSAPropagationCallGraphBuilder {

  public static final boolean DEBUG_TYPE_INFERENCE = false;

  public static final boolean DEBUG_PROPERTIES = false;

  // /////////////////////////////////////////////////////////////////////////
  //
  // language specialization interface
  //
  // /////////////////////////////////////////////////////////////////////////

  protected abstract boolean useObjectCatalog();

  protected boolean isUncataloguedField(IClass type, String fieldName) {
    return false;
  }
  
  // /////////////////////////////////////////////////////////////////////////
  //
  // overall control
  //
  // /////////////////////////////////////////////////////////////////////////

  protected AstSSAPropagationCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache, PointerKeyFactory pointerKeyFactory) {
    super(cha, options, cache, pointerKeyFactory);
  }

  public SSAContextInterpreter makeDefaultContextInterpreters(SSAContextInterpreter appContextInterpreter, AnalysisOptions options,
      IClassHierarchy cha) {
    SSAContextInterpreter c = new DefaultSSAInterpreter(options, getAnalysisCache());
    c = new DelegatingSSAContextInterpreter(new AstContextInsensitiveSSAContextInterpreter(options, getAnalysisCache()), c);

    c = new DelegatingSSAContextInterpreter(new LexicalScopingSSAContextInterpreter(options, getAnalysisCache()), c);

    c = new DelegatingSSAContextInterpreter(ReflectionContextInterpreter.createReflectionContextInterpreter(cha, options, getAnalysisCache()), c);

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

  public static class AstPointerAnalysisImpl extends PointerAnalysisImpl {

    public AstPointerAnalysisImpl(PropagationCallGraphBuilder builder, CallGraph cg, PointsToMap pointsToMap,
        MutableMapping<InstanceKey> instanceKeys, PointerKeyFactory pointerKeys, InstanceKeyFactory iKeyFactory) {
      super(builder, cg, pointsToMap, instanceKeys, pointerKeys, iKeyFactory);
    }

    protected ImplicitPointsToSetVisitor makeImplicitPointsToVisitor(LocalPointerKey lpk) {
      return new AstImplicitPointsToSetVisitor(this, lpk);
    }

    public static class AstImplicitPointsToSetVisitor extends ImplicitPointsToSetVisitor implements AstInstructionVisitor {
      public AstImplicitPointsToSetVisitor(AstPointerAnalysisImpl analysis, LocalPointerKey lpk) {
        super(analysis, lpk);
      }

      public void visitAstLexicalRead(AstLexicalRead instruction) {

      }

      public void visitAstLexicalWrite(AstLexicalWrite instruction) {

      }

      public void visitAstGlobalRead(AstGlobalRead instruction) {
        pointsToSet = analysis.computeImplicitPointsToSetAtGet(node, instruction.getDeclaredField(), -1, true);
      }

      public void visitAstGlobalWrite(AstGlobalWrite instruction) {

      }

       public void visitAssert(AstAssertInstruction instruction) {

      }

      public void visitEachElementGet(EachElementGetInstruction inst) {

      }

      public void visitEachElementHasNext(EachElementHasNextInstruction inst) {

      }

      public void visitIsDefined(AstIsDefinedInstruction inst) {

      }
      
      public void visitEcho(AstEchoInstruction inst) {

      }
    }
  };

  // /////////////////////////////////////////////////////////////////////////
  //
  // top-level node constraint generation
  //
  // /////////////////////////////////////////////////////////////////////////

  protected ExplicitCallGraph createEmptyCallGraph(IClassHierarchy cha, AnalysisOptions options) {
    return new AstCallGraph(cha, options, getAnalysisCache());
  }

  public static class AstInterestingVisitor extends InterestingVisitor implements AstInstructionVisitor {

    public AstInterestingVisitor(int vn) {
      super(vn);
    }

    public void visitAstLexicalRead(AstLexicalRead instruction) {
      bingo = true;
    }

    public void visitAstLexicalWrite(AstLexicalWrite instruction) {
      bingo = true;
    }

    public void visitAstGlobalRead(AstGlobalRead instruction) {
      bingo = true;
    }

    public void visitAstGlobalWrite(AstGlobalWrite instruction) {
      bingo = true;
    }

    public void visitAssert(AstAssertInstruction instruction) {
      bingo = true;
    }

    public void visitEachElementGet(EachElementGetInstruction inst) {
      bingo = true;
    }

    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {

    }

    public void visitIsDefined(AstIsDefinedInstruction inst) {

    }

    public void visitEcho(AstEchoInstruction inst) {

    }
  }

  protected InterestingVisitor makeInterestingVisitor(CGNode node, int vn) {
    return new AstInterestingVisitor(vn);
  }

  public boolean hasNoInterestingUses(CGNode node, int vn, DefUse du) {
    if (node.getMethod() instanceof AstMethod) {
      IntSet uses = ((AstIR) node.getIR()).lexicalInfo().getAllExposedUses();
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

  protected ConstraintVisitor makeVisitor(ExplicitCallGraph.ExplicitNode node) {
    return new AstConstraintVisitor(this, node);
  }

  protected static class AstConstraintVisitor extends ConstraintVisitor implements AstInstructionVisitor {

    private final CallGraph cg;

    public AstConstraintVisitor(AstSSAPropagationCallGraphBuilder builder, ExplicitCallGraph.ExplicitNode node) {
      super(builder, node);
      this.cg = builder.callGraph;
    }

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

    private void visitLexical(final LexicalOperator op) {
      final PointerKey function = getPointerKeyForLocal(1);
      if (contentsAreInvariant(symbolTable, du, 1)) {
        op.doLexicalPointerKeys();
      } else {
        system.newSideEffect(op, function);
      }

      class LexicalScopingCallback implements Function<Object, Object> {
        public Object apply(Object ignore) {
          op.doLexicalPointerKeys();
          return null;
        }

        private LexicalOperator getOperator() {
          return op;
        }

        public int hashCode() {
          return op.hashCode();
        }

        public boolean equals(Object o) {
          return (o instanceof LexicalScopingCallback) && ((LexicalScopingCallback) o).getOperator().equals(op);
        }
      }

      ((AstCGNode) node).addCallback(new LexicalScopingCallback());
    }

    public void visitAstLexicalRead(AstLexicalRead instruction) {
      visitLexical(new LexicalOperator((AstCGNode) node, instruction.getAccesses(), true) {
        protected void action(PointerKey lexicalKey, int vn) {
          PointerKey lval = getPointerKeyForLocal(vn);
          if (lexicalKey instanceof LocalPointerKey) {
            CGNode lnode = ((LocalPointerKey) lexicalKey).getNode();
            int lvn = ((LocalPointerKey) lexicalKey).getValueNumber();
            IR lir = getBuilder().getCFAContextInterpreter().getIR(lnode);
            SymbolTable lsymtab = lir.getSymbolTable();
            DefUse ldu = getAnalysisCache().getSSACache().findOrCreateDU(lir, lnode.getContext());
            if (contentsAreInvariant(lsymtab, ldu, lvn)) {
              InstanceKey[] ik = getInvariantContents(lsymtab, ldu, lnode, lvn);
              system.recordImplicitPointsToSet(lexicalKey);
              for (int i = 0; i < ik.length; i++) {
                system.findOrCreateIndexForInstanceKey(ik[i]);
                system.newConstraint(lval, ik[i]);
              }

              return;
            }
          }

          system.newConstraint(lval, assignOperator, lexicalKey);
        }
      });
    }

    public void visitAstLexicalWrite(AstLexicalWrite instruction) {
      visitLexical(new LexicalOperator((AstCGNode) node, instruction.getAccesses(), false) {
        protected void action(PointerKey lexicalKey, int vn) {
          PointerKey rval = getPointerKeyForLocal(vn);
          if (contentsAreInvariant(symbolTable, du, vn)) {
            InstanceKey[] ik = getInvariantContents(vn);
            system.recordImplicitPointsToSet(rval);
            for (int i = 0; i < ik.length; i++) {
              system.findOrCreateIndexForInstanceKey(ik[i]);
              system.newConstraint(lexicalKey, ik[i]);
            }
          } else {
            system.newConstraint(lexicalKey, assignOperator, rval);
          }
        }
      });
    }

    public void visitAstGlobalRead(AstGlobalRead instruction) {
      visitGetInternal(instruction.getDef(), -1, true, instruction.getDeclaredField());
    }

    public void visitAstGlobalWrite(AstGlobalWrite instruction) {
      visitPutInternal(instruction.getVal(), -1, true, instruction.getDeclaredField());
    }

    public void visitPut(SSAPutInstruction inst) {
      super.visitPut(inst);

      if (inst.isStatic() || !getBuilder().useObjectCatalog())
        return;

      SymbolTable symtab = ir.getSymbolTable();

      int objVn = inst.getRef();
      String fieldName = null;
      try {
        fieldName = inst.getDeclaredField().getName().toUnicodeString();
      } catch (UTFDataFormatException e) {
        Assertions.UNREACHABLE();
      }
      int fieldNameVn = symtab.getConstant(fieldName);

      final PointerKey objKey = getPointerKeyForLocal(objVn);

      final InstanceKey[] fieldNameKeys = getInvariantContents(fieldNameVn);
      assert fieldNameKeys.length == 1;

      if (contentsAreInvariant(symtab, du, objVn)) {
        system.recordImplicitPointsToSet(objKey);
        final InstanceKey[] objKeys = getInvariantContents(objVn);

        for (int i = 0; i < objKeys.length; i++) {
          if (! getBuilder().isUncataloguedField(objKeys[i].getConcreteType(), fieldName)) {
            PointerKey objCatalog = getPointerKeyForObjectCatalog(objKeys[i]);
            if (objCatalog != null) {
              system.newConstraint(objCatalog, fieldNameKeys[0]);
            }
          }
        }

      } else {
        final String hack = fieldName;
        system.newSideEffect(new UnaryOperator<PointsToSetVariable>() {
          public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
            final IntSetVariable objects = (IntSetVariable) rhs;
            if (objects.getValue() != null) {
              objects.getValue().foreach(new IntSetAction() {
                public void act(int optr) {
                  InstanceKey object = system.getInstanceKey(optr);
                  if (! getBuilder().isUncataloguedField(object.getConcreteType(), hack)) {
                    PointerKey cat = getPointerKeyForObjectCatalog(object);
                    if (cat != null) {
                      system.newConstraint(cat, fieldNameKeys[0]);
                    }
                  }
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
            return "field name record: " + objKey;
          }
        }, objKey);
      }
    }

    public void visitAssert(AstAssertInstruction instruction) {

    }

    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {

    }

    public void visitEachElementGet(EachElementGetInstruction inst) {
      int lval = inst.getDef(0);
      final PointerKey lk = getPointerKeyForLocal(lval);

      int rval = inst.getUse(0);
      final PointerKey rk = getPointerKeyForLocal(rval);

      if (contentsAreInvariant(symbolTable, du, rval)) {
        InstanceKey objects[] = getInvariantContents(rval);
        for (int i = 0; i < objects.length; i++) {
          PointerKey catalog = getPointerKeyForObjectCatalog(objects[i]);
          system.newConstraint(lk, assignOperator, catalog);
        }
      }

      else {
        system.newSideEffect(new UnaryOperator<PointsToSetVariable>() {
          public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
            final IntSetVariable objects = (IntSetVariable) rhs;
            if (objects.getValue() != null) {
              objects.getValue().foreach(new IntSetAction() {
                public void act(int optr) {
                  InstanceKey object = system.getInstanceKey(optr);
                  PointerKey objCatalog = getPointerKeyForObjectCatalog(object);
                  if (objCatalog != null) {
                    system.newConstraint(lk, assignOperator, objCatalog);
                  }
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
            return "get catalog op" + rk;
          }
        }, rk);
      }
    }

    public void visitIsDefined(AstIsDefinedInstruction inst) {

    }

    public void visitEcho(AstEchoInstruction inst) {

    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // lexical scoping handling
    //
    // /////////////////////////////////////////////////////////////////////////

    private abstract class LexicalOperator extends UnaryOperator<PointsToSetVariable> {
      private final AstCGNode node;

      private final Access[] accesses;

      private final boolean isLoad;

      private LexicalOperator(AstCGNode node, Access[] accesses, boolean isLoad) {
        this.node = node;
        this.isLoad = isLoad;
        this.accesses = accesses;
      }

      private void doLexicalPointerKeys() {
        for (int i = 0; i < accesses.length; i++) {
          final String name = accesses[i].variableName;
          final String definer = accesses[i].variableDefiner;
          final int vn = accesses[i].valueNumber;

          if (AstTranslator.DEBUG_LEXICAL)
            System.err.println(("looking up lexical parent " + definer));

          for (Iterator<CGNode> DS = getLexicalDefiners(node, definer).iterator(); DS.hasNext();) {
            final CGNode D = DS.next();

            Iterator PS = new NumberedDFSDiscoverTimeIterator<CGNode>(getBuilder().getCallGraph(), node) {
              /**
               * 
               */
              private static final long serialVersionUID = 4546217460630659884L;

              protected void visitEdge(CGNode callee, CGNode caller) {
                CGNode from = (CGNode) caller;
                CGNode to = (CGNode) callee;

                for (Iterator SS = cg.getPossibleSites(from, to); SS.hasNext();) {
                  CallSiteReference site = (CallSiteReference) SS.next();

                  PointerKey V = isLoad ? getLocalReadKey(from, site, name, definer, D) : getLocalWriteKey(from, site, name,
                      definer, D);

                  if (V != null) {
                    action(V, vn);
                  }
                }
              }

              protected Iterator<? extends CGNode> getConnected(CGNode n) {
                if (n.equals(D))
                  return EmptyIterator.instance();
                else
                  return G.getPredNodes(n);
              }
            };

            while (PS.hasNext()) {
              PS.next();
            }
          }
        }
      }

      public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
        doLexicalPointerKeys();
        return NOT_CHANGED;
      }

      abstract protected void action(PointerKey lexicalKey, int vn);

      public String toString() {
        return "lexical op";
      }

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

      public int hashCode() {
        return node.hashCode() * accesses[0].hashCode() * accesses.length;
      }
    }

    private Set<CGNode> getLexicalDefiners(final CGNode opNode, final String definer) {
      if (definer == null) {
        return Collections.singleton(getBuilder().getCallGraph().getFakeRootNode());

      } else {
        final Set<CGNode> result = HashSetFactory.make();
        PointerKey F = getBuilder().getPointerKeyForLocal(opNode, 1);

        IR ir = getBuilder().getCFAContextInterpreter().getIR(opNode);
        SymbolTable symtab = ir.getSymbolTable();
        DefUse du = getAnalysisCache().getSSACache().findOrCreateDU(ir, opNode.getContext());
        if (contentsAreInvariant(symtab, du, 1)) {
          system.recordImplicitPointsToSet(F);
          final InstanceKey[] functionKeys = getInvariantContents(symtab, du, opNode, 1);
          for (int f = 0; f < functionKeys.length; f++) {
            system.findOrCreateIndexForInstanceKey(functionKeys[f]);
            ScopeMappingInstanceKey K = (ScopeMappingInstanceKey) functionKeys[f];
            result.add(K.getDefiningNode(definer));
          }
        } else {
          PointsToSetVariable FV = system.findOrCreatePointsToSet(F);
          if (FV.getValue() != null) {
            FV.getValue().foreach(new IntSetAction() {
              public void act(int ptr) {
                InstanceKey iKey = system.getInstanceKey(ptr);
                if (iKey instanceof ScopeMappingInstanceKey) {
                  ScopeMappingInstanceKey K = (ScopeMappingInstanceKey) iKey;
                  result.add(K.getDefiningNode(definer));
                } else {
                  Assertions.UNREACHABLE("unexpected instance key " + iKey);
                }
              }
            });
          }
        }

        return result;
      }
    }

    private boolean isEqual(Object a, Object b) {
      if (a == null)
        return b == null;
      else
        return a.equals(b);
    }

    private Set<PointerKey> discoveredUpwardFunargs = HashSetFactory.make();

    private void addUpwardFunargConstraints(PointerKey lhs, String name, String definer, CGNode definingNode) {
      discoveredUpwardFunargs.add(lhs);

      LexicalInformation LI = ((AstIR) definingNode.getIR()).lexicalInfo();
      Pair[] names = LI.getExposedNames();
      for (int i = 0; i < names.length; i++) {
        if (name.equals(names[i].fst) && definer.equals(names[i].snd)) {
          int vn = LI.getExitExposedUses()[i];
          if (vn > 0) {
            IR ir = getBuilder().getCFAContextInterpreter().getIR(definingNode);
            DefUse du = getBuilder().getCFAContextInterpreter().getDU(definingNode);
            SymbolTable st = ir.getSymbolTable();

            PointerKey rhs = getBuilder().getPointerKeyForLocal(definingNode, vn);

            if (contentsAreInvariant(st, du, vn)) {
              system.recordImplicitPointsToSet(rhs);
              final InstanceKey[] objs = getInvariantContents(st, du, definingNode, vn);
              for (int f = 0; f < objs.length; f++) {
                system.findOrCreateIndexForInstanceKey(objs[f]);
                system.newConstraint(lhs, objs[f]);
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

          public boolean equals(Object x) {
            return (x instanceof UpwardFunargPointerKey) && super.equals(x)
                && (definingNode == null?
                    definingNode == ((UpwardFunargPointerKey) x).getDefiningNode():
                    definingNode.equals(((UpwardFunargPointerKey) x).getDefiningNode()));
          }

          public int hashCode() {
            return super.hashCode() * ((definingNode == null)? 17: definingNode.hashCode());
          }

          public String toString() {
            return "[upward:" + getName() + ":" + definingNode + "]";
          }
        }

        PointerKey result = new UpwardFunargPointerKey(name);

        if (!discoveredUpwardFunargs.contains(result)  && definingNode != null) {
          addUpwardFunargConstraints(result, name, definer, definingNode);
        }

        return result;
      }
    }

    private PointerKey getLocalReadKey(CGNode n, CallSiteReference callSite, String name, String definer, CGNode definingNode) {
      IMethod M = n.getMethod();
      if (n == getBuilder().getCallGraph().getFakeRootNode()) {
        return handleRootLexicalReference(name, definer, definingNode);
      }

      else if (M instanceof AstMethod) {
        AstIR ir = (AstIR) n.getIR();
        int pc = callSite.getProgramCounter();
        LexicalInformation L = ((AstIR) n.getIR()).lexicalInfo();

        // some people have no lexical uses at all
        if (L == null) {
          return null;
        }

        AbstractLexicalInvoke I = (AbstractLexicalInvoke) ir.getInstructions()[pc];

        // find existing explicit lexical use
        for (int i = I.getNumberOfParameters(); i <= I.getLastLexicalUse(); i++) {
          Access A = I.getLexicalUse(i);
          if (A.variableName.equals(name) && isEqual(A.variableDefiner, definer)) {
            return getBuilder().getPointerKeyForLocal(n, A.valueNumber);
          }
        }

        // make new lexical use
        int values[] = L.getExposedUses(pc);
        Pair names[] = L.getExposedNames();
        if (names != null && names.length > 0) {
          for (int i = 0; i < names.length; i++) {
            if (name.equals(names[i].fst) && isEqual(definer, names[i].snd)) {
              if (values[i] == -1)
                return null;

              I.addLexicalUse(new Access(name, definer, values[i]));

              if (SSAConversion.DEBUG_UNDO)
                System.err.println(("copy use #" + (-i - 1) + " to use #" + (I.getNumberOfUses() - 1) + " at inst " + pc));

              SSAConversion.copyUse(ir, pc, -i - 1, pc, I.getNumberOfUses() - 1);

              ((AstCallGraph.AstCGNode)n).setLexicallyMutatedIR(ir);
              
              return getBuilder().getPointerKeyForLocal(n, values[i]);
            }
          }
        }

        return null;

      } else {
        return null;
      }
    }

    private PointerKey getLocalWriteKey(CGNode n, CallSiteReference callSite, String name, String definer, CGNode definingNode) {
      IMethod M = n.getMethod();
      if (n == getBuilder().getCallGraph().getFakeRootNode()) {
        return handleRootLexicalReference(name, definer, definingNode);
      }

      else if (M instanceof AstMethod) {
        AstMethod AstM = (AstMethod) M;
        AstIR ir = (AstIR) n.getIR();
        LexicalInformation L = ir.lexicalInfo();

        // some people have no lexical uses at all
        if (L == null)
          return null;

        int pc = callSite.getProgramCounter();
        AbstractLexicalInvoke I = (AbstractLexicalInvoke) ir.getInstructions()[pc];

        // find existing explicit lexical def
        for (int i = 2; i < I.getNumberOfDefs(); i++) {
          Access A = I.getLexicalDef(i);
          if (A.variableName.equals(name) && isEqual(A.variableDefiner, definer)) {
            return getBuilder().getPointerKeyForLocal(n, A.valueNumber);
          }
        }

        // make new lexical def
        int values[] = L.getExposedUses(pc);
        Pair names[] = L.getExposedNames();
        if (names != null && names.length > 0) {
          for (int i = 0; i < names.length; i++) {
            if (name.equals(names[i].fst) && isEqual(definer, names[i].snd)) {
              if (values[i] == -1)
                return null;

              // if values[i] was altered by copy propagation, we must undo
              // that to ensure we do not bash the wrong value number in the
              // the next steps.
              SSAConversion.undoCopyPropagation(ir, pc, -i - 1);
              // possibly new instruction due to renames, so get it again
              I = (AbstractLexicalInvoke) ir.getInstructions()[pc];

              I.addLexicalDef(new Access(name, definer, values[i]));

              if (SSAConversion.DEBUG_UNDO)
                System.err.println(("new def of " + values[i] + " at inst " + pc + ": " + I));

              // new def has broken SSA form for values[i], so fix for that
              // value
              MutableIntSet vs = IntSetUtil.make();
              vs.add(values[i]);
              SSAConversion.convert(AstM, ir, getOptions().getSSAOptions());

              // now redo analysis
              // TODO: only values[i] uses need to be re-done.
              ir.lexicalInfo().handleAlteration();
              ((AstCallGraph.AstCGNode)n).setLexicallyMutatedIR(ir);
              getAnalysisCache().getSSACache().invalidateDU(M, n.getContext());
              // addConstraintsFromChangedNode(n);
              getBuilder().markChanged(n);

              // get SSA-renamed def from call site instruction
              return getLocalWriteKey(n, callSite, name, definer, definingNode);
            }
          }
        }

        return null;
      } else {
        return null;
      }
    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // property manipulation handling
    //
    // /////////////////////////////////////////////////////////////////////////

    private interface ReflectedFieldAction {
      void action(AbstractFieldPointerKey fieldKey);

      void dump(AbstractFieldPointerKey fieldKey, boolean constObj, boolean constProp);
    }

    private void newFieldOperation(CGNode opNode, final int objVn, final int fieldsVn, final boolean isLoadOperation,
        final ReflectedFieldAction action) {
      IR ir = getBuilder().getCFAContextInterpreter().getIR(opNode);
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
          for (int i = 0; i < x.length; i++) {
            System.err.print((x[i].toString() + " "));
          }
        } else {
          System.err.print((" obj:" + system.findOrCreatePointsToSet(objKey)));
        }

        if (contentsAreInvariant(symtab, du, fieldsVn)) {
          System.err.print(" constant prop:");
          InstanceKey[] x = getInvariantContents(symtab, du, opNode, fieldsVn);
          for (int i = 0; i < x.length; i++) {
            System.err.print((x[i].toString() + " "));
          }
        } else {
          System.err.print((" props:" + system.findOrCreatePointsToSet(fieldKey)));
        }

        System.err.print("\n");
      }

      // make sure instance keys get mapped for PointerAnalysisImpl
      if (contentsAreInvariant(symtab, du, objVn)) {
        InstanceKey[] x = getInvariantContents(symtab, du, opNode, objVn);
        for (int i = 0; i < x.length; i++) {
          system.findOrCreateIndexForInstanceKey(x[i]);
        }
      }
      if (contentsAreInvariant(symtab, du, fieldsVn)) {
        InstanceKey[] x = getInvariantContents(symtab, du, opNode, fieldsVn);
        for (int i = 0; i < x.length; i++) {
          system.findOrCreateIndexForInstanceKey(x[i]);
        }
      }

      // process field access
      if (contentsAreInvariant(symtab, du, objVn)) {
        system.recordImplicitPointsToSet(objKey);
        final InstanceKey[] objKeys = getInvariantContents(symtab, du, opNode, objVn);

        if (contentsAreInvariant(symtab, du, fieldsVn)) {
          system.recordImplicitPointsToSet(fieldKey);
          InstanceKey[] fieldsKeys = getInvariantContents(symtab, du, opNode, fieldsVn);

          for (int o = 0; o < objKeys.length; o++) {
            PointerKey objCatalog = getPointerKeyForObjectCatalog(objKeys[o]);
            for (int f = 0; f < fieldsKeys.length; f++) {
              if (isLoadOperation) {
                for (Iterator keys = getPointerKeysForReflectedFieldRead(objKeys[o], fieldsKeys[f]); keys.hasNext();) {
                  AbstractFieldPointerKey key = (AbstractFieldPointerKey) keys.next();
                  if (DEBUG_PROPERTIES)
                    action.dump(key, true, true);
                  action.action(key);
                }
              } else {
                if (objCatalog != null) {
                  system.newConstraint(objCatalog, fieldsKeys[f]);
                }
                for (Iterator keys = getPointerKeysForReflectedFieldWrite(objKeys[o], fieldsKeys[f]); keys.hasNext();) {
                  AbstractFieldPointerKey key = (AbstractFieldPointerKey) keys.next();
                  if (DEBUG_PROPERTIES)
                    action.dump(key, true, true);
                  action.action(key);
                }
              }
            }
          }

        } else {
          if (!isLoadOperation) {
            for (int o = 0; o < objKeys.length; o++) {
              PointerKey objCatalog = getPointerKeyForObjectCatalog(objKeys[o]);
              if (objCatalog != null) {
                system.newConstraint(objCatalog, assignOperator, fieldKey);
              }
            }
          }

          system.newSideEffect(new UnaryOperator<PointsToSetVariable>() {
            public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
              final IntSetVariable fields = (IntSetVariable) rhs;
              if (fields.getValue() != null) {
                fields.getValue().foreach(new IntSetAction() {
                  public void act(int fptr) {
                    InstanceKey field = system.getInstanceKey(fptr);
                    for (int o = 0; o < objKeys.length; o++) {
                      for (Iterator keys = isLoadOperation ? getPointerKeysForReflectedFieldRead(objKeys[o], field)
                          : getPointerKeysForReflectedFieldWrite(objKeys[o], field); keys.hasNext();) {
                        AbstractFieldPointerKey key = (AbstractFieldPointerKey) keys.next();
                        if (DEBUG_PROPERTIES)
                          action.dump(key, false, true);
                        action.action(key);
                      }
                    }
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
              return "field op" + objVn + ", " + fieldsVn;
            }
          }, fieldKey);
        }

      } else {
        if (contentsAreInvariant(symtab, du, fieldsVn)) {
          system.recordImplicitPointsToSet(fieldKey);
          final InstanceKey[] fieldsKeys = getInvariantContents(symtab, du, opNode, fieldsVn);

          system.newSideEffect(new UnaryOperator<PointsToSetVariable>() {
            public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
              final IntSetVariable objects = (IntSetVariable) rhs;
              if (objects.getValue() != null) {
                objects.getValue().foreach(new IntSetAction() {
                  public void act(int optr) {
                    InstanceKey object = system.getInstanceKey(optr);
                    PointerKey objCatalog = getPointerKeyForObjectCatalog(object);
                    for (int f = 0; f < fieldsKeys.length; f++) {
                      if (isLoadOperation) {
                        for (Iterator keys = getPointerKeysForReflectedFieldRead(object, fieldsKeys[f]); keys.hasNext();) {
                          AbstractFieldPointerKey key = (AbstractFieldPointerKey) keys.next();
                          if (DEBUG_PROPERTIES)
                            action.dump(key, true, false);
                          action.action(key);
                        }
                      } else {
                        if (objCatalog != null) {
                          system.newConstraint(objCatalog, fieldsKeys[f]);
                        }
                        for (Iterator keys = getPointerKeysForReflectedFieldWrite(object, fieldsKeys[f]); keys.hasNext();) {
                          AbstractFieldPointerKey key = (AbstractFieldPointerKey) keys.next();
                          if (DEBUG_PROPERTIES)
                            action.dump(key, true, false);
                          action.action(key);
                        }
                      }
                    }
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
              return "field op" + objVn + ", " + fieldsVn;
            }
          }, objKey);

        } else {
          system.newSideEffect(new AbstractOperator<PointsToSetVariable>() {
            public byte evaluate(PointsToSetVariable lhs, final IVariable[] rhs) {
              final IntSetVariable receivers = (IntSetVariable) rhs[0];
              final IntSetVariable fields = (IntSetVariable) rhs[1];
              if (receivers.getValue() != null && fields.getValue() != null) {
                receivers.getValue().foreach(new IntSetAction() {
                  public void act(int rptr) {
                    final InstanceKey receiver = system.getInstanceKey(rptr);

                    if (!isLoadOperation) {
                      PointerKey cat = getPointerKeyForObjectCatalog(receiver);
                      if (cat != null) {
                        system.newConstraint(cat, assignOperator, fieldKey);
                      }
                    }

                    fields.getValue().foreach(new IntSetAction() {
                      public void act(int fptr) {
                        InstanceKey field = system.getInstanceKey(fptr);
                        for (Iterator keys = isLoadOperation ? getPointerKeysForReflectedFieldRead(receiver, field)
                            : getPointerKeysForReflectedFieldWrite(receiver, field); keys.hasNext();) {
                          AbstractFieldPointerKey key = (AbstractFieldPointerKey) keys.next();
                          if (DEBUG_PROPERTIES)
                            action.dump(key, false, false);
                          action.action(key);
                        }
                      }
                    });
                  }
                });
              }

              return NOT_CHANGED;
            }

            public String toString() {
              return "field op";
            }

            public boolean equals(Object o) {
              return o == this;
            }

            public int hashCode() {
              return System.identityHashCode(this);
            }
          }, objKey, fieldKey);
        }
      }

      if (DEBUG_PROPERTIES) {
        System.err.println("finished\n");
      }
    }

    protected void newFieldWrite(CGNode opNode, int objVn, int fieldsVn, int rhsVn) {
      IR ir = getBuilder().getCFAContextInterpreter().getIR(opNode);
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

    protected void newFieldWrite(CGNode opNode, int objVn, int fieldsVn, final InstanceKey[] rhsFixedValues) {
      try {

        newFieldOperation(opNode, objVn, fieldsVn, false, new ReflectedFieldAction() {
          public void dump(AbstractFieldPointerKey fieldKey, boolean constObj, boolean constProp) {
            System.err.println(("writing fixed rvals to " + fieldKey + " " + constObj + ", " + constProp));
            for (int i = 0; i < rhsFixedValues.length; i++) {
              System.err.println(("writing " + rhsFixedValues[i]));
            }
          }

          public void action(AbstractFieldPointerKey fieldKey) {
            if (!representsNullType(fieldKey.getInstanceKey())) {
              for (int i = 0; i < rhsFixedValues.length; i++) {
                system.findOrCreateIndexForInstanceKey(rhsFixedValues[i]);
                system.newConstraint(fieldKey, rhsFixedValues[i]);
              }
            }
          }
        });

      } catch (RuntimeException e) {
        System.err.println("error: " + e);
        System.err.println(getBuilder().getCFAContextInterpreter().getIR(opNode));
        throw e;
      }
    }

    protected void newFieldWrite(CGNode opNode, int objVn, int fieldsVn, final PointerKey rhs) {
      newFieldOperation(opNode, objVn, fieldsVn, false, new ReflectedFieldAction() {
        public void dump(AbstractFieldPointerKey fieldKey, boolean constObj, boolean constProp) {
          System.err.println(("write " + rhs + " to " + fieldKey + " " + constObj + ", " + constProp));
        }

        public void action(AbstractFieldPointerKey fieldKey) {
          if (!representsNullType(fieldKey.getInstanceKey())) {
            system.newConstraint(fieldKey, assignOperator, rhs);
          }
        }
      });
    }

    protected void newFieldRead(CGNode opNode, int objVn, int fieldsVn, int lhsVn) {
      newFieldRead(opNode, objVn, fieldsVn, getBuilder().getPointerKeyForLocal(opNode, lhsVn));
    }

    protected void newFieldRead(CGNode opNode, int objVn, int fieldsVn, final PointerKey lhs) {
      newFieldOperation(opNode, objVn, fieldsVn, true, new ReflectedFieldAction() {
        public void dump(AbstractFieldPointerKey fieldKey, boolean constObj, boolean constProp) {
          System.err.println(("read " + lhs + " from " + fieldKey + " " + constObj + ", " + constProp));
        }

        public void action(AbstractFieldPointerKey fieldKey) {
          if (!representsNullType(fieldKey.getInstanceKey())) {
            system.newConstraint(lhs, assignOperator, fieldKey);
          }
        }
      });
    }
  }
}
