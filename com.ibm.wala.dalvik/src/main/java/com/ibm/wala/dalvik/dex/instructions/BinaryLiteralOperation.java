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
import com.ibm.wala.dalvik.classLoader.Literal;
import com.ibm.wala.dalvik.dex.instructions.BinaryOperation.DalvikBinaryOp;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.IShiftInstruction;
import org.jf.dexlib2.Opcode;

public class BinaryLiteralOperation extends Instruction {

  public static enum OpID {
    CMPL_FLOAT,
    CMPG_FLOAT,
    CMPL_DOUBLE,
    CMPG_DOUBLE,
    CMPL_LONG,
    CMPG_LONG,
    CMPL_INT,
    CMPG_INT,
    ADD_INT,
    RSUB_INT,
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
    RSUB_LONG,
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
    RSUB_FLOAT,
    MUL_FLOAT,
    DIV_FLOAT,
    REM_FLOAT,
    ADD_DOUBLE,
    RSUB_DOUBLE,
    MUL_DOUBLE,
    DIV_DOUBLE,
    REM_DOUBLE
  }

  public final OpID op;
  public final int oper1;
  public final Literal oper2;
  public final int destination;

  public BinaryLiteralOperation(
      int pc,
      OpID op,
      int destination,
      int oper1,
      Literal oper2,
      Opcode opcode,
      DexIMethod method) {
    super(pc, opcode, method);
    this.op = op;
    this.destination = destination;
    this.oper1 = oper1;
    this.oper2 = oper2;
  }

  @Override
  public void visit(Visitor visitor) {
    visitor.visitBinaryLiteral(this);
  }

  public IBinaryOpInstruction.IOperator getOperator() {
    switch (op) {
      case CMPL_FLOAT:
      case CMPL_DOUBLE:
      case CMPL_LONG:
      case CMPL_INT:
        return DalvikBinaryOp.LT;
      case CMPG_FLOAT:
      case CMPG_DOUBLE:
      case CMPG_LONG:
      case CMPG_INT:
        return DalvikBinaryOp.GT;
      case ADD_INT:
      case ADD_LONG:
      case ADD_DOUBLE:
      case ADD_FLOAT:
        return IBinaryOpInstruction.Operator.ADD;
      case RSUB_INT:
      case RSUB_LONG:
      case RSUB_DOUBLE:
      case RSUB_FLOAT:
        return IBinaryOpInstruction.Operator.SUB;
      case MUL_INT:
      case MUL_LONG:
      case MUL_DOUBLE:
      case MUL_FLOAT:
        return IBinaryOpInstruction.Operator.MUL;
      case DIV_INT:
      case DIV_LONG:
      case DIV_DOUBLE:
      case DIV_FLOAT:
        return IBinaryOpInstruction.Operator.DIV;
      case REM_INT:
      case REM_LONG:
      case REM_DOUBLE:
      case REM_FLOAT:
        return IBinaryOpInstruction.Operator.REM;
      case AND_INT:
      case AND_LONG:
        return IBinaryOpInstruction.Operator.AND;
      case OR_INT:
      case OR_LONG:
        return IBinaryOpInstruction.Operator.OR;
      case XOR_INT:
      case XOR_LONG:
        return IBinaryOpInstruction.Operator.XOR;
      case SHL_INT:
      case SHL_LONG:
        return IShiftInstruction.Operator.SHL;
      case SHR_INT:
      case SHR_LONG:
        return IShiftInstruction.Operator.SHR;
      case USHR_INT:
      case USHR_LONG:
        return IShiftInstruction.Operator.USHR;
      default:
        return null;
    }
  }

  public boolean isFloat() {
    switch (op) {
      case CMPL_FLOAT:
      case CMPG_FLOAT:
      case CMPL_DOUBLE:
      case CMPG_DOUBLE:
      case ADD_FLOAT:
      case RSUB_FLOAT:
      case MUL_FLOAT:
      case DIV_FLOAT:
      case REM_FLOAT:
      case ADD_DOUBLE:
      case RSUB_DOUBLE:
      case MUL_DOUBLE:
      case DIV_DOUBLE:
      case REM_DOUBLE:
        return true;
      default:
        return false;
    }
  }

  public boolean isUnsigned() {
    switch (op) {
      case AND_INT:
      case OR_INT:
      case XOR_INT:
      case SHL_INT:
      case USHR_INT:
      case AND_LONG:
      case OR_LONG:
      case XOR_LONG:
      case SHL_LONG:
      case USHR_LONG:
        return true;
      default:
        return false;
    }
  }

  public boolean isSub() {
    switch (op) {
      case RSUB_DOUBLE:
      case RSUB_FLOAT:
      case RSUB_INT:
      case RSUB_LONG:
        return true;
      default:
        return false;
    }
  }

  @Override
  public String toString() {
    return String.format(
        "%04dpc: v%d = v%d %s v%d",
        this.pc, this.destination, this.oper1, this.op.toString(), this.oper2.value);
  }
}
