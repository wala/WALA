/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.translator;

import org.mozilla.javascript.ast.ArrayComprehension;
import org.mozilla.javascript.ast.ArrayComprehensionLoop;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.BreakStatement;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.Comment;
import org.mozilla.javascript.ast.ConditionalExpression;
import org.mozilla.javascript.ast.ContinueStatement;
import org.mozilla.javascript.ast.DoLoop;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.EmptyExpression;
import org.mozilla.javascript.ast.EmptyStatement;
import org.mozilla.javascript.ast.ErrorNode;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Jump;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Label;
import org.mozilla.javascript.ast.LabeledStatement;
import org.mozilla.javascript.ast.LetNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NewExpression;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.RegExpLiteral;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.SwitchCase;
import org.mozilla.javascript.ast.SwitchStatement;
import org.mozilla.javascript.ast.Symbol;
import org.mozilla.javascript.ast.ThrowStatement;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.UpdateExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;
import org.mozilla.javascript.ast.WhileLoop;
import org.mozilla.javascript.ast.WithStatement;
import org.mozilla.javascript.ast.XmlDotQuery;
import org.mozilla.javascript.ast.XmlElemRef;
import org.mozilla.javascript.ast.XmlExpression;
import org.mozilla.javascript.ast.XmlFragment;
import org.mozilla.javascript.ast.XmlLiteral;
import org.mozilla.javascript.ast.XmlMemberGet;
import org.mozilla.javascript.ast.XmlPropRef;
import org.mozilla.javascript.ast.XmlRef;
import org.mozilla.javascript.ast.XmlString;
import org.mozilla.javascript.ast.Yield;

public abstract class TypedNodeVisitor<R, A> {

  public R visit(AstNode node, A arg) {
    if (node instanceof ArrayComprehension) {
      return visitArrayComprehension((ArrayComprehension) node, arg);
    } else if (node instanceof WhileLoop) {
      return visitWhileLoop((WhileLoop) node, arg);
    } else if (node instanceof ArrayComprehensionLoop) {
      return visitArrayComprehensionLoop((ArrayComprehensionLoop) node, arg);
    } else if (node instanceof ArrayLiteral) {
      return visitArrayLiteral((ArrayLiteral) node, arg);
    } else if (node instanceof Assignment) {
      return visitAssignment((Assignment) node, arg);
    } else if (node instanceof AstRoot) {
      return visitAstRoot((AstRoot) node, arg);
    } else if (node instanceof Block) {
      return visitBlock((Block) node, arg);
    } else if (node instanceof BreakStatement) {
      return visitBreakStatement((BreakStatement) node, arg);
    } else if (node instanceof CatchClause) {
      return visitCatchClause((CatchClause) node, arg);
    } else if (node instanceof Comment) {
      return visitComment((Comment) node, arg);
    } else if (node instanceof ConditionalExpression) {
      return visitConditionalExpression((ConditionalExpression) node, arg);
    } else if (node instanceof ContinueStatement) {
      return visitContinueStatement((ContinueStatement) node, arg);
    } else if (node instanceof DoLoop) {
      return visitDoLoop((DoLoop) node, arg);
    } else if (node instanceof ElementGet) {
      return visitElementGet((ElementGet) node, arg);
    } else if (node instanceof EmptyExpression) {
      return visitEmptyExpression((EmptyExpression) node, arg);
    } else if (node instanceof EmptyStatement) {
      return visitEmptyStatement((EmptyStatement) node, arg);
    } else if (node instanceof ErrorNode) {
      return visitErrorNode((ErrorNode) node, arg);
    } else if (node instanceof ExpressionStatement) {
      return visitExpressionStatement((ExpressionStatement) node, arg);
    } else if (node instanceof ForInLoop) {
      return visitForInLoop((ForInLoop) node, arg);
    } else if (node instanceof ForLoop) {
      return visitForLoop((ForLoop) node, arg);
    } else if (node instanceof NewExpression) {
      return visitNewExpression((NewExpression) node, arg);
    } else if (node instanceof FunctionCall) {
      return visitFunctionCall((FunctionCall) node, arg);
    } else if (node instanceof FunctionNode) {
      return visitFunctionNode((FunctionNode) node, arg);
    } else if (node instanceof IfStatement) {
      return visitIfStatement((IfStatement) node, arg);
    } else if (node instanceof KeywordLiteral) {
      return visitKeywordLiteral((KeywordLiteral) node, arg);
    } else if (node instanceof Label) {
      return visitLabel((Label) node, arg);
    } else if (node instanceof LabeledStatement) {
      return visitLabeledStatement((LabeledStatement) node, arg);
    } else if (node instanceof LetNode) {
      return visitLetNode((LetNode) node, arg);
    } else if (node instanceof Name) {
      return visitName((Name) node, arg);
    } else if (node instanceof NumberLiteral) {
      return visitNumberLiteral((NumberLiteral) node, arg);
    } else if (node instanceof ObjectLiteral) {
      return visitObjectLiteral((ObjectLiteral) node, arg);
    } else if (node instanceof ObjectProperty) {
      return visitObjectProperty((ObjectProperty) node, arg);
    } else if (node instanceof ParenthesizedExpression) {
      return visitParenthesizedExpression((ParenthesizedExpression) node, arg);
    } else if (node instanceof PropertyGet) {
      return visitPropertyGet((PropertyGet) node, arg);
    } else if (node instanceof RegExpLiteral) {
      return visitRegExpLiteral((RegExpLiteral) node, arg);
    } else if (node instanceof ReturnStatement) {
      return visitReturnStatement((ReturnStatement) node, arg);
    } else if (node instanceof Scope) {
      return visitScope((Scope) node, arg);
    } else if (node instanceof ScriptNode) {
      return visitScriptNode((ScriptNode) node, arg);
    } else if (node instanceof StringLiteral) {
      return visitStringLiteral((StringLiteral) node, arg);
    } else if (node instanceof SwitchCase) {
      return visitSwitchCase((SwitchCase) node, arg);
    } else if (node instanceof SwitchStatement) {
      return visitSwitchStatement((SwitchStatement) node, arg);
    } else if (node instanceof ThrowStatement) {
      return visitThrowStatement((ThrowStatement) node, arg);
    } else if (node instanceof TryStatement) {
      return visitTryStatement((TryStatement) node, arg);
    } else if (node instanceof UnaryExpression) {
      return visitUnaryExpression((UnaryExpression) node, arg);
    } else if (node instanceof UpdateExpression) {
      return visitUpdateExpression((UpdateExpression) node, arg);
    } else if (node instanceof VariableDeclaration) {
      return visitVariableDeclaration((VariableDeclaration) node, arg);
    } else if (node instanceof VariableInitializer) {
      return visitVariableInitializer((VariableInitializer) node, arg);
    } else if (node instanceof WithStatement) {
      return visitWithStatement((WithStatement) node, arg);
    } else if (node instanceof XmlDotQuery) {
      return visitXmlDotQuery((XmlDotQuery) node, arg);
    } else if (node instanceof XmlElemRef) {
      return visitXmlElemRef((XmlElemRef) node, arg);
    } else if (node instanceof XmlExpression) {
      return visitXmlExpression((XmlExpression) node, arg);
    } else if (node instanceof XmlLiteral) {
      return visitXmlLiteral((XmlLiteral) node, arg);
    } else if (node instanceof XmlMemberGet) {
      return visitXmlMemberGet((XmlMemberGet) node, arg);
    } else if (node instanceof XmlPropRef) {
      return visitXmlPropRef((XmlPropRef) node, arg);
    } else if (node instanceof XmlString) {
      return visitXmlString((XmlString) node, arg);
    } else if (node instanceof Yield) {
      return visitYield((Yield) node, arg);
    } else if (node instanceof InfixExpression) {
      return visitInfixExpression((InfixExpression) node, arg);
    } else if (node instanceof Jump) {
      return visitJump((Jump) node, arg);
    } else {
      throw new Error("unexpected node type " + node.getClass().getName());
    }
  }

  public abstract R visitArrayComprehension(ArrayComprehension node, A arg);

  public abstract R visitArrayComprehensionLoop(ArrayComprehensionLoop node, A arg);

  public abstract R visitArrayLiteral(ArrayLiteral node, A arg);

  public abstract R visitAssignment(Assignment node, A arg);

  public abstract R visitAstRoot(AstRoot node, A arg);

  public abstract R visitBlock(Block node, A arg);

  public abstract R visitBreakStatement(BreakStatement node, A arg);

  public abstract R visitCatchClause(CatchClause node, A arg);

  public abstract R visitComment(Comment node, A arg);

  public abstract R visitConditionalExpression(ConditionalExpression node, A arg);

  public abstract R visitContinueStatement(ContinueStatement node, A arg);

  public abstract R visitDoLoop(DoLoop node, A arg);

  public abstract R visitElementGet(ElementGet node, A arg);

  public abstract R visitEmptyExpression(EmptyExpression node, A arg);

  public abstract R visitEmptyStatement(EmptyStatement node, A arg);

  public abstract R visitErrorNode(ErrorNode node, A arg);

  public abstract R visitExpressionStatement(ExpressionStatement node, A arg);

  public abstract R visitForInLoop(ForInLoop node, A arg);

  public abstract R visitForLoop(ForLoop node, A arg);

  public abstract R visitFunctionCall(FunctionCall node, A arg);

  public abstract R visitFunctionNode(FunctionNode node, A arg);

  public abstract R visitIfStatement(IfStatement node, A arg);

  public abstract R visitInfixExpression(InfixExpression node, A arg);

  public abstract R visitJump(Jump node, A arg);

  public abstract R visitKeywordLiteral(KeywordLiteral node, A arg);

  public abstract R visitLabel(Label node, A arg);

  public abstract R visitLabeledStatement(LabeledStatement node, A arg);

  public abstract R visitLetNode(LetNode node, A arg);

  public abstract R visitName(Name node, A arg);

  public abstract R visitNewExpression(NewExpression node, A arg);

  public abstract R visitNumberLiteral(NumberLiteral node, A arg);

  public abstract R visitObjectLiteral(ObjectLiteral node, A arg);

  public abstract R visitObjectProperty(ObjectProperty node, A arg);

  public abstract R visitParenthesizedExpression(ParenthesizedExpression node, A arg);

  public abstract R visitPropertyGet(PropertyGet node, A arg);

  public abstract R visitRegExpLiteral(RegExpLiteral node, A arg);

  public abstract R visitReturnStatement(ReturnStatement node, A arg);

  public abstract R visitScope(Scope node, A arg);

  public abstract R visitScriptNode(ScriptNode node, A arg);

  public abstract R visitStringLiteral(StringLiteral node, A arg);

  public abstract R visitSwitchCase(SwitchCase node, A arg);

  public abstract R visitSwitchStatement(SwitchStatement node, A arg);

  public abstract R visitSymbol(Symbol node, A arg);

  public abstract R visitThrowStatement(ThrowStatement node, A arg);

  public abstract R visitTryStatement(TryStatement node, A arg);

  public abstract R visitUnaryExpression(UnaryExpression node, A arg);

  public abstract R visitUpdateExpression(UpdateExpression node, A arg);

  public abstract R visitVariableDeclaration(VariableDeclaration node, A arg);

  public abstract R visitVariableInitializer(VariableInitializer node, A arg);

  public abstract R visitWhileLoop(WhileLoop node, A arg);

  public abstract R visitWithStatement(WithStatement node, A arg);

  public abstract R visitXmlDotQuery(XmlDotQuery node, A arg);

  public abstract R visitXmlElemRef(XmlElemRef node, A arg);

  public abstract R visitXmlExpression(XmlExpression node, A arg);

  public abstract R visitXmlFragment(XmlFragment node, A arg);

  public abstract R visitXmlLiteral(XmlLiteral node, A arg);

  public abstract R visitXmlMemberGet(XmlMemberGet node, A arg);

  public abstract R visitXmlPropRef(XmlPropRef node, A arg);

  public abstract R visitXmlRef(XmlRef node, A arg);

  public abstract R visitXmlString(XmlString node, A arg);

  public abstract R visitYield(Yield node, A arg);
}
