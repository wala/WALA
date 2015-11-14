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
 *  Jonathan Bardin     <astrosus@gmail.com>
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

package com.ibm.wala.dalvik.classLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.wala.dalvik.dex.instructions.Instruction;

/**
 * Collection of Instruction wich allow to get an instruction from its table
 * index id or from its bytecode index.
 * It's not allowed to remove an element.
 *
 */
public class InstructionArray implements Collection<Instruction> {
    List<Instruction> instructions;
    Map<Integer, Integer> pc2index;
    List<Integer> index2pc;

    public InstructionArray() {
        instructions = new ArrayList<Instruction>();
        pc2index = new HashMap<Integer, Integer>();
        index2pc = new ArrayList<Integer>();
    }

    public boolean add(Instruction e) {
        boolean ret = instructions.add(e);

        if (ret) {
            pc2index.put(e.pc, size() - 1);
            index2pc.add(e.pc);
        }

        return ret;
    }

    public boolean addAll(Collection<? extends Instruction> c) {
        boolean ret = false;

        for (Instruction instruction : c) {
            ret |= add(instruction);
        }

        return ret;
    }

    public boolean contains(Object o) {
        return instructions.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return instructions.containsAll(c);
    }

    public boolean equals(Object o) {
        return instructions.equals(o);
    }

    public int hashCode() {
        return instructions.hashCode();
    }

    public int indexOf(Object o) {
        return instructions.indexOf(o);
    }

    public boolean isEmpty() {
        return instructions.isEmpty();
    }

    public Iterator<Instruction> iterator() {
        return instructions.iterator();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public int size() {
        return instructions.size();
    }

    public Object[] toArray() {
        return instructions.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return instructions.toArray(a);
    }

    /**
     * @param pc
     *            the byte code index.
     * @return The index of the instruction of given byte code index
     */
    public int getIndexFromPc(int pc) {
    	if (!pc2index.containsKey(pc) && pc2index.containsKey(pc+1)) {
    		pc++;
    	}
        return pc2index.get(pc);
    }

    /**
     * @param index
     *            the instruction index.
     * @return The byte code address of the instruction index
     */
    public int getPcFromIndex(int index) {
        return index2pc.get(index);
    }


    /**
     * @param id
     * @return The instruction from its id.
     */
    public Instruction getFromId(int id) {
        return instructions.get(id);
    }

    /**
     * @param pc
     * @return The instruction from its pc.
     */
    public Instruction getFromPc(int pc) {
        return instructions.get(pc2index.get(pc));

    }
}
