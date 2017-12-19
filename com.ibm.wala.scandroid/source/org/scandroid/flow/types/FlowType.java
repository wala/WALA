/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/**
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
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

package org.scandroid.flow.types;

import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.ISSABasicBlock;

/**
 * Flow types represent specific instances of sources or sinks.
 * 
 * In contrast to the Source/Sink specs, these have ties to specific locations
 * in the source.
 * 
 * @author creswick
 */
public abstract class FlowType<E extends ISSABasicBlock> {
    private final BasicBlockInContext<E> block;
    private final boolean source;

    protected FlowType(BasicBlockInContext<E> block, boolean source) {
        this.block = block;
        this.source = source;
    }
    
    public final BasicBlockInContext<E> getBlock() {
        return block;
    }

    public final boolean isSource() {
        return source;
    }
    
    @Override
    public String toString() {
        return "block=" + block + ", source=" + source + ", desc=" + descString();
    }
    
    public String descString() {
        if(source)
            return "I";
        else
            return "O";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((block == null) ? 0 : block.getNumber());
        result = prime * result + (source ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked")
        FlowType<E> other = (FlowType<E>) obj;
        if (block == null) {
            if (other.block != null)
                return false;
        } else if (block.getNumber() != other.block.getNumber()) {
            return false;
        }
        if (source != other.source)
            return false;
        return true;
    }

    /**
     * custom comparison for BasicBlockInContext.  The WALA .equals() 
     * implementation eventually delegates to pointer equality, which is too 
     * specific for our needs. 
     * 
     * @param a
     * @param b
     */
    @SuppressWarnings("unused")
	private boolean compareBlocks(BasicBlockInContext<E> a,
            BasicBlockInContext<E> b) {
        if (null == a || null == b) {
            return false;
        }

        // delegate to the defined implementation, but only if it's true.
        if (a.equals(b)) {
            return true;
        }

        if (a.getNumber() != b.getNumber()) {
            return false;
        }

        if (!a.getMethod().getSignature().equals(b.getMethod().getSignature())) {
            return false;
        }
        return true;
    }
    
    public abstract <R> R visit(FlowTypeVisitor<E, R> v);
    
    public static interface FlowTypeVisitor<E extends ISSABasicBlock, R> {
    	R visitFieldFlow(FieldFlow<E> flow);
    	R visitIKFlow(IKFlow<E> flow);
    	R visitParameterFlow(ParameterFlow<E> flow);
    	R visitReturnFlow(ReturnFlow<E> flow);
		R visitStaticFieldFlow(StaticFieldFlow<E> flow);
    }
}
