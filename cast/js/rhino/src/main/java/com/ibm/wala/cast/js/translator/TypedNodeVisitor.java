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
    if (node instanceof ArrayComprehension arrayComprehension) {
      return visitArrayComprehension(arrayComprehension, arg);
    } else if (node instanceof WhileLoop whileLoop) {
      return visitWhileLoop(whileLoop, arg);
    } else if (node instanceof ArrayComprehensionLoop arrayComprehensionLoop) {
      return visitArrayComprehensionLoop(arrayComprehensionLoop, arg);
    } else if (node instanceof ArrayLiteral arrayLiteral) {
      return visitArrayLiteral(arrayLiteral, arg);
    } else if (node instanceof Assignment assignment) {
      return visitAssignment(assignment, arg);
    } else if (node instanceof AstRoot astRoot) {
      return visitAstRoot(astRoot, arg);
    } else if (node instanceof Block block) {
      return visitBlock(block, arg);
    } else if (node instanceof BreakStatement breakStatement) {
      return visitBreakStatement(breakStatement, arg);
    } else if (node instanceof CatchClause catchClause) {
      return visitCatchClause(catchClause, arg);
    } else if (node instanceof Comment comment) {
      return visitComment(comment, arg);
    } else if (node instanceof ConditionalExpression conditionalExpression) {
      return visitConditionalExpression(conditionalExpression, arg);
    } else if (node instanceof ContinueStatement continueStatement) {
      return visitContinueStatement(continueStatement, arg);
    } else if (node instanceof DoLoop doLoop) {
      return visitDoLoop(doLoop, arg);
    } else if (node instanceof ElementGet elementGet) {
      return visitElementGet(elementGet, arg);
    } else if (node instanceof EmptyExpression emptyExpression) {
      return visitEmptyExpression(emptyExpression, arg);
    } else if (node instanceof EmptyStatement emptyStatement) {
      return visitEmptyStatement(emptyStatement, arg);
    } else if (node instanceof ErrorNode errorNode) {
      return visitErrorNode(errorNode, arg);
    } else if (node instanceof ExpressionStatement expressionStatement) {
      return visitExpressionStatement(expressionStatement, arg);
    } else if (node instanceof ForInLoop forInLoop) {
      return visitForInLoop(forInLoop, arg);
    } else if (node instanceof ForLoop forLoop) {
      return visitForLoop(forLoop, arg);
    } else if (node instanceof NewExpression newExpression) {
      return visitNewExpression(newExpression, arg);
    } else if (node instanceof FunctionCall functionCall) {
      return visitFunctionCall(functionCall, arg);
    } else if (node instanceof FunctionNode functionNode) {
      return visitFunctionNode(functionNode, arg);
    } else if (node instanceof IfStatement ifStatement) {
      return visitIfStatement(ifStatement, arg);
    } else if (node instanceof KeywordLiteral keywordLiteral) {
      return visitKeywordLiteral(keywordLiteral, arg);
    } else if (node instanceof Label label) {
      return visitLabel(label, arg);
    } else if (node instanceof LabeledStatement labeledStatement) {
      return visitLabeledStatement(labeledStatement, arg);
    } else if (node instanceof LetNode letNode) {
      return visitLetNode(letNode, arg);
    } else if (node instanceof Name name) {
      return visitName(name, arg);
    } else if (node instanceof NumberLiteral numberLiteral) {
      return visitNumberLiteral(numberLiteral, arg);
    } else if (node instanceof ObjectLiteral objectLiteral) {
      return visitObjectLiteral(objectLiteral, arg);
    } else if (node instanceof ObjectProperty objectProperty) {
      return visitObjectProperty(objectProperty, arg);
    } else if (node instanceof ParenthesizedExpression parenthesizedExpression) {
      return visitParenthesizedExpression(parenthesizedExpression, arg);
    } else if (node instanceof PropertyGet propertyGet) {
      return visitPropertyGet(propertyGet, arg);
    } else if (node instanceof RegExpLiteral regExpLiteral) {
      return visitRegExpLiteral(regExpLiteral, arg);
    } else if (node instanceof ReturnStatement returnStatement) {
      return visitReturnStatement(returnStatement, arg);
    } else if (node instanceof SwitchStatement switchStatement) {
      // `SwitchStatement` extends `Scope`, so we must check for the former _before_ the latter.
      return visitSwitchStatement(switchStatement, arg);
    } else if (node instanceof Scope scope) {
      return visitScope(scope, arg);
    } else if (node instanceof ScriptNode scriptNode) {
      return visitScriptNode(scriptNode, arg);
    } else if (node instanceof StringLiteral stringLiteral) {
      return visitStringLiteral(stringLiteral, arg);
    } else if (node instanceof SwitchCase switchCase) {
      return visitSwitchCase(switchCase, arg);
    } else if (node instanceof ThrowStatement throwStatement) {
      return visitThrowStatement(throwStatement, arg);
    } else if (node instanceof TryStatement tryStatement) {
      return visitTryStatement(tryStatement, arg);
    } else if (node instanceof UnaryExpression unaryExpression) {
      return visitUnaryExpression(unaryExpression, arg);
    } else if (node instanceof UpdateExpression updateExpression) {
      return visitUpdateExpression(updateExpression, arg);
    } else if (node instanceof VariableDeclaration variableDeclaration) {
      return visitVariableDeclaration(variableDeclaration, arg);
    } else if (node instanceof VariableInitializer variableInitializer) {
      return visitVariableInitializer(variableInitializer, arg);
    } else if (node instanceof WithStatement withStatement) {
      return visitWithStatement(withStatement, arg);
    } else if (node instanceof XmlDotQuery xmlDotQuery) {
      return visitXmlDotQuery(xmlDotQuery, arg);
    } else if (node instanceof XmlElemRef xmlElemRef) {
      return visitXmlElemRef(xmlElemRef, arg);
    } else if (node instanceof XmlExpression xmlExpression) {
      return visitXmlExpression(xmlExpression, arg);
    } else if (node instanceof XmlLiteral xmlLiteral) {
      return visitXmlLiteral(xmlLiteral, arg);
    } else if (node instanceof XmlMemberGet xmlMemberGet) {
      return visitXmlMemberGet(xmlMemberGet, arg);
    } else if (node instanceof XmlPropRef xmlPropRef) {
      return visitXmlPropRef(xmlPropRef, arg);
    } else if (node instanceof XmlString xmlString) {
      return visitXmlString(xmlString, arg);
    } else if (node instanceof Yield yield) {
      return visitYield(yield, arg);
    } else if (node instanceof InfixExpression infixExpression) {
      return visitInfixExpression(infixExpression, arg);
    } else if (node instanceof Jump nodes) {
      return visitJump(nodes, arg);
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
