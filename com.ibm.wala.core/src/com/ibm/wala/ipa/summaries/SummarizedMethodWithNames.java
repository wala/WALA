/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package com.ibm.wala.ipa.summaries;

import java.util.Map;

import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.strings.Atom;

/**
 *  A SummarizedMethod (for synthetic functions) with variable names.
 *
 *  Using this class instead of a normal SummarizedMethod enables the use of human-readable variable
 *  names in synthetic methods. This should not change th analysis-result but may come in handy when
 *  debugging.
 *
 *  @author     Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since      2013-11-25
 */
public class SummarizedMethodWithNames extends SummarizedMethod {

    private static final boolean DEBUG = false;
  
    private final MethodSummary summary;
    private final Map<Integer, Atom> localNames;

    public SummarizedMethodWithNames(MethodReference ref, MethodSummary summary, IClass declaringClass, Map<Integer, Atom> localNames) 
                throws NullPointerException {
        super(ref, summary, declaringClass);
        
        this.summary = summary;
        this.localNames = localNames;

        if (DEBUG) { System.err.println("From old MSUM"); }
    }
    
    @SuppressWarnings("unused")
    public SummarizedMethodWithNames(MethodReference ref, VolatileMethodSummary summary, IClass declaringClass) 
                throws NullPointerException {
        super(ref, summary.getMethodSummary(), declaringClass);
        this.summary = summary.getMethodSummary();
        this.localNames = summary.getLocalNames();
        if (DEBUG && this.localNames.isEmpty()) {
          System.err.println("Local names are empty for " + ref);
        }
    }

    public static class SyntheticIRWithNames extends SyntheticIR {
        private final SSA2LocalMap localMap;

        public static class SyntheticSSA2LocalMap implements SSA2LocalMap {
            private final Map<Integer, Atom> localNames;

            public SyntheticSSA2LocalMap(Map<Integer, Atom> localNames) {
                this.localNames = localNames;
            }

            /**
             *  Does not respect index.
             */
            @Override
            public String[] getLocalNames(int index, int vn) {
                if (DEBUG) { System.err.printf("IR.getLocalNames({}, {})", index, vn); }
                if (this.localNames.containsKey(vn)) {
                    return new String[] { this.localNames.get(vn).toString() };
                } else {
                    return null;
                }
            }
        }

        public SyntheticIRWithNames(IMethod method, Context context, AbstractCFG cfg, SSAInstruction[] instructions, 
                SSAOptions options, Map< Integer, ConstantValue > constants, Map<Integer, Atom> localNames) throws AssertionError {
            super(method, context, cfg, instructions, options, constants);
            this.localMap = new SyntheticSSA2LocalMap(localNames);
        }

        @Override
        public SSA2LocalMap getLocalMap() {
            return this.localMap;
        }
    }

    /**
     *  Returns the variable name to a ssa-number.
     *
     *  Does not respect the value of bcIndex.
     */
    @Override
    public String getLocalVariableName(int bcIndex, int localNumber) {
        if (this.localNames.containsKey(localNumber)) {
            String name = this.localNames.get(localNumber).toString();
            if (DEBUG) { System.err.printf("getLocalVariableName(bc={}, no={}) = {}", bcIndex, localNumber, name); }
            return name;
        } else {
            if (DEBUG) { System.err.printf("No name for {}", localNumber); }
            return super.getLocalVariableName(bcIndex, localNumber);
        }
    }

    @Override
    public boolean hasLocalVariableTable() {
        return true;
    }

    @Override
    public IR makeIR(Context context, SSAOptions options) {
        final SSAInstruction instrs[] = getStatements(options);
        final IR ir = new SyntheticIRWithNames(this, Everywhere.EVERYWHERE, makeControlFlowGraph(instrs), instrs, 
                options, summary.getConstants(), localNames);

        return ir;
    }
}
