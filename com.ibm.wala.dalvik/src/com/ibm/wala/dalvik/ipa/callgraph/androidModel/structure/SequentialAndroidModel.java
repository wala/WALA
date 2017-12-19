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
package com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure;

import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;
import com.ibm.wala.util.ssa.SSAValueManager;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;

/**
 *  Functions get called once in sequential order.
 *
 *  No loops are inserted into the model.
 *
 *  This model should not be particular useful in practice. However it might come in
 *  handy for debugging purposes or as a skeleton for an other Model.
 *
 *  @author     Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since      2013-09-18
 */
public final class SequentialAndroidModel extends AbstractAndroidModel {
//    private static final Logger logger = LoggerFactory.getLogger(SequentialAndroidModel.class);
    
    //protected VolatileMethodSummary body;
    //protected JavaInstructionFactory insts;
    //protected DexFakeRootMethod.ReuseParameters paramTypes;

    /**
     *
     *  @param  body    The MethodSummary to add instructions to
     *  @param  insts   Will be used to generate the instructions
     */
    public SequentialAndroidModel(VolatileMethodSummary body, TypeSafeInstructionFactory insts,
            SSAValueManager paramManager, Iterable<? extends Entrypoint> entryPoints) {
        super(body, insts, paramManager, entryPoints);
    }

     /**
      * Does not insert any special handling.
      *
      * {@inheritDoc}
      */
    @Override
    protected int enterAT_FIRST(int PC) { 
        return PC;
    }

     /**
      *  Does not insert any special handling.
      *
      * {@inheritDoc}
      */
    @Override
    protected int enterBEFORE_LOOP (int PC) {
        return PC;
    }

    /**
     * Does not insert any special handling.
     *
     * {@inheritDoc}
     */
    @Override
    protected int enterSTART_OF_LOOP (int PC) {
        return PC;
    }

    /**
     * Does not insert any special handling.
     *
     * {@inheritDoc}
     */
    @Override
    protected int enterMIDDLE_OF_LOOP (int PC) {
        return PC;
    }
    
    /**
     * Does not insert any special handling.
     *
     * {@inheritDoc}
     */
    @Override
    protected int enterMULTIPLE_TIMES_IN_LOOP (int PC) {
        return PC;
    }

    /**
     * Does not insert any special handling.
     *
     * {@inheritDoc}
     */
    @Override
    protected int enterEND_OF_LOOP (int PC) {
        return PC;
    }

    /**
     * Does not insert any special handling.
     *
     * {@inheritDoc}
     */
    @Override
    protected int enterAFTER_LOOP (int PC) {
        return PC;
    }

    /**
     * Does not insert any special handling.
     *
     * {@inheritDoc}
     */
    @Override
    protected int enterAT_LAST (int PC) {
        return PC;
    }

    /**
     * Does not insert any special handling.
     *
     * {@inheritDoc}
     */
    @Override
    protected int leaveAT_LAST (int PC) {
        return PC;
    }
}
