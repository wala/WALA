/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.tree.visit;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;

/**
 * Extend {@link CAstVisitor}{@code <C>} to delegate unimplemented functionality to another visitor.
 * Needed to work around Java's retarded multiple inheritance rules. TODO: document me.
 *
 * @author Igor Peshansky
 */
public abstract class DelegatingCAstVisitor<C extends CAstVisitor.Context> extends CAstVisitor<C> {

  /**
   * Construct a context for a File entity or delegate by default.
   *
   * @param context a visitor-specific context in which this file was visited
   * @param n the file entity
   */
  @Override
  protected C makeFileContext(C context, CAstEntity n) {
    return delegate.makeFileContext(context, n);
  }
  /**
   * Construct a context for a Type entity or delegate by default.
   *
   * @param context a visitor-specific context in which this type was visited
   * @param n the type entity
   */
  @Override
  protected C makeTypeContext(C context, CAstEntity n) {
    return delegate.makeTypeContext(context, n);
  }
  /**
   * Construct a context for a Code entity or delegate by default.
   *
   * @param context a visitor-specific context in which the code was visited
   * @param n the code entity
   */
  @Override
  protected C makeCodeContext(C context, CAstEntity n) {
    return delegate.makeCodeContext(context, n);
  }

  /**
   * Construct a context for a LocalScope node or delegate by default.
   *
   * @param context a visitor-specific context in which the local scope was visited
   * @param n the local scope node
   */
  @Override
  protected C makeLocalContext(C context, CAstNode n) {
    return delegate.makeLocalContext(context, n);
  }
  /**
   * Construct a context for an Unwind node or delegate by default.
   *
   * @param context a visitor-specific context in which the unwind was visited
   * @param n the unwind node
   */
  @Override
  protected C makeUnwindContext(C context, CAstNode n, CAstVisitor<C> visitor) {
    return delegate.makeUnwindContext(context, n, visitor);
  }

  /**
   * Get the parent entity for a given entity.
   *
   * @param entity the child entity
   * @return the parent entity for the given entity
   */
  @Override
  protected CAstEntity getParent(CAstEntity entity) {
    return delegate.getParent(entity);
  }

  /**
   * Set the parent entity for a given entity.
   *
   * @param entity the child entity
   * @param parent the parent entity
   */
  @Override
  protected void setParent(CAstEntity entity, CAstEntity parent) {
    delegate.setParent(entity, parent);
  }

  private final CAstVisitor<C> delegate;

  protected final CAstVisitor<C> delegate() {
    return delegate;
  }

  /**
   * Delegating {@link CAstVisitor}{@code <C>} constructor. Needs to have a valid (non-null)
   * delegate visitor.
   *
   * @param delegate the visitor to delegate to for default implementation
   */
  protected DelegatingCAstVisitor(CAstVisitor<C> delegate) {
    assert delegate != null;
    this.delegate = delegate;
  }

  /**
   * Entity processing hook; sub-classes are expected to override if they introduce new entity
   * types. Should invoke super.doVisitEntity() for unprocessed entities.
   *
   * @return true if entity was handled
   */
  @Override
  protected boolean doVisitEntity(CAstEntity n, C context, CAstVisitor<C> visitor) {
    return delegate.doVisitEntity(n, context, visitor);
  }

  /**
   * Enter the entity visitor.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean enterEntity(CAstEntity n, C context, CAstVisitor<C> visitor) {
    return delegate.enterEntity(n, context, visitor);
  }
  /**
   * Post-process an entity after visiting it.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   */
  @Override
  protected void postProcessEntity(CAstEntity n, C context, CAstVisitor<C> visitor) {
    delegate.postProcessEntity(n, context, visitor);
  }

  /**
   * Visit any entity. Override only this to change behavior for all entities.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  public boolean visitEntity(CAstEntity n, C context, CAstVisitor<C> visitor) {
    return delegate.visitEntity(n, context, visitor);
  }
  /**
   * Leave any entity. Override only this to change behavior for all entities.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   */
  @Override
  public void leaveEntity(CAstEntity n, C context, CAstVisitor<C> visitor) {
    delegate.leaveEntity(n, context, visitor);
  }

  /**
   * Visit a File entity.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param fileContext a visitor-specific context for this file
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitFileEntity(
      CAstEntity n, C context, C fileContext, CAstVisitor<C> visitor) {
    return delegate.visitFileEntity(n, context, fileContext, visitor);
  }
  /**
   * Leave a File entity.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param fileContext a visitor-specific context for this file
   */
  @Override
  protected void leaveFileEntity(CAstEntity n, C context, C fileContext, CAstVisitor<C> visitor) {
    delegate.leaveFileEntity(n, context, fileContext, visitor);
  }
  /**
   * Visit a Field entity.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitFieldEntity(CAstEntity n, C context, CAstVisitor<C> visitor) {
    return delegate.visitFieldEntity(n, context, visitor);
  }
  /**
   * Leave a Field entity.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   */
  @Override
  protected void leaveFieldEntity(CAstEntity n, C context, CAstVisitor<C> visitor) {
    delegate.leaveFieldEntity(n, context, visitor);
  }
  /**
   * Visit a Type entity.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param typeContext a visitor-specific context for this type
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitTypeEntity(
      CAstEntity n, C context, C typeContext, CAstVisitor<C> visitor) {
    return delegate.visitTypeEntity(n, context, typeContext, visitor);
  }
  /**
   * Leave a Type entity.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param typeContext a visitor-specific context for this type
   */
  @Override
  protected void leaveTypeEntity(CAstEntity n, C context, C typeContext, CAstVisitor<C> visitor) {
    delegate.leaveTypeEntity(n, context, typeContext, visitor);
  }
  /**
   * Visit a Function entity.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this function
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitFunctionEntity(
      CAstEntity n, C context, C codeContext, CAstVisitor<C> visitor) {
    return delegate.visitFunctionEntity(n, context, codeContext, visitor);
  }
  /**
   * Leave a Function entity.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this function
   */
  @Override
  protected void leaveFunctionEntity(
      CAstEntity n, C context, C codeContext, CAstVisitor<C> visitor) {
    delegate.leaveFunctionEntity(n, context, codeContext, visitor);
  }
  /**
   * Visit a Script entity.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this script
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitScriptEntity(
      CAstEntity n, C context, C codeContext, CAstVisitor<C> visitor) {
    return delegate.visitScriptEntity(n, context, codeContext, visitor);
  }
  /**
   * Leave a Script entity.
   *
   * @param n the entity to process
   * @param context a visitor-specific context
   * @param codeContext a visitor-specific context for this script
   */
  @Override
  protected void leaveScriptEntity(CAstEntity n, C context, C codeContext, CAstVisitor<C> visitor) {
    delegate.leaveScriptEntity(n, context, codeContext, visitor);
  }

  /**
   * Node processing hook; sub-classes are expected to override if they introduce new node types.
   * Should invoke super.doVisit() for unprocessed nodes.
   *
   * @return true if node was handled
   */
  @Override
  protected boolean doVisit(CAstNode n, C context, CAstVisitor<C> visitor) {
    return delegate.doVisit(n, context, visitor);
  }

  /**
   * Enter the node visitor.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean enterNode(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.enterNode(n, c, visitor);
  }
  /**
   * Post-process a node after visiting it.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void postProcessNode(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.postProcessNode(n, c, visitor);
  }

  /**
   * Visit any node. Override only this to change behavior for all nodes.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  public boolean visitNode(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitNode(n, c, visitor);
  }
  /**
   * Leave any node. Override only this to change behavior for all nodes.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  public void leaveNode(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveNode(n, c, visitor);
  }

  /**
   * Visit a FunctionExpr node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitFunctionExpr(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitFunctionExpr(n, c, visitor);
  }
  /**
   * Leave a FunctionExpr node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveFunctionExpr(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveFunctionExpr(n, c, visitor);
  }
  /**
   * Visit a FunctionStmt node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitFunctionStmt(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitFunctionStmt(n, c, visitor);
  }
  /**
   * Leave a FunctionStmt node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveFunctionStmt(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveFunctionStmt(n, c, visitor);
  }
  /**
   * Visit a ClassStmt node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitClassStmt(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitClassStmt(n, c, visitor);
  }
  /**
   * Leave a FunctionStmt node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveClassStmt(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveClassStmt(n, c, visitor);
  }
  /**
   * Visit a LocalScope node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitLocalScope(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitLocalScope(n, c, visitor);
  }
  /**
   * Leave a LocalScope node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveLocalScope(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveLocalScope(n, c, visitor);
  }
  /**
   * Visit a BlockExpr node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitBlockExpr(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitBlockExpr(n, c, visitor);
  }
  /**
   * Leave a BlockExpr node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveBlockExpr(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveBlockExpr(n, c, visitor);
  }
  /**
   * Visit a BlockStmt node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitBlockStmt(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitBlockStmt(n, c, visitor);
  }
  /**
   * Leave a BlockStmt node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveBlockStmt(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveBlockStmt(n, c, visitor);
  }
  /**
   * Visit a Loop node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitLoop(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitLoop(n, c, visitor);
  }
  /**
   * Visit a Loop node after processing the loop header.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveLoopHeader(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveLoopHeader(n, c, visitor);
  }
  /**
   * Leave a Loop node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveLoop(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveLoop(n, c, visitor);
  }
  /**
   * Visit a GetCaughtException node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitGetCaughtException(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitGetCaughtException(n, c, visitor);
  }
  /**
   * Leave a GetCaughtException node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveGetCaughtException(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveGetCaughtException(n, c, visitor);
  }
  /**
   * Visit a This node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitThis(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitThis(n, c, visitor);
  }
  /**
   * Leave a This node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveThis(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveThis(n, c, visitor);
  }
  /**
   * Visit a Super node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitSuper(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitSuper(n, c, visitor);
  }
  /**
   * Leave a Super node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveSuper(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveSuper(n, c, visitor);
  }
  /**
   * Visit a Call node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitCall(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitCall(n, c, visitor);
  }
  /**
   * Leave a Call node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveCall(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveCall(n, c, visitor);
  }
  /**
   * Visit a Var node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitVar(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitVar(n, c, visitor);
  }
  /**
   * Leave a Var node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveVar(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveVar(n, c, visitor);
  }
  /**
   * Visit a Constant node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitConstant(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitConstant(n, c, visitor);
  }
  /**
   * Leave a Constant node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveConstant(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveConstant(n, c, visitor);
  }
  /**
   * Visit a BinaryExpr node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitBinaryExpr(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitBinaryExpr(n, c, visitor);
  }
  /**
   * Leave a BinaryExpr node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveBinaryExpr(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveBinaryExpr(n, c, visitor);
  }
  /**
   * Visit a UnaryExpr node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitUnaryExpr(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitUnaryExpr(n, c, visitor);
  }
  /**
   * Leave a UnaryExpr node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveUnaryExpr(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveUnaryExpr(n, c, visitor);
  }
  /**
   * Visit an ArrayLength node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitArrayLength(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitArrayLength(n, c, visitor);
  }
  /**
   * Leave an ArrayLength node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveArrayLength(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveArrayLength(n, c, visitor);
  }
  /**
   * Visit an ArrayRef node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitArrayRef(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitArrayRef(n, c, visitor);
  }
  /**
   * Leave an ArrayRef node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveArrayRef(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveArrayRef(n, c, visitor);
  }
  /**
   * Visit a DeclStmt node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitDeclStmt(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitDeclStmt(n, c, visitor);
  }
  /**
   * Leave a DeclStmt node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveDeclStmt(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveDeclStmt(n, c, visitor);
  }
  /**
   * Visit a Return node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitReturn(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitReturn(n, c, visitor);
  }
  /**
   * Leave a Return node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveReturn(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveReturn(n, c, visitor);
  }
  /**
   * Visit an Ifgoto node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitIfgoto(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitIfgoto(n, c, visitor);
  }
  /**
   * Leave an Ifgoto node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveIfgoto(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveIfgoto(n, c, visitor);
  }
  /**
   * Visit a Goto node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitGoto(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitGoto(n, c, visitor);
  }
  /**
   * Leave a Goto node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveGoto(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveGoto(n, c, visitor);
  }
  /**
   * Visit a LabelStmt node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitLabelStmt(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitLabelStmt(n, c, visitor);
  }
  /**
   * Leave a LabelStmt node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveLabelStmt(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveLabelStmt(n, c, visitor);
  }
  /**
   * Visit an IfStmt node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitIfStmt(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitIfStmt(n, c, visitor);
  }
  /**
   * Visit an IfStmt node after processing the condition.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveIfStmtCondition(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveIfStmtCondition(n, c, visitor);
  }
  /**
   * Visit an IfStmt node after processing the true clause.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveIfStmtTrueClause(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveIfStmtTrueClause(n, c, visitor);
  }
  /**
   * Leave an IfStmt node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveIfStmt(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveIfStmt(n, c, visitor);
  }
  /**
   * Visit an IfExpr node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitIfExpr(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitIfExpr(n, c, visitor);
  }
  /**
   * Visit an IfExpr node after processing the condition.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveIfExprCondition(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveIfExprCondition(n, c, visitor);
  }
  /**
   * Visit an IfExpr node after processing the true clause.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveIfExprTrueClause(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveIfExprTrueClause(n, c, visitor);
  }
  /**
   * Leave an IfExpr node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveIfExpr(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveIfExpr(n, c, visitor);
  }
  /**
   * Visit a New node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitNew(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitNew(n, c, visitor);
  }
  /**
   * Leave a New node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveNew(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveNew(n, c, visitor);
  }
  /**
   * Visit an ObjectLiteral node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitObjectLiteral(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitObjectLiteral(n, c, visitor);
  }
  /**
   * Visit an ObjectLiteral node after processing the {i}th field initializer.
   *
   * @param n the node to process
   * @param i the field position that was initialized
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveObjectLiteralFieldInit(CAstNode n, int i, C c, CAstVisitor<C> visitor) {
    delegate.leaveObjectLiteralFieldInit(n, i, c, visitor);
  }
  /**
   * Leave an ObjectLiteral node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveObjectLiteral(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveObjectLiteral(n, c, visitor);
  }
  /**
   * Visit an ArrayLiteral node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitArrayLiteral(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitArrayLiteral(n, c, visitor);
  }
  /**
   * Visit an ArrayLiteral node after processing the array object.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveArrayLiteralObject(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveArrayLiteralObject(n, c, visitor);
  }
  /**
   * Visit an ArrayLiteral node after processing the {i}th element initializer.
   *
   * @param n the node to process
   * @param i the index that was initialized
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveArrayLiteralInitElement(CAstNode n, int i, C c, CAstVisitor<C> visitor) {
    delegate.leaveArrayLiteralInitElement(n, i, c, visitor);
  }
  /**
   * Leave a ArrayLiteral node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveArrayLiteral(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveArrayLiteral(n, c, visitor);
  }
  /**
   * Visit an ObjectRef node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitObjectRef(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitObjectRef(n, c, visitor);
  }
  /**
   * Leave an ObjectRef node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveObjectRef(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveObjectRef(n, c, visitor);
  }
  /**
   * Visit an Assign node. Override only this to change behavior for all assignment nodes.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  public boolean visitAssign(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitAssign(n, c, visitor);
  }
  /**
   * Leave an Assign node. Override only this to change behavior for all assignment nodes.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  public void leaveAssign(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveAssign(n, c, visitor);
  }
  /**
   * Visit an ArrayRef Assignment node after visiting the RHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitArrayRefAssign(
      CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) {
    return delegate.visitArrayRefAssign(n, v, a, c, visitor);
  }
  /**
   * Visit an ArrayRef Assignment node after visiting the LHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveArrayRefAssign(
      CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) {
    delegate.leaveArrayRefAssign(n, v, a, c, visitor);
  }
  /**
   * Visit an ArrayRef Op/Assignment node after visiting the RHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitArrayRefAssignOp(
      CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) {
    return delegate.visitArrayRefAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit an ArrayRef Op/Assignment node after visiting the LHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveArrayRefAssignOp(
      CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) {
    delegate.leaveArrayRefAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit an ObjectRef Assignment node after visiting the RHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitObjectRefAssign(
      CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) {
    return delegate.visitObjectRefAssign(n, v, a, c, visitor);
  }
  /**
   * Visit an ObjectRef Assignment node after visiting the LHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveObjectRefAssign(
      CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) {
    delegate.leaveObjectRefAssign(n, v, a, c, visitor);
  }
  /**
   * Visit an ObjectRef Op/Assignment node after visiting the RHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitObjectRefAssignOp(
      CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) {
    return delegate.visitObjectRefAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit an ObjectRef Op/Assignment node after visiting the LHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveObjectRefAssignOp(
      CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) {
    delegate.leaveObjectRefAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit a BlockExpr Assignment node after visiting the RHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitBlockExprAssign(
      CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) {
    return delegate.visitBlockExprAssign(n, v, a, c, visitor);
  }
  /**
   * Visit a BlockExpr Assignment node after visiting the LHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveBlockExprAssign(
      CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) {
    delegate.leaveBlockExprAssign(n, v, a, c, visitor);
  }
  /**
   * Visit a BlockExpr Op/Assignment node after visiting the RHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitBlockExprAssignOp(
      CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) {
    return delegate.visitBlockExprAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit a BlockExpr Op/Assignment node after visiting the LHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveBlockExprAssignOp(
      CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) {
    delegate.leaveBlockExprAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit a Var Assignment node after visiting the RHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitVarAssign(
      CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) {
    return delegate.visitVarAssign(n, v, a, c, visitor);
  }
  /**
   * Visit a Var Assignment node after visiting the LHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveVarAssign(CAstNode n, CAstNode v, CAstNode a, C c, CAstVisitor<C> visitor) {
    delegate.leaveVarAssign(n, v, a, c, visitor);
  }
  /**
   * Visit a Var Op/Assignment node after visiting the RHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitVarAssignOp(
      CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) {
    return delegate.visitVarAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit a Var Op/Assignment node after visiting the LHS.
   *
   * @param n the LHS node to process
   * @param v the RHS node to process
   * @param a the assignment node to process
   * @param pre whether the value before the operation should be used
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveVarAssignOp(
      CAstNode n, CAstNode v, CAstNode a, boolean pre, C c, CAstVisitor<C> visitor) {
    delegate.leaveVarAssignOp(n, v, a, pre, c, visitor);
  }
  /**
   * Visit a Switch node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitSwitch(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitSwitch(n, c, visitor);
  }
  /**
   * Visit a Switch node after processing the switch value.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveSwitchValue(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveSwitchValue(n, c, visitor);
  }
  /**
   * Leave a Switch node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveSwitch(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveSwitch(n, c, visitor);
  }
  /**
   * Visit a Throw node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitThrow(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitThrow(n, c, visitor);
  }
  /**
   * Leave a Throw node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveThrow(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveThrow(n, c, visitor);
  }
  /**
   * Visit a Catch node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitCatch(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitCatch(n, c, visitor);
  }
  /**
   * Leave a Catch node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveCatch(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveCatch(n, c, visitor);
  }
  /**
   * Visit an Unwind node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitUnwind(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitUnwind(n, c, visitor);
  }
  /**
   * Leave an Unwind node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveUnwind(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveUnwind(n, c, visitor);
  }
  /**
   * Visit a Try node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitTry(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitTry(n, c, visitor);
  }
  /**
   * Visit a Try node after processing the try block.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveTryBlock(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveTryBlock(n, c, visitor);
  }
  /**
   * Leave a Try node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveTry(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveTry(n, c, visitor);
  }
  /**
   * Visit an Empty node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitEmpty(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitEmpty(n, c, visitor);
  }
  /**
   * Leave an Empty node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveEmpty(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveEmpty(n, c, visitor);
  }
  /**
   * Visit a Primitive node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitPrimitive(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitPrimitive(n, c, visitor);
  }
  /**
   * Leave a Primitive node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leavePrimitive(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leavePrimitive(n, c, visitor);
  }
  /**
   * Visit a Void node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitVoid(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitVoid(n, c, visitor);
  }
  /**
   * Leave a Void node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveVoid(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveVoid(n, c, visitor);
  }
  /**
   * Visit a Cast node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitCast(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitCast(n, c, visitor);
  }
  /**
   * Leave a Cast node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveCast(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveCast(n, c, visitor);
  }
  /**
   * Visit an InstanceOf node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitInstanceOf(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitInstanceOf(n, c, visitor);
  }
  /**
   * Leave an InstanceOf node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveInstanceOf(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveInstanceOf(n, c, visitor);
  }

  /**
   * Visit a LocalScope node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   * @return true if no further processing is needed
   */
  @Override
  protected boolean visitSpecialParentScope(CAstNode n, C c, CAstVisitor<C> visitor) {
    return delegate.visitSpecialParentScope(n, c, visitor);
  }
  /**
   * Leave a LocalScope node.
   *
   * @param n the node to process
   * @param c a visitor-specific context
   */
  @Override
  protected void leaveSpecialParentScope(CAstNode n, C c, CAstVisitor<C> visitor) {
    delegate.leaveSpecialParentScope(n, c, visitor);
  }
}
