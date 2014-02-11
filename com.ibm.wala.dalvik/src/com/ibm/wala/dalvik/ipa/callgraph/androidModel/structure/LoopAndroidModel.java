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

import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.IExecutionOrder;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.ExecutionOrder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary; 
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;

import com.ibm.wala.util.ssa.SSAValueManager;
import com.ibm.wala.util.ssa.SSAValue;
import com.ibm.wala.util.ssa.SSAValue.TypeKey;
import com.ibm.wala.util.ssa.SSAValue.VariableKey;

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;

import java.lang.Iterable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Builds an Android Model incorporating two loops.
 *
 *  Functions are inserted in sequence until ExecutionOrder.START_OF_LOOP is reached.
 *  This loop is closed later when AFTER_LOOP gets stepped over.
 *
 *  Functions in MULTIPLE_TIMES_IN_LOOP are in a single inner loop.
 *
 *  This structure may be used to model an Application where no state is kept over the 
 *  restart of the Application (instance-state) or when the potential restart of the App
 *  shall be ignored.
 *
 *  {@inheritDoc}
 *
 *  @author     Tobias Blaschke <code@tobiasblaschke.de>
 *  @since      2013-09-18
 */
public class LoopAndroidModel extends AbstractAndroidModel {
    private static final Logger logger = LoggerFactory.getLogger(LoopAndroidModel.class);
    
    //protected VolatileMethodSummary body;
    //protected JavaInstructionFactory insts;
    //protected DexFakeRootMethod.ReuseParameters paramTypes;
    /**
     *
     *  @param  body    The MethodSummary to add instructions to
     *  @param  insts   Will be used to generate the instructions
     */
    public LoopAndroidModel(VolatileMethodSummary body, TypeSafeInstructionFactory insts,
            SSAValueManager paramManager, Iterable<? extends Entrypoint> entryPoints) {
        super(body, insts, paramManager, entryPoints);
    }

    /**
     * Does not insert any special handling.
     *
     * {@inheritDoc}
     */
    protected int enterAT_FIRST(int PC) {
        return PC;
    }

     /**
      * Does not insert any special handling.
      *
      * {@inheritDoc}
      */
    protected int enterBEFORE_LOOP (int PC) {
        return PC;
    }

    private int outerLoopPC = -1;
    Map<TypeReference, SSAValue> outerStartingPhis;
    /**
     * Prepares the PC to get looped to.
     *
     * Thus it tries to assure a new basic block starts here. Additionally it reserves some
     * space for the insertion of Phi-Functions.
     *
     * {@inheritDoc}
     */
    protected int enterSTART_OF_LOOP (int PC) {
        logger.info("PC {} is the jump target of START_OF_LOOP", PC);
        
        this.outerLoopPC = PC;
        PC = makeBrakingNOP(this.outerLoopPC);
        paramManager.scopeDown(true);

        // Top-Half of Phi-Handling
        outerStartingPhis = new HashMap<TypeReference, SSAValue>();
        List<TypeReference> outerPhisNeeded = returnTypesBetween(ExecutionOrder.START_OF_LOOP,
                ExecutionOrder.AFTER_LOOP);
        
        for (TypeReference phiType: outerPhisNeeded) {
            final TypeKey phiKey = new TypeKey(phiType.getName());
            if (paramManager.isSeen(phiKey, false)) {
                final SSAValue newValue = paramManager.getFree(phiType, phiKey);
                outerStartingPhis.put(phiType, newValue);
            }
        }

        body.reserveProgramCounters (outerPhisNeeded.size());
        // Actual Phis will be placed by the bottom-half handler...

        PC = body.getNextProgramCounter();  // Needed if no calls
        return PC;
    }

    /**
     *  Does not insert any special handling.
     *
     *  {@inheritDoc}
     */
    protected int enterMIDDLE_OF_LOOP (int PC) {
        return PC;
    }
    
    /**
     *  Prepares the PC to get looped to.
     *
     *  Thus it tries to assure a new basic block starts here. Additionally it reserves some
     *  space for the insertion of Phi-Functions.
     *
     *  {@inheritDoc}
     */
    protected int enterMULTIPLE_TIMES_IN_LOOP (int PC) {

        paramManager.scopeDown(true);
        // Like in START_OF_LOOP:
        // 1. Make sure a new basic block starts here, for axample by calling
        // a nop()-Function?
        // 2. Store away the PC of the start of the basic block
        // 3. Reserve some space for SSA-Phi instructions by increasing the PC
        // 4. Make sure nothing gets written at that PCs (the user should be
        // able to use body.getNextProgramCounter() )

        return PC;
    }

    /**
     *  Loops to the functions in MULTIPLE_TIMES_IN_LOOP.
     *
     *  It inserts a gotoInstruction and fills the space reserved before with actual PhiInstructions
     *
     *  {@inheritDoc}
     */
    protected int enterEND_OF_LOOP (int PC) {
        paramManager.scopeUp();
        // If there are instructions in MULTIPLE_TIMES_IN_LOOP:
        // 1. Generate a SSAGotoInstruction branching to MULTIPLE_TIMES_IN_LOOP
        // 2. Fill the space MULTIPLE_TIMES_IN_LOOP reserved for Phi-Instructions
        // with actual Phi-Instructions

        return PC;
    }

    /**
     *  Loops to START_OF_LOOP.
     *
     *  It inserts a gotoInstruction and fills the space reserved before with actual PhiInstructions
     *
     *  {@inheritDoc}
     */
    protected int enterAFTER_LOOP (int PC) {
        assert(outerLoopPC > 0) : "Somehow you managed to get the loop-target negative. This is wierd!";

        // Insert the Phis at the beginning of the Block
        int phiPC = outerLoopPC + 1;
        boolean oldAllowReserved = body.allowReserved(true);
        logger.info("Setting block-inner Phis");
        for (TypeReference phiType : outerStartingPhis.keySet()) {
            final SSAValue oldPhi = outerStartingPhis.get(phiType);
            final List<SSAValue> forPhi = new ArrayList<SSAValue>(2);
            forPhi.add(paramManager.getSuper(oldPhi.key));
            forPhi.add(paramManager.getCurrent(oldPhi.key));
            
            SSAPhiInstruction phi = insts.PhiInstruction(phiPC, oldPhi, forPhi); 
            phiPC++;
            body.addStatement(phi);
            paramManager.setPhi(oldPhi, phi);
        }
        body.allowReserved(oldAllowReserved);
       
        // Close the Loop
        logger.info("Closing Loop");
        logger.info("PC {}: Goto {}", PC, outerLoopPC);
        body.addStatement(insts.GotoInstruction(PC, outerLoopPC));
        paramManager.scopeUp();
        
        // Add Phi-Statements at the beginning of this block...
        logger.info("Setting outer-block Phis");
        for (TypeReference phiType : outerStartingPhis.keySet()) {
            final VariableKey  phiKey = outerStartingPhis.get(phiType).key;
            PC = body.getNextProgramCounter();

            List<SSAValue> all = paramManager.getAllForPhi(phiKey);
            logger.debug("Into phi {} for {}", all, phiType.getName());
            // Narf ... unpacking...

            paramManager.invalidate(phiKey);
            final SSAValue newValue = paramManager.getFree(phiType, phiKey);
            SSAPhiInstruction phi = insts.PhiInstruction(PC, newValue, all);
            body.addStatement(phi);

            paramManager.setPhi(newValue, phi);
        }
        PC = body.getNextProgramCounter();
        return PC;
    }

    /**
     *  Does not insert any special handling.
     *
     *  {@inheritDoc}
     */
    protected int enterAT_LAST (int PC) {
        return PC;
    }

    /**
     *  Does not insert any special handling.
     *
     *  {@inheritDoc}
     */
    protected int leaveAT_LAST (int PC) {
        logger.info("Leaving Model with PC = {}", PC);
        return PC;
    }

}
