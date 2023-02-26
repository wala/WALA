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

import com.ibm.wala.cast.ir.translator.ConstantFoldingRewriter;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.impl.CAstOperator;

public class JSConstantFoldingRewriter extends ConstantFoldingRewriter {

  public JSConstantFoldingRewriter(CAst Ast) {
    super(Ast);
  }

  @Override
  protected Object eval(CAstOperator op, Object lhs, Object rhs) {
    if (op == CAstOperator.OP_ADD) {
      if (lhs instanceof String || rhs instanceof String) {
        return String.valueOf(lhs) + rhs;
      } else if (lhs instanceof Number && rhs instanceof Number) {
        return ((Number) lhs).doubleValue() + ((Number) rhs).doubleValue();
      }
    } else if (op == CAstOperator.OP_BIT_AND) {

    } else if (op == CAstOperator.OP_BIT_OR) {

    } else if (op == CAstOperator.OP_BIT_XOR) {

    } else if (op == CAstOperator.OP_BITNOT) {

    } else if (op == CAstOperator.OP_CONCAT) {
      if (lhs instanceof String || rhs instanceof String) {
        return String.valueOf(lhs) + rhs;
      }

    } else if (op == CAstOperator.OP_DIV) {
      if (lhs instanceof Number && rhs instanceof Number) {
        return ((Number) lhs).doubleValue() / ((Number) rhs).doubleValue();
      }

    } else if (op == CAstOperator.OP_EQ) {

    } else if (op == CAstOperator.OP_GE) {
      if (lhs instanceof Number && rhs instanceof Number) {
        return ((Number) lhs).doubleValue() >= ((Number) rhs).doubleValue();
      }

    } else if (op == CAstOperator.OP_GT) {
      if (lhs instanceof Number && rhs instanceof Number) {
        return ((Number) lhs).doubleValue() > ((Number) rhs).doubleValue();
      }

    } else if (op == CAstOperator.OP_LE) {
      if (lhs instanceof Number && rhs instanceof Number) {
        return ((Number) lhs).doubleValue() <= ((Number) rhs).doubleValue();
      }

    } else if (op == CAstOperator.OP_LSH) {

    } else if (op == CAstOperator.OP_LT) {
      if (lhs instanceof Number && rhs instanceof Number) {
        return ((Number) lhs).doubleValue() < ((Number) rhs).doubleValue();
      }

    } else if (op == CAstOperator.OP_MOD) {
      if (lhs instanceof Number && rhs instanceof Number) {
        return ((Number) lhs).doubleValue() % ((Number) rhs).doubleValue();
      }

    } else if (op == CAstOperator.OP_MUL) {
      if (lhs instanceof Number && rhs instanceof Number) {
        return ((Number) lhs).doubleValue() * ((Number) rhs).doubleValue();
      }

    } else if (op == CAstOperator.OP_NE) {

    } else if (op == CAstOperator.OP_NOT) {

    } else if (op == CAstOperator.OP_REL_AND) {

    } else if (op == CAstOperator.OP_REL_OR) {

    } else if (op == CAstOperator.OP_REL_XOR) {

    } else if (op == CAstOperator.OP_RSH) {

    } else if (op == CAstOperator.OP_STRICT_EQ) {

    } else if (op == CAstOperator.OP_STRICT_NE) {

    } else if (op == CAstOperator.OP_SUB) {
      if (lhs instanceof Number && rhs instanceof Number) {
        return ((Number) lhs).doubleValue() - ((Number) rhs).doubleValue();
      }

    } else if (op == CAstOperator.OP_URSH) {

    }

    // no constant value
    return null;
  }
}
