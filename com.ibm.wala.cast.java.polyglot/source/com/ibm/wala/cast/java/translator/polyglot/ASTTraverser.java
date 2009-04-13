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
 * Created on Sep 1, 2005
 */
package com.ibm.wala.cast.java.translator.polyglot;

import polyglot.ast.ArrayAccess;
import polyglot.ast.ArrayAccessAssign;
import polyglot.ast.ArrayInit;
import polyglot.ast.ArrayTypeNode;
import polyglot.ast.Assert;
import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.BooleanLit;
import polyglot.ast.Branch;
import polyglot.ast.Call;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Case;
import polyglot.ast.Cast;
import polyglot.ast.Catch;
import polyglot.ast.CharLit;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassLit;
import polyglot.ast.Conditional;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Do;
import polyglot.ast.Empty;
import polyglot.ast.Eval;
import polyglot.ast.Field;
import polyglot.ast.FieldAssign;
import polyglot.ast.FieldDecl;
import polyglot.ast.FloatLit;
import polyglot.ast.For;
import polyglot.ast.Formal;
import polyglot.ast.If;
import polyglot.ast.Import;
import polyglot.ast.Initializer;
import polyglot.ast.Instanceof;
import polyglot.ast.IntLit;
import polyglot.ast.Labeled;
import polyglot.ast.Local;
import polyglot.ast.LocalAssign;
import polyglot.ast.LocalClassDecl;
import polyglot.ast.LocalDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.New;
import polyglot.ast.NewArray;
import polyglot.ast.Node;
import polyglot.ast.NullLit;
import polyglot.ast.PackageNode;
import polyglot.ast.Return;
import polyglot.ast.Special;
import polyglot.ast.StringLit;
import polyglot.ast.Switch;
import polyglot.ast.SwitchBlock;
import polyglot.ast.Synchronized;
import polyglot.ast.Throw;
import polyglot.ast.Try;
import polyglot.ast.Unary;
import polyglot.ast.While;

import com.ibm.wala.cast.java.translator.polyglot.PolyglotJava2CAstTranslator.MethodContext;
import com.ibm.wala.cast.java.translator.polyglot.PolyglotJava2CAstTranslator.WalkContext;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.util.debug.Assertions;

/**
 * Wrapper for the logic (nasty cascaded instanceof tests) necessary to visit a Polyglot AST and dispatch to the appropriate
 * TranslatingVisitor methods for each AST node type.
 * 
 * @author rfuhrer
 */
public class ASTTraverser {
  protected ASTTraverser() {
  }

  public static CAstNode visit(Node n, TranslatingVisitor tv, WalkContext wc) {
    if (n instanceof MethodDecl) {
      return tv.visit((MethodDecl) n, (MethodContext) wc);
    } else if (n instanceof ConstructorDecl) {
      return tv.visit((ConstructorDecl) n, (MethodContext) wc);
    } else if (n instanceof FieldDecl) {
      return tv.visit((FieldDecl) n, (MethodContext) wc);
    } else if (n instanceof Import) {
      return tv.visit((Import) n, wc);
    } else if (n instanceof PackageNode) {
      return tv.visit((PackageNode) n, wc);
    } else if (n instanceof CanonicalTypeNode) {
      return tv.visit((CanonicalTypeNode) n, wc);
    } else if (n instanceof ArrayTypeNode) {
      return tv.visit((ArrayTypeNode) n, wc);
    } else if (n instanceof ArrayInit) {
      return tv.visit((ArrayInit) n, wc);
    } else if (n instanceof ArrayAccessAssign) {
      return tv.visit((ArrayAccessAssign) n, wc);
    } else if (n instanceof FieldAssign) {
      return tv.visit((FieldAssign) n, wc);
    } else if (n instanceof LocalAssign) {
      return tv.visit((LocalAssign) n, wc);
    } else if (n instanceof Binary) {
      return tv.visit((Binary) n, wc);
    } else if (n instanceof Call) {
      return tv.visit((Call) n, wc);
    } else if (n instanceof ConstructorCall) {
      return tv.visit((ConstructorCall) n, wc);
    } else if (n instanceof Cast) {
      return tv.visit((Cast) n, wc);
    } else if (n instanceof Conditional) {
      return tv.visit((Conditional) n, wc);
    } else if (n instanceof Instanceof) {
      return tv.visit((Instanceof) n, wc);
    } else if (n instanceof BooleanLit) {
      return tv.visit((BooleanLit) n, wc);
    } else if (n instanceof ClassLit) {
      return tv.visit((ClassLit) n, wc);
    } else if (n instanceof FloatLit) {
      return tv.visit((FloatLit) n, wc);
    } else if (n instanceof NullLit) {
      return tv.visit((NullLit) n, wc);
    } else if (n instanceof CharLit) {
      return tv.visit((CharLit) n, wc);
    } else if (n instanceof IntLit) {
      return tv.visit((IntLit) n, wc);
    } else if (n instanceof StringLit) {
      return tv.visit((StringLit) n, wc);
    } else if (n instanceof New) {
      return tv.visit((New) n, wc);
    } else if (n instanceof NewArray) {
      return tv.visit((NewArray) n, wc);
    } else if (n instanceof Special) {
      return tv.visit((Special) n, wc);
    } else if (n instanceof Unary) {
      return tv.visit((Unary) n, wc);
    } else if (n instanceof ArrayAccess) {
      return tv.visit((ArrayAccess) n, wc);
    } else if (n instanceof Field) {
      return tv.visit((Field) n, wc);
    } else if (n instanceof Local) {
      return tv.visit((Local) n, wc);
    } else if (n instanceof ClassBody) {
      return tv.visit((ClassBody) n, wc);
    } else if (n instanceof ClassDecl) {
      return tv.visit((ClassDecl) n, wc);
    } else if (n instanceof Initializer) {
      return tv.visit((Initializer) n, wc);
    } else if (n instanceof Assert) {
      return tv.visit((Assert) n, wc);
    } else if (n instanceof Branch) {
      return tv.visit((Branch) n, wc);
    } else if (n instanceof SwitchBlock) { // must test for this one before Block
      return tv.visit((SwitchBlock) n, wc);
    } else if (n instanceof Block) { // must test for this one before Block
      return tv.visit((Block) n, wc);
    } else if (n instanceof Catch) {
      return tv.visit((Catch) n, wc);
    } else if (n instanceof If) {
      return tv.visit((If) n, wc);
    } else if (n instanceof Labeled) {
      return tv.visit((Labeled) n, wc);
    } else if (n instanceof LocalClassDecl) {
      return tv.visit((LocalClassDecl) n, wc);
    } else if (n instanceof Do) {
      return tv.visit((Do) n, wc);
    } else if (n instanceof For) {
      return tv.visit((For) n, wc);
    } else if (n instanceof While) {
      return tv.visit((While) n, wc);
    } else if (n instanceof Switch) {
      return tv.visit((Switch) n, wc);
    } else if (n instanceof Synchronized) {
      return tv.visit((Synchronized) n, wc);
    } else if (n instanceof Try) {
      return tv.visit((Try) n, wc);
    } else if (n instanceof Empty) {
      return tv.visit((Empty) n, wc);
    } else if (n instanceof Eval) {
      return tv.visit((Eval) n, wc);
    } else if (n instanceof LocalDecl) {
      return tv.visit((LocalDecl) n, wc);
    } else if (n instanceof Return) {
      return tv.visit((Return) n, wc);
    } else if (n instanceof Case) {
      return tv.visit((Case) n, wc);
    } else if (n instanceof Throw) {
      return tv.visit((Throw) n, wc);
    } else if (n instanceof Formal) {
      return tv.visit((Formal) n, wc);
    } else {
      Assertions.UNREACHABLE("Unhandled node " + n + " of type " + n.getClass().getName() + " in ASTTraverser.visit().");
      return null;
    }
  }
}
