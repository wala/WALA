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

import com.ibm.wala.dalvik.classLoader.DexIMethod;
import com.ibm.wala.types.TypeReference;

public class ArrayGet extends Instruction {

    public enum Type{t_int,t_wide,t_boolean,t_byte,t_char, t_object, t_short}

    public final int destination;
    public final int array;
    public final int offset;
    public final Type type;

    public ArrayGet(int pc, int destination, int array, int offset, Type type, Opcode op, DexIMethod method) {
        super(pc, op, method);
        this.destination = destination;
        this.array = array;
        this.offset = offset;
        this.type = type;
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.visitArrayGet(this);
    }

    public TypeReference getType()
    {
        return getType(type);
    }

    public static TypeReference getType(Type type) {
        switch(type)
        {
        case t_int:
            return TypeReference.Int;
        case t_wide:
            return TypeReference.Long;
        case t_boolean:
            return TypeReference.Boolean;
        case t_byte:
            return TypeReference.Byte;
        case t_char:
            return TypeReference.Char;
        case t_object:
            return TypeReference.JavaLangObject;
        case t_short:
            return TypeReference.Short;
        default:
            return null;
        }
    }
}
