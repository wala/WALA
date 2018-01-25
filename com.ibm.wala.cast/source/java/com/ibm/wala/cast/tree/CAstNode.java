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
package com.ibm.wala.cast.tree;

/**
 *  This interface represents nodes of CAPA Abstract Syntax Trees.  It
 * is a deliberately minimal interface, simply assuming that the nodes
 * form a tree and have some minimal state at each node.  In
 * particular, a node has a kind---which should be one of the symbolic
 * constants in this file---and potentially has child nodes, a
 * constant values, or possibly both.
 *
 *  Note that there is no support for mutating these trees.  This is
 * deliberate, and should not be changed.  We do not want to force all
 * clients of the capa ast to handle mutating programs.  In
 * particular, the DOMO infrastructure has many forms of caching and
 * other operations that rely on the underlying program being
 * immutable.  If you need to mutate these trees for some reason---and
 * think carefully if you really need to, since this is meant to be
 * essentially a wire format between components---make specialized
 * implementations that understand how to do that.
 *
 *  Also note that this interface does not assume that you need some
 * great big class hierarchy to structure types of nodes in an ast.
 * Some people prefer such hierarchies as a matter of taste, but this
 * interface is designed to not inflict this design choice on others.
 *
 *  Finally note that the set of node types in this file is not meant
 * to be exhaustive.  As new languages are added, feel free to add new
 * nodes types as needed.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 * @author Robert M. Fuhrer (rfuhrer@watson.ibm.com)
 *
 */
public interface CAstNode {

  // statement kinds
  /**
   * Represents a standard case statement. Children:
   * </ul>
   *   <li>condition expression
   *   <li>BLOCK_STMT containing all the cases
   * </ul>
   */
  public static final int SWITCH = 1;

  /**
   * Represents a standard while loop. Children:
   * <ul>
   *   <li>expression denoting the loop condition
   *   <li>statement denoting the loop body
   * </ul>
   */
  public static final int LOOP = 2;

  /**
   * Represents a block of sequential statements. Children:
   * <ul>
   *   <li>statement #1
   *   <li>statement #2
   *   <li>...
   * </ul>
   */
  public static final int BLOCK_STMT = 3;

  /**
   *  Represents a standard try/catch statement. Note that while some
   * languages choose to bundle together the notion of try/catch and
   * the notion of unwind-protect (aka 'finally'), the CAst does not.
   * There is a separate UNWIND node type.  Children: 
   * <ul> 
   *  <li>the code of the try block.  
   *  <li>the code of the catch block <li>...
   * </ul>
   */
  public static final int TRY = 4;

  /**
   * Represents an expression statement (e.g. "foo();"). Children:
   * <ul>
   *   <li>the expression
   * </ul>
   */
  public static final int EXPR_STMT = 5;
  public static final int DECL_STMT = 6;
  public static final int RETURN = 7;
  public static final int GOTO = 8;
  public static final int BREAK = 9;
  public static final int CONTINUE = 10;
  public static final int IF_STMT = 11;
  public static final int THROW = 12;
  public static final int FUNCTION_STMT = 13;
  public static final int ASSIGN = 14;
  public static final int ASSIGN_PRE_OP = 15;
  public static final int ASSIGN_POST_OP = 16;
  public static final int LABEL_STMT = 17;
  public static final int IFGOTO = 18;
  public static final int EMPTY = 19;
  public static final int RETURN_WITHOUT_BRANCH = 20;
  public static final int CATCH = 21;
  public static final int UNWIND = 22;
  public static final int MONITOR_ENTER = 23;
  public static final int MONITOR_EXIT = 24;
  public static final int ECHO = 25;
  public static final int YIELD_STMT = 26;
  public static final int FORIN_LOOP = 27;

  // expression kinds
  public static final int FUNCTION_EXPR = 100;
  public static final int EXPR_LIST = 101;
  public static final int CALL = 102;
  public static final int GET_CAUGHT_EXCEPTION = 103;

  /**
   * Represents a block of sequentially-executed nodes, the last of which produces
   * the value for the entire block (like progn from lisp).
   * Children:
   * <ul>
   *   <li>node 1
   *   <li>node 2
   *   <li>...
   *   <li>block value expression
   * </ul>
   */
  public static final int BLOCK_EXPR = 104;
  public static final int BINARY_EXPR = 105;
  public static final int UNARY_EXPR = 106;
  public static final int IF_EXPR = 107;
  public static final int ANDOR_EXPR = 108; // TODO blow away?
  public static final int NEW = 109;
  public static final int OBJECT_LITERAL = 110;
  public static final int VAR = 111;
  public static final int OBJECT_REF = 112;
  public static final int CHOICE_EXPR = 113;
  public static final int CHOICE_CASE = 114;
  public static final int SUPER = 115;
  public static final int THIS = 116;
  public static final int ARRAY_LITERAL = 117;
  public static final int CAST = 118;
  public static final int INSTANCEOF = 119;
  public static final int ARRAY_REF = 120;
  public static final int ARRAY_LENGTH = 121;
  public static final int TYPE_OF = 122;
  public static final int EACH_ELEMENT_HAS_NEXT = 123;
  public static final int EACH_ELEMENT_GET = 124;
  public static final int LIST_EXPR = 125;
  public static final int EMPTY_LIST_EXPR = 126;
  public static final int TYPE_LITERAL_EXPR = 127;
  public static final int IS_DEFINED_EXPR = 128;
  public static final int MACRO_VAR = 129;
  public static final int NARY_EXPR = 130;
  // new nodes with an explicit enclosing argument, e.g. "outer.new Inner()". They are mostly treated the same, except in JavaCAst2IRTranslator.doNewObject
  public static final int NEW_ENCLOSING = 131;
  public static final int COMPREHENSION_EXPR = 132;
  
  // explicit lexical scopes
  public static final int LOCAL_SCOPE = 200;
  public static final int SPECIAL_PARENT_SCOPE = 201;

  // literal expression kinds
  public static final int CONSTANT = 300;
  public static final int OPERATOR = 301;

  // special stuff
  public static final int PRIMITIVE = 400;
  public static final int ERROR = 401;
  public static final int VOID = 402;
  public static final int ASSERT = 403;
  public static final int INCLUDE = 404;
  public static final int NAMED_ENTITY_REF = 405;

  public static final int SUB_LANGUAGE_BASE = 1000;

  /** 
   * What kind of node is this?  Should return some constant from this file.
   */ 
  int getKind();

  /**
   *  Returns the constant value represented by this node, if
   * appropriate, and null otherwise.
   */
  Object getValue();

  /**
   *  Return the nth child of this node.  If there is no such child,
   * this method should throw a NoSuchElementException.
   */
  CAstNode getChild(int n);

  /**
   * How many children does this node have?
   */
  int getChildCount();

}
