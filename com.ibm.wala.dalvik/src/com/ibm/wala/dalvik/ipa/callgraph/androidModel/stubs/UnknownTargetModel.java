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
package com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModelClass;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.MiniModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.Instantiator;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.ssa.ParameterAccessor;
import com.ibm.wala.util.ssa.SSAValue;
import com.ibm.wala.util.ssa.SSAValueManager;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;
import com.ibm.wala.util.strings.Atom;

/**
 *  This Model is used to start an Android-Component of unknown Target.
 *  
 *  All internal Components of a Type (if given) then an ExternalModel is called.
 *  Used by the IntentContextInterpreter.
 *
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextInterpreter
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-10-18
 */
public class UnknownTargetModel  extends AndroidModel {
    public final Atom name;
    private boolean doMini = true;
    private MiniModel miniModel = null;
    private ExternalModel externalModel = null;
    private final AndroidComponent target;
    // uses AndroidModel.cha;

    /**
     *  The UnknownTargetModel does not call any entrypoints on it's own.
     *
     *  Instead it first creates a restricted AndroidModel and an ExternalModel.
     *  These are actually called.
     */
    @Override
    protected boolean selectEntryPoint(AndroidEntryPoint ep) {
        return false;
    }
   
    /**
     *  @param  target  Component Type, may be null: No restrictions are imposed on AndroidModel then
     */
    public UnknownTargetModel(final IClassHierarchy cha, final AnalysisOptions options, final IAnalysisCacheView cache, 
            AndroidComponent target) {
        super(cha, options, cache);
        
        if (target == null) {   // TODO: Enable
            throw new IllegalArgumentException("The component type requested to create an UnknownTargetModel for was null");
        }
        String sName = target.toString();
        String cName = Character.toUpperCase(sName.charAt(0)) + sName.substring(1).toLowerCase();
        this.name = Atom.findOrCreateAsciiAtom("startUnknown" + cName);
        this.target = target;

        //this.allInternal = new MiniModel(cha, options, cache, target);
        //this.external = new ExternalModel(cha, options, cache, target);
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
    protected void build(Atom name, Collection<? extends AndroidEntryPoint> entrypoints) throws CancelException {
        assert ((entrypoints == null) || (! entrypoints.iterator().hasNext()));

        {   // Check if this Application has components, that implement target. If not we don't
            // have to build a MiniModel.
            doMini = false;
            for (final AndroidEntryPoint ep : AndroidEntryPointManager.ENTRIES) {
                if (ep.belongsTo(this.target)) {
                    doMini = true;
                    break;
                }
            }
        }
       
        if (doMini) {
            miniModel = new MiniModel(this.cha, this.options, this.cache, this.target);
        }
        externalModel = new ExternalModel(this.cha, this.options, this.cache, this.target);

        final Descriptor descr;
//        final Selector selector;
        {
            if (doMini) {
                final TypeName[] othersA = miniModel.getDescriptor().getParameters();
                final Set<TypeName> others;
                if (othersA != null) {
                    others = new HashSet<>(Arrays.asList(othersA));
                } else {
                    
                    others = new HashSet<>();
                }
                doMini = others.size() > 0;
                others.addAll(Arrays.asList(externalModel.getDescriptor().getParameters()));
                descr = Descriptor.findOrCreate(others.toArray(new TypeName[] {}), TypeReference.VoidName); // Return the intent of external? TODO
            } else {
                descr = Descriptor.findOrCreate(externalModel.getDescriptor().getParameters(), TypeReference.VoidName);
           }
//           selector = new Selector(name, descr);
        }

        /*{   // Skip construction if there already exists a model wit this name. This should
            // not happen.
            final AndroidModelClass mClass = AndroidModelClass.getInstance(this.cha);
            if (mClass.containsMethod(selector)) {
                
                this.built = true;
                this.model = (SummarizedMethod) mClass.getMethod(selector);
                return;
            }
        } // */

        {   // Set some properties of the later method
            this.klass = AndroidModelClass.getInstance(this.cha);
            this.mRef = MethodReference.findOrCreate(AndroidModelClass.ANDROID_MODEL_CLASS, name, descr);
            this.body = new VolatileMethodSummary(new MethodSummary(this.mRef));
            this.body.setStatic(true);
        }

        {   // Start building
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
            }; // of this.model
        }

        this.built = true;
    }

    /**
     *  Fill the model with instructions.
     *
     *  Call both models: ExternalModel, MiniModel
     */
     //@Override
     private void populate(Iterable<? extends AndroidEntryPoint> entrypoints) throws CancelException {
        assert ((entrypoints == null) || (! entrypoints.iterator().hasNext())); 
        assert (! built) : "You can only build once";

        final TypeSafeInstructionFactory instructionFactory = new TypeSafeInstructionFactory(this.cha);
        final ParameterAccessor pAcc = new ParameterAccessor(this.mRef, /* hasImplicitThis */ false);
        final SSAValueManager pm = new SSAValueManager(pAcc);
        final Instantiator instantiator = new Instantiator(this.body, instructionFactory, pm, this.cha, this.mRef, this.scope);

        if (doMini) { // Call a MiniModel
            //final MiniModel miniModel = new MiniModel(this.cha, this.options, this.cache, this.target);
            final IMethod mini = miniModel.getMethod();
            final ParameterAccessor miniAcc = new ParameterAccessor(mini);
            final List<SSAValue> params = pAcc.connectThrough(miniAcc, null, null, this.cha, instantiator, false, null, null);
            final SSAValue excpetion = pm.getException();
            final int pc = this.body.getNextProgramCounter();
            final CallSiteReference site = CallSiteReference.make(pc, mini.getReference(), IInvokeInstruction.Dispatch.STATIC);
            final SSAInstruction invokation = instructionFactory.InvokeInstruction(pc, params, excpetion, site);
            this.body.addStatement(invokation);
        }

        final SSAValue extRet;
        { // Call the externalTarget Model
            //final ExternalModel externalModel = new ExternalModel(this.cha, this.options, this.cache, this.target);
            final IMethod external = externalModel.getMethod();
            final ParameterAccessor externalAcc = new ParameterAccessor(external);
            final List<SSAValue> params = pAcc.connectThrough(externalAcc, null, null, this.cha, instantiator, false, null, null);
            final SSAValue excpetion = pm.getException();
            extRet = pm.getUnmanaged(external.getReturnType() , "extRet");
            final int pc = this.body.getNextProgramCounter();
            final CallSiteReference site = CallSiteReference.make(pc, external.getReference(), IInvokeInstruction.Dispatch.STATIC);
            final SSAInstruction invokation = instructionFactory.InvokeInstruction(pc, extRet, params, excpetion, site);
            this.body.addStatement(invokation);
        }
        // TODO: Do somethig with extRet?

        this.body.setLocalNames(pm.makeLocalNames());
     }
        /*
        final ParameterAccessor internalAcc = new ParameterAccessor(this.allInternal.getMethod());
        final ParameterAccessor externalAcc = new ParameterAccessor(this.external.getMethod());
        final ParameterAccessor thisAcc = new ParameterAccessor(this.mRef,  false);
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
            final List<SSAValue> args = thisAcc.connectThrough(externalAcc, null, null , getClassHierarchy(), instantiator,
                    false, null, null);
            final IMethod externalMethod = this.external.getMethod();

            {
                
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
    }*/

}


