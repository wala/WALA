/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * This file is a derivative of code released under the terms listed below.
 *
 */
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package com.ibm.wala.dalvik.dex.instructions;

import com.ibm.wala.dalvik.classLoader.DexIMethod;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.IShiftInstruction;
import org.jf.dexlib2.Opcode;

public class BinaryOperation extends Instruction {

  public enum OpID {
    CMPL_FLOAT,
    CMPG_FLOAT,
    CMPL_DOUBLE,
    CMPG_DOUBLE,
    CMPL_LONG,
    CMPG_LONG,
    CMPL_INT,
    CMPG_INT,
    ADD_INT,
    SUB_INT,
    MUL_INT,
    DIV_INT,
    REM_INT,
    AND_INT,
    OR_INT,
    XOR_INT,
    SHL_INT,
    SHR_INT,
    USHR_INT,
    ADD_LONG,
    SUB_LONG,
    MUL_LONG,
    DIV_LONG,
    REM_LONG,
    AND_LONG,
    OR_LONG,
    XOR_LONG,
    SHL_LONG,
    SHR_LONG,
    USHR_LONG,
    ADD_FLOAT,
    SUB_FLOAT,
    MUL_FLOAT,
    DIV_FLOAT,
    REM_FLOAT,
    ADD_DOUBLE,
    SUB_DOUBLE,
    MUL_DOUBLE,
    DIV_DOUBLE,
    REM_DOUBLE
  }

  /** for binary ops not defined in JVML */
  public enum DalvikBinaryOp implements IBinaryOpInstruction.IOperator {
    LT,
    GT;

    @Override
    public String toString() {
      return super.toString().toLowerCase();
    }
  }

  public final OpID op;
  public final int oper1;
  public final int oper2;
  public final int destination;

  public BinaryOperation(
      int pc, OpID op, int destination, int oper1, int oper2, Opcode opcode, DexIMethod method) {
    super(pc, opcode, method);
    this.op = op;
    this.destination = destination;
    this.oper1 = oper1;
    this.oper2 = oper2;
  }

  @Override
  public void visit(Visitor visitor) {
    visitor.visitBinaryOperation(this);
  }

  public IBinaryOpInstruction.IOperator getOperator() {
    return switch (op) {
      case CMPL_FLOAT, CMPL_INT, CMPL_LONG, CMPL_DOUBLE -> DalvikBinaryOp.LT;
      case CMPG_FLOAT, CMPG_INT, CMPG_LONG, CMPG_DOUBLE -> DalvikBinaryOp.GT;
      case ADD_INT, ADD_DOUBLE, ADD_FLOAT, ADD_LONG -> IBinaryOpInstruction.Operator.ADD;
      case SUB_INT, SUB_DOUBLE, SUB_FLOAT, SUB_LONG -> IBinaryOpInstruction.Operator.SUB;
      case MUL_INT, MUL_DOUBLE, MUL_FLOAT, MUL_LONG -> IBinaryOpInstruction.Operator.MUL;
      case DIV_INT, DIV_DOUBLE, DIV_FLOAT, DIV_LONG -> IBinaryOpInstruction.Operator.DIV;
      case REM_INT, REM_DOUBLE, REM_FLOAT, REM_LONG -> IBinaryOpInstruction.Operator.REM;
      case AND_INT, AND_LONG -> IBinaryOpInstruction.Operator.AND;
      case OR_INT, OR_LONG -> IBinaryOpInstruction.Operator.OR;
      case XOR_INT, XOR_LONG -> IBinaryOpInstruction.Operator.XOR;
      case SHL_INT, SHL_LONG -> IShiftInstruction.Operator.SHL;
      case SHR_INT, SHR_LONG -> IShiftInstruction.Operator.SHR;
      case USHR_INT, USHR_LONG -> IShiftInstruction.Operator.USHR;
    };
  }

  public boolean isFloat() {
    return switch (op) {
      case CMPL_FLOAT,
          CMPG_FLOAT,
          CMPL_DOUBLE,
          CMPG_DOUBLE,
          ADD_FLOAT,
          SUB_FLOAT,
          MUL_FLOAT,
          DIV_FLOAT,
          REM_FLOAT,
          ADD_DOUBLE,
          SUB_DOUBLE,
          MUL_DOUBLE,
          DIV_DOUBLE,
          REM_DOUBLE ->
          true;
      default -> false;
    };
  }

  public boolean isUnsigned() {
    return switch (op) {
      case AND_INT,
          OR_INT,
          XOR_INT,
          SHL_INT,
          USHR_INT,
          AND_LONG,
          OR_LONG,
          XOR_LONG,
          SHL_LONG,
          USHR_LONG ->
          true;
      default -> false;
    };
  }
}
