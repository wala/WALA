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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.ExecutionOrder;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.IExecutionOrder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.ssa.SSAValueManager;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;

/**
 *  Aids in handling code to be inserted at given points into the model.
 *
 *  Overload this class to change the structure of the model. When the model is being built the
 *  enterLABEL-functions are called when ever a label gets stepped over.
 *  <p>
 *  You can then add instructions to the body using the insts-Instruction factory. Instructions 
 *  don't have to be in ascending order. Instead they will be sorted by their IIndex once the model
 *  gets finished.
 *
 *  If you want to add loops to the model you might want to have a look at AndroidModelParameterManager
 *  which aids in keeping track of SSA-Variables and adding Phi-Functions.
 *
 *  @see        com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.ExecutionOrder
 *  @see        com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.AndroidModelParameterManager
 *
 *  @author     Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since      2013-09-07
 */
public abstract class AbstractAndroidModel  {
    private static final Logger logger = LoggerFactory.getLogger(AbstractAndroidModel.class);
    
    private ExecutionOrder currentSection = null;
    protected VolatileMethodSummary body = null;
    protected TypeSafeInstructionFactory insts = null;
    protected SSAValueManager paramManager = null;
    protected Iterable<? extends Entrypoint> entryPoints = null;
    private IExecutionOrder lastQueriedMethod = null;   // Used for sanity checks only
    
    //
    //  Helper functions
    //

    /**
     *  Return a List of all Types returned by functions between start (inclusive) and end (exclusive).
     *
     *  @return That list
     *  @throws IllegalArgumentException if an EntryPoint was not an AndroidEntryPoint
     */
    protected List<TypeReference> returnTypesBetween(IExecutionOrder start, IExecutionOrder end) {
        assert (start != null) : "The argument start was null";
        assert (end != null) : "The argument end was null";

        List<TypeReference> returnTypes = new ArrayList<>();
        for (Entrypoint ep : this.entryPoints) {
            if (ep instanceof AndroidEntryPoint) {
                AndroidEntryPoint aep = (AndroidEntryPoint)ep;
                if ((aep.compareTo(start) >= 0) &&
                        (aep.compareTo(end) <= 0)) {
                    if (! (aep.getMethod().getReturnType().equals(TypeReference.Void) ||
                           aep.getMethod().getReturnType().isPrimitiveType ())) {
                        if (! returnTypes.contains(aep.getMethod().getReturnType())) {  // TODO: Use a set?
                            returnTypes.add(aep.getMethod().getReturnType());
                        }
                    }
                 }
            } else {
                throw new IllegalArgumentException("Entrypoint (given to Constructor) is not an AndroidEntryPoint!");
            }
        }

        return returnTypes;
    }
    //
    //  The rest :)
    //

    /**
     *  If you don't intend to use the paramManager, you can pass null. However all other parameters are required.
     *
     *  @param  body    The MethodSummary to add instructions to
     *  @param  insts   Will be used to generate the instructions
     *  @param  paramManager aids in handling SSA-Values
     *  @param  entryPoints This iterable has to contain only instances of AnroidEntryPoint.
     */
    public AbstractAndroidModel(VolatileMethodSummary body, TypeSafeInstructionFactory insts, 
            SSAValueManager paramManager, Iterable<? extends Entrypoint> entryPoints) {

        if (body == null) {
            throw new IllegalArgumentException("The argument body may not be null.");
        }
        if (insts == null) {
            throw new IllegalArgumentException("The argument insts may not be null.");
        }
        if (entryPoints == null) {
            throw new IllegalArgumentException("The argument entryPoints may not be null.");
        }
        //if (!(entryPoints.hasNext())) {
        //    throw new IllegalArgumentException("The iterable entryPoints may not be empty.");
        //}
        this.body = body;
        this.insts = insts;
        this.paramManager = paramManager;
        this.entryPoints = entryPoints;
    }

    /**
     *  Determines for an AndroidEntryPoint if a label got skipped over.
     *
     *  If a label got skipped over special handling code has to be inserted before 
     *  the entrypoints invocation.
     *
     *  This function is expected to be called on entrypoints in ascending order.
     *
     *  You are expected to call {@link #enter(ExecutionOrder, int)} iff a
     *  Label got skipped over.
     *
     *  @param  order       The entrypoint in question
     *  @return true        if a label got stepped over
     *  @throws IllegalArgumentException    If the entrypoints weren't in ascending order
     *  @throws IllegalStateException   if you didn't call enter()
     */
    public final boolean hadSectionSwitch(IExecutionOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("the argument order may not be null.");
        }
        if (this.currentSection == null) {
            if (this.lastQueriedMethod != null) {
                throw new IllegalStateException("You didn't call AbstractAndroidModel.enter(AT_FIRST) after a section-switch");
            }
            // The first method is added to the model
            // don't set this.currentSection here or enter() will not actually enter;)
            this.lastQueriedMethod = order;
            return true;
        }
        if (order.compareTo(lastQueriedMethod) < 0) {
            throw new IllegalArgumentException("This method is meant to be called on AndoidEntrypoints in ascending order");
        }
        if  ((currentSection.compareTo(lastQueriedMethod.getSection()) != 0) &&
            (order.getSection().compareTo(lastQueriedMethod.getSection()) == 0)) {
            throw new IllegalStateException("You didn't call AbstractAndroidModel.enter(" + order.getSection() + ") after a section-switch");
        }

        this.lastQueriedMethod = order;
        return (this.currentSection.compareTo(order.getSection()) < 0);
    }

     /**
     *  Gets called when Label ExecutionOrder.AT_FIRST got stepped over.
     *
     *  In most cases you don't want to invoke this function directly but to use 
     *  {@code enter(ExecutionOrder.AT_FIRST, int)} instead.
     *
     *  Sideeffects: currentSection is updated, instructions are inserted into the body
     *
     *  @param  PC          Program Counter instructions shall be placed at. In most cases
     *      you'll simply pass body.getNextProgramCounter()
     *  @return             Program Counter after insertion of the code
     */
    protected int enterAT_FIRST(int PC) { return PC; }

     /**
     *  Gets called when Label ExecutionOrder.BEFORE_LOOP got stepped over.
     *
     *  In most cases you don't want to invoke this function directly but to use 
     *  {@code enter(ExecutionOrder.BEFORE_LOOP, int)} instead
     *
     *  Sideeffects: currentSection is updated, instructions are inserted into the body
     *
     *  @param  PC          Program Counter instructions shall be placed at. In most cases
     *      you'll simply pass body.getNextProgramCounter()
     *  @return             Program Counter after insertion of the code
     */
    protected int enterBEFORE_LOOP (int PC) { return PC; }

    /**
     *  Gets called when Label ExecutionOrder.START_OF_LOOP got stepped over.
     *
     *  In most cases you don't want to invoke this function directly but to use 
     *  {@code enter(ExecutionOrder.START_OF_LOOP, int)} instead
     *
     *  Sideeffects: currentSection is updated, instructions are inserted into the body
     *
     *  @param  PC          Program Counter instructions shall be placed at. In most cases
     *      you'll simply pass body.getNextProgramCounter()
     *  @return             Program Counter after insertion of the code
     */
    protected int enterSTART_OF_LOOP (int PC) { return PC; }

    /**
     *  Gets called when Label ExecutionOrder.MIDDLE_OF_LOOP got stepped over.
     *
     *  In most cases you don't want to invoke this function directly but to use 
     *  {@code enter(ExecutionOrder.MIDDLE_OF_LOOP, int)} instead
     *
     *  Sideeffects: currentSection is updated, instructions are inserted into the body
     *
     *  @param  PC          Program Counter instructions shall be placed at. In most cases
     *      you'll simply pass body.getNextProgramCounter()
     *  @return             Program Counter after insertion of the code
     */
    protected int enterMIDDLE_OF_LOOP (int PC) { return PC; }
    
    /**
     *  Gets called when Label ExecutionOrder.MULTIPLE_TIMES_IN_LOOP got stepped over.
     *
     *  In most cases you don't want to invoke this function directly but to use 
     *  {@code enter(ExecutionOrder.MULTIPLE_TIMES_IN_LOOP, int)} instead
     *
     *  Sideeffects: currentSection is updated, instructions are inserted into the body
     *
     *  @param  PC          Program Counter instructions shall be placed at. In most cases
     *      you'll simply pass body.getNextProgramCounter()
     *  @return             Program Counter after insertion of the code
     */
    protected int enterMULTIPLE_TIMES_IN_LOOP (int PC) { return PC; }

    /**
     *  Gets called when Label ExecutionOrder.END_OF_LOOP got stepped over.
     *
     *  In most cases you don't want to invoke this function directly but to use 
     *  {@code enter(ExecutionOrder.END_OF_LOOP, int)} instead
     *
     *  Sideeffects: currentSection is updated, instructions are inserted into the body
     *
     *  @param  PC          Program Counter instructions shall be placed at. In most cases
     *      you'll simply pass body.getNextProgramCounter()
     *  @return             Program Counter after insertion of the code
     */
    protected int enterEND_OF_LOOP (int PC) { return PC; }

    /**
     *  Gets called when Label ExecutionOrder.AFTER_LOOP got stepped over.
     *
     *  In most cases you don't want to invoke this function directly but to use 
     *  {@code enter(ExecutionOrder.AFTER_LOOP, int)} instead
     *
     *  Sideeffects: currentSection is updated, instructions are inserted into the body
     *
     *  @param  PC          Program Counter instructions shall be placed at. In most cases
     *      you'll simply pass body.getNextProgramCounter()
     *  @return             Program Counter after insertion of the code
     */
    protected int enterAFTER_LOOP (int PC) { return PC; }

    /**
     *  Gets called when Label ExecutionOrder.AT_LAST got stepped over.
     *
     *  In most cases you don't want to invoke this function directly but to use 
     *  {@code enter(ExecutionOrder.AT_LAST, int)} instead
     *  
     *  Sideeffects: currentSection is updated, instructions are inserted into the body
     *
     *  @param  PC          Program Counter instructions shall be placed at. In most cases
     *      you'll simply pass body.getNextProgramCounter()
     *  @return             Program Counter after insertion of the code
     */
    protected int enterAT_LAST (int PC) { return PC; }

    /**
     *  Gets called when the model gets finished.
     *
     *  In most cases you don't want to invoke this function directly but to use 
     *  {@link #finish(int)} instead
     *  
     *  Sideeffects: instructions are inserted into the body
     *
     *  @param  PC          Program Counter instructions shall be placed at. In most cases
     *      you'll simply pass body.getNextProgramCounter()
     *  @return             Program Counter after insertion of the code
     */
    protected int leaveAT_LAST (int PC) { return PC; }

    /**
     *  Dispatches to the enterLABEL-functions. Does also call functions to any labels that
     *  got stepped over.
     *
     *  @param  section     The Section to enter
     *  @param  PC          Program Counter instructions shall be placed at. In most cases
     *      you'll simply pass body.getNextProgramCounter()
     *  @return             Program Counter after insertion of the code
     *
     *  @throws IllegalArgumentException if you didn't use sections in ascending order, pc is negative
     */
    public int enter (ExecutionOrder section, int PC) {
        section = section.getSection(); // Just to be shure

        if ((this.currentSection != null) && (this.currentSection.compareTo(section) >= 0)) {
            if (this.currentSection.compareTo(section) == 0) {
                logger.error("You entered {} twice! Ignoring second atempt.", section);
            } else {
                throw new IllegalArgumentException("Sections must be in ascending order! When trying to " +
                    "enter " + this.currentSection.toString() + " from " + section.toString());
            }
        }

        if (PC < 0) {
            throw new IllegalArgumentException("The PC can't be negative!");
        }


        if (section.compareTo(AndroidEntryPoint.ExecutionOrder.AT_FIRST) == 0) {
            if (this.currentSection != null) {
                throw new IllegalArgumentException("Sections must be in ascending order!");
            }
        }
        if ((this.currentSection == null) &&
                (section.compareTo(AndroidEntryPoint.ExecutionOrder.AT_FIRST) >= 0)) {
            logger.info("ENTER: AT_FIRST");
            PC = enterAT_FIRST(PC);
            this.currentSection = AndroidEntryPoint.ExecutionOrder.AT_FIRST;
        }

        if ((this.currentSection.compareTo(AndroidEntryPoint.ExecutionOrder.AT_FIRST) <= 0) &&
                (section.compareTo(AndroidEntryPoint.ExecutionOrder.BEFORE_LOOP) >= 0)) {
            logger.info("ENTER: BEFORE_LOOP");
            PC = enterBEFORE_LOOP(PC);
            this.currentSection = AndroidEntryPoint.ExecutionOrder.BEFORE_LOOP;
        }

        if ((this.currentSection.compareTo(AndroidEntryPoint.ExecutionOrder.BEFORE_LOOP) <= 0) &&
                (section.compareTo(AndroidEntryPoint.ExecutionOrder.START_OF_LOOP) >= 0)) {
            logger.info("ENTER: START_OF_LOOP");
            PC = enterSTART_OF_LOOP(PC);
            this.currentSection = AndroidEntryPoint.ExecutionOrder.START_OF_LOOP;
        }

        if ((this.currentSection.compareTo(AndroidEntryPoint.ExecutionOrder.START_OF_LOOP) <= 0) &&
                (section.compareTo(AndroidEntryPoint.ExecutionOrder.MIDDLE_OF_LOOP) >= 0)) {
            logger.info("ENTER: MIDDLE_OF_LOOP");
            PC = enterMIDDLE_OF_LOOP(PC);
            this.currentSection = AndroidEntryPoint.ExecutionOrder.MIDDLE_OF_LOOP;
        }

        if ((this.currentSection.compareTo(AndroidEntryPoint.ExecutionOrder.MIDDLE_OF_LOOP) <= 0) &&
                (section.compareTo(AndroidEntryPoint.ExecutionOrder.MULTIPLE_TIMES_IN_LOOP) >= 0)) {
            PC = enterMULTIPLE_TIMES_IN_LOOP(PC);
            logger.info("ENTER: MULTIPLE_TIMES_IN_LOOP");
            this.currentSection = AndroidEntryPoint.ExecutionOrder.MULTIPLE_TIMES_IN_LOOP;
        }

        if ((this.currentSection.compareTo(AndroidEntryPoint.ExecutionOrder.MULTIPLE_TIMES_IN_LOOP) <= 0) &&
                (section.compareTo(AndroidEntryPoint.ExecutionOrder.END_OF_LOOP) >= 0)) {
            logger.info("ENTER: END_OF_LOOP");
            PC = enterEND_OF_LOOP(PC);
            this.currentSection = AndroidEntryPoint.ExecutionOrder.END_OF_LOOP;
        }

        if ((this.currentSection.compareTo(AndroidEntryPoint.ExecutionOrder.END_OF_LOOP) <= 0) &&
                (section.compareTo(AndroidEntryPoint.ExecutionOrder.AFTER_LOOP) >= 0)) {
            logger.info("ENTER: AFTER_LOOP");
            PC = enterAFTER_LOOP(PC);
            this.currentSection = AndroidEntryPoint.ExecutionOrder.AFTER_LOOP;
        }

        if ((this.currentSection.compareTo(AndroidEntryPoint.ExecutionOrder.AFTER_LOOP) <= 0) &&
            (section.compareTo(AndroidEntryPoint.ExecutionOrder.AT_LAST) >= 0)) {
            logger.info("ENTER: AT_LAST");
            PC = enterAT_LAST(PC);
            this.currentSection = AndroidEntryPoint.ExecutionOrder.AT_LAST;
        }

        return PC;
    }

    /**
     *  Calls all remaining enterLABEL-functions, finally calls leaveAT_LAST.
     *
     *  Then Locks the model and frees some memory.
     *
     *  @param  PC          Program Counter instructions shall be placed at. In most cases
     *      you'll simply pass body.getNextProgramCounter()
     *  @return             Program Counter after insertion of the code
     *
     *  @throws IllegalStateException if called on an empty model
     */
    public int finish (int PC) { /* package private */
        if (this.currentSection == null) {
            throw new IllegalStateException("Called finish() on a model that doesn't " +
                    "contain any sections - an empty model of" + this.body.getMethod().toString());
        }
        if ((this.currentSection.compareTo(AndroidEntryPoint.ExecutionOrder.AT_LAST) < 0)) {
            PC = enter(AndroidEntryPoint.ExecutionOrder.AT_LAST, PC);
        }
        PC = leaveAT_LAST(PC);

        // Lock everything:
        currentSection = new ExecutionOrder(Integer.MAX_VALUE);
        // Free memory:
        body = null;
        insts = null;
        paramManager = null;
        entryPoints = null;
        lastQueriedMethod = null;

        return PC;
    }
}
