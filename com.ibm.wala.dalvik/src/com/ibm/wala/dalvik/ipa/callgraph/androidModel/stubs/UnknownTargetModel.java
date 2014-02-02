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

import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.dalvik.util.AndroidTypes;
import com.ibm.wala.types.Selector;
import com.ibm.wala.ipa.callgraph.impl.FakeRootClass;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModelClass;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.MiniModel;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentStarters.StarterFlags;

import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.Instantiator;
import com.ibm.wala.util.ssa.SSAValueManager;

import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary; 
import com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.shrikeBT.IInvokeInstruction;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.ExecutionOrder;
import com.ibm.wala.classLoader.IClass;

import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.summaries.BypassMethodTargetSelector;
import com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader;
import com.ibm.wala.types.ClassLoaderReference;

import com.ibm.wala.ssa.IR;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.SSAOptions;

import com.ibm.wala.util.ssa.ParameterAccessor;
import com.ibm.wala.util.ssa.ParameterAccessor.Parameter;
import com.ibm.wala.util.ssa.SSAValue;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import com.ibm.wala.util.CancelException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;

/**
 *  This Model is used to start an Android-Component of unknown Target.
 *  
 *  All internal Components of a Type (if given) then an ExternalModel is called.
 *  Used by the IntentContextInterpreter.
 *
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextInterpreter
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-10-18
 */
public class UnknownTargetModel  extends AndroidModel {
    private static Logger logger = LoggerFactory.getLogger(UnknownTargetModel.class);

    public final Atom name;
    private final AndroidComponent target;
    // uses AndroidModel.cha;
    private final MiniModel allInternal;
    private final ExternalModel external;

    /**
     *  @param  target  Component Type, may be null
     */
    public UnknownTargetModel(final IClassHierarchy cha, final AnalysisOptions options, final AnalysisCache cache, 
            AndroidComponent target) throws CancelException {
        super(cha, options, cache);
        
        if (target == null) {   // TODO: Enable
            throw new IllegalArgumentException("The component type requested to create an UnknownTargetModel for was null");
        }
        this.name = Atom.findOrCreateAsciiAtom("startUnknown" + target.toString());
        this.target = target;

        this.allInternal = new MiniModel(cha, options, cache, target);
        this.external = new ExternalModel(cha, options, cache, target);

        logger.debug("Will be known as {}/{}", AndroidModelClass.ANDROID_MODEL_CLASS.getName(), this.name); 
    }

    //@Override
    private void register(SummarizedMethod model) {
        AndroidModelClass mClass = AndroidModelClass.getInstance(cha);
        if (!(mClass.containsMethod(model.getSelector()))) {
            mClass.addMethod(model);
        }
    }

    @Override
    public SummarizedMethod getMethod() throws CancelException {
        if (!built) {
            super.build(this.name);
            this.register(super.model);
        }

        return super.model; 
    }
 
    
    @Override
    protected boolean selectEntryPoint(AndroidEntryPoint ep) {
        try {
            if (! (ep.getMethod().equals(this.allInternal.getMethod()) || ep.getMethod().equals(this.external.getMethod()))) {
                // logger.error("Asked for unexpected EP: " + ep); - is ok
                return false;
            } else {
                return true;
            }
        } catch (CancelException e) {
            throw new IllegalStateException(e);
        }
    }
 
    @Override
    protected void build(Atom name, Collection<? extends AndroidEntryPoint> entrypoints) throws CancelException {
        // Start evil hack(TM)
        final AndroidEntryPoint internalAsEp = new AndroidEntryPoint(new ExecutionOrder(1), this.allInternal.getMethod(), this.cha);
        final AndroidEntryPoint externalAsEp = new AndroidEntryPoint(new ExecutionOrder(2), this.external.getMethod(), this.cha);
        List<AndroidEntryPoint> pseudoEps = new ArrayList<AndroidEntryPoint>(2);
        pseudoEps.add(internalAsEp);
        pseudoEps.add(externalAsEp);
        super.build(name, pseudoEps);
    }

    /*
    @Override 
    protected void build(Atom name, Iterable<? extends Entrypoint> entrypoints) throws CancelException {

        final IMethod allInternalMethod = allInternal.getMethod();
        final int allIntenalParamCount = allInternalMethod.getNumberOfParameters();
        final IMethod externalMethod = external.getMethod();
        final int externalParamcount = externalMethod.getNumberOfParameters();

        final List<TypeName> myParams = new ArrayList<TypeName>(allIntenalParamCount);

        for (int i = 0 ; i < allIntenalParamCount; ++i) {
            myParams.add(allInternalMethod.getParameterType(i).getName());
        }

        for (int i = 0 ; i < externalParamcount; ++i) {
            final TypeName param = externalMethod.getParameterType(i).getName();
            if (!(myParams.contains(param))) {
                myParams.add(param);
            }
        }

        TypeName[] aMyParams = new TypeName[myParams.size()];
        aMyParams = myParams.toArray(aMyParams);

        this.descr = Descriptor.findOrCreate(aMyParams, TypeReference.VoidName);
        this.mRef = MethodReference.findOrCreate(AndroidModelClass.ANDROID_MODEL_CLASS, name, this.descr);
        final Selector selector = this.mRef.getSelector();

        // Assert not registered yet
        final AndroidModelClass mClass = AndroidModelClass.getInstance(this.cha);
        this.klass = mClass;
        if (mClass.containsMethod(selector)) {
            this.model = (SummarizedMethod) mClass.getMethod(selector);
            return;
        }

         this.body = new VolatileMethodSummary(new MethodSummary(this.mRef));
         this.body.setStatic(true);

         logger.debug("The Selector of the method will be " + selector);
         populate(null);

         this.model = new SummarizedMethod(this.mRef, this.body.getMethodSummary(), this.klass) {
            @Override
            public TypeReference getParameterType (int i) {
                IClassHierarchy cha = getClassHierarchy();
                TypeReference tRef = super.getParameterType(i);

                if (tRef.isClassType()) {
                    if (cha.lookupClass(tRef) != null) {
                        return tRef;
                    } else {
                        for (IClass c : cha) {
                            if (c.getName().toString().equals(tRef.getName().toString())) {
                                return c.getReference();
                            }
                        }
                    }

                    throw new IllegalStateException("Error looking up " + tRef);
                } else {
                    return tRef;
                }
            }
        };

        this.built = true;
    }*/

    /**
     *  Fill the model with instructions.
     *
     *  Call both models: ExternalModel, MiniModel
     */
     //@Override
     private void populate(Iterable<? extends AndroidEntryPoint> entrypoints) throws CancelException {
        assert (! built) : "You can only build once";

        final ParameterAccessor internalAcc = new ParameterAccessor(this.allInternal.getMethod());
        final ParameterAccessor externalAcc = new ParameterAccessor(this.external.getMethod());
        final ParameterAccessor thisAcc = new ParameterAccessor(this.mRef, /* hasImplicitThis */ false);
        final SSAValueManager pm = new SSAValueManager(thisAcc);
        final JavaInstructionFactory instructionFactory = new JavaInstructionFactory(); // TODO: Use a typesafe factory?
        final Instantiator instantiator = new Instantiator(this.body, new TypeSafeInstructionFactory(getClassHierarchy()), 
                pm, getClassHierarchy(), this.mRef, this.scope);

        int nextLocal = thisAcc.getFirstAfter();    // TODO: Use manager?
      
        final List<SSAValue> internalArgs; 
        { // Call the MiniModel
            // Map through the parameters of this.mRef to this.allInternal
            final List<SSAValue> args = thisAcc.connectThrough(internalAcc, null, null, getClassHierarchy(), instantiator, false,
                    null, null);
            internalArgs = args;
            final IMethod allInternalMethod = this.allInternal.getMethod();

            {
                logger.debug("Calling {} using {}", this.allInternal.getMethod(), args);
                final int callPC = this.body.getNextProgramCounter();
                final CallSiteReference site = CallSiteReference.make(callPC, allInternalMethod.getReference(),
                        IInvokeInstruction.Dispatch.STATIC);
                final int exception = nextLocal++;
                assert (exception > 0);
                final SSAInstruction invokation = instructionFactory.InvokeInstruction(callPC, 
                        internalAcc.forInvokeStatic(args), exception, site);
                this.body.addStatement(invokation);
            }
        }

        final int externalReturnIntent = nextLocal++;
        {   // Call the external model
            final List<SSAValue> args = thisAcc.connectThrough(externalAcc, null, null /*internalArgs*/, getClassHierarchy(), instantiator,
                    false, null, null);
            final IMethod externalMethod = this.external.getMethod();

            {
                logger.debug("Calling {} using parameters {}", externalMethod, args);
                final int callPC = this.body.getNextProgramCounter();
                final CallSiteReference site = CallSiteReference.make(callPC, externalMethod.getReference(),
                        IInvokeInstruction.Dispatch.STATIC);
                final int exception = nextLocal++;
                final SSAInstruction invokation = instructionFactory.InvokeInstruction(callPC, externalReturnIntent, 
                        externalAcc.forInvokeStatic(args), exception, site);
                this.body.addStatement(invokation);
            }
        }
       
        // TODO: Phi-Together returnIntents and return it. Or at least handle external
    }

}


