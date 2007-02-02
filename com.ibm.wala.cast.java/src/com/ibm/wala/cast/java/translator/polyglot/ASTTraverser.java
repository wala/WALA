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

import polyglot.ast.*;

import com.ibm.wala.cast.java.translator.polyglot.PolyglotJava2CAstTranslator.*;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.util.debug.Assertions;

/**
 * Wrapper for the logic (nasty cascaded instanceof tests) necessary to visit a Polyglot AST
 * and dispatch to the appropriate TranslatingVisitor methods for each AST node type.
 * @author rfuhrer
 */
public class ASTTraverser {
    protected ASTTraverser() { }

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
	    Assertions.UNREACHABLE("Unhandled node type in ASTTraverser.visit(): "+n.getClass());
	    return null;
	}
    }
}
