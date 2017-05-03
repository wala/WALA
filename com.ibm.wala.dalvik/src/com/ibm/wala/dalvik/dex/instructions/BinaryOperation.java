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

import org.jf.dexlib.Code.Opcode;

import com.ibm.wala.cast.ir.ssa.CAstBinaryOp;
import com.ibm.wala.dalvik.classLoader.DexIMethod;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IShiftInstruction;

public class BinaryOperation extends Instruction {

    public static enum OpID {CMPL_FLOAT,CMPG_FLOAT,
        CMPL_DOUBLE,CMPG_DOUBLE,
        CMPL_LONG,CMPG_LONG,
        CMPL_INT,CMPG_INT,
        ADD_INT,SUB_INT,MUL_INT,DIV_INT,REM_INT,AND_INT,OR_INT,XOR_INT,SHL_INT,SHR_INT,USHR_INT,
        ADD_LONG,SUB_LONG,MUL_LONG,DIV_LONG,REM_LONG,AND_LONG,OR_LONG,XOR_LONG,SHL_LONG,SHR_LONG,USHR_LONG,
        ADD_FLOAT,SUB_FLOAT,MUL_FLOAT,DIV_FLOAT,REM_FLOAT,
        ADD_DOUBLE,SUB_DOUBLE,MUL_DOUBLE,DIV_DOUBLE,REM_DOUBLE}

    public final OpID op;
    public final int oper1;
    public final int oper2;
    public final int destination;

    public BinaryOperation(int pc, OpID op, int destination, int oper1, int oper2, Opcode opcode, DexIMethod method) {
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

    public IBinaryOpInstruction.IOperator getOperator()
    {
        switch(op)
        {
        case CMPL_FLOAT:
            return CAstBinaryOp.LT;
        case CMPG_FLOAT:
            return CAstBinaryOp.GT;
        case CMPL_DOUBLE:
            return CAstBinaryOp.LT;
        case CMPG_DOUBLE:
            return CAstBinaryOp.GT;
        case CMPL_LONG:
            return CAstBinaryOp.LT;
        case CMPG_LONG:
            return CAstBinaryOp.GT;
        case CMPL_INT:
            return CAstBinaryOp.LT;
        case CMPG_INT:
            return CAstBinaryOp.GT;
        case ADD_INT:
            return IBinaryOpInstruction.Operator.ADD;
        case SUB_INT:
            return IBinaryOpInstruction.Operator.SUB;
        case MUL_INT:
            return IBinaryOpInstruction.Operator.MUL;
        case DIV_INT:
            return IBinaryOpInstruction.Operator.DIV;
        case REM_INT:
            return IBinaryOpInstruction.Operator.REM;
        case AND_INT:
            return IBinaryOpInstruction.Operator.AND;
        case OR_INT:
            return IBinaryOpInstruction.Operator.OR;
        case XOR_INT:
            return IBinaryOpInstruction.Operator.XOR;
        case SHL_INT:
            return IShiftInstruction.Operator.SHL;
        case SHR_INT:
            return IShiftInstruction.Operator.SHR;
        case USHR_INT:
            return IShiftInstruction.Operator.USHR;
        case ADD_LONG:
            return IBinaryOpInstruction.Operator.ADD;
        case SUB_LONG:
            return IBinaryOpInstruction.Operator.SUB;
        case MUL_LONG:
            return IBinaryOpInstruction.Operator.MUL;
        case DIV_LONG:
            return IBinaryOpInstruction.Operator.DIV;
        case REM_LONG:
            return IBinaryOpInstruction.Operator.REM;
        case AND_LONG:
            return IBinaryOpInstruction.Operator.AND;
        case OR_LONG:
            return IBinaryOpInstruction.Operator.OR;
        case XOR_LONG:
            return IBinaryOpInstruction.Operator.XOR;
        case SHL_LONG:
            return IShiftInstruction.Operator.SHL;
        case SHR_LONG:
            return IShiftInstruction.Operator.SHR;
        case USHR_LONG:
            return IShiftInstruction.Operator.USHR;
        case ADD_FLOAT:
            return IBinaryOpInstruction.Operator.ADD;
        case SUB_FLOAT:
            return IBinaryOpInstruction.Operator.SUB;
        case MUL_FLOAT:
            return IBinaryOpInstruction.Operator.MUL;
        case DIV_FLOAT:
            return IBinaryOpInstruction.Operator.DIV;
        case REM_FLOAT:
            return IBinaryOpInstruction.Operator.REM;
        case ADD_DOUBLE:
            return IBinaryOpInstruction.Operator.ADD;
        case SUB_DOUBLE:
            return IBinaryOpInstruction.Operator.SUB;
        case MUL_DOUBLE:
            return IBinaryOpInstruction.Operator.MUL;
        case DIV_DOUBLE:
            return IBinaryOpInstruction.Operator.DIV;
        case REM_DOUBLE:
            return IBinaryOpInstruction.Operator.REM;
        }
        return null;
    }
    public boolean isFloat()
    {
        switch(op)
        {
        case CMPL_FLOAT:
        case CMPG_FLOAT:
        case CMPL_DOUBLE:
        case CMPG_DOUBLE:
        case ADD_FLOAT:
        case SUB_FLOAT:
        case MUL_FLOAT:
        case DIV_FLOAT:
        case REM_FLOAT:
        case ADD_DOUBLE:
        case SUB_DOUBLE:
        case MUL_DOUBLE:
        case DIV_DOUBLE:
        case REM_DOUBLE:
            return true;
        default:
            return false;
        }
    }

    public boolean isUnsigned() {
    	
    	/**
    	 * At the moment, WALA asserts that false is returned here. Maybe WALA will support unsigned
    	 * arithmetic in its IR some time in the future - then the commented code will make sense.
    	 */
    	return false;
//        switch(op)
//        {
//        case AND_INT:
//        case OR_INT:
//        case XOR_INT:
//        case SHL_INT:
//        case USHR_INT:
//        case AND_LONG:
//        case OR_LONG:
//        case XOR_LONG:
//        case SHL_LONG:
//        case USHR_LONG:
//            return true;
//        default:
//            return false;
//        }
    }

}
