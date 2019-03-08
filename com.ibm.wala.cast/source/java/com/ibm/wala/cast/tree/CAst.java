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
package com.ibm.wala.cast.tree;

import java.util.List;

/**
 * The main interface for creating CAPA Abstract Syntax Trees. This interface provides essentially a
 * factory for creating AST nodes in a tree structure. There is no strong assumption about the
 * meaning of specific nodes; however, the `kind' argument to a makeNode call should be a value from
 * the constants in the CAstNode interface. The other arguments to makeNode calls are child nodes.
 * The structure of the tree is a matter of agreement between providers and consumers of specific
 * trees.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public interface CAst {

  /** Make a node of type kind with no children. */
  CAstNode makeNode(int kind);

  /** Make a node of type kind with one child. */
  CAstNode makeNode(int kind, CAstNode c1);

  /** Make a node of type kind with two children. */
  CAstNode makeNode(int kind, CAstNode c1, CAstNode c2);

  /** Make a node of type kind with three children. */
  CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3);

  /** Make a node of type kind with four children. */
  CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4);
  /** Make a node of type kind with five children. */
  CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4, CAstNode c5);

  /** Make a node of type kind with six children. */
  CAstNode makeNode(
      int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4, CAstNode c5, CAstNode c6);

  /** Make a node of type kind specifying an array of children. */
  CAstNode makeNode(int kind, CAstNode... cs);

  /** Make a node of type kind giving a first child and array of the rest. */
  CAstNode makeNode(int kind, CAstNode firstChild, CAstNode[] otherChildren);

  /** Make a node of type kind specifying a list of children. */
  CAstNode makeNode(int kind, List<CAstNode> cs);

  /** Make a boolean constant node. */
  CAstNode makeConstant(boolean value);

  /** Make a char constant node. */
  CAstNode makeConstant(char value);

  /** Make a short integer constant node. */
  CAstNode makeConstant(short value);

  /** Make an integer constant node. */
  CAstNode makeConstant(int value);

  /** Make a long integer constant node. */
  CAstNode makeConstant(long value);

  /** Make a double-precision floating point constant node. */
  CAstNode makeConstant(double value);

  /** Make a single-precision floating point constant node. */
  CAstNode makeConstant(float value);

  /** Make an arbitrary object constant node. */
  CAstNode makeConstant(Object value);

  /** Make a new identifier, unqiue to this CAst instance. */
  String makeUnique();
}
