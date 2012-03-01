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
package com.ibm.wala.cast.js.translator;

import java.io.UTFDataFormatException;

import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.js.loader.JSCallSiteReference;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.ssa.JSInstructionFactory;
import com.ibm.wala.cast.js.ssa.JavaScriptInstanceOf;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSymbol;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * Specialization of {@link AstTranslator} for JavaScript.
 *
 */
public class JSAstTranslator extends AstTranslator {
  private final static boolean DEBUG = false;

  public JSAstTranslator(JavaScriptLoader loader) {
    super(loader);
  }

  private boolean isPrologueScript(WalkContext context) {
    return JavaScriptLoader.bootstrapFileNames.contains( context.getModule().getName() );
  }

  protected boolean useDefaultInitValues() {
    return false;
  }

  protected boolean hasImplicitGlobals() {
    return true;
  }

  protected boolean treatGlobalsAsLexicallyScoped() {
    return false;
  }

  protected boolean useLocalValuesForLexicalVars() {
    return !AstTranslator.NEW_LEXICAL;
  }
  
  protected TypeReference defaultCatchType() {
    return JavaScriptTypes.Root;
  }

  protected TypeReference makeType(CAstType type) {
    Assertions.UNREACHABLE("JavaScript does not use CAstType");
    return null;
  }

  /**
   * generate an instruction that checks if readVn is undefined and throws an exception if it isn't
   */
  private void addDefinedCheck(CAstNode n, WalkContext context, int readVn) {
    context.cfg().addPreNode(n);
    context.cfg().addInstruction(((JSInstructionFactory) insts).CheckReference(readVn));

    CAstNode target = context.getControlFlow().getTarget(n, JavaScriptTypes.ReferenceError);
    if (target != null) {
      context.cfg().addPreEdge(n, target, true);
    } else {
      context.cfg().addPreEdgeToExit(n, true);
    }
    context.cfg().newBlock(true);
  }

  protected int doLexicallyScopedRead(CAstNode n, WalkContext context, String name) {
    int readVn = super.doLexicallyScopedRead(n, context, name);
    // should get an exception if name is undefined
    addDefinedCheck(n, context, readVn);
    return readVn;
  }

  protected int doGlobalRead(CAstNode n, WalkContext context, String name) {
    int readVn = super.doGlobalRead(n, context, name);
    // add a check if name is undefined, unless we're reading the value 'undefined'
    if (!("undefined".equals(name) || "$$undefined".equals(name))) {
      addDefinedCheck(n, context, readVn);
    }
    return readVn;
  }

  protected boolean defineType(CAstEntity type, WalkContext wc) {
    Assertions.UNREACHABLE("JavaScript doesn't have types. I suggest you look elsewhere for your amusement.");
    return false;
  }

  protected void defineField(CAstEntity topEntity, WalkContext wc, CAstEntity n) {
    Assertions.UNREACHABLE("JavaScript doesn't have fields");
  }

  protected String composeEntityName(WalkContext parent, CAstEntity f) {
    if (f.getKind() == CAstEntity.SCRIPT_ENTITY)
      return f.getName();
    else
      return parent.getName() + "/" + f.getName();
  }

  protected void declareFunction(CAstEntity N, WalkContext context) {
    String fnName = composeEntityName(context, N);
    if (N.getKind() == CAstEntity.SCRIPT_ENTITY) {
      ((JavaScriptLoader) loader).defineScriptType("L" + fnName, N.getPosition(), N, context);
    } else if (N.getKind() == CAstEntity.FUNCTION_ENTITY) {
      ((JavaScriptLoader) loader).defineFunctionType("L" + fnName, N.getPosition(), N, context);
    } else {
      Assertions.UNREACHABLE();
    }
  }

  protected void defineFunction(CAstEntity N, WalkContext definingContext, AbstractCFG cfg, SymbolTable symtab,
      boolean hasCatchBlock, TypeReference[][] caughtTypes, boolean hasMonitorOp, AstLexicalInformation LI,
      DebuggingInformation debugInfo) {
    if (DEBUG)
      System.err.println(("\n\nAdding code for " + N));
    String fnName = composeEntityName(definingContext, N);

    if (DEBUG)
      System.err.println(cfg);
 
    ((JavaScriptLoader) loader).defineCodeBodyCode("L" + fnName, cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, LI,
        debugInfo);
  }

  protected void doThrow(WalkContext context, int exception) {
    context.cfg().addInstruction(insts.ThrowInstruction(exception));
  }

  protected void doCall(WalkContext context, CAstNode call, int result, int exception, CAstNode name, int receiver, int[] arguments) {
    MethodReference ref = 
      name.getValue().equals("ctor") ? JavaScriptMethods.ctorReference 
          : name.getValue().equals("dispatch") ? JavaScriptMethods.dispatchReference 
              : AstMethodReference.fnReference(JavaScriptTypes.CodeBody);

    context.cfg().addInstruction(
        ((JSInstructionFactory) insts).Invoke(receiver, result, arguments, exception, 
            new JSCallSiteReference(ref, context.cfg().getCurrentInstruction())));

    context.cfg().addPreNode(call, context.getUnwindState());

    // this new block is for the normal termination case
    context.cfg().newBlock(true);

    // exceptional case: flow to target given in CAst, or if null, the exit node
    if (context.getControlFlow().getTarget(call, null) != null)
      context.cfg().addPreEdge(call, context.getControlFlow().getTarget(call, null), true);
    else
      context.cfg().addPreEdgeToExit(call, true);
  }

  protected void doNewObject(WalkContext context, CAstNode newNode, int result, Object type, int[] arguments) {
    assert arguments == null;
    TypeReference typeRef = TypeReference.findOrCreate(JavaScriptTypes.jsLoader, TypeName.string2TypeName("L" + type));

    context.cfg().addInstruction(
        insts.NewInstruction(result, NewSiteReference.make(context.cfg().getCurrentInstruction(), typeRef)));
  }

  protected void doMaterializeFunction(CAstNode n, WalkContext context, int result, int exception, CAstEntity fn) {
    int nm = context.currentScope().getConstantValue("L" + composeEntityName(context, fn));
    // "Function" is the name we use to model the constructor of function values
    int tmp = super.doGlobalRead(n, context, "Function");
    context.cfg().addInstruction(
        ((JSInstructionFactory) insts).Invoke(tmp, result, new int[] { nm }, exception, new JSCallSiteReference(
            JavaScriptMethods.ctorReference, context.cfg().getCurrentInstruction())));
  }

  public void doArrayRead(WalkContext context, int result, int arrayValue, CAstNode arrayRef, int[] dimValues) {
    Assertions.UNREACHABLE("JSAstTranslator.doArrayRead() called!");
  }

  public void doArrayWrite(WalkContext context, int arrayValue, CAstNode arrayRef, int[] dimValues, int rval) {
    Assertions.UNREACHABLE("JSAstTranslator.doArrayWrite() called!");
  }

  protected void doFieldRead(WalkContext context, int result, int receiver, CAstNode elt, CAstNode readNode) {
    this.visit(elt, context, this);
    int x = context.currentScope().allocateTempValue();

    context.cfg().addInstruction(((JSInstructionFactory) insts).AssignInstruction(x, receiver));

    context.cfg().addInstruction(((JSInstructionFactory) insts).PrototypeLookup(x, x));
    
    if (elt.getKind() == CAstNode.CONSTANT && elt.getValue() instanceof String) {
      String field = (String) elt.getValue();
      // symtab needs to have this value
      context.currentScope().getConstantValue(field);
      context.cfg().addInstruction(((JSInstructionFactory) insts).GetInstruction(result, x, field));
    } else {
      context.cfg().addInstruction(((JSInstructionFactory) insts).PropertyRead(result, x, context.getValue(elt)));
    }

    // generate code to handle read of non-existent property
    if (context.getControlFlow().getMappedNodes().contains(readNode)) {
      context.cfg().addPreNode(readNode, context.getUnwindState());

      context.cfg().newBlock(true);

      if (context.getControlFlow().getTarget(readNode, JavaScriptTypes.TypeError) != null)
        context.cfg().addPreEdge(readNode, context.getControlFlow().getTarget(readNode, JavaScriptTypes.TypeError), true);
      else
        context.cfg().addPreEdgeToExit(readNode, true);
    }
  }

  protected void doFieldWrite(WalkContext context, int receiver, CAstNode elt, CAstNode parent, int rval) {
    this.visit(elt, context, this);
    if (elt.getKind() == CAstNode.CONSTANT && elt.getValue() instanceof String) {
      String field = (String) elt.getValue();
      if (isPrologueScript(context) && "__proto__".equals(field)) {
        context.cfg().addInstruction(((JSInstructionFactory) insts).SetPrototype(receiver, rval));
      } else {
        context.currentScope().getConstantValue(field);
        SSAPutInstruction put = ((JSInstructionFactory) insts).PutInstruction(receiver, rval, field);
        try {
          assert field.equals(put.getDeclaredField().getName().toUnicodeString());
        } catch (UTFDataFormatException e) {
          Assertions.UNREACHABLE();
        }
        context.cfg().addInstruction(put);
      }
    } else {
      context.cfg().addInstruction(((JSInstructionFactory) insts).PropertyWrite(receiver, context.getValue(elt), rval));
    }
  }

  private void doPrimitiveNew(WalkContext context, int resultVal, String typeName) {
    doNewObject(context, null, resultVal, typeName + "Object", null);
    // set the class property of the new object
    int rval = context.currentScope().getConstantValue(typeName);
    context.currentScope().getConstantValue("class");
    context.cfg().addInstruction(((JSInstructionFactory) insts).PutInstruction(resultVal, rval, "class"));
  }

  protected void doPrimitive(int resultVal, WalkContext context, CAstNode primitiveCall) {
    try {
      String name = (String) primitiveCall.getChild(0).getValue();
      if (name.equals("GlobalNaN")) {
        context.cfg().addInstruction(
            ((JSInstructionFactory) insts).AssignInstruction(resultVal,
                context.currentScope().getConstantValue(new Float(Float.NaN))));
      } else if (name.equals("GlobalInfinity")) {
        context.cfg().addInstruction(
            ((JSInstructionFactory) insts).AssignInstruction(resultVal,
                context.currentScope().getConstantValue(new Float(Float.POSITIVE_INFINITY))));
      } else if (name.equals("MathE")) {
        context.cfg().addInstruction(
            ((JSInstructionFactory) insts)
                .AssignInstruction(resultVal, context.currentScope().getConstantValue(new Double(Math.E))));
      } else if (name.equals("MathPI")) {
        context.cfg().addInstruction(
            ((JSInstructionFactory) insts).AssignInstruction(resultVal, context.currentScope()
                .getConstantValue(new Double(Math.PI))));
      } else if (name.equals("MathSQRT1_2")) {
        context.cfg().addInstruction(
            ((JSInstructionFactory) insts).AssignInstruction(resultVal,
                context.currentScope().getConstantValue(new Double(Math.sqrt(.5)))));
      } else if (name.equals("MathSQRT2")) {
        context.cfg().addInstruction(
            ((JSInstructionFactory) insts).AssignInstruction(resultVal,
                context.currentScope().getConstantValue(new Double(Math.sqrt(2)))));
      } else if (name.equals("MathLN2")) {
        context.cfg().addInstruction(
            ((JSInstructionFactory) insts).AssignInstruction(resultVal,
                context.currentScope().getConstantValue(new Double(Math.log(2)))));
      } else if (name.equals("MathLN10")) {
        context.cfg().addInstruction(
            ((JSInstructionFactory) insts).AssignInstruction(resultVal,
                context.currentScope().getConstantValue(new Double(Math.log(10)))));
      } else if (name.equals("NewObject")) {
        doNewObject(context, null, resultVal, "Object", null);

      } else if (name.equals("NewArray")) {
        doNewObject(context, null, resultVal, "Array", null);

      } else if (name.equals("NewString")) {
        doPrimitiveNew(context, resultVal, "String");

      } else if (name.equals("NewNumber")) {
        doPrimitiveNew(context, resultVal, "Number");

      } else if (name.equals("NewRegExp")) {
        doPrimitiveNew(context, resultVal, "RegExp");

      } else if (name.equals("NewFunction")) {
        doNewObject(context, null, resultVal, "Function", null);

      } else if (name.equals("NewUndefined")) {
        doNewObject(context, null, resultVal, "Undefined", null);

      } else {
        context.cfg().addInstruction(
            ((JSInstructionFactory) insts).AssignInstruction(resultVal, context.currentScope().getConstantValue(null)));
      }
    } catch (ClassCastException e) {
      throw new RuntimeException("Cannot translate primitive " + primitiveCall.getChild(0).getValue());
    }
  }

  protected void doIsFieldDefined(WalkContext context, int result, int ref, CAstNode f) {
    if (f.getKind() == CAstNode.CONSTANT && f.getValue() instanceof String) {
      String field = (String) f.getValue();

      FieldReference fieldRef = FieldReference.findOrCreate(JavaScriptTypes.Root, Atom.findOrCreateUnicodeAtom((String) field),
          JavaScriptTypes.Root);

      context.cfg().addInstruction(((JSInstructionFactory) insts).IsDefinedInstruction(result, ref, fieldRef));

    } else {

      context.cfg().addInstruction(((JSInstructionFactory) insts).IsDefinedInstruction(result, ref, context.getValue(f)));
    }
  }

  protected boolean visitInstanceOf(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    context.setValue(n, result);
    return false;
  }

  protected void leaveInstanceOf(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.getValue(n);

    visit(n.getChild(0), context, visitor);
    int value = context.getValue(n.getChild(0));

    visit(n.getChild(1), context, visitor);
    int type = context.getValue(n.getChild(1));

    context.cfg().addInstruction(new JavaScriptInstanceOf(result, value, type));
  }

  protected void doPrologue(WalkContext context) {
    super.doPrologue(context);
    
    int tempVal = context.currentScope().allocateTempValue();
    doNewObject(context, null, tempVal, "Array", null);
    CAstSymbol args = new CAstSymbolImpl("arguments");
    context.currentScope().declare(args, tempVal);
    //context.cfg().addInstruction(((JSInstructionFactory) insts).PutInstruction(1, tempVal, "arguments"));
  }

  protected boolean doVisit(CAstNode n, WalkContext cntxt, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) cntxt;
    switch (n.getKind()) {
    case CAstNode.TYPE_OF: {
      int result = context.currentScope().allocateTempValue();

      this.visit(n.getChild(0), context, this);
      int ref = context.getValue(n.getChild(0));

      context.cfg().addInstruction(((JSInstructionFactory) insts).TypeOfInstruction(result, ref));

      context.setValue(n, result);
      return true;
    }

    case JavaScriptCAstNode.ENTER_WITH:
    case JavaScriptCAstNode.EXIT_WITH: {

      this.visit(n.getChild(0), context, this);
      int ref = context.getValue(n.getChild(0));

      context.cfg().addInstruction(((JSInstructionFactory) insts).WithRegion(ref, n.getKind() == JavaScriptCAstNode.ENTER_WITH));

      return true;
    }
    default: {
      return false;
    }
    }
  }

}
