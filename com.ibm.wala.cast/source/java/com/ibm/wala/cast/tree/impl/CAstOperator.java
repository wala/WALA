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
package com.ibm.wala.cast.tree.impl;

import java.util.NoSuchElementException;

import com.ibm.wala.cast.tree.CAstNode;

/**
 *  Various operators that are built in to many languages, and hence
 * perhaps deserve special notice in WALA CAst interface.  There is no
 * strong notion of what should be in here, so feel free to add other
 * common operators.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class CAstOperator implements CAstNode {
  private final String op;
  
  protected CAstOperator(String op) { 
    this.op = op; 
  }    

  @Override
  public String toString() { 
    return "OP:" + op;
  }

  @Override
  public int getKind() { 
    return CAstNode.OPERATOR; 
  }
  
  @Override
  public Object getValue() { 
    return op;
  }
  
  @Override
  public CAstNode getChild(int n) {
    throw new NoSuchElementException(); 
  }
    
  @Override
  public int getChildCount() { 
    return 0; 
  }

  /*
   *  The EQ and STRICT_EQ and NE and STRICT_NE pairs of operators are meant to 
   * support languages that define multiple notions of equality, such as Scheme with
   * eql and eq and JavaScript with == and ===.
   */
  public final static CAstOperator OP_EQ = new CAstOperator("==");
  public final static CAstOperator OP_STRICT_EQ = new CAstOperator("===");

  public final static CAstOperator OP_NE = new CAstOperator("!=");
  public final static CAstOperator OP_STRICT_NE = new CAstOperator("!==");

  public final static CAstOperator OP_ADD = new CAstOperator("+");
  public final static CAstOperator OP_CONCAT = new CAstOperator(".");
  public final static CAstOperator OP_DIV = new CAstOperator("/");
  public final static CAstOperator OP_LSH = new CAstOperator("<<");
  public final static CAstOperator OP_MOD = new CAstOperator("%");
  public final static CAstOperator OP_MUL = new CAstOperator("*");
  public final static CAstOperator OP_POW = new CAstOperator("^^^");
  public final static CAstOperator OP_RSH = new CAstOperator(">>");
  public final static CAstOperator OP_URSH = new CAstOperator(">>>");
  public final static CAstOperator OP_SUB = new CAstOperator("-");
  public final static CAstOperator OP_GE = new CAstOperator(">=");
  public final static CAstOperator OP_GT = new CAstOperator(">");
  public final static CAstOperator OP_LE = new CAstOperator("<=");
  public final static CAstOperator OP_LT = new CAstOperator("<");
  public final static CAstOperator OP_NOT = new CAstOperator("!");
  public final static CAstOperator OP_BITNOT = new CAstOperator("~");
  public final static CAstOperator OP_BIT_AND = new CAstOperator("&");
  public final static CAstOperator OP_REL_AND = new CAstOperator("&&");
  public final static CAstOperator OP_BIT_OR = new CAstOperator("|");
  public final static CAstOperator OP_REL_OR = new CAstOperator("||");
  public final static CAstOperator OP_BIT_XOR = new CAstOperator("^");
  public final static CAstOperator OP_REL_XOR = new CAstOperator("^^");
  public final static CAstOperator OP_IN = new CAstOperator("in");

}

