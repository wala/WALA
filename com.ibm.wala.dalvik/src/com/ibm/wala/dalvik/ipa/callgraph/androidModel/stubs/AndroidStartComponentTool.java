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
package com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.dalvik.util.AndroidTypes;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;

import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.util.ssa.SSAValue;
import com.ibm.wala.util.ssa.SSAValueManager;
import com.ibm.wala.util.ssa.ParameterAccessor;
import com.ibm.wala.util.ssa.ParameterAccessor.Parameter;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;

import com.ibm.wala.util.strings.Atom;

import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModelClass;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentStarters.StarterFlags;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentStarters.StartInfo;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Grab and set data of AndroidClasses.
 *
 *  This class is only used by AndroidModel.getMethodAs() as it got a bit lengthy.
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-10-22
 */
public class AndroidStartComponentTool {
    private static Logger logger = LoggerFactory.getLogger(AndroidStartComponentTool.class);
    
    private final IClassHierarchy cha;
    private final MethodReference asMethod;
    private final Set<StarterFlags> flags;
    private final TypeReference caller;
    private final TypeSafeInstructionFactory instructionFactory;
    private final ParameterAccessor acc;
    private final SSAValueManager pm;
    private final VolatileMethodSummary redirect;
    private final Parameter self;
    private final StartInfo info;
    private final CGNode callerNd;
    private AndroidTypes.AndroidContextType callerContext;

    public AndroidStartComponentTool(final IClassHierarchy cha, final MethodReference asMethod, final Set<StarterFlags> flags,
            final TypeReference caller, final TypeSafeInstructionFactory instructionFactory, final ParameterAccessor acc,
            final SSAValueManager pm, final VolatileMethodSummary redirect, final Parameter self,
            final StartInfo info, final CGNode callerNd) {
        
        if (cha == null) {
            throw new IllegalArgumentException("cha may not be null");
        }
        if (asMethod == null) {
            throw new IllegalArgumentException("asMethod may not be null");
        }
        if (flags == null) {
            throw new IllegalArgumentException("Flags may not be null");
        }
        if ((caller == null) && (! flags.contains(StarterFlags.CONTEXT_FREE))) {
            throw new IllegalArgumentException("Caller may not be null if StarterFlags.CONTEXT_FREE is not set. Flags: " + flags);
        }
        if (instructionFactory == null) {
            throw new IllegalArgumentException("The instructionFactory may not be null");
        }
        if (acc == null) {
            throw new IllegalArgumentException("acc may not be null");
        }
        if (pm == null) {
            throw new IllegalArgumentException("pm may not be null");
        }
        if (redirect == null) {
            throw new IllegalArgumentException("self may not be null");
        }
        if (info == null) {
            throw new IllegalArgumentException("info may not be null");
        }
        //if ((callerNd == null) && (! flags.contains(StarterFlags.CONTEXT_FREE))) {
        //    throw new IllegalArgumentException("CallerNd may not be null if StarterFlags.CONTEXT_FREE is not set. Flags: " + flags);
        //}


        logger.debug("Starting Component {} from {} ", info, callerNd);
        this.cha = cha;
        this.asMethod = asMethod;
        this.flags = flags;
        this.caller = caller;
        this.instructionFactory = instructionFactory;
        this.acc = acc;
        this.pm = pm;
        this.redirect = redirect;
        this.self = self;
        this.info = info;
        this.callerNd = callerNd;
    }


    /**
     *  Grab mResultCode and mResultData.
     *
     *  This data is used to call onActivityResult of the caller.
     */
    public void fetchResults(List<? super SSAValue> resultCodes, List<? super SSAValue> resultData, 
            List<? extends SSAValue> allActivities) {
        for (SSAValue activity : allActivities) { 
            final SSAValue tmpResultCode = pm.getUnmanaged(TypeReference.Int, "mResultCode");
            { // Fetch mResultCode
                //redirect.setLocalName(tmpResultCode, "gotResultCode");
                logger.debug("Fetching ResultCode");

                final FieldReference mResultCodeRef = FieldReference.findOrCreate(AndroidTypes.Activity, Atom.findOrCreateAsciiAtom("mResultCode"),
                        TypeReference.Int);
                final int instPC = redirect.getNextProgramCounter();
                final SSAInstruction getInst = instructionFactory.GetInstruction(instPC, tmpResultCode, activity, mResultCodeRef);
                redirect.addStatement(getInst);
            }

            final SSAValue tmpResultData = pm.getUnmanaged(AndroidTypes.Intent, "mResultData");
            { // Fetch mResultData
                //redirect.setLocalName(tmpResultData, "gotResultData");
                logger.debug("Fetching Result data");

                final FieldReference mResultDataRef = FieldReference.findOrCreate(AndroidTypes.Activity, Atom.findOrCreateAsciiAtom("mResultData"),
                        AndroidTypes.Intent);
                final int instPC = redirect.getNextProgramCounter();
                final SSAInstruction getInst = instructionFactory.GetInstruction(instPC, tmpResultData, activity, mResultDataRef);
                redirect.addStatement(getInst);
            } // */

            resultCodes.add(tmpResultCode);
            resultData.add(tmpResultData);
        } // End: for all activities */

        assert (resultCodes.size() == resultData.size());
    }

    /**
     *  Add Phi (if necessary) - not if only one from.
     */
    public SSAValue addPhi(List<? extends SSAValue> from) {
        logger.debug("Add Phi({})", from);

        if (from.size() == 1) {
            return from.get(0);
        } else {
            final SSAValue retVal = this.pm.getUnmanaged(from.get(0).getType(), "forPhi");
            final int phiPC = redirect.getNextProgramCounter();
            final SSAInstruction phi = instructionFactory.PhiInstruction(phiPC, retVal, from);
            this.redirect.addStatement(phi);
            return retVal;
        }
    }
}
