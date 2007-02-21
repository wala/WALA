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
/*
 * Created on Aug 22, 2005
 */
package com.ibm.wala.cast.java.translator.polyglot;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import polyglot.ast.*;
import polyglot.ast.Binary.Operator;
import polyglot.ast.ConstructorCall.Kind;
import polyglot.types.*;
import polyglot.util.Position;

import com.ibm.wala.cast.java.loader.Util;
import com.ibm.wala.cast.java.translator.JavaProcedureEntity;
import com.ibm.wala.cast.java.translator.TranslatorToCAst;
import com.ibm.wala.cast.java.types.JavaType;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.CAstTypeDictionary;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;
import com.ibm.wala.cast.tree.impl.CAstControlFlowRecorder;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstNodeTypeMapRecorder;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstSourcePositionRecorder;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.IteratorPlusOne;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

public class PolyglotJava2CAstTranslator implements TranslatorToCAst {
  protected final CAst fFactory = new CAstImpl();

  protected final NodeFactory fNodeFactory;

  protected final TypeSystem fTypeSystem;

  protected Type fNPEType;

  protected Type fREType; // RuntimeException

  protected final ClassLoaderReference fClassLoaderRef;

  private CAstTypeDictionary fTypeDict;

  private TranslatingVisitor fTranslator;

  protected PolyglotIdentityMapper fIdentityMapper;

  protected final boolean DEBUG = true;

  final protected TranslatingVisitor getTranslator() {
    if (fTranslator == null)
      fTranslator = createTranslator();
    return fTranslator;
  }

  protected TranslatingVisitor createTranslator() {
    return new JavaTranslatingVisitorImpl();
  }

  protected CAstTypeDictionary getTypeDict() {
    if (fTypeDict == null) {
      fTypeDict = createTypeDict();
    }
    return fTypeDict;
  }

  protected PolyglotTypeDictionary createTypeDict() {
    return new PolyglotTypeDictionary(fTypeSystem, this);
  }

  protected static CAstOperator mapUnaryOpcode(Unary.Operator operator) {
    if (operator.equals(Unary.BIT_NOT))
      return CAstOperator.OP_BITNOT;
    if (operator.equals(Unary.NEG))
      return CAstOperator.OP_SUB; // CAst will handle OP_SUB with only 1
    // arg properly !!! Hah!
    if (operator.equals(Unary.NOT))
      return CAstOperator.OP_NOT;
    if (operator.equals(Unary.POS))
      return CAstOperator.OP_ADD; // CAst will throw away OP_ADD with only
    // 1 arg!!!

    if (operator.equals(Unary.POST_DEC))
      return CAstOperator.OP_SUB; // translator will produce different
    // CAstNode types for post dec
    if (operator.equals(Unary.POST_INC))
      return CAstOperator.OP_ADD; // translator will produce different
    // CAstNode types for post inc
    if (operator.equals(Unary.PRE_DEC))
      return CAstOperator.OP_SUB; // translator will produce different
    // CAstNode types for pre dec
    if (operator.equals(Unary.PRE_INC))
      return CAstOperator.OP_ADD; // translator will produce different
    // CAstNode types for pre inc

    Assertions.UNREACHABLE("Java2CAstTranslator.JavaTranslatingVisitorImpl.mapUnaryOpcode(): unrecognized unary operator.");
    return null;
  }

  protected static CAstOperator mapBinaryOpcode(Binary.Operator operator) {
    if (operator.equals(Binary.ADD))
      return CAstOperator.OP_ADD;
    if (operator.equals(Binary.BIT_AND))
      return CAstOperator.OP_BIT_AND;
    if (operator.equals(Binary.BIT_OR))
      return CAstOperator.OP_BIT_OR;
    if (operator.equals(Binary.BIT_XOR))
      return CAstOperator.OP_BIT_XOR;
    if (operator.equals(Binary.COND_AND))
      return CAstOperator.OP_REL_AND;
    if (operator.equals(Binary.COND_OR))
      return CAstOperator.OP_REL_OR;
    if (operator.equals(Binary.DIV))
      return CAstOperator.OP_DIV;
    if (operator.equals(Binary.EQ))
      return CAstOperator.OP_EQ;
    if (operator.equals(Binary.GE))
      return CAstOperator.OP_GE;
    if (operator.equals(Binary.GT))
      return CAstOperator.OP_GT;
    if (operator.equals(Binary.LE))
      return CAstOperator.OP_LE;
    if (operator.equals(Binary.LT))
      return CAstOperator.OP_LT;
    if (operator.equals(Binary.MOD))
      return CAstOperator.OP_MOD;
    if (operator.equals(Binary.MUL))
      return CAstOperator.OP_MUL;
    if (operator.equals(Binary.NE))
      return CAstOperator.OP_NE;
    if (operator.equals(Binary.SHL))
      return CAstOperator.OP_LSH;
    if (operator.equals(Binary.SHR))
      return CAstOperator.OP_RSH;
    if (operator.equals(Binary.SUB))
      return CAstOperator.OP_SUB;
    if (operator.equals(Binary.USHR))
      return CAstOperator.OP_URSH;
    Assertions.UNREACHABLE("Java2CAstTranslator.JavaTranslatingVisitorImpl.mapBinaryOpcode(): unrecognized binary operator.");
    return null;
  }

  protected class JavaTranslatingVisitorImpl implements TranslatingVisitor {
    public CAstNode visit(MethodDecl m, MethodContext mc) {
      if (m.body() == null || m.body().statements().size() == 0)
        return makeNode(mc, fFactory, m, CAstNode.RETURN);
      else
        return walkNodes(m.body(), mc);
    }

    public CAstNode visit(ConstructorDecl cd, MethodContext mc) {
      // Needs to examine the initializers in the ClassContext
      // and glue that code into the right place relative to the
      // constructor method body ("wherever that may turn out to be").
      List/* <FieldDecl|Initializer> */inits = mc.getInitializers();

      Block body = cd.body();
      if (hasSuperCall(body)) {
        // Split at call to super:
        // super();
        // field initializer code
        // remainder of ctor body
        CAstNode[] bodyNodes = new CAstNode[inits.size() + body.statements().size()];

        int idx = 0;
        for (Iterator iter = body.statements().iterator(); iter.hasNext();) {
          Stmt s = (Stmt) iter.next();

          bodyNodes[idx++] = walkNodes(s, mc);
          if (idx == 1) {
            Assertions._assert(isSpecialCallStmt(s, ConstructorCall.SUPER));
            idx = insertInitializers(mc, bodyNodes, idx);
          }
        }
        return makeNode(mc, fFactory, body, CAstNode.BLOCK_STMT, bodyNodes);
      } else if (hasThisCall(body)) {
        return walkNodes(body, mc);
      } else {
        // add explicit call to default super()
        // TODO following superClass lookup of default ctor won't work if we
        // process Object in source...
        ClassType superClass = (ClassType) cd.constructorInstance().container().superType();
        ProcedureInstance defaultSuperCtor = findDefaultCtor(superClass);
        CAstNode[] bodyNodes = new CAstNode[inits.size() + body.statements().size() + 1];
        CallSiteReference callSiteRef = CallSiteReference.make(0, fIdentityMapper.getMethodRef(defaultSuperCtor), IInvokeInstruction.Dispatch.SPECIAL);

        CAstNode superCall = makeNode(mc, fFactory, cd, CAstNode.CALL, makeNode(mc, fFactory, cd, CAstNode.SUPER), fFactory.makeConstant(callSiteRef));

        bodyNodes[0] = superCall;
        insertInitializers(mc, bodyNodes, 1);

        int idx = inits.size() + 1;
        for (Iterator iter = body.statements().iterator(); iter.hasNext(); idx++) {
          Stmt s = (Stmt) iter.next();
          bodyNodes[idx] = walkNodes(s, mc);
        }
        return makeNode(mc, fFactory, body, CAstNode.BLOCK_STMT, bodyNodes);
      }
    }

    private ProcedureInstance findDefaultCtor(ClassType superClass) {
      List/* <ProcedureInstance> */ctors = superClass.constructors();
      for (Iterator iter = ctors.iterator(); iter.hasNext();) {
        ConstructorInstance ctor = (ConstructorInstance) iter.next();
        if (ctor.formalTypes().isEmpty())
          return ctor;
      }
      Assertions.UNREACHABLE("Couldn't find default ctor");
      return null;
    }

    public CAstNode visit(FieldDecl f, MethodContext ctorContext) {
      // Generate CAST node for the initializer (init())
//      Type targetType = f.memberInstance().container();
//      Type fieldType = f.type().type();
      FieldReference fieldRef = fIdentityMapper.getFieldRef(f.fieldInstance());
      // We use null to indicate an OBJECT_REF to a static field, as the
      // FieldReference doesn't
      // hold enough info to determine this. In this case, (unlike field ref)
      // we don't have a
      // target expr to evaluate.
      CAstNode thisNode = f.flags().isStatic() ? null : makeNode(ctorContext, fFactory, f, CAstNode.THIS);
      CAstNode lhsNode = makeNode(ctorContext, fFactory, f, CAstNode.OBJECT_REF, thisNode, fFactory.makeConstant(fieldRef));

      Expr init = f.init();
      CAstNode rhsNode = walkNodes(init, ctorContext);
      CAstNode assNode = makeNode(ctorContext, fFactory, f, CAstNode.ASSIGN, lhsNode, rhsNode);

      return assNode;
    }

    public CAstNode visit(Import i, WalkContext wc) {
      Assertions.UNREACHABLE("walkNodes.ASTTraverser.visit(Import)");
      return null;
    }

    public CAstNode visit(PackageNode p, WalkContext wc) {
      Assertions.UNREACHABLE("walkNodes.ASTTraverser.visit(PackageNode)");
      return null;
    }

    public CAstNode visit(CanonicalTypeNode ctn, WalkContext wc) {
      // We'll take care of this in its surrounding context...
      return makeNode(wc, fFactory, null, CAstNode.EMPTY);
    }

    public CAstNode visit(ArrayTypeNode ctn, WalkContext wc) {
      // We'll take care of this in its surrounding context...
      Assertions.UNREACHABLE("walkNodes.ASTTraverser.visit(CanonicalTypeNode)");
      return null;
    }

    public CAstNode visit(ArrayInit ai, WalkContext wc) {
      if (((ArrayType) ai.type()).base().isNull()) {
        Assertions._assert(false, "bad type " + ai.type() + " for " + ai + " at " + ai.position());
      }

      TypeReference newTypeRef = fIdentityMapper.getTypeRef(ai.type());
      CAstNode[] eltNodes = new CAstNode[ai.elements().size() + 1];
      int idx = 0;

      eltNodes[idx++] = makeNode(wc, fFactory, ai, CAstNode.NEW, fFactory.makeConstant(newTypeRef), fFactory.makeConstant(ai.elements().size()));
      for (Iterator iter = ai.elements().iterator(); iter.hasNext(); idx++) {
        Expr element = (Expr) iter.next();
        eltNodes[idx] = walkNodes(element, wc);
        if (eltNodes[idx] == null) {
          Assertions._assert(eltNodes[idx] != null, element.toString());
        }
      }

      return makeNode(wc, fFactory, ai, CAstNode.ARRAY_LITERAL, eltNodes);
    }

    public CAstNode visit(ArrayAccessAssign aaa, WalkContext wc) {
      return processAssign(aaa, wc);
    }

    public CAstNode visit(FieldAssign fa, WalkContext wc) {
      return processAssign(fa, wc);
    }

    public CAstNode visit(LocalAssign la, WalkContext wc) {
      return processAssign(la, wc);
    }

    private CAstNode processAssign(Assign la, WalkContext wc) {
      if (la.operator() == Assign.ASSIGN)
        return makeNode(wc, fFactory, la, CAstNode.ASSIGN, walkNodes(la.left(), wc), walkNodes(la.right(), wc));
      else
        return makeNode(wc, fFactory, la, CAstNode.ASSIGN_PRE_OP, walkNodes(la.left(), wc), walkNodes(la.right(), wc), mapAssignOperator(la.operator()));
    }

    protected CAstOperator mapAssignOperator(Assign.Operator op) {
      if (op == Assign.ADD_ASSIGN)
        return CAstOperator.OP_ADD;
      else if (op == Assign.BIT_AND_ASSIGN)
        return CAstOperator.OP_BIT_AND;
      else if (op == Assign.BIT_OR_ASSIGN)
        return CAstOperator.OP_BIT_OR;
      else if (op == Assign.BIT_XOR_ASSIGN)
        return CAstOperator.OP_BIT_XOR;
      else if (op == Assign.DIV_ASSIGN)
        return CAstOperator.OP_DIV;
      else if (op == Assign.MOD_ASSIGN)
        return CAstOperator.OP_MOD;
      else if (op == Assign.MUL_ASSIGN)
        return CAstOperator.OP_MUL;
      else if (op == Assign.SHL_ASSIGN)
        return CAstOperator.OP_LSH;
      else if (op == Assign.SHR_ASSIGN)
        return CAstOperator.OP_RSH;
      else if (op == Assign.SUB_ASSIGN)
        return CAstOperator.OP_SUB;
      else if (op == Assign.USHR_ASSIGN)
        return CAstOperator.OP_URSH;
      Assertions.UNREACHABLE("Unknown assignment operator");
      return null;
    }

    public CAstNode visit(Binary b, WalkContext wc) {
      Expr left = b.left();
      Expr right = b.right();
      Operator operator = b.operator();

      if (operator.equals(Binary.COND_AND))
        return makeNode(wc, fFactory, b, CAstNode.IF_EXPR, walkNodes(left, wc), walkNodes(right, wc), fFactory.makeConstant(false));
      else if (operator.equals(Binary.COND_OR))
        return makeNode(wc, fFactory, b, CAstNode.IF_EXPR, walkNodes(left, wc), fFactory.makeConstant(true), walkNodes(right, wc));
      else
        return makeNode(wc, fFactory, b, CAstNode.BINARY_EXPR, mapBinaryOpcode(operator), walkNodes(left, wc), walkNodes(right, wc));
    }

    @SuppressWarnings("unchecked")
    private void handleThrowsFromCall(ProcedureInstance procedureInstance, Node callAstNode, WalkContext wc) {
      List<Type> throwTypes = procedureInstance.throwTypes();
      for (Iterator<Type> iter = new IteratorPlusOne<Type>(throwTypes.iterator(), fREType); iter.hasNext();) {
        Type thrownType = (Type) iter.next();
        Collection/* <Pair<Type,Node>> */catchTargets = wc.getCatchTargets(thrownType);

        for (Iterator targetIter = catchTargets.iterator(); targetIter.hasNext();) {
          Pair/* <Type,Node> */catchTarget = (Pair/* <Type,Node> */) targetIter.next();

          wc.cfg().add(callAstNode, catchTarget.snd, catchTarget.fst);
        }
      }
    }

    public CAstNode visit(Call c, WalkContext wc) {
      MethodInstance methodInstance = c.methodInstance();
      boolean isStatic = methodInstance.flags().isStatic();
      ReferenceType methodOwner = methodInstance.container();

      if (methodOwner.isArray()) {
        List realOne = methodInstance.overrides();
        Assertions._assert(realOne.size() == 2, "bad array method");
        methodInstance = (MethodInstance) realOne.get(1);
        methodOwner = methodInstance.container();
      }

      if (!methodOwner.isClass()) {
        Assertions._assert(false, "owner " + methodOwner + " of " + methodInstance + " is not a class");
      }

      boolean isIntf = ((ClassType) methodOwner).flags().isInterface();
      Receiver target = c.target();
      boolean isSpecial = methodInstance.flags().isPrivate() || (target instanceof Special && ((Special) target).kind() == Special.SUPER);

      CAstNode[] children = new CAstNode[2 + methodInstance.formalTypes().size()]; // including
                                                                                    // the
                                                                                    // MethodReference
      int i = 0;

      if (!isStatic)
        children[i++] = walkNodes(target, wc);
      else
        children[i++] = makeNode(wc, fFactory, null, CAstNode.VOID);

      if (children[0] == null) {
        Assertions._assert(children[0] != null, "no receiver for " + methodInstance + " in " + wc.getEnclosingMethod().signature());
      }

      MethodReference methodRef = fIdentityMapper.getMethodRef(methodInstance);
      int dummyPC = 0; // Just want to wrap the kind of call; the "rear end"
      // won't care about anything else...
      CallSiteReference callSiteRef;

      if (isStatic)
        callSiteRef = CallSiteReference.make(dummyPC, methodRef, IInvokeInstruction.Dispatch.STATIC);
      else if (isIntf)
        callSiteRef = CallSiteReference.make(dummyPC, methodRef, IInvokeInstruction.Dispatch.INTERFACE);
      else if (isSpecial)
        callSiteRef = CallSiteReference.make(dummyPC, methodRef, IInvokeInstruction.Dispatch.SPECIAL);
      else
        callSiteRef = CallSiteReference.make(dummyPC, methodRef, IInvokeInstruction.Dispatch.VIRTUAL);

      children[i++] = fFactory.makeConstant(callSiteRef);
      for (Iterator iter = c.arguments().iterator(); iter.hasNext();) {
        Expr arg = (Expr) iter.next();
        children[i++] = walkNodes(arg, wc);
      }

      handleThrowsFromCall(methodInstance, c, wc);

      CAstNode result = makeNode(wc, fFactory, c, CAstNode.CALL, children);
      wc.cfg().map(c, result);
      return result;
    }

    public CAstNode visit(ConstructorCall cc, WalkContext wc) {
      ConstructorInstance ctorInstance = cc.constructorInstance();
      ReferenceType methodOwner = ctorInstance.container();
      Assertions._assert(methodOwner.isClass());
      MethodReference methodRef = fIdentityMapper.getMethodRef(ctorInstance);

      int dummyPC = 0; // Just want to wrap the kind of call; the "rear end"
      // won't care about anything else...
      CallSiteReference callSiteRef = CallSiteReference.make(dummyPC, methodRef, IInvokeInstruction.Dispatch.SPECIAL);

      CAstNode[] children = new CAstNode[1 + 1 + ctorInstance.formalTypes().size()]; // including
                                                                                      // the
                                                                                      // MethodReference
      int i = 0;

      CAstNode targetNode;

      targetNode = (cc.kind() == ConstructorCall.THIS) ? makeNode(wc, fFactory, cc, CAstNode.THIS) : makeNode(wc, fFactory, cc, CAstNode.SUPER);

      children[i++] = targetNode;
      children[i++] = fFactory.makeConstant(callSiteRef);
      for (Iterator iter = cc.arguments().iterator(); iter.hasNext();) {
        Expr arg = (Expr) iter.next();
        children[i++] = walkNodes(arg, wc);
      }

      handleThrowsFromCall(ctorInstance, cc, wc);

      CAstNode result = makeNode(wc, fFactory, cc, CAstNode.CALL, children);
      wc.cfg().map(cc, result);
      return result;
    }

    public CAstNode visit(Cast c, WalkContext wc) {
      Expr arg = c.expr();
      Type castedTo = c.castType().type();

      // TODO maybe use a TypeReference below instead of a CAstType
      return makeNode(wc, fFactory, c, CAstNode.CAST, fFactory.makeConstant(getTypeDict().getCAstTypeFor(castedTo)), walkNodes(arg, wc));
    }

    public CAstNode visit(Conditional c, WalkContext wc) {
      return makeNode(wc, fFactory, c, CAstNode.IF_EXPR, walkNodes(c.cond(), wc), walkNodes(c.consequent(), wc), walkNodes(c.alternative(), wc));
    }

    public CAstNode visit(Instanceof io, WalkContext wc) {
      return makeNode(wc, fFactory, io, CAstNode.INSTANCEOF, fFactory.makeConstant(getTypeDict().getCAstTypeFor(io.compareType().type())), walkNodes(io.expr(),
          wc));
    }

    public CAstNode visit(BooleanLit bl, WalkContext wc) {
      return fFactory.makeConstant(bl.value());
    }

    public CAstNode visit(ClassLit cl, WalkContext wc) {
      Type litType = cl.typeNode().type();
      String typeName = fIdentityMapper.typeToTypeID(litType);
      return makeNode(wc, fFactory, cl, CAstNode.TYPE_LITERAL_EXPR, fFactory.makeConstant(typeName));
    }

    public CAstNode visit(FloatLit fl, WalkContext wc) {
      return (fl.kind() == FloatLit.FLOAT) ? fFactory.makeConstant((float) fl.value()) : fFactory.makeConstant(fl.value());
    }

    public CAstNode visit(NullLit nl, WalkContext wc) {
      return fFactory.makeConstant(null);
    }

    public CAstNode visit(CharLit cl, WalkContext wc) {
      return fFactory.makeConstant(cl.value());
    }

    public CAstNode visit(IntLit il, WalkContext wc) {
      return makeNode(wc, fFactory, il, CAstNode.CAST, fFactory.makeConstant(getTypeDict().getCAstTypeFor(il.type())), fFactory.makeConstant((int) il.value()));
    }

    public CAstNode visit(StringLit sl, WalkContext wc) {
      return fFactory.makeConstant(sl.value());
    }

    public CAstNode visit(New n, WalkContext wc) {
      CAstEntity anonClass = null;
      String newTypeNameStr;
      TypeReference newTypeRef;
      ConstructorInstance ctorInst = n.constructorInstance();
      MethodReference ctorRef= fIdentityMapper.getMethodRef(ctorInst);

      if (n.body() != null) {
        fIdentityMapper.mapLocalAnonTypeToMethod((ClassType) n.type(), wc.getEnclosingMethod());

        anonClass = walkEntity(n, wc);

        newTypeNameStr = anonClass.getType().getName();
        TypeName newTypeName = TypeName.string2TypeName(newTypeNameStr);
        Selector ctorSel= ctorRef.getSelector();
        newTypeRef = TypeReference.findOrCreate(fClassLoaderRef, newTypeName);
        ctorRef= MethodReference.findOrCreate(newTypeRef, ctorSel);
      } else {
        newTypeRef = fIdentityMapper.getTypeRef(n.type());
      }

      List/* <Expr> */args = n.arguments();
      String tmpName = "ctor temp"; // this name is an illegal Java
      // identifier

      CAstNode newNode = makeNode(wc, fFactory, n, CAstNode.NEW, fFactory.makeConstant(newTypeRef));

      if (n.body() != null)
        wc.addScopedEntity(newNode, anonClass);

      int dummyPC = 0; // Just want to wrap the kind of call; the "rear end"
      // won't care about anything else...
      CallSiteReference callSiteRef = CallSiteReference.make(dummyPC, ctorRef, IInvokeInstruction.Dispatch.SPECIAL);

      CAstNode[] argNodes = new CAstNode[args.size() + 2]; // args + recvr
      // + ctor ref

      int idx = 0;
      argNodes[idx++] = makeNode(wc, fFactory, n, CAstNode.VAR, fFactory.makeConstant(tmpName));
      argNodes[idx++] = fFactory.makeConstant(callSiteRef);
      for (Iterator iter = args.iterator(); iter.hasNext();) {
        Expr arg = (Expr) iter.next();
        argNodes[idx++] = walkNodes(arg, wc);
      }
      CAstNode callNode = makeNode(wc, fFactory, n, CAstNode.CALL, argNodes);
      wc.cfg().map(n, callNode);

      handleThrowsFromCall(ctorInst, n, wc);

      return makeNode(wc, fFactory, n, CAstNode.LOCAL_SCOPE, makeNode(wc, fFactory, n, CAstNode.BLOCK_EXPR, makeNode(wc, fFactory, n, CAstNode.DECL_STMT,
          makeNode(wc, fFactory, n, CAstNode.VAR, fFactory.makeConstant(tmpName)), fFactory.makeConstant(true), fFactory.makeConstant(false), newNode),
          callNode, makeNode(wc, fFactory, n, CAstNode.VAR, fFactory.makeConstant(tmpName))));
    }

    public CAstNode visit(NewArray na, WalkContext wc) {
      Type newType = na.type();
      ArrayInit ai = na.init();
      Assertions._assert(newType.isArray());

      if (ai != null) {
        return visit(ai, wc);
      } else {
        ArrayType arrayType = (ArrayType) newType;
        TypeReference arrayTypeRef = fIdentityMapper.getTypeRef(arrayType);

        List/* <Expr> */dims = na.dims();
        CAstNode[] args = new CAstNode[dims.size() + 1];

        int idx = 0;
        args[idx++] = fFactory.makeConstant(arrayTypeRef);
        for (Iterator iter = dims.iterator(); iter.hasNext();) {
          Expr dimExpr = (Expr) iter.next();
          args[idx++] = walkNodes(dimExpr, wc);
        }
        return makeNode(wc, fFactory, na, CAstNode.NEW, args);
      }
    }

    public CAstNode visit(Special s, WalkContext wc) {
      if (s.qualifier() != null) {
        Type owningType = s.qualifier().type();
        TypeReference owningTypeRef = fIdentityMapper.getTypeRef(owningType);

        return makeNode(wc, fFactory, s, s.kind() == Special.THIS ? CAstNode.THIS : CAstNode.SUPER, fFactory.makeConstant(owningTypeRef));
      } else {
        return makeNode(wc, fFactory, s, s.kind() == Special.THIS ? CAstNode.THIS : CAstNode.SUPER);
      }
    }

    public CAstNode visit(Unary u, WalkContext wc) {
      if (isAssignOp(u.operator())) {
        if (u.operator().isPrefix())
          return makeNode(wc, fFactory, u, CAstNode.ASSIGN_PRE_OP, walkNodes(u.expr(), wc), fFactory.makeConstant(1), mapUnaryOpcode(u.operator()));
        else
          return makeNode(wc, fFactory, u, CAstNode.ASSIGN_POST_OP, walkNodes(u.expr(), wc), fFactory.makeConstant(1), mapUnaryOpcode(u.operator()));
      } else if (u.operator() == Unary.POS) // drop useless unary plus
        // operator
        return walkNodes(u.expr(), wc);
      else if (u.operator() == Unary.NEG) {
        CAstNode zero;
        if (u.expr().type().isLongOrLess())
          zero = fFactory.makeConstant(0L);
        else
          zero = fFactory.makeConstant(0.0);
        return makeNode(wc, fFactory, u, CAstNode.BINARY_EXPR, CAstOperator.OP_SUB, zero, walkNodes(u.expr(), wc));
      } else
        return makeNode(wc, fFactory, u, CAstNode.UNARY_EXPR, mapUnaryOpcode(u.operator()), walkNodes(u.expr(), wc));
    }

    protected boolean isAssignOp(Unary.Operator operator) {
      return operator == Unary.POST_DEC || operator == Unary.POST_INC || operator == Unary.PRE_DEC || operator == Unary.PRE_INC;
    }

    public CAstNode visit(ArrayAccess aa, WalkContext wc) {
      TypeReference eltTypeRef = fIdentityMapper.getTypeRef(aa.type());

      return makeNode(wc, fFactory, aa, CAstNode.ARRAY_REF, walkNodes(aa.array(), wc), fFactory.makeConstant(eltTypeRef), walkNodes(aa.index(), wc));
    }

    public CAstNode visit(Field f, WalkContext wc) {
      Receiver target = f.target();
      Type targetType = target.type();
//      Type fieldType = f.type();

      if (targetType.isArray()) {
        Assertions._assert(f.name().equals("length"));

        return makeNode(wc, fFactory, f, CAstNode.ARRAY_LENGTH, walkNodes(target, wc));
      }
      FieldReference fieldRef = fIdentityMapper.getFieldRef(f.fieldInstance());
      CAstNode targetNode = walkNodes(target, wc);

      if (f.fieldInstance().flags().isStatic()) {
        // JLS says: evaluate the target of the field ref and throw it away.
        // Hence the following block expr, whose 2 children are the target
        // evaluation
        // followed by the OBJECT_REF with a null target child (which the
        // "back-end"
        // CAst -> IR translator interprets as a static ref).
        return makeNode(wc, fFactory, f, CAstNode.BLOCK_EXPR, targetNode, makeNode(wc, fFactory, f, CAstNode.OBJECT_REF, makeNode(wc, fFactory, null,
            CAstNode.VOID), fFactory.makeConstant(fieldRef)));
      } else {
        Collection excTargets = wc.getCatchTargets(fNPEType);
        if (!excTargets.isEmpty()) {
          // connect NPE exception edge to relevant catch targets
          // (presumably only one)
          for (Iterator iterator = excTargets.iterator(); iterator.hasNext();) {
            Pair catchPair = (Pair) iterator.next();
            wc.cfg().add(f, catchPair.snd, fNPEType);
          }
        } else {
          // connect exception edge to exit
          wc.cfg().add(f, CAstControlFlowMap.EXCEPTION_TO_EXIT, fNPEType);
        }
        CAstNode refNode = makeNode(wc, fFactory, f, CAstNode.OBJECT_REF, targetNode, fFactory.makeConstant(fieldRef));

        wc.cfg().map(f, refNode);
        return refNode;
      }
    }

    public CAstNode visit(Local l, WalkContext wc) {
      return makeNode(wc, fFactory, l, CAstNode.VAR, fFactory.makeConstant(l.name()));
    }

    public CAstNode visit(ClassBody cb, WalkContext wc) {
      Assertions.UNREACHABLE("walkNodes.ASTTraverser.visit(ClassBody)");
      return null;
    }

    public CAstNode visit(ClassDecl cd, WalkContext wc) {
      Assertions.UNREACHABLE("walkNodes.ASTTraverser.visit(ClassDecl)");
      return null;
    }

    public CAstNode visit(Initializer i, WalkContext wc) {
      // Perhaps this is invoked from within the ConstructorDecl visit()
      // method...
      return walkNodes(i.body(), wc);
    }

    public CAstNode visit(Assert a, WalkContext wc) {
      return PolyglotJava2CAstTranslator.this.makeNode(wc, fFactory, a, CAstNode.ASSERT, walkNodes(a.cond(), wc));
    }

    public CAstNode visit(Branch b, WalkContext wc) {
      Node target = null;
      if (b.kind() == Branch.BREAK) {
        target = wc.getBreakFor(b.label());
      } else {
        target = wc.getContinueFor(b.label());
      }

      Assertions._assert(target != null);

      CAstNode result = makeNode(wc, fFactory, b, CAstNode.GOTO);

      wc.cfg().map(b, result);
      wc.cfg().add(b, target, null);

      return result;
    }

    public CAstNode visit(Block b, WalkContext wc) {
      CAstNode[] stmtNodes = new CAstNode[b.statements().size()];

      int idx = 0;
      for (Iterator iter = b.statements().iterator(); iter.hasNext(); idx++) {
        Stmt s = (Stmt) iter.next();
        stmtNodes[idx] = walkNodes(s, wc);
      }
      return makeNode(wc, fFactory, b, CAstNode.LOCAL_SCOPE, makeNode(wc, fFactory, b, CAstNode.BLOCK_STMT, stmtNodes));
    }

    public CAstNode visit(SwitchBlock sb, WalkContext wc) {
      CAstNode[] stmtNodes = new CAstNode[sb.statements().size()];

      int idx = 0;
      for (Iterator iter = sb.statements().iterator(); iter.hasNext(); idx++) {
        Stmt s = (Stmt) iter.next();
        stmtNodes[idx] = walkNodes(s, wc);
      }
      return makeNode(wc, fFactory, sb, CAstNode.BLOCK_STMT, stmtNodes);
    }

    public CAstNode visit(Catch c, WalkContext wc) {
      Block body = c.body();
      Formal f = c.formal();

      CAstNode excDecl = makeNode(wc, fFactory, c, CAstNode.CATCH, fFactory.makeConstant(f.name()), walkNodes(body, wc));
      CAstNode localScope = makeNode(wc, fFactory, c, CAstNode.LOCAL_SCOPE, excDecl);

      wc.cfg().map(c, excDecl);
      wc.getNodeTypeMap().add(excDecl, wc.getTypeDictionary().getCAstTypeFor(c.catchType()));
      return localScope;
    }

    public CAstNode visit(If i, WalkContext wc) {
      return makeNode(wc, fFactory, i, CAstNode.IF_STMT, walkNodes(i.cond(), wc), walkNodes(i.consequent(), wc), walkNodes(i.alternative(), wc));
    }

    public CAstNode visit(Labeled l, WalkContext wc) {
      Node stmt = l.statement();
      while (stmt instanceof Block) {
        stmt = (Node) ((Block) stmt).statements().iterator().next();
      }

      wc.getLabelMap().put(stmt, l.label());

      CAstNode result = makeNode(wc, fFactory, l, CAstNode.LABEL_STMT, fFactory.makeConstant(l.label()), walkNodes(l.statement(), wc));

      wc.cfg().map(l, result);

      wc.getLabelMap().remove(stmt);

      return result;
    }

    public CAstNode visit(LocalClassDecl lcd, WalkContext wc) {
      fIdentityMapper.mapLocalAnonTypeToMethod(lcd.decl().type(), wc.getEnclosingMethod());

      CAstEntity classEntity = walkEntity(lcd.decl(), wc);

      final CAstNode lcdNode = makeNode(wc, fFactory, lcd, CAstNode.EMPTY);

      wc.addScopedEntity(lcdNode, classEntity);
      return lcdNode;
    }

    private Node makeBreakTarget(Node loop) {
      return fNodeFactory.Labeled(Position.COMPILER_GENERATED, "breakLabel" + loop.position().toString().replace('.', '_'), fNodeFactory.Empty(Position.COMPILER_GENERATED));
    }

    private Node makeContinueTarget(Node loop) {
      return fNodeFactory.Labeled(Position.COMPILER_GENERATED, "continueLabel" + loop.position().toString().replace('.', '_'), fNodeFactory.Empty(Position.COMPILER_GENERATED));
    }

    public CAstNode visit(Do d, WalkContext wc) {
      Node header = fNodeFactory.Empty(Position.COMPILER_GENERATED);
      Node breakTarget = makeBreakTarget(d);
      Node continueTarget = makeContinueTarget(d);

      CAstNode loopGoto = makeNode(wc, fFactory, d, CAstNode.IFGOTO, walkNodes(d.cond(), wc));

      wc.cfg().map(loopGoto, loopGoto);
      wc.cfg().add(loopGoto, header, Boolean.TRUE);

      String loopLabel = (String) wc.getLabelMap().get(d);

      WalkContext lc = new LoopContext(wc, loopLabel, breakTarget, continueTarget);

      CAstNode continueNode = walkNodes(continueTarget, wc);

      return makeNode(wc, fFactory, d, CAstNode.BLOCK_STMT, walkNodes(header, wc), makeNode(wc, fFactory, d, CAstNode.BLOCK_STMT, walkNodes(d.body(), lc),
          continueNode), loopGoto, walkNodes(breakTarget, wc));
    }

    public CAstNode visit(For f, WalkContext wc) {
      Node breakTarget = makeBreakTarget(f);
      Node continueTarget = makeContinueTarget(f);
      String loopLabel = (String) wc.getLabelMap().get(f);
      WalkContext lc = new LoopContext(wc, loopLabel, breakTarget, continueTarget);

      CAstNode[] inits = new CAstNode[f.inits().size()];
      for (int i = 0; i < inits.length; i++) {
        inits[i] = walkNodes((Node) f.inits().get(i), wc);
      }

      CAstNode[] iters = new CAstNode[f.iters().size()];
      for (int i = 0; i < iters.length; i++) {
        iters[i] = walkNodes((Node) f.iters().get(i), wc);
      }

      CAstNode initsBlock = makeNode(wc, fFactory, f, CAstNode.BLOCK_STMT, inits);
      CAstNode itersBlock = makeNode(wc, fFactory, f, CAstNode.BLOCK_STMT, iters);

      return makeNode(wc, fFactory, f, CAstNode.BLOCK_STMT, initsBlock, makeNode(wc, fFactory, f, CAstNode.LOOP, walkNodes(f.cond(), wc), makeNode(wc,
          fFactory, f, CAstNode.BLOCK_STMT, walkNodes(f.body(), lc), walkNodes(continueTarget, wc), itersBlock)), walkNodes(breakTarget, wc));

    }

    public CAstNode visit(While w, WalkContext wc) {
      Expr c = w.cond();
      Stmt b = w.body();

      Node breakTarget = makeBreakTarget(w);
      Node continueTarget = makeContinueTarget(w);
      String loopLabel = (String) wc.getLabelMap().get(w);
      LoopContext lc = new LoopContext(wc, loopLabel, breakTarget, continueTarget);

      /*
       * The following loop is created sligtly differently than in jscore. It
       * doesn't have a specific target for continue.
       */
      return makeNode(wc, fFactory, w, CAstNode.BLOCK_STMT, makeNode(wc, fFactory, w, CAstNode.LOOP, walkNodes(c, wc), makeNode(wc, fFactory, w,
          CAstNode.BLOCK_STMT, walkNodes(b, lc), walkNodes(continueTarget, wc))), walkNodes(breakTarget, wc));
    }

    public CAstNode visit(Switch s, WalkContext wc) {
      Node breakLabel = 
	fNodeFactory.Labeled(
	  Position.COMPILER_GENERATED, 
	  "switchBreakLabel" + s.position().toString().replace('.', '_'), 
	  fNodeFactory.Empty(Position.COMPILER_GENERATED));
      CAstNode breakAst = walkNodes(breakLabel, wc);
      String loopLabel = (String) wc.getLabelMap().get(s);
      WalkContext child = new SwitchContext(wc, loopLabel, breakLabel);
      Expr cond = s.expr();
      List cases = s.elements();

      // First compute the control flow edges for the various case labels
      for (int i = 0; i < cases.size(); i++) {
        SwitchElement se = (SwitchElement) cases.get(i);
        if (se instanceof Case) {
          Case c = (Case) se;

          if (c.isDefault())
            wc.cfg().add(s, c, CAstControlFlowMap.SWITCH_DEFAULT);
          else
            wc.cfg().add(s, c, fFactory.makeConstant(c.value()));
        }
      }
      CAstNode[] caseNodes = new CAstNode[cases.size()];

      // Now produce the CAst representation for each case
      int idx = 0;
      for (Iterator iter = cases.iterator(); iter.hasNext(); idx++) {
        SwitchElement se = (SwitchElement) iter.next();

        caseNodes[idx] = walkNodes(se, child);
      }

      // Now produce the switch stmt itself
      CAstNode switchAst = makeNode(wc, fFactory, s, CAstNode.SWITCH, walkNodes(cond, wc), makeNode(wc, fFactory, s, CAstNode.BLOCK_STMT, caseNodes));

      wc.cfg().map(s, switchAst);
      wc.cfg().map(breakLabel, breakAst);

      // Finally, wrap the entire switch in a block so that we have a
      // well-defined place to 'break' to.
      return makeNode(wc, fFactory, s, CAstNode.BLOCK_STMT, switchAst, breakAst);
    }

    public CAstNode visit(Synchronized s, WalkContext wc) {
      CAstNode exprNode = walkNodes(s.expr(), wc);
      String exprName = fFactory.makeUnique();
      CAstNode declStmt = makeNode(wc, fFactory, s, CAstNode.DECL_STMT, makeNode(wc, fFactory, s, CAstNode.VAR, fFactory.makeConstant(exprName)), fFactory
          .makeConstant(true), fFactory.makeConstant(false), exprNode);
      CAstNode monitorEnterNode = makeNode(wc, fFactory, s, CAstNode.MONITOR_ENTER, makeNode(wc, fFactory, s, CAstNode.VAR, fFactory.makeConstant(exprName)));
      CAstNode bodyNodes = walkNodes(s.body(), wc);
      CAstNode monitorExitNode = makeNode(wc, fFactory, s, CAstNode.MONITOR_EXIT, makeNode(wc, fFactory, s, CAstNode.VAR, fFactory.makeConstant(exprName)));
      CAstNode tryBody = makeNode(wc, fFactory, s, CAstNode.BLOCK_STMT, monitorEnterNode, bodyNodes);
      CAstNode bigBody = makeNode(wc, fFactory, s, CAstNode.UNWIND, tryBody, monitorExitNode);

      return makeNode(wc, fFactory, s, CAstNode.BLOCK_STMT, declStmt, bigBody);
    }

    public CAstNode visit(Try t, WalkContext wc) {
      List catchBlocks = t.catchBlocks();
      Block finallyBlock = t.finallyBlock();
      Block tryBlock = t.tryBlock();

      // try/finally
      if (catchBlocks.isEmpty()) {
        return makeNode(wc, fFactory, t, CAstNode.UNWIND, walkNodes(tryBlock, wc), walkNodes(finallyBlock, wc));

        // try/catch/[finally]
      } else {
        TryCatchContext tc = new TryCatchContext(wc, t);

        CAstNode tryNode = walkNodes(tryBlock, tc);
        for (Iterator iter = catchBlocks.iterator(); iter.hasNext();) {
          tryNode = makeNode(wc, fFactory, t, CAstNode.TRY, tryNode, walkNodes((Catch) iter.next(), wc));
        }

        // try/catch
        if (finallyBlock == null) {
          return tryNode;

          // try/catch/finally
        } else {
          return makeNode(wc, fFactory, t, CAstNode.UNWIND, tryNode, walkNodes(finallyBlock, wc));
        }
      }
    }

    public CAstNode visit(Empty e, WalkContext wc) {
      CAstNode result = makeNode(wc, fFactory, e, CAstNode.EMPTY);
      wc.cfg().map(e, result);
      return result;
    }

    public CAstNode visit(Eval e, WalkContext wc) {
      return walkNodes(e.expr(), wc);
    }

    public CAstNode visit(LocalDecl ld, WalkContext wc) {
      Expr init = ld.init();
      Type type = ld.declType();
      CAstNode initNode;

      if (init == null) {
        if (type.isLongOrLess())
          initNode = fFactory.makeConstant(0);
        else if (type.isDouble() || type.isFloat())
          initNode = fFactory.makeConstant(0.0);
        else
          initNode = fFactory.makeConstant(null);
      } else
        initNode = walkNodes(init, wc);

      boolean isFinal = ld.flags().isFinal();

      return makeNode(wc, fFactory, ld, CAstNode.DECL_STMT, makeNode(wc, fFactory, ld, CAstNode.VAR, fFactory.makeConstant(ld.name())), fFactory
          .makeConstant(isFinal), fFactory.makeConstant(false), initNode);
    }

    public CAstNode visit(Return r, WalkContext wc) {
      Expr retExpr = r.expr();
      if (retExpr == null)
        return makeNode(wc, fFactory, r, CAstNode.RETURN);
      else
        return makeNode(wc, fFactory, r, CAstNode.RETURN, walkNodes(retExpr, wc));
    }

    public CAstNode visit(Case c, WalkContext wc) {
      CAstNode label = makeNode(wc, fFactory, c, CAstNode.LABEL_STMT, fFactory.makeConstant(c.value()));

      wc.cfg().map(c, label);
      return label;
    }

    public CAstNode visit(Throw t, WalkContext wc) {
      CAstNode result = makeNode(wc, fFactory, t, CAstNode.THROW, walkNodes(t.expr(), wc));
      Type label = t.expr().type();

      wc.cfg().map(t, result);

      Collection/* <Pair<Type,Node>> */catchNodes = wc.getCatchTargets(label);

      for (Iterator iter = catchNodes.iterator(); iter.hasNext();) {
        Pair/* <Type,Node> */catchNode = (Pair/* <Type,Node> */) iter.next();

        wc.cfg().add(t, catchNode.snd, catchNode.fst);
      }

      return result;
    }

    public CAstNode visit(Formal f, WalkContext wc) {
      return makeNode(wc, fFactory, f, CAstNode.VAR, fFactory.makeConstant(f.name()));
    }
  }

  protected static final class CompilationUnitEntity implements CAstEntity {
    private final String fName;

    private final List/* <CAstEntity> */fTopLevelDecls;

    public CompilationUnitEntity(SourceFile file, List/* <CAstEntity> */topLevelDecls) {
      fName = (file.package_() == null) ? "" : file.package_().package_().fullName().replace('.', '/');
      fTopLevelDecls = topLevelDecls;
    }

    public int getKind() {
      return FILE_ENTITY;
    }

    public String getName() {
      return fName;
    }

    public String getSignature() {
      Assertions.UNREACHABLE();
      return null;
    }

    public String[] getArgumentNames() {
      return new String[0];
    }

    public CAstNode[] getArgumentDefaults() {
      return new CAstNode[0];
    }

    public int getArgumentCount() {
      return 0;
    }

    public Map getAllScopedEntities() {
      return Collections.singletonMap(null, fTopLevelDecls);
    }

    public Iterator getScopedEntities(CAstNode construct) {
      Assertions.UNREACHABLE("CompilationUnitEntity asked for AST-related entities, but it has no AST.");
      return null;
    }

    public CAstNode getAST() {
      return null;
    }

    public CAstControlFlowMap getControlFlow() {
      Assertions.UNREACHABLE("CompilationUnitEntity.getControlFlow()");
      return null;
    }

    public CAstSourcePositionMap getSourceMap() {
      Assertions.UNREACHABLE("CompilationUnitEntity.getSourceMap()");
      return null;
    }

    public CAstSourcePositionMap.Position getPosition() {
      Assertions.UNREACHABLE("CompilationUnitEntity.getPosition()");
      return null;
    }

    public CAstNodeTypeMap getNodeTypeMap() {
      Assertions.UNREACHABLE("CompilationUnitEntity.getNodeTypeMap()");
      return null;
    }

    public Collection getQualifiers() {
      return Collections.EMPTY_LIST;
    }

    public CAstType getType() {
      Assertions.UNREACHABLE("CompilationUnitEntity.getType()");
      return null;
    }
  }

  public final class PolyglotJavaType implements JavaType {
    private final CAstTypeDictionary fDict;

    private final TypeSystem fSystem;

    private final ClassType fType;

    private Collection<CAstType> fSuperTypes = null;

    public PolyglotJavaType(ClassType type, CAstTypeDictionary dict, TypeSystem system) {
      super();
      fDict = dict;
      fSystem = system;
      fType = type;
    }

    public String getName() {
      // TODO Will the IdentityMapper do the right thing for anonymous
      // classes?
      // If so, we can delete most of the following logic...
      if (fType.isLocal() || fType.isAnonymous()) {
        return fIdentityMapper.anonLocalTypeToTypeID(fType);
      } else
        return fIdentityMapper.getTypeRef(fType).getName().toString();
    }

    public Collection getSupertypes() {
      if (fSuperTypes == null) {
        buildSuperTypes();
      }
      return fSuperTypes;
    }

    private void buildSuperTypes() {
      // TODO this is a source entity, but it might actually be the root type
      // (Object), so assume # intfs + 1
      Type superType;
      try {
        superType = (fType.superType() == null) ? fSystem.typeForName("java.lang.Object") : fType.superType();
      } catch (SemanticException e) {
        Assertions.UNREACHABLE("Can't find java.lang.Object???");
        return;
      }
      int N = fType.interfaces().size() + 1;

      fSuperTypes = new ArrayList<CAstType>(N);
      // Following assumes that noone can call getSupertypes() before we have
      // created CAstType's for every type in the program being analyzed.
      fSuperTypes.add(fDict.getCAstTypeFor(superType));
      for (Iterator iter = fType.interfaces().iterator(); iter.hasNext();) {
        Type t = (Type) iter.next();
        fSuperTypes.add(fDict.getCAstTypeFor(t));
      }
    }

    public boolean isInterface() {
      return fType.flags().isInterface();
    }
  }

  protected abstract static class CodeBodyEntity implements CAstEntity {
    private final Map<CAstNode,Set<CAstEntity>> fEntities;

    public CodeBodyEntity(Map<CAstNode, CAstEntity> entities) {
      fEntities = new LinkedHashMap<CAstNode, Set<CAstEntity>>();
      for (Iterator keys = entities.keySet().iterator(); keys.hasNext();) {
        CAstNode key = (CAstNode) keys.next();
        fEntities.put(key, Collections.singleton(entities.get(key)));
      }
    }

    public Map getAllScopedEntities() {
      return Collections.unmodifiableMap(fEntities);
    }

    public Iterator getScopedEntities(CAstNode construct) {
      if (fEntities.containsKey(construct)) {
        return ((Set) fEntities.get(construct)).iterator();
      } else {
        return EmptyIterator.instance();
      }
    }

    public String getSignature() {
      return Util.methodEntityToSelector(this).toString();
    }
  }

  protected final class ClassEntity implements CAstEntity {
    @SuppressWarnings("unused")
    private final ClassContext fContext;

    private final ClassType fCT;

    private final String fName;

    private final List/* <CAstEntity> */fEntities;

    private final CAstSourcePositionMap.Position sourcePosition;

    private ClassEntity(ClassContext context, List/* <CAstEntity> */entities, ClassDecl cd, Position p) {
      this(context, entities, cd.type(), cd.name(), p);
    }

    private ClassEntity(ClassContext context, List/* <CAstEntity> */entities, ClassType ct, String name, Position p) {
      fContext = context;
      this.fEntities = entities;
      fCT = ct;
      fName = name;
      sourcePosition = makePosition(p);
    }

    public int getKind() {
      return TYPE_ENTITY;
    }

    public String getName() {
      return fName; // unqualified?
    }

    public String getSignature() {
      return "L" + fName.replace('.', '/') + ";";
    }

    public String[] getArgumentNames() {
      return new String[0];
    }

    public CAstNode[] getArgumentDefaults() {
      return new CAstNode[0];
    }

    public int getArgumentCount() {
      return 0;
    }

    public CAstNode getAST() {
      // This entity has no AST nodes, really.
      return null;
    }

    public Map getAllScopedEntities() {
      return Collections.singletonMap(null, fEntities);
    }

    public Iterator getScopedEntities(CAstNode construct) {
      Assertions.UNREACHABLE("Non-AST-bearing entity (ClassEntity) asked for scoped entities related to a given AST node");
      return null;
    }

    public CAstControlFlowMap getControlFlow() {
      // This entity has no AST nodes, really.
      return null;
    }

    public CAstSourcePositionMap getSourceMap() {
      // This entity has no AST nodes, really.
      return null;
    }

    public CAstSourcePositionMap.Position getPosition() {
      return sourcePosition;
    }

    public CAstNodeTypeMap getNodeTypeMap() {
      // This entity has no AST nodes, really.
      return new CAstNodeTypeMap() {
        public CAstType getNodeType(CAstNode node) {
          throw new UnsupportedOperationException();
        }
      };
    }

    public Collection getQualifiers() {
      return mapFlagsToQualifiers(fCT.flags());
    }

    public CAstType getType() {
      return new PolyglotJavaType(fCT, getTypeDict(), fTypeSystem);
    }
  }

  protected final class ProcedureEntity extends CodeBodyEntity implements JavaProcedureEntity {
    private final CAstNode fPdast;

    private final TypeSystem fSystem;

    private final Type declaringType;

    private final ProcedureInstance fPd;

    private final MethodContext fMc;

    private final String[] argumentNames;

    private ProcedureEntity(CAstNode pdast, TypeSystem system, ProcedureInstance pd, Type declaringType, String[] argumentNames,
        Map<CAstNode,CAstEntity> entities, MethodContext mc) {
      super(entities);
      fPdast = pdast;
      fSystem = system;
      fPd = pd;
      this.declaringType = declaringType;
      this.argumentNames = argumentNames;
      fMc = mc;
    }

    private ProcedureEntity(CAstNode pdast, TypeSystem system, ProcedureInstance pd, String[] argumentNames, Map<CAstNode,CAstEntity> entities,
        MethodContext mc) {
      this(pdast, system, pd, ((MemberInstance)pd).container(), argumentNames, entities, mc);
    }

    public String toString() {
      return fPd.toString();
    }

    public int getKind() {
      return CAstEntity.FUNCTION_ENTITY;
    }

    public String getName() {
      if (fPd instanceof ConstructorInstance) {
        return MethodReference.initAtom.toString();
      } else {
        Assertions._assert(fPd instanceof MethodInstance);
        return ((MethodInstance) fPd).name();
      }
    }

    public String[] getArgumentNames() {
      return argumentNames;
    }

    public CAstNode[] getArgumentDefaults() {
      return new CAstNode[0];
    }

    public int getArgumentCount() {
      return fPd.flags().isStatic() ? fPd.formalTypes().size() : fPd.formalTypes().size() + 1;
    }

    public CAstNode getAST() {
      return fPdast;
    }

    public CAstControlFlowMap getControlFlow() {
      return fMc.cfg();
    }

    public CAstSourcePositionMap getSourceMap() {
      return fMc.pos();
    }

    public CAstSourcePositionMap.Position getPosition() {
      return makePosition(fPd.position());
    }

    public CAstNodeTypeMap getNodeTypeMap() {
      return fMc.getNodeTypeMap();
    }

    public Collection getQualifiers() {
      return mapFlagsToQualifiers(fPd.flags());
    }

    public CAstType getType() {
      return new CAstType.Method() {
        private Collection<CAstType> fExceptionTypes = null;

        private List<CAstType> fParameterTypes = null;

        public CAstType getReturnType() {
          return fMc.getTypeDictionary().getCAstTypeFor((fPd instanceof MethodInstance) ? ((MethodInstance) fPd).returnType() : fSystem.Void());
        }

        public List getArgumentTypes() {
          if (fParameterTypes == null) {
            final List formalTypes = fPd.formalTypes();
            fParameterTypes = new ArrayList<CAstType>(formalTypes.size());

            for (Iterator iter = formalTypes.iterator(); iter.hasNext();) {
              fParameterTypes.add(fMc.getTypeDictionary().getCAstTypeFor((Type) iter.next()));
            }
          }
          return fParameterTypes;
        }

        public String getName() {
          Assertions.UNREACHABLE("CAstType.FunctionImpl#getName() called???");
          return "?";
        }

        public Collection getSupertypes() {
          Assertions.UNREACHABLE("CAstType.FunctionImpl#getSupertypes() called???");
          return null;
        }

        public Collection/* <CAstType> */getExceptionTypes() {
          if (fExceptionTypes == null) {
            fExceptionTypes = new LinkedHashSet<CAstType>();

            List exceptions = fPd.throwTypes();

            if (exceptions != null) {
              for (Iterator iterator = exceptions.iterator(); iterator.hasNext();) {
                Type type = (Type) iterator.next();
                fExceptionTypes.add(fMc.getTypeDictionary().getCAstTypeFor(type));
              }
            }
          }
          return fExceptionTypes;
        }

        public int getArgumentCount() {
          return fPd.formalTypes().size();
        }

        public CAstType getDeclaringType() {
          return getTypeDict().getCAstTypeFor(declaringType);
        }
      };
    }
  }

  protected final class FieldEntity implements CAstEntity {
    private final FieldInstance fFI;

    private final WalkContext fContext;

    private FieldEntity(FieldDecl fd, WalkContext context) {
      super();
      fFI = fd.fieldInstance();
      fContext = context;
    }

    public int getKind() {
      return CAstEntity.FIELD_ENTITY;
    }

    public String getName() {
      return fFI.name();
    }

    public String getSignature() {
      return fFI.name() + fIdentityMapper.typeToTypeID(fFI.type());
    }

    public String[] getArgumentNames() {
      return new String[0];
    }

    public CAstNode[] getArgumentDefaults() {
      return new CAstNode[0];
    }

    public int getArgumentCount() {
      return 0;
    }

    public Iterator getScopedEntities(CAstNode construct) {
      return EmptyIterator.instance();
    }

    public Map getAllScopedEntities() {
      return Collections.EMPTY_MAP;
    }

    public CAstNode getAST() {
      // No AST for a field decl; initializers folded into
      // constructor processing...
      return null;
    }

    public CAstControlFlowMap getControlFlow() {
      // No AST for a field decl; initializers folded into
      // constructor processing...
      return null;
    }

    public CAstSourcePositionMap getSourceMap() {
      // No AST for a field decl; initializers folded into
      // constructor processing...
      return null;
    }

    public CAstSourcePositionMap.Position getPosition() {
      return makePosition(fFI.position());
    }

    public CAstNodeTypeMap getNodeTypeMap() {
      // No AST for a field decl; initializers folded into
      // constructor processing...
      return null;
    }

    public Collection getQualifiers() {
      return mapFlagsToQualifiers(fFI.flags());
    }

    public CAstType getType() {
      return fContext.getTypeDictionary().getCAstTypeFor(fFI.type());
    }
  }

  public interface WalkContext {
    void addScopedEntity(CAstNode node, CAstEntity e);

    // Map/*<CAstNode,CAstEntity>*/ getScopedEntities();

    CAstControlFlowRecorder cfg();

    CAstSourcePositionRecorder pos();

    CAstNodeTypeMapRecorder getNodeTypeMap();

    Collection<Pair<Type,Object>> getCatchTargets(Type label);

    Node getContinueFor(String label);

    Node getBreakFor(String label);

    Node getFinally();

    ProcedureInstance getEnclosingMethod();

    Type getEnclosingType();

    CAstTypeDictionary getTypeDictionary();

    List<ClassMember> getStaticInitializers();

    List<ClassMember> getInitializers();

    Map<Node,String> getLabelMap();
  }

  protected static class DelegatingContext implements WalkContext {
    private final WalkContext parent;

    public WalkContext getParent() {
      return parent;
    }

    protected DelegatingContext(WalkContext parent) {
      this.parent = parent;
    }

    public void addScopedEntity(CAstNode node, CAstEntity e) {
      parent.addScopedEntity(node, e);
    }

    // public Map/*<CAstNode,CAstEntity>*/ getScopedEntities() {
    // return parent.getScopedEntities();
    // }

    public CAstControlFlowRecorder cfg() {
      return parent.cfg();
    }

    public CAstSourcePositionRecorder pos() {
      return parent.pos();
    }

    public CAstNodeTypeMapRecorder getNodeTypeMap() {
      return parent.getNodeTypeMap();
    }

    public Collection<Pair<Type,Object>> getCatchTargets(Type label) {
      return parent.getCatchTargets(label);
    }

    public Node getContinueFor(String label) {
      return parent.getContinueFor(label);
    }

    public Node getBreakFor(String label) {
      return parent.getBreakFor(label);
    }

    public Node getFinally() {
      return parent.getFinally();
    }

    public ProcedureInstance getEnclosingMethod() {
      return parent.getEnclosingMethod();
    }

    public Type getEnclosingType() {
      return parent.getEnclosingType();
    }

    public CAstTypeDictionary getTypeDictionary() {
      return parent.getTypeDictionary();
    }

    public List<ClassMember> getStaticInitializers() {
      return parent.getStaticInitializers();
    }

    public List<ClassMember> getInitializers() {
      return parent.getInitializers();
    }

    public Map<Node,String> getLabelMap() {
      return parent.getLabelMap();
    }

  }

  public class ClassContext extends DelegatingContext {
    // private final ClassDecl cd;
    private final Type type;

    private List<ClassMember>/* <Initializer+FieldDecl> */fInitializers = new ArrayList<ClassMember>();

    private List<ClassMember>/* <Initializer+FieldDecl> */fStaticInitializers = new ArrayList<ClassMember>();

    private List<CAstEntity> fChildren;

    public ClassContext(Type type, List<CAstEntity> entities, WalkContext parent) {
      super(parent);
      this.type = type;
      fChildren = entities;
    }

    public void addScopedEntity(CAstNode node, CAstEntity e) {
      Assertions._assert(node == null);
      fChildren.add(e);
    }

    // public Map/*<CAstNode,CAstEntity>*/ getScopedEntities() {
    // return null; // fChildren;
    // }

    public Type getEnclosingType() {
      return type;
    }

    public List<ClassMember> getInitializers() {
      return fInitializers;
    }

    public List<ClassMember> getStaticInitializers() {
      return fStaticInitializers;
    }

    public CAstControlFlowRecorder cfg() {
      Assertions.UNREACHABLE("ClassContext.cfg()");
      return null;
    }

    public Iterator/* <Pair<Type,Node>> */getCatchTarget(Type label) {
      Assertions.UNREACHABLE("ClassContext.getCatchTarget()");
      return null;
    }

    public Node getFinally() {
      Assertions.UNREACHABLE("ClassContext.getFinally()");
      return null;
    }

    public ProcedureInstance getEnclosingMethod() {
      // No one outside a method defining a local class can see it,
      // so it clearly can't escape through to the method's enclosing
      // type...
      Assertions.UNREACHABLE("ClassContext.getEnclosingMethod()");
      return null;
    }

    public CAstSourcePositionRecorder pos() {
      // No AST, so no AST map
      Assertions.UNREACHABLE("ClassContext.pos()");
      return null;
    }

    public Node getContinueFor(String label) {
      Assertions.UNREACHABLE("ClassContext.getContinueFor() with label " + label + " in " + type);
      return null;
    }

    public Node getBreakFor(String label) {
      System.err.println("Cannot find break target for " + label + " in " + type);
      Assertions.UNREACHABLE("ClassContext.getBreakFor()");
      return null;
    }

    public Map<Node,String> getLabelMap() {
      Assertions.UNREACHABLE("ClassContext.getLabelMap()");
      return null;
    }
  }

  public class CodeBodyContext extends DelegatingContext {
    final CAstSourcePositionRecorder fSourceMap = new CAstSourcePositionRecorder();

    final CAstControlFlowRecorder fCFG = new CAstControlFlowRecorder();

    final CAstNodeTypeMapRecorder fNodeTypeMap = new CAstNodeTypeMapRecorder();

    private final Map<Node,String> labelMap = new HashMap<Node,String>(2);

    private final Map<CAstNode,CAstEntity> fEntities;

    public CodeBodyContext(WalkContext parent, Map<CAstNode,CAstEntity> entities) {
      super(parent);
      fEntities = entities;
    }

    public CAstNodeTypeMapRecorder getNodeTypeMap() {
      return fNodeTypeMap;
    }

    public CAstSourcePositionRecorder pos() {
      return fSourceMap;
    }

    public CAstControlFlowRecorder cfg() {
      return fCFG;
    }

    public void addScopedEntity(CAstNode node, CAstEntity entity) {
      fEntities.put(node, entity);
    }

    public Map/* <CAstNode,CAstEntity> */getScopedEntities() {
      return fEntities;
    }

    public Map<Node,String> getLabelMap() {
      return labelMap;
    }
  }

  public class MethodContext extends CodeBodyContext {
    final ProcedureInstance fPI;

    public MethodContext(ProcedureInstance pi, Map<CAstNode,CAstEntity> entities, WalkContext parent) {
      super(parent, entities);
      fPI = pi;
    }

    public Collection<Pair<Type,Object>> getCatchTargets(Type label) {
      return Collections.singleton(new Pair<Type,Object>(fREType, CAstControlFlowMap.EXCEPTION_TO_EXIT));
    }

    public ProcedureInstance getEnclosingMethod() {
      return fPI;
    }
  }

  private static class TryCatchContext extends DelegatingContext {
    @SuppressWarnings("unused")
    private final Try tryNode;

    Collection<Pair<Type,Object>> fCatchNodes = new ArrayList<Pair<Type,Object>>();

    TryCatchContext(WalkContext parent, Try tryNode) {
      super(parent);
      this.tryNode = tryNode;

      for (Iterator catchIter = tryNode.catchBlocks().iterator(); catchIter.hasNext();) {
        Catch c = (Catch) catchIter.next();
        Pair<Type,Object> p = new Pair<Type,Object>(c.catchType(), c);

        fCatchNodes.add(p);
      }
    }

    public Collection<Pair<Type,Object>> getCatchTargets(Type label) {
      // Look for all matching targets for this thrown type:
      // if supertpe match, then return only matches at this catch
      // if subtype match, then matches here and parent matches
      Collection<Pair<Type,Object>> catchNodes = new ArrayList<Pair<Type,Object>>();

      for (Iterator<Pair<Type,Object>> iter = fCatchNodes.iterator(); iter.hasNext();) {
        Pair<Type,Object> p = (Pair<Type,Object>) iter.next();
        Type catchType = (Type) p.fst;

        // _must_ be caught
        if (label.descendsFrom(catchType) || label.equals(catchType)) {
          catchNodes.add(p);
          return catchNodes;

          // _might_ get caught
        } else if (catchType.descendsFrom(label)) {
          catchNodes.add(p);
          continue;
        }
      }
      catchNodes.addAll(getParent().getCatchTargets(label));
      return catchNodes;
    }

    public List<ClassMember> getStaticInitializers() {
      return null;
    }

    public List<ClassMember> getInitializers() {
      return null;
    }
  }

  protected static class RootContext implements WalkContext {
    final CAstTypeDictionary fTypeDict;

    public RootContext(CAstTypeDictionary typeDict) {
      fTypeDict = typeDict;
    }

    public void addScopedEntity(CAstNode node, CAstEntity e) {
      Assertions.UNREACHABLE("Attempt to call addScopedEntity() on a RootContext.");
    }

    public CAstControlFlowRecorder cfg() {
      Assertions.UNREACHABLE("RootContext.cfg()");
      return null;
    }

    public Collection<Pair<Type,Object>> getCatchTargets(Type label) {
      Assertions.UNREACHABLE("RootContext.getCatchTargets()");
      return null;
    }

    public CAstNodeTypeMapRecorder getNodeTypeMap() {
      Assertions.UNREACHABLE("RootContext.getNodeTypeMap()");
      return null;
    }

    public Node getFinally() {
      Assertions.UNREACHABLE("RootContext.getFinally()");
      return null;
    }

    public CAstSourcePositionRecorder pos() {
      // No AST, so no AST map
      Assertions.UNREACHABLE("RootContext.pos()");
      return null;
    }

    public Node getContinueFor(String label) {
      Assertions.UNREACHABLE("RootContext.getContinueFor()");
      return null;
    }

    public Node getBreakFor(String label) {
      Assertions.UNREACHABLE("RootContext.getBreakFor()");
      return null;
    }

    public ProcedureInstance getEnclosingMethod() {
      Assertions.UNREACHABLE("RootContext.getEnclosingMethod()");
      return null;
    }

    public Type getEnclosingType() {
      Assertions.UNREACHABLE("RootContext.getEnclosingType()");
      return null;
    }

    public CAstTypeDictionary getTypeDictionary() {
      return fTypeDict;
    }

    public List<ClassMember> getStaticInitializers() {
      Assertions.UNREACHABLE("RootContext.getStaticInitializers()");
      return null;
    }

    public List<ClassMember> getInitializers() {
      Assertions.UNREACHABLE("RootContext.getInitializers()");
      return null;
    }

    public Map<Node,String> getLabelMap() {
      Assertions.UNREACHABLE("RootContext.getLabelMap()");
      return null;
    }
  }

  private class SwitchContext extends DelegatingContext {
    protected final String label;

    private final Node breakTo;

    SwitchContext(WalkContext parent, String label, Node breakTo) {
      super(parent);
      this.label = label;
      this.breakTo = breakTo;
    }

    public Node getBreakFor(String label) {
      return (label == null || label.equals(this.label)) ? breakTo : super.getBreakFor(label);
    }

    public List<ClassMember> getStaticInitializers() {
      return null;
    }

    public List<ClassMember> getInitializers() {
      return null;
    }
  }

  private class LoopContext extends SwitchContext {
    private final Node continueTo;

    protected LoopContext(WalkContext parent, String label, Node breakTo, Node continueTo) {
      super(parent, label, breakTo);
      this.continueTo = continueTo;
    }

    public Node getContinueFor(String label) {
      return (label == null || label.equals(this.label)) ? continueTo : super.getContinueFor(label);
    }
  }

  public PolyglotJava2CAstTranslator(ClassLoaderReference clr, NodeFactory nf, TypeSystem ts, PolyglotIdentityMapper identityMapper) {
    fClassLoaderRef = clr;
    fTypeSystem = ts;
    fNodeFactory = nf;
    fIdentityMapper = identityMapper;
    try {
      fNPEType = fTypeSystem.typeForName("java.lang.NullPointerException");
      fREType = fTypeSystem.typeForName("java.lang.RuntimeException");
    } catch (SemanticException e) {
      Assertions.UNREACHABLE("Couldn't find Polyglot type for NPE/RE!");
    }
  }

  private static class PolyglotSourcePosition extends AbstractSourcePosition {
    private final Position p;

    PolyglotSourcePosition(Position p) {
      this.p = p;
    }

    public int getFirstLine() {
      return p.line();
    }

    public int getLastLine() {
      return p.endLine();
    }

    public int getFirstCol() {
      return p.column();
    }

    public int getLastCol() {
      return p.endColumn();
    }

    public URL getURL() {
      try {
        String path= p.path();
        return new URL("file:" + (path.length() == 0 ? p.file() : path));
      } catch (MalformedURLException e) {
        Assertions.UNREACHABLE(e.toString());
        return null;
      }
    }

    public InputStream getInputStream() throws IOException {
      return getURL().openConnection().getInputStream();
    }
  }

  protected CAstSourcePositionMap.Position makePosition(Position p) {
    return new PolyglotSourcePosition(p);
  }

  private void setPos(WalkContext wc, CAstNode cn, Node pn) {
    if (pn != null) {
      wc.pos().setPosition(cn, makePosition(pn.position()));
    }
  }

  private void setPos(WalkContext wc, CAstNode cn, Position p) {
    if (p != null) {
      wc.pos().setPosition(cn, makePosition(p));
    }
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, Node n, int kind) {
    CAstNode cn = Ast.makeNode(kind);
    setPos(wc, cn, n);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, Node n, int kind, CAstNode c[]) {
    CAstNode cn = Ast.makeNode(kind, c);
    setPos(wc, cn, n);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, Node n, int kind, CAstNode c) {
    CAstNode cn = Ast.makeNode(kind, c);
    setPos(wc, cn, n);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, Node n, int kind, CAstNode c1, CAstNode c2) {
    CAstNode cn = Ast.makeNode(kind, c1, c2);
    setPos(wc, cn, n);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, Node n, int kind, CAstNode c1, CAstNode c2, CAstNode c3) {
    CAstNode cn = Ast.makeNode(kind, c1, c2, c3);
    setPos(wc, cn, n);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, Node n, int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4) {
    CAstNode cn = Ast.makeNode(kind, c1, c2, c3, c4);
    setPos(wc, cn, n);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, Node n, int kind) {
    return makeNode(wc, fFactory, n, kind);
  }

  protected CAstNode makeNode(WalkContext wc, Node n, int kind, CAstNode c[]) {
    return makeNode(wc, fFactory, n, kind, c);
  }

  protected CAstNode makeNode(WalkContext wc, Node n, int kind, CAstNode c) {
    return makeNode(wc, fFactory, n, kind, c);
  }

  protected CAstNode makeNode(WalkContext wc, Node n, int kind, CAstNode c1, CAstNode c2) {
    return makeNode(wc, fFactory, n, kind, c1, c2);
  }

  protected CAstNode makeNode(WalkContext wc, Node n, int kind, CAstNode c1, CAstNode c2, CAstNode c3) {
    return makeNode(wc, fFactory, n, kind, c1, c2, c3);
  }

  protected CAstNode makeNode(WalkContext wc, Node n, int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4) {
    return makeNode(wc, fFactory, n, kind, c1, c2, c3, c4);
  }

  protected CAstNode makeNode(WalkContext wc, int kind, Position p) {
    CAstNode cn = fFactory.makeNode(kind);
    setPos(wc, cn, p);
    return cn;
  }

  public CAstEntity translate(Object ast, String fileName) {
    return walkEntity((Node) ast, new RootContext(getTypeDict()));
  }

  public interface IdentityMapper<T, M, F> {
    MethodReference getMethodRef(M method);

    TypeReference getTypeRef(T type);

    FieldReference getFieldRef(F field);
  }

  protected static Collection mapFlagsToQualifiers(Flags flags) {
    Set<CAstQualifier> quals = new LinkedHashSet<CAstQualifier>();

    if (flags.isAbstract())
      quals.add(CAstQualifier.ABSTRACT);
    if (flags.isFinal())
      quals.add(CAstQualifier.FINAL);
    if (flags.isInterface())
      quals.add(CAstQualifier.INTERFACE);
    if (flags.isNative())
      quals.add(CAstQualifier.NATIVE);
    // if (flags.isPackage()) quals.add(CAstQualifier.PACKAGE);
    if (flags.isPrivate())
      quals.add(CAstQualifier.PRIVATE);
    if (flags.isProtected())
      quals.add(CAstQualifier.PROTECTED);
    if (flags.isPublic())
      quals.add(CAstQualifier.PUBLIC);
    if (flags.isStatic())
      quals.add(CAstQualifier.STATIC);
    if (flags.isStrictFP())
      quals.add(CAstQualifier.STRICTFP);
    if (flags.isSynchronized())
      quals.add(CAstQualifier.SYNCHRONIZED);
    if (flags.isTransient())
      quals.add(CAstQualifier.TRANSIENT);
    if (flags.isVolatile())
      quals.add(CAstQualifier.VOLATILE);

    return quals;
  }

  protected void processClassMembers(List<ClassMember> members, DelegatingContext classContext, List<CAstEntity> memberEntities) {
    // Collect all initializer-related gorp
    for (Iterator memberIter = members.iterator(); memberIter.hasNext();) {
      ClassMember member = (ClassMember) memberIter.next();

      if (member instanceof Initializer) {
        Initializer initializer = (Initializer) member;

        if (initializer.flags().isStatic())
          classContext.getStaticInitializers().add(initializer);
        else
          classContext.getInitializers().add(initializer);
      } else if (member instanceof FieldDecl) {
        FieldDecl fd = (FieldDecl) member;

        if (fd.init() != null) {
          if (fd.flags().isStatic())
            classContext.getStaticInitializers().add(fd);
          else
            classContext.getInitializers().add(fd);
        }
      }
    }

    // Now process
    for (Iterator memberIter = members.iterator(); memberIter.hasNext();) {
      ClassMember member = (ClassMember) memberIter.next();

      if (!(member instanceof Initializer)) {
        CAstEntity memberEntity = walkEntity(member, classContext);

        memberEntities.add(memberEntity);
      }
    }
  }

  protected void addConstructorsToAnonymousClass(New n, ParsedClassType anonType, ClassContext classContext, List<CAstEntity> memberEntities) {
    List superConstructors = ((ClassType) anonType.superType()).constructors();

    for (Iterator iter = superConstructors.iterator(); iter.hasNext();) {
      ConstructorInstance superCtor = (ConstructorInstance) iter.next();

      Map<CAstNode,CAstEntity> childEntities = new HashMap<CAstNode,CAstEntity>();
      final MethodContext mc = new MethodContext(superCtor, childEntities, classContext);

      String[] fakeArguments = new String[superCtor.formalTypes().size() + 1];
      for (int i = 0; i < fakeArguments.length; i++) {
        fakeArguments[i] = (i == 0) ? "this" : ("argument" + i);
      }

      List inits = classContext.getInitializers();

      CAstNode[] bodyNodes = new CAstNode[inits.size() + 1];

      CallSiteReference callSiteRef = CallSiteReference.make(0, fIdentityMapper.getMethodRef(superCtor), IInvokeInstruction.Dispatch.SPECIAL);
      CAstNode[] children = new CAstNode[fakeArguments.length + 1];
      children[0] = makeNode(mc, fFactory, n, CAstNode.SUPER);
      children[1] = fFactory.makeConstant(callSiteRef);
      for (int i = 1; i < fakeArguments.length; i++) {
        children[i + 1] = makeNode(mc, fFactory, n, CAstNode.VAR, fFactory.makeConstant(fakeArguments[i]));
      }
      bodyNodes[0] = makeNode(mc, fFactory, n, CAstNode.CALL, children);

      insertInitializers(mc, bodyNodes, 1);

      CAstNode ast = makeNode(mc, fFactory, n, CAstNode.BLOCK_STMT, bodyNodes);

      memberEntities.add(new ProcedureEntity(ast, fTypeSystem, superCtor, anonType, fakeArguments, childEntities, mc));
    }
  }

  static String anonTypeName(ClassType ct) {
    Position pos = ct.position();

    return ct.fullName() + "$" + pos.line() + "$" + pos.column();
  }

  @SuppressWarnings("unchecked")
  protected CAstEntity walkEntity(Node rootNode, final WalkContext context) {
    if (rootNode instanceof SourceFile) {
      SourceFile file = (SourceFile) rootNode;
      List<CAstEntity> declEntities = new ArrayList<CAstEntity>();

      for (Iterator iter = file.decls().iterator(); iter.hasNext();) {
        TopLevelDecl decl = (TopLevelDecl) iter.next();

        declEntities.add(walkEntity(decl, context));
      }
      return new CompilationUnitEntity(file, declEntities);
    } else if (rootNode instanceof ClassDecl) {
      final ClassDecl cd = (ClassDecl) rootNode;
      final List<CAstEntity> memberEntities = new ArrayList<CAstEntity>();
      final ClassContext classContext = new ClassContext(cd.type(), memberEntities, context);

      processClassMembers(cd.body().members(), classContext, memberEntities);

      return new ClassEntity(classContext, memberEntities, cd, cd.position());
    } else if (rootNode instanceof New) {
      final New n = (New) rootNode;
      final List<CAstEntity> memberEntities = new ArrayList<CAstEntity>();
      ParsedClassType anonType = n.anonType();
      String anonTypeName = anonTypeName(anonType);
      final ClassContext classContext = new ClassContext(anonType, memberEntities, context);

      processClassMembers(n.body().members(), classContext, memberEntities);
      addConstructorsToAnonymousClass(n, anonType, classContext, memberEntities);

      return new ClassEntity(classContext, memberEntities, anonType, anonTypeName, n.position());
    } else if (rootNode instanceof ProcedureDecl) {
      final ProcedureDecl pd = (ProcedureDecl) rootNode;
      final Map<CAstNode,CAstEntity> memberEntities = new LinkedHashMap<CAstNode,CAstEntity>();
      final MethodContext mc = new MethodContext(pd.procedureInstance(), memberEntities, context);

      CAstNode pdAST = null;

      if (!pd.flags().isAbstract()) {
        // Presumably the MethodContext's parent is a ClassContext,
        // and he has the list of initializers. Hopefully the following
        // will glue that stuff in the right place in any constructor body.
        pdAST = walkNodes(pd, mc);
      }

      List/* <Formal> */formals = pd.formals();
      String[] argNames;
      int i = 0;
      if (!pd.flags().isStatic()) {
        argNames = new String[formals.size() + 1];
        argNames[i++] = "this";
      } else {
        argNames = new String[formals.size()];
      }
      for (Iterator iter = formals.iterator(); iter.hasNext(); i++) {
        Formal formal = (Formal) iter.next();
        argNames[i] = formal.name();
      }

      return new ProcedureEntity(pdAST, fTypeSystem, pd.procedureInstance(), argNames, memberEntities, mc);
    } else if (rootNode instanceof FieldDecl) {
      final FieldDecl fd = (FieldDecl) rootNode;

      return new FieldEntity(fd, context);
    } else {
      Assertions.UNREACHABLE("Unknown node type for walkEntity():" + rootNode.getClass().getName());
      return null;
    }
  }

  private boolean isSpecialCallStmt(Stmt maybeSuper, Kind kind) {
    if (maybeSuper instanceof ConstructorCall) {
      ConstructorCall cc = (ConstructorCall) maybeSuper;
      return cc.kind() == kind;
    }
    return false;
  }

  private boolean hasSpecialCall(Block body, Kind kind) {
    if (body.statements().size() <= 0)
      return false;

    Stmt maybeSuper = (Stmt) body.statements().get(0);

    return isSpecialCallStmt(maybeSuper, kind);
  }

  private boolean hasSuperCall(Block body) {
    return hasSpecialCall(body, ConstructorCall.SUPER);
  }

  private boolean hasThisCall(Block body) {
    return hasSpecialCall(body, ConstructorCall.THIS);
  }

  private int insertInitializers(WalkContext wc, CAstNode[] initCode, int offset) {
    List/* <FieldDecl|Initializer> */inits = wc.getInitializers();

    for (Iterator iter = inits.iterator(); iter.hasNext(); offset++) {
      ClassMember init = (ClassMember) iter.next();
      CAstNode initNode = walkNodes(init, wc);

      initCode[offset] = initNode;
    }
    return offset;
  }

  protected CAstNode walkNodes(Node n, final WalkContext context) {
    if (n == null)
      return makeNode(context, fFactory, null, CAstNode.EMPTY);
    return ASTTraverser.visit(n, getTranslator(), context);
  }
}
