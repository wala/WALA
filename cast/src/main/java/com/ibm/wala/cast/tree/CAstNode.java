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
 * This interface represents nodes of CAPA Abstract Syntax Trees. It is a deliberately minimal
 * interface, simply assuming that the nodes form a tree and have some minimal state at each node.
 * In particular, a node has a kind---which should be one of the symbolic constants in this
 * file---and potentially has child nodes, a constant values, or possibly both.
 *
 * <p>Note that there is no support for mutating these trees. This is deliberate, and should not be
 * changed. We do not want to force all clients of the capa ast to handle mutating programs. In
 * particular, the DOMO infrastructure has many forms of caching and other operations that rely on
 * the underlying program being immutable. If you need to mutate these trees for some reason---and
 * think carefully if you really need to, since this is meant to be essentially a wire format
 * between components---make specialized implementations that understand how to do that.
 *
 * <p>Also note that this interface does not assume that you need some great big class hierarchy to
 * structure types of nodes in an ast. Some people prefer such hierarchies as a matter of taste, but
 * this interface is designed to not inflict this design choice on others.
 *
 * <p>Finally note that the set of node types in this file is not meant to be exhaustive. As new
 * languages are added, feel free to add new nodes types as needed.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 * @author Robert M. Fuhrer (rfuhrer@watson.ibm.com)
 */
public interface CAstNode {

  // statement kinds
  /**
   * Represents a standard case statement. Children:
   *
   * <ul>
   *   <li>condition expression
   *   <li>BLOCK_STMT containing all the cases
   * </ul>
   */
  int SWITCH = 1;

  /**
   * Represents a standard while loop. Children:
   *
   * <ul>
   *   <li>expression denoting the loop condition
   *   <li>statement denoting the loop body
   * </ul>
   */
  int LOOP = 2;

  /**
   * Represents a block of sequential statements. Children:
   *
   * <ul>
   *   <li>statement #1
   *   <li>statement #2
   *   <li>...
   * </ul>
   */
  int BLOCK_STMT = 3;

  /**
   * Represents a standard try/catch statement. Note that while some languages choose to bundle
   * together the notion of try/catch and the notion of unwind-protect (aka 'finally'), the CAst
   * does not. There is a separate UNWIND node type. Children:
   *
   * <ul>
   *   <li>the code of the try block.
   *   <li>the code of the catch block
   *   <li>...
   * </ul>
   */
  int TRY = 4;

  /**
   * Represents an expression statement (e.g. "foo();"). Children:
   *
   * <ul>
   *   <li>the expression
   * </ul>
   */
  int EXPR_STMT = 5;

  int DECL_STMT = 6;
  int RETURN = 7;
  int GOTO = 8;
  int BREAK = 9;
  int CONTINUE = 10;
  int IF_STMT = 11;
  int THROW = 12;
  int FUNCTION_STMT = 13;
  int ASSIGN = 14;
  int ASSIGN_PRE_OP = 15;
  int ASSIGN_POST_OP = 16;
  int LABEL_STMT = 17;
  int IFGOTO = 18;
  int EMPTY = 19;
  int RETURN_WITHOUT_BRANCH = 20;
  int CATCH = 21;
  int UNWIND = 22;
  int MONITOR_ENTER = 23;
  int MONITOR_EXIT = 24;
  int ECHO = 25;
  int YIELD_STMT = 26;
  int FORIN_LOOP = 27;
  int GLOBAL_DECL = 28;
  int CLASS_STMT = 29;

  // expression kinds
  int FUNCTION_EXPR = 100;
  int EXPR_LIST = 101;
  int CALL = 102;
  int GET_CAUGHT_EXCEPTION = 103;

  /**
   * Represents a block of sequentially-executed nodes, the last of which produces the value for the
   * entire block (like progn from lisp). Children:
   *
   * <ul>
   *   <li>node 1
   *   <li>node 2
   *   <li>...
   *   <li>block value expression
   * </ul>
   */
  int BLOCK_EXPR = 104;

  int BINARY_EXPR = 105;
  int UNARY_EXPR = 106;
  int IF_EXPR = 107;
  int ANDOR_EXPR = 108; // TODO blow away?
  int NEW = 109;
  int OBJECT_LITERAL = 110;
  int VAR = 111;
  int OBJECT_REF = 112;
  int CHOICE_EXPR = 113;
  int CHOICE_CASE = 114;
  int SUPER = 115;
  int THIS = 116;
  int ARRAY_LITERAL = 117;
  int CAST = 118;
  int INSTANCEOF = 119;
  int ARRAY_REF = 120;
  int ARRAY_LENGTH = 121;
  int TYPE_OF = 122;
  int EACH_ELEMENT_HAS_NEXT = 123;
  int EACH_ELEMENT_GET = 124;
  int LIST_EXPR = 125;
  int EMPTY_LIST_EXPR = 126;
  int TYPE_LITERAL_EXPR = 127;
  int IS_DEFINED_EXPR = 128;
  int MACRO_VAR = 129;
  int NARY_EXPR = 130;
  // new nodes with an explicit enclosing argument, e.g. "outer.new Inner()". They are mostly
  // treated the same, except in JavaCAst2IRTranslator.doNewObject
  int NEW_ENCLOSING = 131;
  int COMPREHENSION_EXPR = 132;

  // explicit lexical scopes
  int LOCAL_SCOPE = 200;
  int SPECIAL_PARENT_SCOPE = 201;

  // literal expression kinds
  int CONSTANT = 300;
  int OPERATOR = 301;

  // special stuff
  int PRIMITIVE = 400;
  int ERROR = 401;
  int VOID = 402;
  int ASSERT = 403;
  int INCLUDE = 404;
  int NAMED_ENTITY_REF = 405;

  int SUB_LANGUAGE_BASE = 1000;

  /** What kind of node is this? Should return some constant from this file. */
  int getKind();

  /** Returns the constant value represented by this node, if appropriate, and null otherwise. */
  Object getValue();

  /**
   * Return the nth child of this node. If there is no such child, this method should throw an
   * IndexOutOfBoundsException.
   */
  default CAstNode getChild(int n) {
    return getChildren().get(n);
  }

  /** How many children does this node have? */
  default int getChildCount() {
    return getChildren().size();
  }

  List<CAstNode> getChildren();
}
