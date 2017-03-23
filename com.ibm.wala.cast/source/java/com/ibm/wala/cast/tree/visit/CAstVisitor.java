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
package com.ibm.wala.cast.tree.visit;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author Igor Peshansky
 * Ripped out of Julian's AstTranslator
 * TODO: document me.
 */
public abstract class CAstVisitor<C extends CAstVisitor.Context> {
  
  public static boolean DEBUG = true;

  protected Position currentPosition;
  
  public Position getCurrentPosition() {
    return currentPosition;
  }
  
  protected CAstVisitor() {
    
  }
  
  /**
   * This interface represents a visitor-specific context.  All
   * it knows is how to get its top-level entity.  It is expected
   * that visitors will have classes implementing this interface
   * to collect visitor-specific information.
   *
   * @author Igor Peshansky
   */
  public interface Context {
    
    CAstEntity top();
    
    CAstSourcePositionMap getSourceMap();
  }

  /**
   * Construct a context for a File entity.
   * @param context a visitor-specific context in which this file was visited
   * @param n the file entity
   */
  protected C makeFileContext(C context, CAstEntity n) { return context; }
  /**
   * Construct a context for a Type entity.
   * @param context a visitor-specific context in which this type was visited
   * @param n the type entity
   */
  protected C makeTypeContext(C context, CAstEntity n) { return context; }
  /**
   * Construct a context for a Code entity.
   * @param context a visitor-specific context in which the code was visited
   * @param n the code entity
   */
  protected C makeCodeContext(C context, CAstEntity n) { return context; }

  /**
   * Construct a context for a LocalScope node.
   * @param context a visitor-specific context in which the local scope was visited
   * @param n the local scope node
   */
  protected C makeLocalContext(C context, CAstNode n) { return context; }
  /**
   * Construct a context for an Unwind node.
   * @param context a visitor-specific context in which the unwind was visited
   * @param n the unwind node
   */
  protected C makeUnwindContext(C context, CAstNode n, CAstVisitor<C> visitor) { return context; }

  private final Map<CAstEntity, CAstEntity> entityParents = HashMapFactory.make();

  /**
   * Get the parent entity for a given entity.
   * @param entity the child entity
   * @return the parent entity for the given entity
   */
  protected CAstEntity getParent(CAstEntity entity) {
    return entityParents.get(entity);
  }

  /**
   * Set the parent entity for a given entity.
   * @param entity the child entity
   * @param parent the parent entity
   */
  protected void setParent(CAstEntity entity, CAstEntity parent) {
    entityParents.put(entity, parent);
  }

  /**
   * Entity processing hook; sub-classes are expected to override if they introduce new
   * entity types.
   * Should invoke super.doVisitEntity() for unprocessed entities.
   * @return true if entity was handled
   */
  protected boolean doVisitEntity(CAstEntity n, C context, CAstVisitor<C> visitor) {
    return false;
  }


  /**
   * Visit scoped entities of an entity using a given iterator.
   * Prerequisite (unchecked): i iterates over entities scoped in n.
   * @param n the parent entity of the entities to process
   * @param context a visitor-specific context
   */
  public final void visitScopedEntities(CAstEntity n, Map allScopedEntities, C context, CAstVisitor<C> visitor) {
    for(Iterator i = allScopedEntities.values().iterator(); i.hasNext(); ) {
      visitScopedEntities(n, ((Collection)i.next()).iterator(), context, visitor);
    }
  }

  public final void visitScopedEntities(CAstEntity n, Iterator i, C context, CAstVisitor<C> visitor) {
    while (i.hasNext()) {
      CAstEntity child = (CAstEntity) i.next();
      setParent(child, n);
      visitor.visitEntities(child, context, visitor);
    }
  }
  /**
   * Recursively visit an entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   */
  public final void visitEntities(final CAstEntity n, C context, CAstVisitor<C> visitor) {
    Position restore = currentPosition;
    if (n.getPosition() != null) {
      currentPosition = n.getPosition();
    } else {
      currentPosition = null;
    }
    
    if (visitor.enterEntity(n, context, visitor))
      return;
    switch (n.getKind()) {
    case CAstEntity.FILE_ENTITY: {
      C fileContext = visitor.makeFileContext(context, n);
      if (visitor.visitFileEntity(n, context, fileContext, visitor))
        break;
      visitor.visitScopedEntities(n, n.getAllScopedEntities(), fileContext, visitor);
      visitor.leaveFileEntity(n, context, fileContext, visitor);
      break;
    }
    case CAstEntity.FIELD_ENTITY: {
      if (visitor.visitFieldEntity(n, context, visitor))
        break;
      visitor.leaveFieldEntity(n, context, visitor);
      break;
    }
    case CAstEntity.GLOBAL_ENTITY: {
      if (visitor.visitGlobalEntity(n, context, visitor))
        break;
      visitor.leaveGlobalEntity(n, context, visitor);
      break;
    }
    case CAstEntity.TYPE_ENTITY: {
      C typeContext = visitor.makeTypeContext(context, n);
      if (visitor.visitTypeEntity(n, context, typeContext, visitor))
        break;
      visitor.visitScopedEntities(n, n.getAllScopedEntities(), typeContext, visitor);
      visitor.leaveTypeEntity(n, context, typeContext, visitor);
      break;
    }
    case CAstEntity.FUNCTION_ENTITY: {
      C codeContext = visitor.makeCodeContext(context, n);
      if (visitor.visitFunctionEntity(n, context, codeContext, visitor))
        break;
      // visit the AST if any
      if (n.getAST() != null)
        visitor.visit(n.getAST(), codeContext, visitor);
      // XXX: there may be code that needs to go in here
      // process any remaining scoped children
      visitor.visitScopedEntities(n, n.getScopedEntities(null), codeContext, visitor);
      visitor.leaveFunctionEntity(n, context, codeContext, visitor);
      break;
    }
    case CAstEntity.MACRO_ENTITY: {
      C codeContext = visitor.makeCodeContext(context, n);
      if (visitor.visitMacroEntity(n, context, codeContext, visitor))
        break;
      // visit the AST if any
      if (n.getAST() != null)
        visitor.visit(n.getAST(), codeContext, visitor);
      // XXX: there may be code that needs to go in here
      // process any remaining scoped children
      visitor.visitScopedEntities(n, n.getScopedEntities(null), codeContext, visitor);
      visitor.leaveMacroEntity(n, context, codeContext, visitor);
      break;
    }
    case CAstEntity.SCRIPT_ENTITY: {
      C codeContext = visitor.makeCodeContext(context, n);
      if (visitor.visitScriptEntity(n, context, codeContext, visitor))
        break;
      // visit the AST if any
      if (n.getAST() != null)
        visitor.visit(n.getAST(), codeContext, visitor);
      // XXX: there may be code that needs to go in here
      // process any remaining scoped children
      visitor.visitScopedEntities(n, n.getScopedEntities(null), codeContext, visitor);
      visitor.leaveScriptEntity(n, context, codeContext, visitor);
      break;
    }
    default: {
      if (!visitor.doVisitEntity(n, context, visitor)) {
        System.err.println(("No handler for entity " + n.getName()));
        Assertions.UNREACHABLE("cannot handle entity of kind" + n.getKind());
      }
    }
    }
    visitor.postProcessEntity(n, context, visitor);
    
    currentPosition = restore;
  }

  /**
   * Enter the entity visitor.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean enterEntity(CAstEntity n, C context, CAstVisitor<C> visitor) { return false; }
  /**
   * Post-process an entity after visiting it.
   * @param n the entity to process
   * @param context a visitor-specific context
   */
  protected void postProcessEntity(CAstEntity n, C context, CAstVisitor<C> visitor) { return; }

  /**
   * Visit any entity.  Override only this to change behavior for all entities.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @return true if no further processing is needed
   */
  public boolean visitEntity(CAstEntity n, C context, CAstVisitor<C> visitor) { return false; }
  /**
   * Leave any entity.  Override only this to change behavior for all entities.
   * @param n the entity to process
   * @param context a visitor-specific context
   */
  public void leaveEntity(CAstEntity n, C context, CAstVisitor<C> visitor) { return; }

  /**
   * Visit a File entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param fileC a visitor-specific context for this file
   * @return true if no further processing is needed
   */
  protected boolean visitFileEntity(CAstEntity n, C context, C fileC, CAstVisitor<C> visitor) { return visitor.visitEntity(n, context, visitor); }
  /**
   * Leave a File entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param fileContext a visitor-specific context for this file
   */
  protected void leaveFileEntity(CAstEntity n, C context, C fileContext, CAstVisitor<C> visitor) { visitor.leaveEntity(n, context, visitor); }
  /**
   * Visit a Field entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitFieldEntity(CAstEntity n, C context, CAstVisitor<C> visitor) { return visitor.visitEntity(n, context, visitor); }
  /**
   * Leave a Field entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   */
  protected void leaveFieldEntity(CAstEntity n, C context, CAstVisitor<C> visitor) { visitor.leaveEntity(n, context, visitor); }
  /**
   * Visit a Field entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitGlobalEntity(CAstEntity n, C context, CAstVisitor<C> visitor) { return visitor.visitEntity(n, context, visitor); }
  /**
   * Leave a Field entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   */
  protected void leaveGlobalEntity(CAstEntity n, C context, CAstVisitor<C> visitor) { visitor.leaveEntity(n, context, visitor); }
 /**
   * Visit a Type entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param typeContext a visitor-specific context for this type
   * @return true if no further processing is needed
   */
  protected boolean visitTypeEntity(CAstEntity n, C context, C typeContext, CAstVisitor<C> visitor) { return visitor.visitEntity(n, context, visitor); }
  /**
   * Leave a Type entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param typeContext a visitor-specific context for this type
   */
  protected void leaveTypeEntity(CAstEntity n, C context, C typeContext, CAstVisitor<C> visitor) { visitor.leaveEntity(n, context, visitor); }
  /**
   * Visit a Function entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this function
   * @return true if no further processing is needed
   */
  protected boolean visitFunctionEntity(CAstEntity n, C context, C codeContext, CAstVisitor<C> visitor) { return visitor.visitEntity(n, context, visitor); }
  /**
   * Leave a Function entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this function
   */
  protected void leaveFunctionEntity(CAstEntity n, C context, C codeContext, CAstVisitor<C> visitor) { visitor.leaveEntity(n, context, visitor); }
  /**
   * Visit a Macro entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this macro
   * @return true if no further processing is needed
   */
  protected boolean visitMacroEntity(CAstEntity n, C context, C codeContext, CAstVisitor<C> visitor) { return visitor.visitEntity(n, context, visitor); }
  /**
   * Leave a Macro entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this macro
   */
  protected void leaveMacroEntity(CAstEntity n, C context, C codeContext, CAstVisitor<C> visitor) { visitor.leaveEntity(n, context, visitor); }
  /**
   * Visit a Script entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this script
   * @return true if no further processing is needed
   */
  protected boolean visitScriptEntity(CAstEntity n, C context, C codeContext, CAstVisitor<C> visitor) { return visitor.visitEntity(n, context, visitor); }
  /**
   * Leave a Script entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this script
   */
  protected void leaveScriptEntity(CAstEntity n, C context, C codeContext, CAstVisitor<C> visitor) { visitor.leaveEntity(n, context, visitor); }

  /**
   *  Node processing hook; sub-classes are expected to override if they 
   * introduce new node types.
   *
   * (Should invoke super.doVisit() for unprocessed nodes.)
   *
   * @return true if node was handled
   */
  protected boolean doVisit(CAstNode n, C context, CAstVisitor<C> visitor) {
    return false;
  }

  /**
   *  Node processing hook; sub-classes are expected to override if they
   * introduce new node types that appear on the left hand side of assignment
   * operations.
   *
   * (Should invoke super.doVisit() for unprocessed nodes.)
   *
   * @return true if node was handled
   */
  protected boolean doVisitAssignNodes(CAstNode n, C context, CAstNode v, CAstNode a, CAstVisitor<C> visitor) {
    return false;
  }

  /**
   * Visit children of a node starting at a given index.
   * @param n the parent node of the nodes to process
   * @param start the starting index of the nodes to process
   * @param context a visitor-specific context
   */
  public final void visitChildren(CAstNode n, int start, C context, CAstVisitor<C> visitor) {
    int end = n.getChildCount();
    for (int i = start; i < end; i++)
      visitor.visit(n.getChild(i), context, visitor);
  }
  /**
   * Visit all children of a node.
   * @param n the parent node of the nodes to process
   * @param context a visitor-specific context
   */
  public final void visitAllChildren(CAstNode n, C context, CAstVisitor<C> visitor) {
    visitor.visitChildren(n, 0, context, visitor);
  }
  /**
   * Recursively visit a given node.
   * TODO: do assertions about structure belong here?
   * @param n the node to process
   * @param context a visitor-specific context
   */
  public final void visit(final CAstNode n, C context, CAstVisitor<C> visitor) {
    Position restore = currentPosition;
    if (context != null && context.getSourceMap() != null) {
      Position p = context.getSourceMap().getPosition(n);
      if (p != null) {
        currentPosition = p;
      }
    }
    
    if (visitor.enterNode(n, context, visitor))
      return;

    int NT = n.getKind();
    switch (NT) {
    case CAstNode.FUNCTION_EXPR: {
      if (visitor.visitFunctionExpr(n, context, visitor))
        break;
      visitor.leaveFunctionExpr(n, context, visitor);
      break;
    }

    case CAstNode.FUNCTION_STMT: {
      if (visitor.visitFunctionStmt(n, context, visitor))
        break;
      visitor.leaveFunctionStmt(n, context, visitor);
      break;
    }

    case CAstNode.LOCAL_SCOPE: {
      if (visitor.visitLocalScope(n, context, visitor))
        break;
      C localContext = visitor.makeLocalContext(context, n);
      visitor.visit(n.getChild(0), localContext, visitor);
      visitor.leaveLocalScope(n, context, visitor);
      break;
    }

    case CAstNode.SPECIAL_PARENT_SCOPE: {
      if (visitor.visitSpecialParentScope(n, context, visitor))
        break;
      C localContext = visitor.makeSpecialParentContext(context, n);
      visitor.visit(n.getChild(1), localContext, visitor);
      visitor.leaveSpecialParentScope(n, context, visitor);
      break;
    }

    case CAstNode.BLOCK_EXPR: {
      if (visitor.visitBlockExpr(n, context, visitor))
        break;
      visitor.visitAllChildren(n, context, visitor);
      visitor.leaveBlockExpr(n, context, visitor);
      break;
    }

    case CAstNode.BLOCK_STMT: {
      if (visitor.visitBlockStmt(n, context, visitor))
        break;
      visitor.visitAllChildren(n, context, visitor);
      visitor.leaveBlockStmt(n, context, visitor);
      break;
    }

    case CAstNode.LOOP: {
      if (visitor.visitLoop(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.leaveLoopHeader(n, context, visitor);
      visitor.visit(n.getChild(1), context, visitor);
      visitor.leaveLoop(n, context, visitor);
      break;
    }

    case CAstNode.FORIN_LOOP: {
      if (visitor.visitForIn(n, context, visitor)) {
        break;
      }
      visitor.leaveForIn(n, context, visitor);
      break;
    }
    
    case CAstNode.GET_CAUGHT_EXCEPTION: {
      if (visitor.visitGetCaughtException(n, context, visitor))
        break;
      visitor.leaveGetCaughtException(n, context, visitor);
      break;
    }

    case CAstNode.THIS: {
      if (visitor.visitThis(n, context, visitor))
        break;
      visitor.leaveThis(n, context, visitor);
      break;
    }

    case CAstNode.SUPER: {
      if (visitor.visitSuper(n, context, visitor))
        break;
      visitor.leaveSuper(n, context, visitor);
      break;
    }

    case CAstNode.CALL: {
      if (visitor.visitCall(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.visitChildren(n, 2, context, visitor);
      visitor.leaveCall(n, context, visitor);
      break;
    }

    case CAstNode.VAR: {
      if (visitor.visitVar(n, context, visitor))
        break;
      visitor.leaveVar(n, context, visitor);
      break;
    }

    case CAstNode.CONSTANT: {
      if (visitor.visitConstant(n, context, visitor))
        break;
      visitor.leaveConstant(n, context, visitor);
      break;
    }

    case CAstNode.BINARY_EXPR: {
      if (visitor.visitBinaryExpr(n, context, visitor))
        break;
      visitor.visit(n.getChild(1), context, visitor);
      visitor.visit(n.getChild(2), context, visitor);
      visitor.leaveBinaryExpr(n, context, visitor);
      break;
    }

    case CAstNode.UNARY_EXPR: {
      if (visitor.visitUnaryExpr(n, context, visitor))
        break;
      visitor.visit(n.getChild(1), context, visitor);
      visitor.leaveUnaryExpr(n, context, visitor);
      break;
    }

    case CAstNode.ARRAY_LENGTH: {
      if (visitor.visitArrayLength(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.leaveArrayLength(n, context, visitor);
      break;
    }

    case CAstNode.ARRAY_REF: {
      if (visitor.visitArrayRef(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.visitChildren(n, 2, context, visitor);
      visitor.leaveArrayRef(n, context, visitor);
      break;
    }

    case CAstNode.DECL_STMT: {
      if (visitor.visitDeclStmt(n, context, visitor))
        break;
      if (n.getChildCount() == 2)
        visitor.visit(n.getChild(1), context, visitor);
      visitor.leaveDeclStmt(n, context, visitor);
      break;
    }

    case CAstNode.RETURN: {
      if (visitor.visitReturn(n, context, visitor))
        break;
      if (n.getChildCount() > 0)
        visitor.visit(n.getChild(0), context, visitor);
      visitor.leaveReturn(n, context, visitor);
      break;
    }

    case CAstNode.IFGOTO: {
      if (visitor.visitIfgoto(n, context, visitor))
        break;
      if (n.getChildCount() == 1) {
	visitor.visit(n.getChild(0), context, visitor);
      } else if (n.getChildCount() == 3) {
	visitor.visit(n.getChild(1), context, visitor);
	visitor.visit(n.getChild(2), context, visitor);
      } else {
	Assertions.UNREACHABLE();
      }

      visitor.leaveIfgoto(n, context, visitor);
      break;
    }

    case CAstNode.GOTO: {
      if (visitor.visitGoto(n, context, visitor))
        break;
      visitor.leaveGoto(n, context, visitor);
      break;
    }

    case CAstNode.LABEL_STMT: {
      if (visitor.visitLabelStmt(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      if (n.getChildCount() == 2)
        visitor.visit(n.getChild(1), context, visitor);
      else
        assert n.getChildCount() < 2;
      visitor.leaveLabelStmt(n, context, visitor);
      break;
    }

    case CAstNode.IF_STMT: {
      if (visitor.visitIfStmt(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.leaveIfStmtCondition(n, context, visitor);
      visitor.visit(n.getChild(1), context, visitor);
      visitor.leaveIfStmtTrueClause(n, context, visitor);
      if (n.getChildCount() == 3)
        visitor.visit(n.getChild(2), context, visitor);
      visitor.leaveIfStmt(n, context, visitor);
      break;
    }

    case CAstNode.IF_EXPR: {
      if (visitor.visitIfExpr(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.leaveIfExprCondition(n, context, visitor);
      visitor.visit(n.getChild(1), context, visitor);
      visitor.leaveIfExprTrueClause(n, context, visitor);
      if (n.getChildCount() == 3)
        visitor.visit(n.getChild(2), context, visitor);
      visitor.leaveIfExpr(n, context, visitor);
      break;
    }

    case CAstNode.NEW_ENCLOSING:
    case CAstNode.NEW: {
      if (visitor.visitNew(n, context, visitor))
        break;

      for(int i = 1; i < n.getChildCount(); i++) {
	visitor.visit(n.getChild(i), context, visitor);
      }	  

      visitor.leaveNew(n, context, visitor);
      break;
    }

    case CAstNode.OBJECT_LITERAL: {
      if (visitor.visitObjectLiteral(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      for (int i = 1; i < n.getChildCount(); i+=2) {
        visitor.visit(n.getChild(i), context, visitor);
        visitor.visit(n.getChild(i+1), context, visitor);
        visitor.leaveObjectLiteralFieldInit(n, i, context, visitor);
      }
      visitor.leaveObjectLiteral(n, context, visitor);
      break;
    }

    case CAstNode.ARRAY_LITERAL: {
      if (visitor.visitArrayLiteral(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.leaveArrayLiteralObject(n, context, visitor);
      for (int i = 1; i < n.getChildCount(); i++) {
        visitor.visit(n.getChild(i), context, visitor);
        visitor.leaveArrayLiteralInitElement(n, i, context, visitor);
      }
      visitor.leaveArrayLiteral(n, context, visitor);
      break;
    }

    case CAstNode.OBJECT_REF: {
      if (visitor.visitObjectRef(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.leaveObjectRef(n, context, visitor);
      break;
    }

    case CAstNode.ASSIGN:
    case CAstNode.ASSIGN_PRE_OP:
    case CAstNode.ASSIGN_POST_OP: {
      if (visitor.visitAssign(n, context, visitor))
        break;
      visitor.visit(n.getChild(1), context, visitor);
      // TODO: is this correct?
      if (visitor.visitAssignNodes(n.getChild(0), context, n.getChild(1), n, visitor))
        break;
      visitor.leaveAssign(n, context, visitor);
      break;
    }

    case CAstNode.SWITCH: {
      if (visitor.visitSwitch(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.leaveSwitchValue(n, context, visitor);
      visitor.visit(n.getChild(1), context, visitor);
      visitor.leaveSwitch(n, context, visitor);
      break;
    }

    case CAstNode.THROW: {
      if (visitor.visitThrow(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.leaveThrow(n, context, visitor);
      break;
    }

    case CAstNode.CATCH: {
      if (visitor.visitCatch(n, context, visitor))
        break;
      visitor.visitChildren(n, 1, context, visitor);
      visitor.leaveCatch(n, context, visitor);
      break;
    }

    case CAstNode.UNWIND: {
      if (visitor.visitUnwind(n, context, visitor))
        break;
      C unwindContext = visitor.makeUnwindContext(context, n.getChild(1), visitor);
      visitor.visit(n.getChild(0), unwindContext, visitor);
      visitor.visit(n.getChild(1), context, visitor);
      visitor.leaveUnwind(n, context, visitor);
      break;
    }

    case CAstNode.TRY: {
      if (visitor.visitTry(n, context, visitor))
        break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.leaveTryBlock(n, context, visitor);
      visitor.visit(n.getChild(1), context, visitor);
      visitor.leaveTry(n, context, visitor);
      break;
    }

    case CAstNode.EMPTY: {
      if (visitor.visitEmpty(n, context, visitor))
        break;
      visitor.leaveEmpty(n, context, visitor);
      break;
    }

    case CAstNode.PRIMITIVE: {
      if (visitor.visitPrimitive(n, context, visitor))
        break;
      visitor.visitAllChildren(n, context, visitor);
      visitor.leavePrimitive(n, context, visitor);
      break;
    }

    case CAstNode.VOID: {
      if (visitor.visitVoid(n, context, visitor))
        break;
      visitor.leaveVoid(n, context, visitor);
      break;
    }

    case CAstNode.CAST: {
      if (visitor.visitCast(n, context, visitor))
        break;
      visitor.visit(n.getChild(1), context, visitor);
      visitor.leaveCast(n, context, visitor);
      break;
    }

    case CAstNode.INSTANCEOF: {
      if (visitor.visitInstanceOf(n, context, visitor))
        break;
      visitor.visit(n.getChild(1), context, visitor);
      visitor.leaveInstanceOf(n, context, visitor);
      break;
    }

    case CAstNode.ASSERT: {
      if (visitor.visitAssert(n, context, visitor))
	break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.leaveAssert(n, context, visitor);
      break;	
    }
    
        case CAstNode.EACH_ELEMENT_GET: {
      if (visitor.visitEachElementGet(n, context, visitor))
	break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.visit(n.getChild(1), context, visitor);
      visitor.leaveEachElementGet(n, context, visitor);
      break;	
    }
    
    case CAstNode.EACH_ELEMENT_HAS_NEXT: {
      if (visitor.visitEachElementHasNext(n, context, visitor))
	break;
      visitor.visit(n.getChild(0), context, visitor);
      visitor.visit(n.getChild(1), context, visitor);
      visitor.leaveEachElementHasNext(n, context, visitor);
      break;	
    }
    
    case CAstNode.TYPE_LITERAL_EXPR: {
      if (visitor.visitTypeLiteralExpr(n, context, visitor)) {
	break;
      }
      visitor.visit(n.getChild(0), context, visitor);
      visitor.leaveTypeLiteralExpr(n, context, visitor);
      break;
    }
    
    case CAstNode.IS_DEFINED_EXPR: {
      if (visitor.visitIsDefinedExpr(n, context, visitor)) {
	break;
      }
      visitor.visit(n.getChild(0), context, visitor);
      if (n.getChildCount() == 2){
	visitor.visit(n.getChild(1), context, visitor);
      }
      visitor.leaveIsDefinedExpr(n, context, visitor);
      break;
    }

    case CAstNode.INCLUDE: {
      if (visitor.visitInclude(n, context, visitor)) {
	break;
      }
      visitor.leaveInclude(n, context, visitor);
      break;
    }

    case CAstNode.MACRO_VAR: {
      if (visitor.visitMacroVar(n, context, visitor)) {
	break;
      }
      visitor.leaveMacroVar(n, context, visitor);
      break;
    }

    case CAstNode.ECHO: {
      if (visitor.visitEcho(n, context, visitor)) {
	break;
      }
      for(int i = 0; i < n.getChildCount(); i++) {
	visitor.visit(n.getChild(i), context, visitor);
      }
      visitor.leaveEcho(n, context, visitor);
      break;
    }

    default: {
      if (!visitor.doVisit(n, context, visitor)) {
        System.err.println(("looking at unhandled " + n + "(" + NT + ")" + " of " + n.getClass()));
        Assertions.UNREACHABLE("cannot handle node of kind " + NT);
      }
    }
    }

    if (context != null) {
      visitor.visitScopedEntities(context.top(), context.top().getScopedEntities(n), context, visitor);
    }

    visitor.postProcessNode(n, context, visitor);
    
    currentPosition = restore;
  }

  protected void leaveSpecialParentScope(CAstNode n, C context, CAstVisitor<C> visitor) {
    visitor.leaveNode(n, context, visitor);
  }

  protected C makeSpecialParentContext(C context, CAstNode n) {
    return context;
  }

  protected boolean visitSpecialParentScope(CAstNode n, C context, CAstVisitor<C> visitor) {
    return visitor.visitNode(n, context, visitor);
  }

  /**
   * Process the given array reference node. Factored out so that derived languages can reuse this
   * code for specially-marked types of array references (as in X10, for which different instruction
   * types get generated, but whose structure is essentially the same as an ordinary array reference).
   */
  protected boolean doVisitArrayRefNode(CAstNode n, CAstNode v, CAstNode a, boolean assign, boolean preOp, C context, CAstVisitor<C> visitor) {
    if (assign ? visitor.visitArrayRefAssign(n, v, a, context, visitor)
        : visitor.visitArrayRefAssignOp(n, v, a, preOp, context, visitor))
      return true;
    visitor.visit(n.getChild(0), context, visitor);
    // XXX: we don't really need to visit array dims twice!
    visitor.visitChildren(n, 2, context, visitor);
    if (assign)
      visitor.leaveArrayRefAssign(n, v, a, context, visitor);
    else
      visitor.leaveArrayRefAssignOp(n, v, a, preOp, context, visitor);
    return false;
  }

  protected boolean visitAssignNodes(CAstNode n, C context, CAstNode v, CAstNode a, CAstVisitor<C> visitor) {
    int NT = a.getKind();
    boolean assign = NT == CAstNode.ASSIGN;
    boolean preOp = NT == CAstNode.ASSIGN_PRE_OP;
    switch (n.getKind()) {
    case CAstNode.ARRAY_REF: {
      if (doVisitArrayRefNode(n, v, a, assign, preOp, context, visitor)) {
        return true;
      }

      break;
    }

    case CAstNode.OBJECT_REF: {
      if (assign ? visitor.visitObjectRefAssign(n, v, a, context, visitor) : visitor.visitObjectRefAssignOp(n, v, a, preOp,
          context, visitor))
        return true;
      visitor.visit(n.getChild(0), context, visitor);
      if (assign)
        visitor.leaveObjectRefAssign(n, v, a, context, visitor);
      else
        visitor.leaveObjectRefAssignOp(n, v, a, preOp, context, visitor);
      break;
    }

    case CAstNode.BLOCK_EXPR: {
      if (assign ? visitor.visitBlockExprAssign(n, v, a, context, visitor) : visitor.visitBlockExprAssignOp(n, v, a, preOp,
          context, visitor))
        return true;
      // FIXME: is it correct to ignore all the other children?
      if (visitor.visitAssignNodes(n.getChild(n.getChildCount() - 1), context, v, a, visitor))
        return true;
      if (assign)
        visitor.leaveBlockExprAssign(n, v, a, context, visitor);
      else
        visitor.leaveBlockExprAssignOp(n, v, a, preOp, context, visitor);
      break;
    }

    case CAstNode.VAR: {
      if (assign ? visitor.visitVarAssign(n, v, a, context, visitor) : visitor.visitVarAssignOp(n, v, a, preOp, context, visitor))
        return true;
      if (assign)
        visitor.leaveVarAssign(n, v, a, context, visitor);
      else
        visitor.leaveVarAssignOp(n, v, a, preOp, context, visitor);
      break;
    }

    default: {
      if (!visitor.doVisitAssignNodes(n, context, v, a, visitor)) {
        if (DEBUG) {
          System.err.println(("cannot handle assign to kind " + n.getKind()));
        }
        throw new UnsupportedOperationException("cannot handle assignment: " + CAstPrinter.print(a, context.getSourceMap()));
      }
    }
    }
    return false;
  }

  /**
   * Enter the node visitor.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean enterNode(CAstNode n, C c, CAstVisitor<C> visitor) { return false; }
  /**
   * Post-process a node after visiting it.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void postProcessNode(CAstNode n, C c, CAstVisitor<C> visitor) { return; }

  /**
   * Visit any node.  Override only this to change behavior for all nodes.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  public boolean visitNode(CAstNode n, C c, CAstVisitor<C> visitor) { return false; }
  /**
   * Leave any node.  Override only this to change behavior for all nodes.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  public void leaveNode(CAstNode n, C c, CAstVisitor<C> visitor) { return; }

  /**
   * Visit a FunctionExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitFunctionExpr(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a FunctionExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveFunctionExpr(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a FunctionStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitFunctionStmt(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a FunctionStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveFunctionStmt(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a LocalScope node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitLocalScope(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a LocalScope node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveLocalScope(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a BlockExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitBlockExpr(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a BlockExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveBlockExpr(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a BlockStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitBlockStmt(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a BlockStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveBlockStmt(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Loop node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitLoop(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Visit a For..In node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitForIn(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Visit a Loop node after processing the loop header.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveLoopHeader(CAstNode n, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Leave a Loop node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveLoop(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Leave a For..In node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveForIn(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a GetCaughtException node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitGetCaughtException(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a GetCaughtException node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveGetCaughtException(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a This node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitThis(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a This node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveThis(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Super node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitSuper(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a Super node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveSuper(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Call node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitCall(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a Call node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveCall(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Var node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitVar(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a Var node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveVar(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Constant node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitConstant(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a Constant node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveConstant(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a BinaryExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitBinaryExpr(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a BinaryExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveBinaryExpr(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a UnaryExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitUnaryExpr(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a UnaryExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveUnaryExpr(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an ArrayLength node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitArrayLength(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an ArrayLength node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveArrayLength(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an ArrayRef node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitArrayRef(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an ArrayRef node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveArrayRef(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a DeclStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitDeclStmt(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a DeclStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveDeclStmt(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Return node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitReturn(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a Return node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveReturn(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an Ifgoto node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitIfgoto(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an Ifgoto node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfgoto(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Goto node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitGoto(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a Goto node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveGoto(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a LabelStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitLabelStmt(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a LabelStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveLabelStmt(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an IfStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitIfStmt(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Visit an IfStmt node after processing the condition.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfStmtCondition(CAstNode n, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Visit an IfStmt node after processing the true clause.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfStmtTrueClause(CAstNode n, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Leave an IfStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfStmt(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an IfExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitIfExpr(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Visit an IfExpr node after processing the condition.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfExprCondition(CAstNode n, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Visit an IfExpr node after processing the true clause.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfExprTrueClause(CAstNode n, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Leave an IfExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfExpr(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a New node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitNew(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a New node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveNew(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an ObjectLiteral node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitObjectLiteral(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Visit an ObjectLiteral node after processing the {i}th field initializer.
   * @param n the node to process
   * @param i the field position that was initialized
   * @param c a visitor-specific context
   */
  protected void leaveObjectLiteralFieldInit(CAstNode n, int i, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Leave an ObjectLiteral node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveObjectLiteral(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an ArrayLiteral node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitArrayLiteral(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Visit an ArrayLiteral node after processing the array object.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveArrayLiteralObject(CAstNode n, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Visit an ArrayLiteral node after processing the {i}th element initializer.
   * @param n the node to process
   * @param i the index that was initialized
   * @param c a visitor-specific context
   */
  protected void leaveArrayLiteralInitElement(CAstNode n, int i, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Leave a ArrayLiteral node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveArrayLiteral(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an ObjectRef node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitObjectRef(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an ObjectRef node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveObjectRef(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an Assign node.  Override only this to change behavior for all assignment nodes.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  public boolean visitAssign(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an Assign node.  Override only this to change behavior for all assignment nodes.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  public void leaveAssign(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an ArrayRef Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitArrayRefAssign(CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) { /* empty */ return false; }
  /**
   * Visit an ArrayRef Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   */
  protected void leaveArrayRefAssign(CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Visit an ArrayRef Op/Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitArrayRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) { /* empty */ return false; }
  /**
   * Visit an ArrayRef Op/Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   */
  protected void leaveArrayRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Visit an ObjectRef Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitObjectRefAssign(CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) { /* empty */ return false; }
  /**
   * Visit an ObjectRef Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   */
  protected void leaveObjectRefAssign(CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Visit an ObjectRef Op/Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitObjectRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) { /* empty */ return false; }
  /**
   * Visit an ObjectRef Op/Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   */
  protected void leaveObjectRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Visit a BlockExpr Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitBlockExprAssign(CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) { /* empty */ return false; }
  /**
   * Visit a BlockExpr Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   */
  protected void leaveBlockExprAssign(CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Visit a BlockExpr Op/Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitBlockExprAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) { /* empty */ return false; }
  /**
   * Visit a BlockExpr Op/Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   */
  protected void leaveBlockExprAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Visit a Var Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitVarAssign(CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) { /* empty */ return false; }
  /**
   * Visit a Var Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   */
  protected void leaveVarAssign(CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Visit a Var Op/Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitVarAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) { /* empty */ return false; }
  /**
   * Visit a Var Op/Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   */
  protected void leaveVarAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Visit a Switch node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitSwitch(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Visit a Switch node after processing the switch value.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveSwitchValue(CAstNode n, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Leave a Switch node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveSwitch(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Throw node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitThrow(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a Throw node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveThrow(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Catch node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitCatch(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a Catch node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveCatch(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an Unwind node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitUnwind(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an Unwind node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveUnwind(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Try node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitTry(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Visit a Try node after processing the try block.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveTryBlock(CAstNode n, C c, CAstVisitor<C> visitor) { /* empty */ }
  /**
   * Leave a Try node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveTry(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an Empty node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitEmpty(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an Empty node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveEmpty(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Primitive node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitPrimitive(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a Primitive node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leavePrimitive(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Void node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitVoid(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a Void node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveVoid(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit a Cast node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitCast(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave a Cast node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveCast(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an InstanceOf node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitInstanceOf(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an InstanceOf node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveInstanceOf(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an InstanceOf node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveAssert(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  protected boolean visitAssert(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an InstanceOf node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected boolean visitEachElementHasNext(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  protected void leaveEachElementHasNext(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an InstanceOf node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitEachElementGet(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an FOR_EACH_ELEMENT_GET node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveEachElementGet(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }
  /**
   * Visit an TYPE_LITERAL_EXPR node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitTypeLiteralExpr(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an TYPE_LITERAL_EXPR node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveTypeLiteralExpr(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }

  protected boolean visitIsDefinedExpr(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an IS_DEFINED_EXPR node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIsDefinedExpr(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }

  protected boolean visitEcho(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an ECHO node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveEcho(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }

  protected boolean visitInclude(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an INCLUDE node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveInclude(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }

  protected boolean visitMacroVar(CAstNode n, C c, CAstVisitor<C> visitor) { return visitor.visitNode(n, c, visitor); }
  /**
   * Leave an MACRO_VAR node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveMacroVar(CAstNode n, C c, CAstVisitor<C> visitor) { visitor.leaveNode(n, c, visitor); }

}
