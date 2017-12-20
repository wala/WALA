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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModelClass;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.dalvik.util.AndroidTypes;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.ssa.ParameterAccessor;
import com.ibm.wala.util.ssa.SSAValue;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;
import com.ibm.wala.util.strings.Atom;

/**
 *  This is generates a dummy for the call to an external Activity.
 *
 *  Is used by the IntentContextInterpreter if an Intent is marked as beeing external.
 *
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextInterpreter
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-10-15
 */
public class ExternalModel extends AndroidModel {

    public final Atom name;
    private SummarizedMethod activityModel;
//    private final AndroidComponent target;
    // uses AndroidModel.cha;

    /**
     *  Do not call any EntryPoint.
     *  
     *  {@inheritDoc}
     */
    @Override
    protected boolean selectEntryPoint(AndroidEntryPoint ep) {
        return false;
    }

    public ExternalModel(final IClassHierarchy cha, final AnalysisOptions options, final IAnalysisCacheView cache, 
            AndroidComponent target) {
        super(cha, options, cache);
        
        if (target == null) {
            throw new IllegalArgumentException("The component type requested to create an ExternalModel for was null");
        }
        this.name = Atom.findOrCreateAsciiAtom("startExternal" + target.toString());
//        this.target = target;

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
            this.activityModel = super.model;
        }

        return this.activityModel; 
    }

    @Override 
    protected void build(Atom name, Collection<? extends AndroidEntryPoint> entrypoints) {
        assert ((entrypoints == null) || (! entrypoints.iterator().hasNext()));

        final TypeName intentName = AndroidTypes.IntentName;
        final TypeName bundleName = AndroidTypes.BundleName;
        final Descriptor descr = Descriptor.findOrCreate(new TypeName[] {
                            intentName, TypeReference.IntName, bundleName}, intentName);
        this.mRef = MethodReference.findOrCreate(AndroidModelClass.ANDROID_MODEL_CLASS, name, descr);
        final Selector selector = new Selector(name, descr); 

        // Assert not registered yet
        final AndroidModelClass mClass = AndroidModelClass.getInstance(this.cha);
        if (mClass.containsMethod(selector)) {
            this.model = (SummarizedMethod) mClass.getMethod(selector);
            return;
        }

         this.body = new VolatileMethodSummary(new MethodSummary(this.mRef));
         this.body.setStatic(true);

         
         populate(null);

         this.klass = AndroidModelClass.getInstance(this.cha);

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
    }

    /**
     *  Fill the model with instructions.
     *
     *  Read the extra-data associated with the Intent. 
     *  Read the optional bundle argument
     *  Write data to the Intent to return.
     */
    //@Override
    private void populate(Iterable<? extends AndroidEntryPoint> entrypoints) {
        assert ((entrypoints == null) || (! entrypoints.iterator().hasNext()));
        assert (! built) : "You can only build once";
        final TypeSafeInstructionFactory instructionFactory = new TypeSafeInstructionFactory(getClassHierarchy());

        // mRef is:  startExternal...(Intent, int, Bundle) --> Intent
        final ParameterAccessor pAcc = new ParameterAccessor(this.mRef, /* hasImplicitThis */ false);
//        final AndroidModelParameterManager pm = new AndroidModelParameterManager(pAcc);

        // See this.build() for parameter mapping
        final SSAValue inIntent = pAcc.firstOf(AndroidTypes.IntentName);
        assert (inIntent.getNumber() == 1);
        final SSAValue inBundle = pAcc.firstOf(AndroidTypes.BundleName);
        assert (inBundle.getNumber() == 3) : "Wrong bundle " + inBundle + " of " + this.mRef;   // TODO: Verify was 2
        int nextLocal = pAcc.getFirstAfter();
        //assert (nextLocal == 4);    // was 3

        SSAValue outBundle;
        SSAValue outIntent;

        { // Read out Intent extras
             

            final int callPC = this.body.getNextProgramCounter();
            // Bundle Intent.getExtras()
            final Selector mSel = Selector.make("getExtras()Landroid/os/Bundle;");
            final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.Intent, mSel);
            final CallSiteReference site = CallSiteReference.make(callPC, mRef, IInvokeInstruction.Dispatch.VIRTUAL);
            final SSAValue exception = new SSAValue(nextLocal++, TypeReference.JavaLangException, this.mRef, "exception");
            outBundle = new SSAValue(nextLocal++, inBundle);
            final List<SSAValue> params = new ArrayList<>(1);
            params.add(inIntent);
            final SSAInstruction invokation = instructionFactory.InvokeInstruction(callPC, outBundle, 
                    params, exception, site);
            this.body.addStatement(invokation);            
        }

        /*{ // Read from the bundle returned by the Intent extras           // TODO Defunct
             
            // TODO: If I clone it - does it access all?
            
            final int callPC = this.body.getNextProgramCounter();
            // Bundle Intent.getExtras()
            final Selector mSel = Selector.make("clone()Landroid/os/Bundle;");
            final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.Bundle, mSel);
            final CallSiteReference site = CallSiteReference.make(callPC, mRef, IInvokeInstruction.Dispatch.VIRTUAL);
            final SSAValue exception = new SSAValue(nextLocal++, TypeReference.JavaLangException, this.mRef, "exception");
            final SSAValue myBundle = outBundle;
            outBundle = new SSAValue(nextLocal++, outBundle);
            final List<SSAValue> params = new ArrayList<SSAValue>(1);
            params.add(myBundle);
            final SSAInstruction invokation = instructionFactory.InvokeInstruction(callPC, outBundle, 
                    params, exception, site);
            this.body.addStatement(invokation);            
        }

        { // Read from the bundle given as argument
             
            // TODO: If I clone it - does it access all?
            final int callPC = this.body.getNextProgramCounter();
            // Bundle Intent.getExtras()
            final Selector mSel = Selector.make("clone()Landroid/os/Bundle;");
            final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.Bundle, mSel);
            final CallSiteReference site = CallSiteReference.make(callPC, mRef, IInvokeInstruction.Dispatch.VIRTUAL);
            final SSAValue exception = new SSAValue(nextLocal++, TypeReference.JavaLangException, this.mRef, "exception");
            outBundle = new SSAValue(nextLocal++, outBundle);
            final List<SSAValue> params = new ArrayList<SSAValue>(1);
            params.add(inBundle);
            final SSAInstruction invokation = instructionFactory.InvokeInstruction(callPC, outBundle, 
                    params, exception, site);
            this.body.addStatement(invokation);            
        }*/

        { // Call Intent.putExtra(String name, int value) do add some new info
            
            final SSAValue outName = new SSAValue(nextLocal++, TypeReference.JavaLangString, this.mRef, "outName");
            this.body.addConstant(outName.getNumber(), new ConstantValue("my.extra.object"));
            final SSAValue outValue = new SSAValue(nextLocal++, TypeReference.Int, this.mRef, "outValue");   // Assign value?

            final int callPC = this.body.getNextProgramCounter();
            // void onActivityResult (int requestCode, int resultCode, Intent data)
            final Selector mSel = Selector.make("putExtra(Ljava/lang/String;I)Landroid/content/Intent;");
            final MethodReference mRef = MethodReference.findOrCreate(AndroidTypes.Intent, mSel);
            final CallSiteReference site = CallSiteReference.make(callPC, mRef, IInvokeInstruction.Dispatch.VIRTUAL);
            final SSAValue exception = new SSAValue(nextLocal++, TypeReference.JavaLangException, this.mRef, "exception");
            outIntent = new SSAValue(nextLocal++, inIntent);
            final List<SSAValue> params = new ArrayList<>(3);
            params.add(inIntent);
            params.add(outName);
            params.add(outValue);
            final SSAInstruction invokation = instructionFactory.InvokeInstruction(callPC, outIntent, params, 
                    exception, site);
            this.body.addStatement(invokation);            
        }

        { // Add return statement on intent
            

            final int returnPC = this.body.getNextProgramCounter();
            final SSAInstruction returnInstruction = instructionFactory.ReturnInstruction(returnPC, outIntent);
            this.body.addStatement(returnInstruction);
        }
    }
}


