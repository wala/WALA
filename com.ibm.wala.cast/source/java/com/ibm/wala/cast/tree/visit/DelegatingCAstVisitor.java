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

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;

/**
 * @author Igor Peshansky
 * Extend CAstVisitor to delegate unimplemented functionality to another
 * visitor.  Needed to work around Java's retarded multiple inheritance rules.
 * TODO: document me.
 */
public abstract class DelegatingCAstVisitor extends CAstVisitor {

  /**
   * Construct a context for a File entity or delegate by default.
   * @param context a visitor-specific context in which this file was visited
   * @param n the file entity
   */
  protected Context makeFileContext(Context context, CAstEntity n) {
    return delegate.makeFileContext(context, n);
  }
  /**
   * Construct a context for a Type entity or delegate by default.
   * @param context a visitor-specific context in which this type was visited
   * @param n the type entity
   */
  protected Context makeTypeContext(Context context, CAstEntity n) {
    return delegate.makeTypeContext(context, n);
  }
  /**
   * Construct a context for a Code entity or delegate by default.
   * @param context a visitor-specific context in which the code was visited
   * @param n the code entity
   */
  protected Context makeCodeContext(Context context, CAstEntity n) {
    return delegate.makeCodeContext(context, n);
  }

  /**
   * Construct a context for a LocalScope node or delegate by default.
   * @param context a visitor-specific context in which the local scope was visited
   * @param n the local scope node
   */
  protected Context makeLocalContext(Context context, CAstNode n) {
    return delegate.makeLocalContext(context, n);
  }
  /**
   * Construct a context for an Unwind node or delegate by default.
   * @param context a visitor-specific context in which the unwind was visited
   * @param n the unwind node
   */
  protected Context makeUnwindContext(Context context, CAstNode n, CAstVisitor visitor) {
    return delegate.makeUnwindContext(context, n, visitor);
  }

  /**
   * Get the parent entity for a given entity.
   * @param entity the child entity
   * @return the parent entity for the given entity
   */
  protected CAstEntity getParent(CAstEntity entity) {
    return delegate.getParent(entity);
  }

  /**
   * Set the parent entity for a given entity.
   * @param entity the child entity
   * @param parent the parent entity
   */
  protected void setParent(CAstEntity entity, CAstEntity parent) {
    delegate.setParent(entity, parent);
  }

  private final CAstVisitor delegate;

  protected final CAstVisitor delegate() { return delegate; }

  /**
   * Delegating CAstVisitor constructor.
   * Needs to have a valid (non-null) delegate visitor.
   * @param delegate the visitor to delegate to for default implementation
   */
  protected DelegatingCAstVisitor(CAstVisitor delegate) {
    assert delegate != null;
    this.delegate = delegate;
  }

  /**
   * Entity processing hook; sub-classes are expected to override if they
   * introduce new entity types.
   * Should invoke super.doVisitEntity() for unprocessed entities.
   * @return true if entity was handled
   */
  protected boolean doVisitEntity(CAstEntity n, Context context, CAstVisitor visitor) {
    return delegate.doVisitEntity(n, context, visitor);
  }

  /**
   * Enter the entity visitor.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean enterEntity(CAstEntity n, Context context, CAstVisitor visitor) {
    return delegate.enterEntity(n, context, visitor);
  }
  /**
   * Post-process an entity after visiting it.
   * @param n the entity to process
   * @param context a visitor-specific context
   */
  protected void postProcessEntity(CAstEntity n, Context context, CAstVisitor visitor) {
    delegate.postProcessEntity(n, context, visitor);
  }

  /**
   * Visit any entity.  Override only this to change behavior for all entities.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @return true if no further processing is needed
   */
  public boolean visitEntity(CAstEntity n, Context context, CAstVisitor visitor) {
    return delegate.visitEntity(n, context, visitor);
  }
  /**
   * Leave any entity.  Override only this to change behavior for all entities.
   * @param n the entity to process
   * @param context a visitor-specific context
   */
  public void leaveEntity(CAstEntity n, Context context, CAstVisitor visitor) {
    delegate.leaveEntity(n, context, visitor);
  }

  /**
   * Visit a File entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param fileContext a visitor-specific context for this file
   * @return true if no further processing is needed
   */
  protected boolean visitFileEntity(CAstEntity n, Context context, Context fileContext, CAstVisitor visitor) {
    return delegate.visitFileEntity(n, context, fileContext, visitor);
  }
  /**
   * Leave a File entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param fileContext a visitor-specific context for this file
   */
  protected void leaveFileEntity(CAstEntity n, Context context, Context fileContext, CAstVisitor visitor) {
    delegate.leaveFileEntity(n, context, fileContext, visitor);
  }
  /**
   * Visit a Field entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitFieldEntity(CAstEntity n, Context context, CAstVisitor visitor) {
    return delegate.visitFieldEntity(n, context, visitor);
  }
  /**
   * Leave a Field entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   */
  protected void leaveFieldEntity(CAstEntity n, Context context, CAstVisitor visitor) {
    delegate.leaveFieldEntity(n, context, visitor);
  }
  /**
   * Visit a Type entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param typeContext a visitor-specific context for this type
   * @return true if no further processing is needed
   */
  protected boolean visitTypeEntity(CAstEntity n, Context context, Context typeContext, CAstVisitor visitor) {
    return delegate.visitTypeEntity(n, context, typeContext, visitor);
  }
  /**
   * Leave a Type entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param typeContext a visitor-specific context for this type
   */
  protected void leaveTypeEntity(CAstEntity n, Context context, Context typeContext, CAstVisitor visitor) {
    delegate.leaveTypeEntity(n, context, typeContext, visitor);
  }
  /**
   * Visit a Function entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this function
   * @return true if no further processing is needed
   */
  protected boolean visitFunctionEntity(CAstEntity n, Context context, Context codeContext, CAstVisitor visitor) {
    return delegate.visitFunctionEntity(n, context, codeContext, visitor);
  }
  /**
   * Leave a Function entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this function
   */
  protected void leaveFunctionEntity(CAstEntity n, Context context, Context codeContext, CAstVisitor visitor) {
    delegate.leaveFunctionEntity(n, context, codeContext, visitor);
  }
  /**
   * Visit a Script entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this script
   * @return true if no further processing is needed
   */
  protected boolean visitScriptEntity(CAstEntity n, Context context, Context codeContext, CAstVisitor visitor) {
    return delegate.visitScriptEntity(n, context, codeContext, visitor);
  }
  /**
   * Leave a Script entity.
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this script
   */
  protected void leaveScriptEntity(CAstEntity n, Context context, Context codeContext, CAstVisitor visitor) {
    delegate.leaveScriptEntity(n, context, codeContext, visitor);
  }

  /**
   * Node processing hook; sub-classes are expected to override if they
   * introduce new node types.
   * Should invoke super.doVisit() for unprocessed nodes.
   * @return true if node was handled
   */
  protected boolean doVisit(CAstNode n, Context context, CAstVisitor visitor) {
    return delegate.doVisit(n, context, visitor);
  }

  /**
   * Enter the node visitor.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean enterNode(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.enterNode(n, c, visitor);
  }
  /**
   * Post-process a node after visiting it.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void postProcessNode(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.postProcessNode(n, c, visitor);
  }

  /**
   * Visit any node.  Override only this to change behavior for all nodes.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  public boolean visitNode(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitNode(n, c, visitor);
  }
  /**
   * Leave any node.  Override only this to change behavior for all nodes.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  public void leaveNode(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveNode(n, c, visitor);
  }

  /**
   * Visit a FunctionExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitFunctionExpr(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitFunctionExpr(n, c, visitor);
  }
  /**
   * Leave a FunctionExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveFunctionExpr(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveFunctionExpr(n, c, visitor);
  }
  /**
   * Visit a FunctionStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitFunctionStmt(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitFunctionStmt(n, c, visitor);
  }
  /**
   * Leave a FunctionStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveFunctionStmt(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveFunctionStmt(n, c, visitor);
  }
  /**
   * Visit a LocalScope node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitLocalScope(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitLocalScope(n, c, visitor);
  }
  /**
   * Leave a LocalScope node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveLocalScope(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveLocalScope(n, c, visitor);
  }
  /**
   * Visit a BlockExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitBlockExpr(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitBlockExpr(n, c, visitor);
  }
  /**
   * Leave a BlockExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveBlockExpr(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveBlockExpr(n, c, visitor);
  }
  /**
   * Visit a BlockStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitBlockStmt(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitBlockStmt(n, c, visitor);
  }
  /**
   * Leave a BlockStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveBlockStmt(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveBlockStmt(n, c, visitor);
  }
  /**
   * Visit a Loop node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitLoop(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitLoop(n, c, visitor);
  }
  /**
   * Visit a Loop node after processing the loop header.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveLoopHeader(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveLoopHeader(n, c, visitor);
  }
  /**
   * Leave a Loop node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveLoop(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveLoop(n, c, visitor);
  }
  /**
   * Visit a GetCaughtException node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitGetCaughtException(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitGetCaughtException(n, c, visitor);
  }
  /**
   * Leave a GetCaughtException node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveGetCaughtException(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveGetCaughtException(n, c, visitor);
  }
  /**
   * Visit a This node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitThis(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitThis(n, c, visitor);
  }
  /**
   * Leave a This node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveThis(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveThis(n, c, visitor);
  }
  /**
   * Visit a Super node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitSuper(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitSuper(n, c, visitor);
  }
  /**
   * Leave a Super node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveSuper(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveSuper(n, c, visitor);
  }
  /**
   * Visit a Call node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitCall(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitCall(n, c, visitor);
  }
  /**
   * Leave a Call node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveCall(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveCall(n, c, visitor);
  }
  /**
   * Visit a Var node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitVar(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitVar(n, c, visitor);
  }
  /**
   * Leave a Var node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveVar(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveVar(n, c, visitor);
  }
  /**
   * Visit a Constant node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitConstant(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitConstant(n, c, visitor);
  }
  /**
   * Leave a Constant node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveConstant(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveConstant(n, c, visitor);
  }
  /**
   * Visit a BinaryExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitBinaryExpr(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitBinaryExpr(n, c, visitor);
  }
  /**
   * Leave a BinaryExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveBinaryExpr(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveBinaryExpr(n, c, visitor);
  }
  /**
   * Visit a UnaryExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitUnaryExpr(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitUnaryExpr(n, c, visitor);
  }
  /**
   * Leave a UnaryExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveUnaryExpr(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveUnaryExpr(n, c, visitor);
  }
  /**
   * Visit an ArrayLength node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitArrayLength(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitArrayLength(n, c, visitor);
  }
  /**
   * Leave an ArrayLength node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveArrayLength(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveArrayLength(n, c, visitor);
  }
  /**
   * Visit an ArrayRef node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitArrayRef(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitArrayRef(n, c, visitor);
  }
  /**
   * Leave an ArrayRef node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveArrayRef(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveArrayRef(n, c, visitor);
  }
  /**
   * Visit a DeclStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitDeclStmt(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitDeclStmt(n, c, visitor);
  }
  /**
   * Leave a DeclStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveDeclStmt(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveDeclStmt(n, c, visitor);
  }
  /**
   * Visit a Return node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitReturn(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitReturn(n, c, visitor);
  }
  /**
   * Leave a Return node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveReturn(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveReturn(n, c, visitor);
  }
  /**
   * Visit an Ifgoto node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitIfgoto(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitIfgoto(n, c, visitor);
  }
  /**
   * Leave an Ifgoto node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfgoto(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveIfgoto(n, c, visitor);
  }
  /**
   * Visit a Goto node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitGoto(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitGoto(n, c, visitor);
  }
  /**
   * Leave a Goto node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveGoto(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveGoto(n, c, visitor);
  }
  /**
   * Visit a LabelStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitLabelStmt(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitLabelStmt(n, c, visitor);
  }
  /**
   * Leave a LabelStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveLabelStmt(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveLabelStmt(n, c, visitor);
  }
  /**
   * Visit an IfStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitIfStmt(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitIfStmt(n, c, visitor);
  }
  /**
   * Visit an IfStmt node after processing the condition.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfStmtCondition(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveIfStmtCondition(n, c, visitor);
  }
  /**
   * Visit an IfStmt node after processing the true clause.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfStmtTrueClause(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveIfStmtTrueClause(n, c, visitor);
  }
  /**
   * Leave an IfStmt node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfStmt(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveIfStmt(n, c, visitor);
  }
  /**
   * Visit an IfExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitIfExpr(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitIfExpr(n, c, visitor);
  }
  /**
   * Visit an IfExpr node after processing the condition.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfExprCondition(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveIfExprCondition(n, c, visitor);
  }
  /**
   * Visit an IfExpr node after processing the true clause.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfExprTrueClause(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveIfExprTrueClause(n, c, visitor);
  }
  /**
   * Leave an IfExpr node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveIfExpr(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveIfExpr(n, c, visitor);
  }
  /**
   * Visit a New node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitNew(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitNew(n, c, visitor);
  }
  /**
   * Leave a New node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveNew(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveNew(n, c, visitor);
  }
  /**
   * Visit an ObjectLiteral node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitObjectLiteral(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitObjectLiteral(n, c, visitor);
  }
  /**
   * Visit an ObjectLiteral node after processing the {i}th field initializer.
   * @param n the node to process
   * @param i the field position that was initialized
   * @param c a visitor-specific context
   */
  protected void leaveObjectLiteralFieldInit(CAstNode n, int i, Context c, CAstVisitor visitor) {
    delegate.leaveObjectLiteralFieldInit(n, i, c, visitor);
  }
  /**
   * Leave an ObjectLiteral node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveObjectLiteral(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveObjectLiteral(n, c, visitor);
  }
  /**
   * Visit an ArrayLiteral node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitArrayLiteral(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitArrayLiteral(n, c, visitor);
  }
  /**
   * Visit an ArrayLiteral node after processing the array object.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveArrayLiteralObject(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveArrayLiteralObject(n, c, visitor);
  }
  /**
   * Visit an ArrayLiteral node after processing the {i}th element initializer.
   * @param n the node to process
   * @param i the index that was initialized
   * @param c a visitor-specific context
   */
  protected void leaveArrayLiteralInitElement(CAstNode n, int i, Context c, CAstVisitor visitor) {
    delegate.leaveArrayLiteralInitElement(n, i, c, visitor);
  }
  /**
   * Leave a ArrayLiteral node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveArrayLiteral(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveArrayLiteral(n, c, visitor);
  }
  /**
   * Visit an ObjectRef node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitObjectRef(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitObjectRef(n, c, visitor);
  }
  /**
   * Leave an ObjectRef node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveObjectRef(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveObjectRef(n, c, visitor);
  }
  /**
   * Visit an Assign node.  Override only this to change behavior for all assignment nodes.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  public boolean visitAssign(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitAssign(n, c, visitor);
  }
  /**
   * Leave an Assign node.  Override only this to change behavior for all assignment nodes.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  public void leaveAssign(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveAssign(n, c, visitor);
  }
  /**
   * Visit an ArrayRef Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitArrayRefAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    return delegate.visitArrayRefAssign(n, v, a, c, visitor);
  }
  /**
   * Visit an ArrayRef Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   */
  protected void leaveArrayRefAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    delegate.leaveArrayRefAssign(n, v, a, c, visitor);
  }
  /**
   * Visit an ArrayRef Op/Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitArrayRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) {
    return delegate.visitArrayRefAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit an ArrayRef Op/Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   */
  protected void leaveArrayRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) {
    delegate.leaveArrayRefAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit an ObjectRef Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitObjectRefAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    return delegate.visitObjectRefAssign(n, v, a, c, visitor);
  }
  /**
   * Visit an ObjectRef Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   */
  protected void leaveObjectRefAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    delegate.leaveObjectRefAssign(n, v, a, c, visitor);
  }
  /**
   * Visit an ObjectRef Op/Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitObjectRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) {
    return delegate.visitObjectRefAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit an ObjectRef Op/Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   */
  protected void leaveObjectRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) {
    delegate.leaveObjectRefAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit a BlockExpr Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitBlockExprAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    return delegate.visitBlockExprAssign(n, v, a, c, visitor);
  }
  /**
   * Visit a BlockExpr Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   */
  protected void leaveBlockExprAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    delegate.leaveBlockExprAssign(n, v, a, c, visitor);
  }
  /**
   * Visit a BlockExpr Op/Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitBlockExprAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) {
    return delegate.visitBlockExprAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit a BlockExpr Op/Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   */
  protected void leaveBlockExprAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) {
    delegate.leaveBlockExprAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit a Var Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitVarAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    return delegate.visitVarAssign(n, v, a, c, visitor);
  }
  /**
   * Visit a Var Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   */
  protected void leaveVarAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    delegate.leaveVarAssign(n, v, a, c, visitor);
  }
  /**
   * Visit a Var Op/Assignment node after visiting the RHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitVarAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) {
    return delegate.visitVarAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit a Var Op/Assignment node after visiting the LHS.
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   */
  protected void leaveVarAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) {
    delegate.leaveVarAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit a Switch node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitSwitch(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitSwitch(n, c, visitor);
  }
  /**
   * Visit a Switch node after processing the switch value.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveSwitchValue(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveSwitchValue(n, c, visitor);
  }
  /**
   * Leave a Switch node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveSwitch(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveSwitch(n, c, visitor);
  }
  /**
   * Visit a Throw node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitThrow(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitThrow(n, c, visitor);
  }
  /**
   * Leave a Throw node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveThrow(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveThrow(n, c, visitor);
  }
  /**
   * Visit a Catch node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitCatch(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitCatch(n, c, visitor);
  }
  /**
   * Leave a Catch node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveCatch(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveCatch(n, c, visitor);
  }
  /**
   * Visit an Unwind node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitUnwind(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitUnwind(n, c, visitor);
  }
  /**
   * Leave an Unwind node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveUnwind(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveUnwind(n, c, visitor);
  }
  /**
   * Visit a Try node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitTry(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitTry(n, c, visitor);
  }
  /**
   * Visit a Try node after processing the try block.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveTryBlock(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveTryBlock(n, c, visitor);
  }
  /**
   * Leave a Try node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveTry(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveTry(n, c, visitor);
  }
  /**
   * Visit an Empty node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitEmpty(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitEmpty(n, c, visitor);
  }
  /**
   * Leave an Empty node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveEmpty(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveEmpty(n, c, visitor);
  }
  /**
   * Visit a Primitive node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitPrimitive(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitPrimitive(n, c, visitor);
  }
  /**
   * Leave a Primitive node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leavePrimitive(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leavePrimitive(n, c, visitor);
  }
  /**
   * Visit a Void node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitVoid(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitVoid(n, c, visitor);
  }
  /**
   * Leave a Void node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveVoid(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveVoid(n, c, visitor);
  }
  /**
   * Visit a Cast node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitCast(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitCast(n, c, visitor);
  }
  /**
   * Leave a Cast node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveCast(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveCast(n, c, visitor);
  }
  /**
   * Visit an InstanceOf node.
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  protected boolean visitInstanceOf(CAstNode n, Context c, CAstVisitor visitor) {
    return delegate.visitInstanceOf(n, c, visitor);
  }
  /**
   * Leave an InstanceOf node.
   * @param n the node to process
   * @param c a visitor-specific context
   */
  protected void leaveInstanceOf(CAstNode n, Context c, CAstVisitor visitor) {
    delegate.leaveInstanceOf(n, c, visitor);
  }
}
