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
package com.ibm.wala.cast.js.ipa.callgraph;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Set;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.cast.ipa.callgraph.AstSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.ipa.callgraph.GlobalObjectKey;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.CAstUnaryOp;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.cast.js.analysis.typeInference.JSTypeInference;
import com.ibm.wala.cast.js.ipa.callgraph.JSSSAPropagationCallGraphBuilder.JSPointerAnalysisImpl.JSImplicitPointsToSetVisitor;
import com.ibm.wala.cast.js.ssa.JSInstructionVisitor;
import com.ibm.wala.cast.js.ssa.JavaScriptCheckReference;
import com.ibm.wala.cast.js.ssa.JavaScriptInstanceOf;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite;
import com.ibm.wala.cast.js.ssa.JavaScriptTypeOfInstruction;
import com.ibm.wala.cast.js.ssa.JavaScriptWithRegion;
import com.ibm.wala.cast.js.ssa.PrototypeLookup;
import com.ibm.wala.cast.js.ssa.SetPrototype;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointsToMap;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.PropagationSystem;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.SSAAbstractBinaryInstruction;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.CancelRuntimeException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.strings.Atom;

/**
 * Specialized pointer analysis constraint generation for JavaScript.
 * 
 * <h2>Global object handling</h2>
 * 
 * The global object is represented by a {@link GlobalObjectKey} stored in
 * {@link #globalObject}. {@link AstGlobalRead} and {@link AstGlobalWrite}
 * instructions are treated as accessing properties of the global object; see
 * {@link JSConstraintVisitor#visitAstGlobalRead(AstGlobalRead)},
 * {@link JSConstraintVisitor#visitAstGlobalWrite(AstGlobalWrite)}, and
 * {@link JSImplicitPointsToSetVisitor#visitAstGlobalRead(AstGlobalRead)}.
 * Finally, we need to represent direct flow of the global object to handle
 * receiver argument semantics (see
 * {@link org.mozilla.javascript.RhinoToAstTranslator}). To do so, we create a
 * reference to a global named {@link #GLOBAL_OBJ_VAR_NAME}, which is handled
 * specially in {@link JSConstraintVisitor#visitAstGlobalRead(AstGlobalRead)}.
 */
@SuppressWarnings("javadoc")
public class JSSSAPropagationCallGraphBuilder extends AstSSAPropagationCallGraphBuilder {

  public static final boolean DEBUG_LEXICAL = false;

  public static final boolean DEBUG_TYPE_INFERENCE = false;

  /**
   * name to be used internally to pass around the global object
   */
  public static final String GLOBAL_OBJ_VAR_NAME = "__WALA__int3rnal__global";

  private final GlobalObjectKey globalObject;

  @Override
  public GlobalObjectKey getGlobalObject(Atom language) {
    assert language.equals(JavaScriptTypes.jsName);
    return globalObject;
  }

  /**
   * is field a direct (WALA-internal) reference to the global object?
   */
  private static boolean directGlobalObjectRef(FieldReference field) {
    return field.getName().toString().endsWith(GLOBAL_OBJ_VAR_NAME);
  }

  private static FieldReference makeNonGlobalFieldReference(FieldReference field) {
    String nonGlobalFieldName = field.getName().toString().substring(7);
    field = FieldReference.findOrCreate(JavaScriptTypes.Root, Atom.findOrCreateUnicodeAtom(nonGlobalFieldName),
        JavaScriptTypes.Root);
    return field;
  }

  private URL scriptBaseURL;

  public URL getBaseURL() {
    return scriptBaseURL;
  }

  public void setBaseURL(URL url) {
    this.scriptBaseURL = url;
  }

  protected JSSSAPropagationCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache,
      PointerKeyFactory pointerKeyFactory) {
    super(cha, options, cache, pointerKeyFactory);
    globalObject = new GlobalObjectKey(cha.lookupClass(JavaScriptTypes.Root));
  }

  @Override
  protected boolean isConstantRef(SymbolTable symbolTable, int valueNumber) {
    if (valueNumber == -1) {
      return false;
    }
    return symbolTable.isConstant(valueNumber);
  }

  // ///////////////////////////////////////////////////////////////////////////
  //
  // language specialization interface
  //
  // ///////////////////////////////////////////////////////////////////////////

  @Override
  protected boolean useObjectCatalog() {
    return true;
  }

  @Override
  protected boolean isUncataloguedField(IClass type, String fieldName) {
    if (!type.getReference().equals(JavaScriptTypes.Object)) {
      return true;
    }
    return "prototype".equals(fieldName) || "constructor".equals(fieldName) || "arguments".equals(fieldName)
        || "class".equals(fieldName) || "$value".equals(fieldName) || "__proto__".equals(fieldName);
  }

  @Override
  protected AbstractFieldPointerKey fieldKeyForUnknownWrites(AbstractFieldPointerKey fieldKey) {
    // TODO: fix this.  null is wrong.
    return null;
    // return ReflectedFieldPointerKey.mapped(new ConcreteTypeKey(cha.lookupClass(JavaScriptTypes.String)), fieldKey.getInstanceKey());
  }

  // ///////////////////////////////////////////////////////////////////////////
  //
  // top-level node constraint generation
  //
  // ///////////////////////////////////////////////////////////////////////////

  private final static FieldReference prototypeRef;
  static {
    FieldReference x = null;
    try {
      byte[] utf8 = "__proto__".getBytes("UTF-8");
      x = FieldReference.findOrCreate(JavaScriptTypes.Root, Atom.findOrCreate(utf8, 0, utf8.length), JavaScriptTypes.Root);
    } catch (UnsupportedEncodingException e) {
      assert false;
    }
    prototypeRef = x;
  }

  public PointerKey getPointerKeyForGlobalVar(String varName) {
    FieldReference fieldRef = FieldReference.findOrCreate(JavaScriptTypes.Root, Atom.findOrCreateUnicodeAtom(varName),
        JavaScriptTypes.Root);
    IField f = cha.resolveField(fieldRef);
    assert f != null : "couldn't resolve " + varName;
    return getPointerKeyForInstanceField(getGlobalObject(JavaScriptTypes.jsName), f);
  }
  @Override
  protected ExplicitCallGraph createEmptyCallGraph(IClassHierarchy cha, AnalysisOptions options) {
    return new JSCallGraph(cha, options, getAnalysisCache());
  }

  protected TypeInference makeTypeInference(IR ir, IClassHierarchy cha) {
    TypeInference ti = new JSTypeInference(ir, cha);

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

  @Override
  protected void addAssignmentsForCatchPointerKey(PointerKey exceptionVar, Set<IClass> catchClasses, PointerKey e) {
    system.newConstraint(exceptionVar, assignOperator, e);
  }

  public static class JSInterestingVisitor extends AstInterestingVisitor implements com.ibm.wala.cast.js.ssa.JSInstructionVisitor {
    public JSInterestingVisitor(int vn) {
      super(vn);
    }

    @Override
    public void visitBinaryOp(final SSABinaryOpInstruction instruction) {
      bingo = true;
    }

    @Override
    public void visitJavaScriptInvoke(JavaScriptInvoke instruction) {
      bingo = true;
    }

    @Override
    public void visitJavaScriptPropertyRead(JavaScriptPropertyRead instruction) {
      bingo = true;
    }

    @Override
    public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite instruction) {
      bingo = true;
    }

    @Override
    public void visitTypeOf(JavaScriptTypeOfInstruction inst) {
      bingo = true;
    }

    @Override
    public void visitJavaScriptInstanceOf(JavaScriptInstanceOf instruction) {
      bingo = true;
    }

    @Override
    public void visitCheckRef(JavaScriptCheckReference instruction) {

    }

    @Override
    public void visitWithRegion(JavaScriptWithRegion instruction) {

    }

    @Override
    public void visitSetPrototype(SetPrototype instruction) {
      bingo = true;
    }

    @Override
    public void visitPrototypeLookup(PrototypeLookup instruction) {
      bingo = true;
    }
  }

  @Override
  protected InterestingVisitor makeInterestingVisitor(CGNode node, int vn) {
    return new JSInterestingVisitor(vn);
  }
  
  // ///////////////////////////////////////////////////////////////////////////
  //
  // specialized pointer analysis
  //
  // ///////////////////////////////////////////////////////////////////////////

  public static class JSPointerAnalysisImpl extends AstSSAPropagationCallGraphBuilder.AstPointerAnalysisImpl {

    JSPointerAnalysisImpl(PropagationCallGraphBuilder builder, CallGraph cg, PointsToMap pointsToMap,
        MutableMapping<InstanceKey> instanceKeys, PointerKeyFactory pointerKeys, InstanceKeyFactory iKeyFactory) {
      super(builder, cg, pointsToMap, instanceKeys, pointerKeys, iKeyFactory);
    }

    public static class JSImplicitPointsToSetVisitor extends AstImplicitPointsToSetVisitor implements
        com.ibm.wala.cast.js.ssa.JSInstructionVisitor {

      public JSImplicitPointsToSetVisitor(AstPointerAnalysisImpl analysis, LocalPointerKey lpk) {
        super(analysis, lpk);
      }

      @Override
      public void visitJavaScriptInvoke(JavaScriptInvoke instruction) {

      }

      @Override
      public void visitTypeOf(JavaScriptTypeOfInstruction instruction) {

      }

      @Override
      public void visitJavaScriptPropertyRead(JavaScriptPropertyRead instruction) {

      }

      @Override
      public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite instruction) {

      }

      @Override
      public void visitJavaScriptInstanceOf(JavaScriptInstanceOf instruction) {

      }

      @Override
      public void visitCheckRef(JavaScriptCheckReference instruction) {

      }

      @Override
      public void visitWithRegion(JavaScriptWithRegion instruction) {

      }

      @Override
      public void visitAstGlobalRead(AstGlobalRead instruction) {
        JSPointerAnalysisImpl jsAnalysis = (JSPointerAnalysisImpl) analysis;
        FieldReference field = makeNonGlobalFieldReference(instruction.getDeclaredField());
        assert !directGlobalObjectRef(field);
        IField f = jsAnalysis.builder.getCallGraph().getClassHierarchy().resolveField(field);
        assert f != null;
        MutableSparseIntSet S = MutableSparseIntSet.makeEmpty();
        InstanceKey globalObj = ((AstSSAPropagationCallGraphBuilder) jsAnalysis.builder).getGlobalObject(JavaScriptTypes.jsName);
        PointerKey fkey = analysis.getHeapModel().getPointerKeyForInstanceField(globalObj, f);
        if (fkey != null) {
          OrdinalSet<InstanceKey> pointees = analysis.getPointsToSet(fkey);
          IntSet set = pointees.getBackingSet();
          if (set != null) {
            S.addAll(set);
          }
        }
        pointsToSet = new OrdinalSet<>(S, analysis.getInstanceKeyMapping());
      }

      @Override
      public void visitSetPrototype(SetPrototype instruction) {
      }

      @Override
      public void visitPrototypeLookup(PrototypeLookup instruction) {
      }

    }

    @Override
    protected ImplicitPointsToSetVisitor makeImplicitPointsToVisitor(LocalPointerKey lpk) {
      return new JSImplicitPointsToSetVisitor(this, lpk);
    }
  }

  @Override
  protected PropagationSystem makeSystem(AnalysisOptions options) {
    return new PropagationSystem(callGraph, pointerKeyFactory, instanceKeyFactory) {
      @Override
      public PointerAnalysis<InstanceKey> makePointerAnalysis(PropagationCallGraphBuilder builder) {
        return new JSPointerAnalysisImpl(builder, cg, pointsToMap, instanceKeys, pointerKeyFactory, instanceKeyFactory);
      }
    };
  }

  // ///////////////////////////////////////////////////////////////////////////
  //
  // IR visitor specialization for JavaScript
  //
  // ///////////////////////////////////////////////////////////////////////////

  @Override
  public JSConstraintVisitor makeVisitor(CGNode node) {
    if (AstSSAPropagationCallGraphBuilder.DEBUG_PROPERTIES) {
      final IMethod method = node.getMethod();
      if (method instanceof AstMethod) {
        System.err.println("\n\nNode: " + node);
        final IR ir = node.getIR();
        System.err.println("Position: " + getSomePositionForMethod(ir, (AstMethod) method));
        // System.err.println(ir);
      }
    }
    return new JSConstraintVisitor(this, node);
  }

  public static Position getSomePositionForMethod(IR ir, AstMethod method) {
    SSAInstruction[] instructions = ir.getInstructions();
    for (int i = 0; i < instructions.length; i++) {
      Position p = method.getSourcePosition(i);
      if (p != null) {
        return p;
      }
    }
    return null;
  }

  public static class JSConstraintVisitor extends AstConstraintVisitor implements JSInstructionVisitor {

    public JSConstraintVisitor(AstSSAPropagationCallGraphBuilder builder, CGNode node) {
      super(builder, node);
    }

    @Override
    public void visitUnaryOp(SSAUnaryOpInstruction inst) {
      if (inst.getOpcode() == IUnaryOpInstruction.Operator.NEG) {
        addLvalTypeKeyConstraint(inst, JavaScriptTypes.Boolean);
      } else if (inst.getOpcode() == CAstUnaryOp.MINUS) {
        addLvalTypeKeyConstraint(inst, JavaScriptTypes.Number);        
      }
    }

    /**
     * add a constraint indicating that the value def'd by inst can point to a
     * value of type t
     */
    private void addLvalTypeKeyConstraint(SSAInstruction inst, TypeReference t) {
      int lval = inst.getDef(0);
      PointerKey lk = getPointerKeyForLocal(lval);

      IClass bool = getClassHierarchy().lookupClass(t);
      InstanceKey key = new ConcreteTypeKey(bool);

      system.newConstraint(lk, key);
    }

    @Override
    public void visitIsDefined(AstIsDefinedInstruction inst) {
      addLvalTypeKeyConstraint(inst, JavaScriptTypes.Boolean);

    }

    @Override
    public void visitJavaScriptInstanceOf(JavaScriptInstanceOf inst) {
      addLvalTypeKeyConstraint(inst, JavaScriptTypes.Boolean);
    }

    @Override
    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {
      addLvalTypeKeyConstraint(inst, JavaScriptTypes.Boolean);
    }

    @Override
    public void visitTypeOf(JavaScriptTypeOfInstruction instruction) {
      addLvalTypeKeyConstraint(instruction, JavaScriptTypes.String);
    }

    @Override
    public void visitAstGlobalRead(AstGlobalRead instruction) {
      int lval = instruction.getDef();
      FieldReference field = makeNonGlobalFieldReference(instruction.getDeclaredField());
      PointerKey def = getPointerKeyForLocal(lval);
      assert def != null;
      IField f = getClassHierarchy().resolveField(field);
      assert f != null : "could not resolve referenced global " + field;
      if (hasNoInterestingUses(lval)) {
        system.recordImplicitPointsToSet(def);
      } else {
        InstanceKey globalObj = getBuilder().getGlobalObject(JavaScriptTypes.jsName);
        if (directGlobalObjectRef(field)) {
          // points-to set is just the global object
          system.newConstraint(def, globalObj);
        } else {
          system.findOrCreateIndexForInstanceKey(globalObj);
          PointerKey p = getPointerKeyForInstanceField(globalObj, f);
          system.newConstraint(def, assignOperator, p);
        }
      }

    }

    @Override
    public void visitAstGlobalWrite(AstGlobalWrite instruction) {
      int rval = instruction.getVal();
      FieldReference field = makeNonGlobalFieldReference(instruction.getDeclaredField());
      IField f = getClassHierarchy().resolveField(field);
      assert f != null : "could not resolve referenced global " + field;
      assert !f.getFieldTypeReference().isPrimitiveType();
      InstanceKey globalObj = getBuilder().getGlobalObject(JavaScriptTypes.jsName);
      system.findOrCreateIndexForInstanceKey(globalObj);
      PointerKey p = getPointerKeyForInstanceField(globalObj, f);

      PointerKey rvalKey = getPointerKeyForLocal(rval);
      if (contentsAreInvariant(symbolTable, du, rval)) {
        system.recordImplicitPointsToSet(rvalKey);
        InstanceKey[] ik = getInvariantContents(rval);
        for (InstanceKey element : ik) {
          system.newConstraint(p, element);
        }
      } else {
        system.newConstraint(p, assignOperator, rvalKey);
      }

    }

    @Override
    public void visitBinaryOp(final SSABinaryOpInstruction instruction) {
      handleBinaryOp(instruction, node, symbolTable, du);
    }

    @Override
    public void visitJavaScriptPropertyRead(JavaScriptPropertyRead instruction) {
      if (AstSSAPropagationCallGraphBuilder.DEBUG_PROPERTIES) {
        Position instructionPosition = getInstructionPosition(instruction);
        if (instructionPosition != null) {
          System.err.println("processing read instruction " + instruction + ", position " + instructionPosition);
        }
      }
      newFieldRead(node, instruction.getUse(0), instruction.getUse(1), instruction.getDef(0));
    }

    private Position getInstructionPosition(SSAInstruction instruction) {
      IMethod method = node.getMethod();
      if (method instanceof AstMethod) {
        return ((AstMethod) method).getSourcePosition(instruction.iindex);
      }
      return null;
    }

    @Override
    public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite instruction) {
      if (AstSSAPropagationCallGraphBuilder.DEBUG_PROPERTIES) {
        Position instructionPosition = getInstructionPosition(instruction);
        if (instructionPosition != null) {
          System.err.println("processing write instruction " + instruction + ", position " + instructionPosition);
        }
      }
      newFieldWrite(node, instruction.getUse(0), instruction.getUse(1), instruction.getUse(2));
    }

    @Override
    public void visitJavaScriptInvoke(JavaScriptInvoke instruction) {
      if (instruction.getDeclaredTarget().equals(JavaScriptMethods.dispatchReference)) {
        handleJavascriptDispatch(instruction);
      } else {
        if (! instruction.getDeclaredTarget().equals(JavaScriptMethods.ctorReference)) {
          System.err.println(instruction);
        }
        visitInvokeInternal(instruction, new DefaultInvariantComputer());
      }
    }

    private void handleJavascriptDispatch(final JavaScriptInvoke instruction, final InstanceKey receiverType) {
      int functionVn = instruction.getFunction();

      ReflectedFieldAction fieldDispatchAction = new ReflectedFieldAction() {
        @Override
        public void action(final AbstractFieldPointerKey fieldKey) {
            class FieldValueDispatch extends UnaryOperator<PointsToSetVariable> {
              private JavaScriptInvoke getInstruction() { return instruction; }
              private InstanceKey getReceiver() { return receiverType; }
              private AbstractFieldPointerKey getProperty() { return fieldKey; }
              private CGNode getNode() { return node; }
              
              private MutableIntSet previous = IntSetUtil.make();
              
              @Override
              public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable ptrs) {
                if (ptrs.getValue() != null) {
                  ptrs.getValue().foreachExcluding(previous, x -> {
                    final InstanceKey functionObj = system.getInstanceKey(x);
                    visitInvokeInternal(instruction, new DefaultInvariantComputer() {
                      @Override
                      public InstanceKey[][] computeInvariantParameters(SSAAbstractInvokeInstruction call) {
                        InstanceKey[][] x = super.computeInvariantParameters(call);
                        if (x == null) {
                          x = new InstanceKey[call.getNumberOfUses()][];
                        }
                        x[0] = new InstanceKey[]{ functionObj };
                        x[1] = new InstanceKey[]{ receiverType };
                        return x;
                      }
                    });
                  });
                  previous.addAll(ptrs.getValue());
                }
                return NOT_CHANGED;
              }
              @Override
              public int hashCode() {
                return instruction.hashCode() * fieldKey.hashCode() * receiverType.hashCode();
              }
              @Override
              public boolean equals(Object o) {
                return o instanceof FieldValueDispatch &&
                ((FieldValueDispatch)o).getNode().equals(node) &&
                ((FieldValueDispatch)o).getInstruction() == instruction &&
                ((FieldValueDispatch)o).getProperty().equals(fieldKey) &&
                ((FieldValueDispatch)o).getReceiver().equals(receiverType);
              }
              @Override
              public String toString() {
                return "sub-dispatch for " + instruction + ": " + receiverType + ", " + fieldKey;
              } 
            }
          
            system.newSideEffect(new FieldValueDispatch(), fieldKey);
        }
        @Override
        public void dump(AbstractFieldPointerKey fieldKey, boolean constObj, boolean constProp) {
          System.err.println("dispatch to " + receiverType + "." + fieldKey + " for " + instruction);
        }
      };

      TransitivePrototypeKey prototypeObjs = new TransitivePrototypeKey(receiverType);
      InstanceKey[] objKeys = new InstanceKey[]{ receiverType };
      if (contentsAreInvariant(symbolTable, du, functionVn)) {
        InstanceKey[] fieldsKeys = getInvariantContents(functionVn);
        newFieldOperationObjectAndFieldConstant(true, fieldDispatchAction, objKeys, fieldsKeys);
        newFieldOperationOnlyFieldConstant(true, fieldDispatchAction, prototypeObjs, fieldsKeys);
      } else {
        PointerKey fieldKey = getPointerKeyForLocal(functionVn);
        newFieldOperationOnlyObjectConstant(true, fieldDispatchAction, fieldKey, objKeys);
        newFieldFullOperation(true, fieldDispatchAction, prototypeObjs, fieldKey);
      }
    }
    
    private void handleJavascriptDispatch(final JavaScriptInvoke instruction) {
      int receiverVn = instruction.getUse(1);
      PointerKey receiverKey = getPointerKeyForLocal(receiverVn);
      if (contentsAreInvariant(symbolTable, du, receiverVn)) {
          system.recordImplicitPointsToSet(receiverKey);
          InstanceKey[] ik = getInvariantContents(receiverVn);
          for (InstanceKey element : ik) {
            handleJavascriptDispatch(instruction, element);
          }
      } else {
        class ReceiverForDispatchOp extends UnaryOperator<PointsToSetVariable> {
          MutableIntSet previous = IntSetUtil.make();
          
          private JavaScriptInvoke getInstruction() {
            return instruction;
          }
          
          @Override
          public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
            if (rhs.getValue() != null) {
              rhs.getValue().foreachExcluding(previous, x -> {
                try {
                  MonitorUtil.throwExceptionIfCanceled(getBuilder().monitor);
                } catch (CancelException e) {
                  throw new CancelRuntimeException(e);
                }
                InstanceKey ik = system.getInstanceKey(x);
                handleJavascriptDispatch(instruction, ik);
              });
              previous.addAll(rhs.getValue());
            }
            return NOT_CHANGED;
          }

          @Override
          public int hashCode() {
            return instruction.hashCode(); 
          }

          @Override
          public boolean equals(Object o) {
            return o instanceof ReceiverForDispatchOp && ((ReceiverForDispatchOp)o).getInstruction()==getInstruction();
          }

          @Override
          public String toString() {
            return "receiver for dispatch: " + instruction;
          }
        }

        system.newSideEffect(new ReceiverForDispatchOp(), receiverKey);
      }
    }
    
    // ///////////////////////////////////////////////////////////////////////////
    //
    // string manipulation handling for binary operators
    //
    // ///////////////////////////////////////////////////////////////////////////

    private void handleBinaryOp(final SSABinaryOpInstruction instruction, final CGNode node, final SymbolTable symbolTable,
        final DefUse du) {
      int lval = instruction.getDef(0);
      PointerKey lk = getPointerKeyForLocal(lval);
      final PointsToSetVariable lv = system.findOrCreatePointsToSet(lk);

      final int arg1 = instruction.getUse(0);
      final int arg2 = instruction.getUse(1);

      class BinaryOperator extends AbstractOperator<PointsToSetVariable> {

        private CGNode getNode() {
          return node;
        }

        private SSAAbstractBinaryInstruction getInstruction() {
          return instruction;
        }

        @Override
        public String toString() {
          return "BinOp: " + getInstruction();
        }

        @Override
        public int hashCode() {
          return 17 * getInstruction().getUse(0) * getInstruction().getUse(1);
        }

        @Override
        public boolean equals(Object o) {
          if (o instanceof BinaryOperator) {
            BinaryOperator op = (BinaryOperator) o;
            return op.getNode().equals(getNode()) && op.getInstruction().getUse(0) == getInstruction().getUse(0)
                && op.getInstruction().getUse(1) == getInstruction().getUse(1)
                && op.getInstruction().getDef(0) == getInstruction().getDef(0);
          } else {
            return false;
          }
        }

        private InstanceKey[] getInstancesArray(int vn) {
          if (contentsAreInvariant(symbolTable, du, vn)) {
            return getInvariantContents(vn);
          } else {
            PointsToSetVariable v = system.findOrCreatePointsToSet(getPointerKeyForLocal(vn));
            if (v.getValue() == null || v.size() == 0) {
              return new InstanceKey[0];
            } else {
              final Set<InstanceKey> temp = HashSetFactory.make();
              v.getValue().foreach(keyIndex -> temp.add(system.getInstanceKey(keyIndex)));

              return temp.toArray(new InstanceKey[temp.size()]);
            }
          }
        }

        private boolean isStringConstant(InstanceKey k) {
          return (k instanceof ConstantKey) && k.getConcreteType().getReference().equals(JavaScriptTypes.String);
        }

        private boolean addKey(InstanceKey k) {
          int n = system.findOrCreateIndexForInstanceKey(k);
          if (!lv.contains(n)) {
            lv.add(n);
            return true;
          } else {
            return false;
          }
        }

        @Override
        public byte evaluate(PointsToSetVariable lhs, final PointsToSetVariable[] rhs) {
          boolean doDefault = false;
          byte changed = NOT_CHANGED;

          InstanceKey[] iks1 = getInstancesArray(arg1);
          InstanceKey[] iks2 = getInstancesArray(arg2);

          if ((instruction.getOperator() == BinaryOpInstruction.Operator.ADD) && (getOptions().getTraceStringConstants())) {
            for (InstanceKey element : iks1) {
              if (isStringConstant(element)) {
                for (InstanceKey element2 : iks2) {
                  if (isStringConstant(element2)) {
                    try {
                      MonitorUtil.throwExceptionIfCanceled(builder.monitor);
                    } catch (CancelException e) {
                      throw new CancelRuntimeException(e);
                    }
                    String v1 = (String) ((ConstantKey<?>) element).getValue();
                    String v2 = (String) ((ConstantKey<?>) element2).getValue();
                    if (v1.indexOf(v2) == -1 && v2.indexOf(v1) == -1) {
                      InstanceKey lvalKey = getInstanceKeyForConstant(v1 + v2);
                      if (addKey(lvalKey)) {
                        changed = CHANGED;
                      }
                    } else {
                      doDefault = true;
                    }
                  } else {
                    doDefault = true;
                  }
                }
              } else {
                doDefault = true;
              }
            }
          } else {
            doDefault = true;
          }

          if (doDefault) {
              for (InstanceKey element : iks1) {
                for (InstanceKey element2 : iks2) {
                  try {
                    MonitorUtil.throwExceptionIfCanceled(builder.monitor);
                  } catch (CancelException e) {
                    throw new CancelRuntimeException(e);
                  }
                  if (handleBinaryOperatorArgs(element, element2)) {
                    changed = CHANGED;
                  }
                }
              }
          }
          
          if (iks1 == null || iks1.length == 0 || iks2 == null || iks2.length == 0) {
            if (iks1 != null) {
              for(InstanceKey ik : iks1) {
                if (addKey(ik)) {
                  changed = CHANGED;
                }
              }
            }
            if (iks2 != null) {
              for(InstanceKey ik : iks2) {
                if (addKey(ik)) {
                  changed = CHANGED;
                }
              }
            }
            
            System.err.println(instruction);
          }
          
          return changed;
        }
        
        private boolean isNumberType(Language l, TypeReference t) {
          return l.isDoubleType(t)||l.isFloatType(t)||l.isIntType(t)||l.isLongType(t);
        }
        
        protected boolean handleBinaryOperatorArgs(InstanceKey left, InstanceKey right) {
          Language l = node.getMethod().getDeclaringClass().getClassLoader().getLanguage();
          if (l.isStringType(left.getConcreteType().getReference()) || l.isStringType(right.getConcreteType().getReference())) {
            return addKey(new ConcreteTypeKey(node.getClassHierarchy().lookupClass(l.getStringType())));
          } else if (isNumberType(l, left.getConcreteType().getReference()) && isNumberType(l, right.getConcreteType().getReference())) {
            if (left instanceof ConstantKey && right instanceof ConstantKey) {
              return addKey(new ConcreteTypeKey(node.getClassHierarchy().lookupClass(JavaScriptTypes.Number)));
            } else {
              return addKey(left) || addKey(right);
            }
          } else {
            return false;
          }
         }

      }

      BinaryOperator B = new BinaryOperator();
      if (contentsAreInvariant(symbolTable, du, arg1)) {
        if (contentsAreInvariant(symbolTable, du, arg2)) {
          B.evaluate(null, null);
        } else {
          system.newConstraint(lk, B, getPointerKeyForLocal(arg2));
        }
      } else {
        PointerKey k1 = getPointerKeyForLocal(arg1);
        if (contentsAreInvariant(symbolTable, du, arg2)) {
          system.newConstraint(lk, B, k1);
        } else {
          system.newConstraint(lk, B, k1, getPointerKeyForLocal(arg2));
        }
      }
    }

    @Override
    public void visitCheckRef(JavaScriptCheckReference instruction) {
      // TODO Auto-generated method stub

    }

    @Override
    public void visitWithRegion(JavaScriptWithRegion instruction) {
      // TODO Auto-generated method stub

    }

    private final UnaryOperator<PointsToSetVariable> transitivePrototypeOp = new UnaryOperator<PointsToSetVariable>() {
      @Override
      public byte evaluate(final PointsToSetVariable lhs, PointsToSetVariable rhs) {
        class Op implements IntSetAction {
          private boolean changed = false;
          
          @Override
          public void act(int x) {
            InstanceKey protoObj = system.getInstanceKey(x);
            PointerKey protoObjKey = new TransitivePrototypeKey(protoObj);
            changed |= system.newStatement(lhs, assignOperator, system.findOrCreatePointsToSet(protoObjKey), true, true);
          }        
        }
        
        if (rhs.getValue() != null) {
          Op op = new Op();
          rhs.getValue().foreach(op); 
          return (op.changed? CHANGED: NOT_CHANGED);
        }
        return NOT_CHANGED;
      }

      @Override
      public int hashCode() {
        return -896435647;
      }

      @Override
      public boolean equals(Object o) {
        return o == this;
      }

      @Override
      public String toString() {
        return "transitivePrototypeOp";
      }
      
    };
        
    @Override
    public void visitSetPrototype(SetPrototype instruction) {
      visitPutInternal(instruction.getUse(1), instruction.getUse(0), false, prototypeRef);
      
      assert contentsAreInvariant(symbolTable, du, instruction.getUse(0));      
      if (contentsAreInvariant(symbolTable, du, instruction.getUse(1))) {
        for(InstanceKey newObj : getInvariantContents(instruction.getUse(0))) {   
          PointerKey newObjKey = new TransitivePrototypeKey(newObj);
          for(InstanceKey protoObj : getInvariantContents(instruction.getUse(1))) {   
            system.newConstraint(newObjKey, protoObj);
            system.newConstraint(newObjKey, assignOperator, new TransitivePrototypeKey(protoObj));
          }
        }
      } else {
        for(InstanceKey newObj : getInvariantContents(instruction.getUse(0))) {   
          PointerKey newObjKey = new TransitivePrototypeKey(newObj);
          system.newConstraint(newObjKey, assignOperator, getPointerKeyForLocal(instruction.getUse(1)));
          system.newConstraint(newObjKey, transitivePrototypeOp, getPointerKeyForLocal(instruction.getUse(1)));
        }
      }
    }

    @Override
    public void visitPrototypeLookup(PrototypeLookup instruction) {
      if (contentsAreInvariant(symbolTable, du, instruction.getUse(0))) {
        for(InstanceKey rhsObj : getInvariantContents(instruction.getUse(0))) {   
          // property can come from object itself...
          system.newConstraint(getPointerKeyForLocal(instruction.getDef(0)), rhsObj);
        
          // ...or prototype objects
          system.newConstraint(getPointerKeyForLocal(instruction.getDef(0)), assignOperator, new TransitivePrototypeKey(rhsObj));
        }
      } else {
        // property can come from object itself...
        system.newConstraint(getPointerKeyForLocal(instruction.getDef(0)), assignOperator, getPointerKeyForLocal(instruction.getUse(0)));
      
        // ...or prototype objects
        system.newConstraint(getPointerKeyForLocal(instruction.getDef(0)), transitivePrototypeOp, getPointerKeyForLocal(instruction.getUse(0)));
      }
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  //
  // function call handling
  //
  // //////////////////////////////////////////////////////////////////////////

  @Override
  protected void processCallingConstraints(CGNode caller, SSAAbstractInvokeInstruction instruction, CGNode target,
      InstanceKey[][] constParams, PointerKey uniqueCatchKey) {
    processCallingConstraintsInternal(this, caller, instruction, target, constParams, uniqueCatchKey);
  }

  @SuppressWarnings("unused")
  public static void processCallingConstraintsInternal(AstSSAPropagationCallGraphBuilder builder, CGNode caller, SSAAbstractInvokeInstruction instruction, CGNode target,
      InstanceKey[][] constParams, PointerKey uniqueCatchKey) {
        
    IRView sourceIR = builder.getCFAContextInterpreter().getIRView(caller);
    SymbolTable sourceST = sourceIR.getSymbolTable();

    IRView targetIR = builder.getCFAContextInterpreter().getIRView(target);
    SymbolTable targetST = targetIR.getSymbolTable();

    JSConstraintVisitor targetVisitor = null;
    int av = -1;
    for (int v = 0; v <= targetST.getMaxValueNumber(); v++) {
      String[] vns = targetIR.getLocalNames(1, v);
      for (int n = 0; vns != null && n < vns.length; n++) {
        if ("arguments".equals(vns[n])) {
          av = v;
          targetVisitor = (JSConstraintVisitor) builder.makeVisitor(target);
          break;
        }
      }
    }

    int paramCount = targetST.getParameterValueNumbers().length;
    int argCount = instruction.getNumberOfParameters();
    
    // the first two arguments are the function object and the receiver, neither of which
    // should become part of the arguments array
    int num_pseudoargs = 2;

    // pass actual arguments to formals in the normal way
    for (int i = 0; i < Math.min(paramCount, argCount); i++) {
      InstanceKey[] fn = new InstanceKey[] { builder.getInstanceKeyForConstant(JavaScriptTypes.String, ""+(i-num_pseudoargs)) };
      PointerKey F = builder.getTargetPointerKey(target, i);

      if (constParams != null && constParams[i] != null) {
        for (int j = 0; j < constParams[i].length; j++) {
          builder.getSystem().newConstraint(F, constParams[i][j]);
        }

        if (av != -1 && i >= num_pseudoargs) {
          targetVisitor.newFieldWrite(target, av, fn, constParams[i]);
        }
        
      } else {
        PointerKey A = builder.getPointerKeyForLocal(caller, instruction.getUse(i));
        builder.getSystem().newConstraint(F, (F instanceof FilteredPointerKey) ? builder.filterOperator : assignOperator, A);

        if (av != -1 && i >= num_pseudoargs) {
          targetVisitor.newFieldWrite(target, av, fn, F);
        }
      }
    }

    // extra actual arguments get assigned into the ``arguments'' object
    if (paramCount < argCount) {
      if (av != -1) {
        for (int i = paramCount; i < argCount; i++) {
          InstanceKey[] fn = new InstanceKey[] { builder.getInstanceKeyForConstant(JavaScriptTypes.String, ""+(i-num_pseudoargs)) };
          if (constParams != null && constParams[i] != null && i >= num_pseudoargs) {
              targetVisitor.newFieldWrite(target, av, fn, constParams[i]);
          } else if(i >= num_pseudoargs) {
            PointerKey A = builder.getPointerKeyForLocal(caller, instruction.getUse(i));
            targetVisitor.newFieldWrite(target, av, fn, A);
          }
        }
      }
    }

    // extra formal parameters get null (extra args are ignored here)
    else if (argCount < paramCount) {
      int nullvn = sourceST.getNullConstant();
      DefUse sourceDU = builder.getCFAContextInterpreter().getDU(caller);
      InstanceKey[] nullkeys = builder.getInvariantContents(sourceST, sourceDU, caller, nullvn, builder);
      for (int i = argCount; i < paramCount; i++) {
        PointerKey F = builder.getPointerKeyForLocal(target, targetST.getParameter(i));
        for (InstanceKey nullkey : nullkeys) {
          builder.getSystem().newConstraint(F, nullkey);
        }
      }
    }

    // write `length' in argument objects
    if (av != -1) {
      InstanceKey[] svn = new InstanceKey[] { builder.getInstanceKeyForConstant(JavaScriptTypes.Number, argCount-1) };
      InstanceKey[] lnv = new InstanceKey[] { builder.getInstanceKeyForConstant(JavaScriptTypes.String, "length") };
      targetVisitor.newFieldWrite(target, av, lnv, svn);
    }

    // return values
    if (instruction.getDef(0) != -1) {
      PointerKey RF = builder.getPointerKeyForReturnValue(target);
      PointerKey RA = builder.getPointerKeyForLocal(caller, instruction.getDef(0));
      builder.getSystem().newConstraint(RA, assignOperator, RF);
    }

    PointerKey EF = builder.getPointerKeyForExceptionalReturnValue(target);
    if (SHORT_CIRCUIT_SINGLE_USES && uniqueCatchKey != null) {
      // e has exactly one use. so, represent e implicitly
      builder.getSystem().newConstraint(uniqueCatchKey, assignOperator, EF);
    } else {
      PointerKey EA = builder.getPointerKeyForLocal(caller, instruction.getDef(1));
      builder.getSystem().newConstraint(EA, assignOperator, EF);
    }  
  }

  @Override
  protected boolean sameMethod(CGNode opNode, String definingMethod) {
    return definingMethod.equals(opNode.getMethod().getReference().getDeclaringClass().getName().toString());
  }
}
