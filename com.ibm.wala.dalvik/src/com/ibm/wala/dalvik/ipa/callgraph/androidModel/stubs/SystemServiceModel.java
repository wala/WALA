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

import java.util.Collection;
import java.util.HashSet;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModelClass;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.Instantiator;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.util.AndroidTypes;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.ssa.ParameterAccessor;
import com.ibm.wala.util.ssa.SSAValue;
import com.ibm.wala.util.ssa.SSAValueManager;
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
public class SystemServiceModel extends AndroidModel {
    public final Atom name;
    private SummarizedMethod activityModel;
    private final String target;
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

    public SystemServiceModel(final IClassHierarchy cha, final AnalysisOptions options, final IAnalysisCacheView cache, 
            Atom target) {
        super(cha, options, cache);
        
        if (target == null) {
            throw new IllegalArgumentException("The target requested to create an SystemServiceModel for was null");
        }
        String sName = target.toString();
        String cName = Character.toUpperCase(sName.charAt(0)) + sName.substring(1);
        this.name = Atom.findOrCreateAsciiAtom("startSystemService" + cName);
        this.target = target.toString();
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

        final Descriptor descr = Descriptor.findOrCreate(new TypeName[] {
                            AndroidTypes.ContextName}, TypeName.string2TypeName("Ljava/lang/Object"));

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
     *  TODO: use "global" instances
     */
    //@Override
    private void populate(Iterable<? extends AndroidEntryPoint> entrypoints) {
        assert ((entrypoints == null) || (! entrypoints.iterator().hasNext()));
        assert (! built) : "You can only build once";
        
        final TypeSafeInstructionFactory instructionFactory = new TypeSafeInstructionFactory(getClassHierarchy());
        final VolatileMethodSummary body = this.body;
        final ParameterAccessor pAcc = new ParameterAccessor(this.mRef, /* hasImplicitThis */ false);
        final SSAValueManager pm = new SSAValueManager(pAcc);
        final Instantiator instantiator = new Instantiator(body, instructionFactory, pm, cha, mRef, scope); 

        //final SSAValue context = pAcc.firstOf(AndroidTypes.ContextName);
        final SSAValue retVal;

        if (this.target.equals("phone")) {
            
            retVal = instantiator.createInstance(AndroidTypes.TelephonyManager, false, new SSAValue.UniqueKey(), new HashSet<>(pAcc.all()));
        //} else if (this.target.equals("Lwindow")) { // TODO: Is an interface
        //     
        //    final TypeName wmN = TypeName.findOrCreate("Landroid/view/WindowManager");
        //    final TypeReference wmT = TypeReference.findOrCreate(ClassLoaderReference.Primordial, wmN);
        //    retVal = instantiator.createInstance(wmT, false, new SSAValue.UniqueKey(), new HashSet<Parameter>(pAcc.all()));
        //} else if (this.target.equals("Llayout_inflater")) {
        //} else if (this.target.equals("Lactivity")) {
        //} else if (this.target.equals("Lpower")) {
        //} else if (this.target.equals("Lalarm")) {
        //} else if (this.target.equals("Lnotification")) {
        } else if (this.target.equals("keyguard")) {
             
            final TypeName n = TypeName.findOrCreate("Landroid/app/KeyguardManager");
            final TypeReference T = TypeReference.findOrCreate(ClassLoaderReference.Primordial, n);
            retVal = instantiator.createInstance(T, false, new SSAValue.UniqueKey(), new HashSet<>(pAcc.all()));
        } else if (this.target.equals("location")) {
             
            final TypeName n = TypeName.findOrCreate("Landroid/location/LocationManager");
            final TypeReference T = TypeReference.findOrCreate(ClassLoaderReference.Primordial, n);
            retVal = instantiator.createInstance(T, false, new SSAValue.UniqueKey(), new HashSet<>(pAcc.all()));
        } else if (this.target.equals("search")) {
             // TODO: Param: Handler
            final TypeName n = TypeName.findOrCreate("Landroid/app/SearchManager");
            final TypeReference T = TypeReference.findOrCreate(ClassLoaderReference.Primordial, n);
            retVal = instantiator.createInstance(T, false, new SSAValue.UniqueKey(), new HashSet<>(pAcc.all()));
        //} else if (this.target.equals("Lvibrator")) { // TODO: Is abstract
        } else if (this.target.equals("connection")) {
             // TODO: use ConnectivityManager.from
            final TypeName n = TypeName.findOrCreate("Landroid/net/ConnectivityManager");
            final TypeReference T = TypeReference.findOrCreate(ClassLoaderReference.Primordial, n);
            retVal = instantiator.createInstance(T, false, new SSAValue.UniqueKey(), new HashSet<>(pAcc.all()));
        } else if (this.target.equals("wifi")) {
             // Handle Params: Context context, IWifiManager service
            final TypeName n = TypeName.findOrCreate("Landroid/net/wifi/WifiManager");
            final TypeReference T = TypeReference.findOrCreate(ClassLoaderReference.Primordial, n);
            retVal = instantiator.createInstance(T, false, new SSAValue.UniqueKey(), new HashSet<>(pAcc.all()));
        } else if (this.target.equals("input_method")) {
             // TODO: Use InputMethodManager.getInstance?
            final TypeName n = TypeName.findOrCreate("Landroid/view/inputmethod/InputMethodManager");
            final TypeReference T = TypeReference.findOrCreate(ClassLoaderReference.Primordial, n);
            retVal = instantiator.createInstance(T, false, new SSAValue.UniqueKey(), new HashSet<>(pAcc.all()));
        } else if (this.target.equals("uimode")) {
            
            final TypeName n = TypeName.findOrCreate("Landroid/app/UiModeManager");
            final TypeReference T = TypeReference.findOrCreate(ClassLoaderReference.Primordial, n);
            retVal = instantiator.createInstance(T, false, new SSAValue.UniqueKey(), new HashSet<>(pAcc.all()));
        } else if (this.target.equals("download")) {
                // TODO: Params ContentResolver resolver, String packageName
            final TypeName n = TypeName.findOrCreate("Landroid/app/DownloadManager");
            final TypeReference T = TypeReference.findOrCreate(ClassLoaderReference.Primordial, n);
            retVal = instantiator.createInstance(T, false, new SSAValue.UniqueKey(), new HashSet<>(pAcc.all()));
        } else {
            retVal = pm.getUnmanaged(TypeReference.JavaLangObject, "notFound");
            this.body.addConstant(retVal.getNumber(), new ConstantValue(null));
            retVal.setAssigned();
            
        }


        { // Add return statement on intent
            

            final int returnPC = this.body.getNextProgramCounter();
            final SSAInstruction returnInstruction = instructionFactory.ReturnInstruction(returnPC, retVal);
            this.body.addStatement(returnInstruction);
        }
    }
}


