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

import com.ibm.wala.cast.ir.ssa.CAstUnaryOp;
import com.ibm.wala.dalvik.classLoader.DexIMethod;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction.IOperator;
import com.ibm.wala.util.debug.Assertions;

public class UnaryOperation extends Instruction {

    public static enum OpID {MOVE, MOVE_WIDE, MOVE_EXCEPTION, NOT, NEGINT, NOTINT, NEGLONG, NOTLONG, NEGFLOAT, NEGDOUBLE, DOUBLETOLONG, DOUBLETOFLOAT, INTTOBYTE, INTTOCHAR, INTTOSHORT, DOUBLETOINT, FLOATTODOUBLE, FLOATTOLONG, FLOATTOINT, LONGTODOUBLE, LONGTOFLOAT, LONGTOINT, INTTODOUBLE, INTTOFLOAT, INTTOLONG}

    public final OpID op;
    public final int source;
    public final int destination;

    public UnaryOperation(int pc, OpID op, int destination, int source, Opcode opcode, DexIMethod method) {
        super(pc, opcode, method);
        this.op = op;
        this.destination = destination;
        this.source = source;
    }

    @Override
    public void visit(Visitor visitor)
    {
        visitor.visitUnaryOperation(this);
    }

    public boolean isConversion()
    {
        switch(op)
        {
        case DOUBLETOLONG:
        case DOUBLETOFLOAT:
        case INTTOBYTE:
        case INTTOCHAR:
        case INTTOSHORT:
        case DOUBLETOINT:
        case FLOATTODOUBLE:
        case FLOATTOLONG:
        case FLOATTOINT:
        case LONGTODOUBLE:
        case LONGTOFLOAT:
        case LONGTOINT:
        case INTTODOUBLE:
        case INTTOFLOAT:
        case INTTOLONG:
            return true;
        default:
            return false;
        }
    }

    public boolean isMove()
    {
        switch(op)
        {
        case MOVE:
        case MOVE_WIDE:
            return true;
        default:
            return false;
        }
    }

    public IOperator getOperator() {
        switch(op)
        {
        // SSA unary ops
        case NOT:
            return CAstUnaryOp.BITNOT;
        case NEGINT:
            return IUnaryOpInstruction.Operator.NEG;
        case NOTINT:
            return CAstUnaryOp.BITNOT;
        case NEGLONG:
            return IUnaryOpInstruction.Operator.NEG;
        case NOTLONG:
            return CAstUnaryOp.BITNOT;
        case NEGFLOAT:
            return IUnaryOpInstruction.Operator.NEG;
        case NEGDOUBLE:
            return IUnaryOpInstruction.Operator.NEG;
        default:
        	Assertions.UNREACHABLE();
            return null;
        }
    }
}
