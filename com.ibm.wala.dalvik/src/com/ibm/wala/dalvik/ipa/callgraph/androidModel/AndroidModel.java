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
package com.ibm.wala.dalvik.ipa.callgraph.androidModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.FlatInstantiator;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior.InstanceBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.Instantiator;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.ReuseParameters;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.AbstractAndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.AndroidBoot;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.AndroidStartComponentTool;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.ExternalModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.SystemServiceModel;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentStarters;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentStarters.StarterFlags;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.dalvik.util.AndroidTypes;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchyDweller;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ipa.summaries.SummarizedMethodWithNames;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.ssa.ParameterAccessor;
import com.ibm.wala.util.ssa.ParameterAccessor.Parameter;
import com.ibm.wala.util.ssa.SSAValue;
import com.ibm.wala.util.ssa.SSAValue.TypeKey;
import com.ibm.wala.util.ssa.SSAValue.VariableKey;
import com.ibm.wala.util.ssa.SSAValue.WeaklyNamedKey;
import com.ibm.wala.util.ssa.SSAValueManager;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;
import com.ibm.wala.util.strings.Atom;
// For debug:
/**
 *  The model to be executed at application start.
 *
 *  This method models the lifecycle of an Android Application. By doing so it calls all AndroidEntryPoints
 *  set in the AnalysisOptions.
 *  <p>
 *  Between the calls to the AndroidEntryPoints special behavior is inserted. You can change that behavior
 *  by implementing an AbstractAndroidModel or set one of the existing ones in the AnalysisOptions.
 *
 *  Additionally care of how types are instantiated is taken. You can change this behavior by setting the
 *  IInstanciationBehavior in the AnalysisOptions.
 *  
 *  Smaller Models exist: 
 *      * MiniModel calls all components of a specific type (for example all Activities)
 *      * MicroModel calls a single specific component
 *      * ExternalModel doesn't call anything but fiddles with the data on its own
 *
 *  All these Models are added to a synthetic AndroidModelClass.
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public class AndroidModel /* makes SummarizedMethod */ 
        implements IClassHierarchyDweller {
    private final Atom name = Atom.findOrCreateAsciiAtom("AndroidModel");
    public MethodReference mRef;

    protected IClassHierarchy cha;
    protected AnalysisOptions options;
    protected IAnalysisCacheView cache;
    private AbstractAndroidModel labelSpecial;
    private IInstantiationBehavior instanceBehavior;
    private SSAValueManager paramManager;
    private ParameterAccessor modelAcc;
    private ReuseParameters reuseParameters;
    protected final AnalysisScope scope;

    protected VolatileMethodSummary body;
//    private JavaInstructionFactory instructionFactory;

    private IProgressMonitor monitor;
    private int maxProgress;
 
    /*
     *  static: "boot" only once. How to assert done by the right one?
     */
    protected static boolean doBoot = true;

    protected IClass klass;
    protected boolean built;
    protected SummarizedMethod model;

    public AndroidModel(final IClassHierarchy cha, final AnalysisOptions options, final IAnalysisCacheView cache) {
        this.options = options;
        this.cha = cha;
        this.cache = cache;
        this.built = false;
        this.scope = options.getAnalysisScope();

        this.instanceBehavior = AndroidEntryPointManager.MANAGER.getInstantiationBehavior(cha);

    }

    /**
     *  Generates the model on a sub-set of Entrypoints. 
     *
     *  Asks {@link #selectEntryPoint(AndroidEntryPoint)} for each EntryPoint known to the AndroidEntryPointManager,
     *  if the EntryPoint should be included in the model. Then calls {@link #build(Atom, Collection)}
     *  on these.
     *
     *  @param  name    The name the generated method will be known as
     */
    protected void build(Atom name) throws CancelException {
        final List<AndroidEntryPoint> restrictedEntries = new ArrayList<>();

        for (AndroidEntryPoint ep: AndroidEntryPointManager.ENTRIES) {
            if (selectEntryPoint(ep)) {
                restrictedEntries.add(ep);
            }
        }
        build(name, restrictedEntries);
    }

    public Atom getName() {
        return this.name;
    }

    public boolean isStatic() {
        return true;
    }

    public TypeName getReturnType() {
        return TypeReference.VoidName;
    }

    public Descriptor getDescriptor() throws CancelException {
        return getMethod().getDescriptor();
    }

    /**
     *  Generate the SummarizedMethod for the model (in this.model).
     *
     *  The actual generated model depends on the on the properties of this overloaded class. Most 
     *  generated methods should reside in the AndroidModelClass and take AndroidComponents as well
     *  as some parameters (these marked REUSE) to the EntryPoints of the components.
     *
     *  Use {@link #getMethod()} to retrieve the method generated here or getMethodAs to get a version
     *  which is wrapped to another signature.
     *
     *  @param  name            The name the generated method will be known as
     *  @param  entrypoints     The functions to call additionally to boot-code and XXX
     */
    protected void build(Atom name, Collection<? extends AndroidEntryPoint> entrypoints) throws CancelException {

        // register
        this.klass = cha.lookupClass(AndroidModelClass.ANDROID_MODEL_CLASS);
       
        if (this.klass == null) {
            // add to cha
            
            this.klass = AndroidModelClass.getInstance(cha);
            cha.addClass(this.klass);
        }

        this.reuseParameters = new ReuseParameters(this.instanceBehavior, this);
//        this.instructionFactory = new JavaInstructionFactory(); // TODO: TSIF
       
        // Complete the signature of the method
        reuseParameters.collectParameters(entrypoints);
        this.mRef = reuseParameters.toMethodReference(null);
        this.modelAcc = new ParameterAccessor(this.mRef, (! isStatic()));
        this.paramManager = new SSAValueManager(modelAcc);

        final Selector selector = this.mRef.getSelector();
        final AndroidModelClass mClass = AndroidModelClass.getInstance(cha);
        if (mClass.containsMethod(selector)) {
            
            assert (mClass.getMethod(selector) instanceof SummarizedMethod);
            this.model = (SummarizedMethod) mClass.getMethod(selector);
            return;
        }
        this.body = new VolatileMethodSummary(new MethodSummary(this.mRef));
        this.body.setStatic(true);

        this.labelSpecial = AndroidEntryPointManager.MANAGER.makeModelBehavior(this.body, new TypeSafeInstructionFactory(cha),
                this.paramManager, entrypoints);

        this.monitor = AndroidEntryPointManager.MANAGER.getProgressMonitor();
        this.maxProgress = entrypoints.size();

        AndroidModel.doBoot &= AndroidEntryPointManager.MANAGER.getDoBootSequence();

        // BUILD
        this.monitor.beginTask("Building " + name, this.maxProgress);
        populate(entrypoints);


        assert (cha.lookupClass(AndroidModelClass.ANDROID_MODEL_CLASS) != null) : "Adding the class failed!";

        if (this.klass == null) {
            throw new IllegalStateException("Could not find ANDROID_MODEL_CLASS in cha.");
        }

        this.body.setLocalNames(this.paramManager.makeLocalNames());
        this.model = new SummarizedMethodWithNames(this.mRef, this.body, this.klass) {
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
                    return tRef;
                    //throw new IllegalStateException("Error looking up " + tRef);
                } else {
                    return tRef;
                }
            }
        };
    
        this.built = true;
    }

    private void register(SummarizedMethod model) {
        IClass klass = getDeclaringClass();
        ((AndroidModelClass)klass).setMacroModel(model);
    }

    /**
     *  Building the SummarizedMethod is delayed upon the first class to this method.
     *
     *  @return the method for this model as generated by build()
     */
    public SummarizedMethod getMethod() throws CancelException {
        if (!built) {
            build(this.name);
            register(this.model);
        }

        return this.model;
    }

    /**
     *  The class the Method representing this Model resides in.
     *
     *  Most likely the AndroidModelClass.
     */
    public IClass getDeclaringClass() {
        return this.klass;
    }

    /**
     *  Overridden by models to restraint Entrypoints.
     *
     *  For each entrypoint this method is queried if it should be part of the model.
     *
     *  @param  ep  The EntryPoint in question
     *  @return if the given EntryPoint shall be part of the model
     */
    protected boolean selectEntryPoint(AndroidEntryPoint ep) {
        return  true;
    }

    /**
     *  Add Instructions to the model.
     *
     *  {@link #build(Atom, Collection)} prepares the MethodSummary, then calls populate() to
     *  add the instructions, then finishes the model. Populate is only an extra function to shorten build(),
     *  calling it doesn't make sense in an other context.
     */
    private void populate(Iterable<? extends AndroidEntryPoint> entrypoints) throws CancelException {
        assert (! built) : "You can only build once";
        int currentProgress = 0;

        final TypeSafeInstructionFactory tsif = new TypeSafeInstructionFactory(this.cha);
        final Instantiator instantiator = new Instantiator (this.body, tsif, this.paramManager, this.cha, this.mRef, this.scope);
        boolean enteredASection = false;
        //
        //  Add preparing code to the model
        //
        if (AndroidModel.doBoot) {
//            final Set<Parameter> allActivities = new HashSet<Parameter>(modelAcc.allExtend(AndroidTypes.ActivityName, getClassHierarchy()));
            //assert(allActivities.size() > 0) : "There are no Activities in the Model"; // XXX
//            final IntentStarters.StartInfo toolInfo = IntentStarters.StartInfo.makeContextFree(null);
//            final AndroidStartComponentTool tool = new AndroidStartComponentTool(this.cha, this.mRef, toolInfo.getFlags(),
//                    /* caller */ null, tsif, modelAcc, this.paramManager, this.body, /* self */ null, toolInfo, /* callerNd */ null);
            final SSAValue application;
            {
                final SSAValue tmpApp = modelAcc.firstExtends(AndroidTypes.ApplicationName, this.cha);
                if (tmpApp != null) {
                    application = tmpApp;
                } else {
                    // Generate a real one?
                    
                    application = paramManager.getUnmanaged(AndroidTypes.Application, "app");
                    this.body.addConstant(application.getNumber(), new ConstantValue(null));
                    application.setAssigned();
                }
            }
            final SSAValue nullIntent;
            {
                nullIntent = paramManager.getUnmanaged(AndroidTypes.Intent, "nullIntent");
                this.body.addConstant(nullIntent.getNumber(), new ConstantValue(null));
                nullIntent.setAssigned();
            }
            final SSAValue nullBinder;
            {
                nullBinder = paramManager.getUnmanaged(AndroidTypes.IBinder, "nullBinder");
                this.body.addConstant(nullBinder.getNumber(), new ConstantValue(null));
                nullBinder.setAssigned();
            }

            
            {
                final AndroidBoot boot = new AndroidBoot(); 
                boot.addBootCode(tsif, paramManager, this.body);
                //tool.attachActivities(allActivities, application, boot.getMainThread(), /* Should be application context TODO */
                //        boot.getPackageContext(), nullBinder, nullIntent); 
            }

            // TODO: Assign context to the other components
        }
        
        

        for (final AndroidEntryPoint ep : entrypoints) {
            this.monitor.subTask(ep.getMethod().getReference().getSignature() );
            
            if (! selectEntryPoint(ep)) {
                assert(false): "The ep should not reach here!";
                
                currentProgress++;
                continue;
            }

            //
            //  Is special handling to be inserted?
            //
            if (this.labelSpecial.hadSectionSwitch(ep.order)) {
                
                this.labelSpecial.enter(ep.getSection(), body.getNextProgramCounter());
                enteredASection = true;
            }

            //
            //  Collect arguments to ep
            //  if there are multiple paramses call the entrypoint multiple times
            //
            List<List<SSAValue>> paramses = new ArrayList<>(1);
            {
                final List<Integer> mutliTypePositions = new ArrayList<>();
                { // Add single-type parameters and collect positions for multi-type
                    final List<SSAValue> params = new ArrayList<>(ep.getNumberOfParameters());
                    paramses.add(params);

                    for (int i = 0; i < ep.getNumberOfParameters(); ++i) {
                        if (ep.getParameterTypes(i).length != 1) {
                             mutliTypePositions.add(i);
                            params.add(null); // will get set later
                        } else {
                            for (final TypeReference type : ep.getParameterTypes(i)) {
                                if (this.instanceBehavior.getBehavior(type.getName(), ep.getMethod(), null) == InstanceBehavior.REUSE) {
                                    params.add(this.paramManager.getCurrent(new TypeKey(type.getName())));
                                } else if (type.isPrimitiveType()) {
                                    params.add(paramManager.getUnmanaged(type, "p"));
                                } else {
                                    // It is an CREATE parameter
                                    final boolean asManaged = false;
                                    final VariableKey key = null;   // auto-generates UniqueKey
                                    final Set<SSAValue> seen = null; 
                                    params.add(instantiator.createInstance(type, asManaged, key, seen));
                                }
                            }
                        }
                    }
                }

                // Now for the mutliTypePositions: we'll build the Cartesian product for these
                for (int positionInMutliTypePosition = 0; positionInMutliTypePosition < mutliTypePositions.size(); ++positionInMutliTypePosition) {
                    final Integer multiTypePosition = mutliTypePositions.get(positionInMutliTypePosition);
                    final TypeReference[] typesOnPosition = ep.getParameterTypes(multiTypePosition);
                    final int typeCountOnPosition = typesOnPosition.length;

                    { // Extend the list size to hold the product
                        final List<List<SSAValue>> new_paramses = new ArrayList<>(paramses.size() * typeCountOnPosition);

                        for (int i = 0; i < typeCountOnPosition; ++i) {
                            //new_paramses.addAll(paramses); *grrr* JVM! You could copy at least null - but noooo...
                            for (final List<SSAValue> params : paramses) {
                                final List<SSAValue> new_params = new ArrayList<>(params.size());
                                new_params.addAll(params);
                                new_paramses.add(new_params);
                            }
                        }
                        paramses = new_paramses;
                    }

                    { // set the current multiTypePosition
                        for (int i = 0; i < paramses.size(); ++i) {
                            final List<SSAValue> params = paramses.get(i);
                            assert(params.get(multiTypePosition) == null) : "Expected null, got " + params.get(multiTypePosition) + " iter " + i;

                            // XXX: This could be faster, but well...
                            final TypeReference type = typesOnPosition[(i * (positionInMutliTypePosition + 1)) % typeCountOnPosition];

                            if (this.instanceBehavior.getBehavior(type.getName(), ep.getMethod(), null) == InstanceBehavior.REUSE) {
                                params.set(multiTypePosition, this.paramManager.getCurrent(new TypeKey(type.getName())));
                            } else if (type.isPrimitiveType()) {
                                params.set(multiTypePosition, paramManager.getUnmanaged(type, "p"));
                            } else {
                                // It is an CREATE parameter
                                final boolean asManaged = false;
                                final VariableKey key = null;   // auto-generates UniqueKey
                                final Set<SSAValue> seen = null; 
                                params.set(multiTypePosition, instantiator.createInstance(type, asManaged, key, seen));
                            }
                        }
                    }
                }
            }

            /*{ // DEBUG
                if (paramses.size() > 1) {
                    System.out.println("\n\nParamses on " + ep.getMethod().getSignature() + ":");
                    for (final List<SSAValue> params : paramses) {
                        System.out.println("\t" + params);
                    }
                }
            } // */

            //
            //  Insert the call optionally handling its return value
            //
            for (final List<SSAValue> params : paramses) {

                final int callPC = body.getNextProgramCounter();
                final CallSiteReference site = ep.makeSite(callPC);
                final SSAAbstractInvokeInstruction invokation;
                final SSAValue exception = paramManager.getException();  // will hold the exception object of ep

                if (ep.getMethod().getReturnType().equals(TypeReference.Void)) {
                    invokation = tsif.InvokeInstruction(callPC, params, exception, site);
                    this.body.addStatement(invokation);
                } else {
                    //  Check if we have to mix in the return value of this ep using a Phi
                    final TypeReference returnType = ep.getMethod().getReturnType();
                    final TypeKey returnKey = new TypeKey(returnType.getName());

                    if (this.paramManager.isSeen(returnKey)) {
                        // if it's seen it most likely is a REUSE-Type. However probably it makes sense for 
                        // other types too so we don't test on isReuse.
                        

                        final SSAValue oldValue = this.paramManager.getCurrent(returnKey);
                        this.paramManager.invalidate(returnKey);
                        final SSAValue returnValue = paramManager.getUnallocated(returnType, returnKey);

                        invokation = tsif.InvokeInstruction(callPC, returnValue, params, exception, site); 
                        this.body.addStatement(invokation);
                        this.paramManager.setAllocation(returnValue, invokation);

                        // ... and Phi things together ...
                        this.paramManager.invalidate(returnKey);
                        final SSAValue newValue = this.paramManager.getFree(returnType, returnKey);
                        final int phiPC = body.getNextProgramCounter();
                        final List<SSAValue> toPhi = new ArrayList<>(2);
                        toPhi.add(oldValue);
                        toPhi.add(returnValue);
                        final SSAPhiInstruction phi = tsif.PhiInstruction(phiPC, newValue, toPhi);
                        this.body.addStatement(phi);
                        this.paramManager.setPhi(newValue, phi);
                    } else {
                        // Just throw away the return value
                        final SSAValue returnValue = paramManager.getUnmanaged(returnType, new SSAValue.UniqueKey());
                        invokation = tsif.InvokeInstruction(callPC, returnValue, params, exception, site);
                        this.body.addStatement(invokation);
                    }
                }
            }

            this.monitor.worked(++currentProgress);
            MonitorUtil.throwExceptionIfCanceled(this.monitor); 
        }

        
        //  Close all sections by "jumping over" the remaining labels
        if (enteredASection) {
            labelSpecial.finish(body.getNextProgramCounter());
        }
        this.monitor.done();
    }

    /**
     *  Get method of the Model in an other Signature.
     *
     *  Generates a new Method that wraps the model so it can be called using the given Signature. 
     *  Flags control the behavior of that wrapper.
     *
     *  Arguments to the wrapping function are "connected through" to the model based on their type only,
     *  so if there are multiple Arguments of the same type this may yield to unexpected connections.
     *
     *  This method is called by the IntentCoentextInterpreter.
     *
     *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextInterpreter
     *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentStarters
     *
     *  @param  asMethod    The signature to generate
     *  @param  caller      The class of the caller; only needed depending on the flags
     *  @param  info        The IntentSterter used
     *  @param  callerNd    CGNoodle of the caller - may be null
     *  @return A wrapper that calls the model
     */
    public SummarizedMethod getMethodAs(MethodReference asMethod, TypeReference caller,
            IntentStarters.StartInfo info, CGNode callerNd) throws CancelException {
        Set<StarterFlags> flags = null;
        if (info != null) {
          flags = info.getFlags();
        }
        //System.out.println("\n\nAS: " + asMethod + "\n\n");
        if (!built) {
            getMethod();
        }
        if (asMethod == null) {
            throw new IllegalArgumentException("asMethod may not be null");
        }
        if (flags == null) {
            flags = Collections.emptySet();
        }

        final TypeSafeInstructionFactory instructionFactory = new TypeSafeInstructionFactory(getClassHierarchy());
        final ParameterAccessor acc = new ParameterAccessor(asMethod, /* hasImplicitThis: */ true);
        //final AndroidModelParameterManager pm = new AndroidModelParameterManager(acc);
        final SSAValueManager pm = new SSAValueManager(acc);
        if (callerNd != null) {
            pm.breadCrumb = "Caller: " + caller + " Context: " + callerNd.getContext() + " Model: " + this.getClass() + " Name: " + this.getName();
        } else {
            pm.breadCrumb = "Caller: " + caller + " Model: " + this.getClass();
        }
        final VolatileMethodSummary redirect = new VolatileMethodSummary(new MethodSummary(asMethod));
        redirect.setStatic(false);
        final Instantiator instantiator = new Instantiator(redirect, instructionFactory, pm, this.cha, asMethod, this.scope);
        final Parameter self;
        { 
            self = acc.getThisAs(caller);
            pm.setAllocation(self, null);
            //self = acc.getThis();
        }

        final ParameterAccessor modelAcc = new ParameterAccessor(this.model);
        /*{ // DEBUG
            System.out.println("FOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO: Calleeeeeee" + this.model);
            for (SSAValue calleeParam : modelAcc.all()) {
                System.out.println("\tCalleeArg: " + calleeParam);
            }
            try {
                System.out.println("\tCalleeP1: " + modelAcc.getParameter(1));
                System.out.println("\tCalleeM0: " + this.model.getParameterType(0));
                System.out.println("\tCalleeP2: " + modelAcc.getParameter(2));
                System.out.println("\tCalleeM1: " + this.model.getParameterType(1));
                MethodReference modelRef = this.model.getReference();
                System.out.println("\tmRef: " + modelRef);
                System.out.println("\tCalleeR0: " + modelRef.getParameterType(0));
                System.out.println("\tCalleeR1: " + modelRef.getParameterType(1));
            } catch (Exception e) { }
        }*/
        final List<Parameter> modelsActivities = modelAcc.allExtend(AndroidTypes.ActivityName, getClassHierarchy()); // are in models scope
        final List<SSAValue> allActivities = new ArrayList<>(modelsActivities.size());   // create instances in this scope 
        for (Parameter activity: modelsActivities) {
            final TypeReference activityType = activity.getType();
            final Parameter inAsMethod = acc.firstOf(activityType);
            
            if (inAsMethod != null) {
                allActivities.add(inAsMethod);
            } else {
                final Atom fdName = activityType.getName().getClassName();
                final AndroidModelClass mClass = AndroidModelClass.getInstance(cha);

                if (AndroidEntryPointManager.MANAGER.doFlatComponents()) {
                    if (mClass.getField(fdName) != null) {
                        final IField field = mClass.getField(fdName);
                        final int instPC = redirect.getNextProgramCounter();
                        final SSAValue target = pm.getUnallocated(activityType, new SSAValue.WeaklyNamedKey(activityType.getName(), 
                                    "got" + fdName.toString()));
                        final SSAInstruction getInst = instructionFactory.GetInstruction(instPC, target, field.getReference());
                        redirect.addStatement(getInst);
                        pm.setAllocation(target, getInst);
                        allActivities.add(target);
                    } else {
                        final SSAValue newInstance = instantiator.createInstance(activityType, false, null, null);
                        allActivities.add(newInstance);

                        mClass.putField(fdName, activityType);
                        final int instPC = redirect.getNextProgramCounter();
                        final FieldReference fdRef = FieldReference.findOrCreate(mClass.getReference(), fdName, activityType);
                        final SSAInstruction putInst = instructionFactory.PutInstruction(instPC, newInstance, fdRef);
                        redirect.addStatement(putInst);
                        System.out.println("All activities new: " + newInstance);
                    }
                } else {
                    final SSAValue newInstance = instantiator.createInstance(activityType, false, null, null);
                    allActivities.add(newInstance);
                }
            }
        }
        assert(allActivities.size() == modelsActivities.size());

        // The defaults for connectThrough
        final Set<SSAValue> defaults = new HashSet<>();
        { // Calls that don't take a bundle usually call through with a null-bundle
            final SSAValue nullBundle = pm.getUnmanaged(AndroidTypes.Bundle, "nullBundle");
            redirect.addConstant(nullBundle.getNumber(), new ConstantValue(null));
            nullBundle.setAssigned();
            defaults.add(nullBundle);
        }
        { // We may have an incoming parameter of [Intent - unpack it to Intent
            final TypeName intentArray = TypeName.findOrCreate("[Landroid/content/Intent");
            final SSAValue incoming = acc.firstOf(intentArray);
            // TODO: Take all not only the first
            if (incoming != null) {
                final VariableKey unpackedIntentKey = new WeaklyNamedKey(AndroidTypes.IntentName, "unpackedIntent");
                final SSAValue unpackedIntent = pm.getUnallocated(AndroidTypes.Intent, unpackedIntentKey);
                final int pc = redirect.getNextProgramCounter();
                final SSAInstruction fetch = instructionFactory.ArrayLoadInstruction(pc, unpackedIntent, incoming, 0);
                redirect.addStatement(fetch);
                pm.setAllocation(unpackedIntent, fetch);
                defaults.add(unpackedIntent);
            }
        }
        final SSAValue intent = acc.firstExtends(AndroidTypes.Intent, cha);

        final AndroidStartComponentTool tool = new AndroidStartComponentTool(getClassHierarchy(), asMethod, flags, caller, instructionFactory,
                acc, pm, redirect, self, info);

        final AndroidTypes.AndroidContextType contextType;
        final SSAValue androidContext;  // of AndroidTypes.Context: The callers android-context
        
        androidContext = tool.fetchCallerContext();
        contextType = tool.typeOfCallerContext();

        try { // Add additional Info if Exception occurs...

        // TODO: Check, that caller is an activity where necessary!

        //final SSAValue iBinder = tool.fetchIBinder(androidContext);
        //tool.assignIBinder(iBinder, allActivities);
        if (intent != null) {
            tool.setIntent(intent, allActivities);
        } else if (! info.isSystemService()) {  // it's normal for SystemServices
            
        }

        // Call the model
        {
            final List<SSAValue> redirectParams = acc.connectThrough(modelAcc, new HashSet<>(allActivities), defaults,
                    getClassHierarchy(), /* IInstantiator this.createInstance(type, redirect, pm)  */ instantiator, false, null, null);
            final int callPC = redirect.getNextProgramCounter();
            final CallSiteReference site = CallSiteReference.make(callPC, this.model.getReference(),
                    IInvokeInstruction.Dispatch.STATIC);
            final SSAAbstractInvokeInstruction invokation;
            final SSAValue exception = pm.getException();
         
            if (this.model.getReference().getReturnType().equals(TypeReference.Void)) {
                invokation = instructionFactory.InvokeInstruction(callPC, redirectParams, exception, site);
            } else {
                // it's startExternal or SystemService
                if (this instanceof SystemServiceModel) {
                    final SSAValue svc = pm.getUnmanaged(TypeReference.JavaLangObject, "systemService");
                    invokation = instructionFactory.InvokeInstruction(callPC, svc, redirectParams, exception, site);

                    // SHORTCUT:
                    redirect.addStatement(invokation);
                    if (instructionFactory.isAssignableFrom(svc.getType(), svc.getValidIn().getReturnType())) {
                    	final int returnPC = redirect.getNextProgramCounter();
                    	final SSAInstruction returnInstruction = instructionFactory.ReturnInstruction(returnPC, svc);
                    	redirect.addStatement(returnInstruction);
                    }

                    final IClass declaringClass = this.cha.lookupClass(asMethod.getDeclaringClass());   
                    if (declaringClass == null) {
                        throw new IllegalStateException("Unable to retreive te IClass of " + asMethod.getDeclaringClass() + " from " +
                                "Method " + asMethod.toString());
                    }
                    redirect.setLocalNames(pm.makeLocalNames());
                    SummarizedMethod override = new SummarizedMethodWithNames(mRef, redirect, declaringClass);
                    return override;
                } else if (this instanceof ExternalModel) {
                    final SSAValue trash = pm.getUnmanaged(AndroidTypes.Intent, "trash");
                    invokation = instructionFactory.InvokeInstruction(callPC, trash, redirectParams, exception, site);
                } else {
                    throw new UnsupportedOperationException("Can't handle a " + this.model.getClass());
                }
            }
            redirect.addStatement(invokation);
        }

        // Optionally call onActivityResult
        if (flags.contains(StarterFlags.CALL_ON_ACTIVITY_RESULT) &&      // TODO: Test multiple activities
           (! flags.contains(StarterFlags.CONTEXT_FREE))) {             // TODO: Doesn't this work without context?
            // Collect all Activity.mResultCode and Activity.mResultData

            // Result information of all activities.
            final List<SSAValue> resultCodes = new ArrayList<>();
            final List<SSAValue> resultData = new ArrayList<>();
            final SSAValue mResultCode; // = Phi(resultCodes)
            final SSAValue mResultData; // = Phi(resultData)
           
            tool.fetchResults(resultCodes, resultData, allActivities); 

            if (resultCodes.size() == 0) {
                throw new IllegalStateException("The call " + asMethod + " from " + caller + " failed, as the model " + this.model + 
                        " did not take an activity to read the result from");
            }

            mResultCode = tool.addPhi(resultCodes);
            mResultData = tool.addPhi(resultData);

            { // Send back the results
                // TODO: Assert caller is an Activity
                final SSAValue outRequestCode = acc.firstOf(TypeReference.Int);   // TODO: Check is's the right parameter
                
                final int callPC = redirect.getNextProgramCounter();
                // void onActivityResult (int requestCode, int resultCode, Intent data)
                final Selector mSel = Selector.make("onActivityResult(IILandroid/content/Intent;)V");
                final MethodReference mRef = MethodReference.findOrCreate(caller, mSel);
                final CallSiteReference site = CallSiteReference.make(callPC, mRef, IInvokeInstruction.Dispatch.VIRTUAL);
                //final SSAValue exception = new SSAValue(pm.getUnmanaged(), TypeReference.JavaLangException, asMethod, "exception");
                final SSAValue exception = pm.getException();
                final List<SSAValue> params = new ArrayList<>();
                params.add(self);
                params.add(outRequestCode); // Was an agument to start...
                params.add(mResultCode);
                params.add(mResultData);
                final SSAInstruction invokation = instructionFactory.InvokeInstruction(callPC, params, exception, site);
                redirect.addStatement(invokation);
                
                
            } // */
        }

        final IClass declaringClass = this.cha.lookupClass(asMethod.getDeclaringClass());   
        if (declaringClass == null) {
            throw new IllegalStateException("Unable to retreive te IClass of " + asMethod.getDeclaringClass() + " from " +
                    "Method " + asMethod.toString());
        }
        // TODO: Throw into an other loader
        redirect.setLocalNames(pm.makeLocalNames());
        SummarizedMethod override = new SummarizedMethodWithNames(mRef, redirect, declaringClass);

        //assert(asMethod.getReturnType().equals(TypeReference.Void)) : "getMethodAs does not support return values. Requested: " + 
        //    asMethod.getReturnType().toString();                  // TODO: Implement

        return override;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("\nOccured in getMethodAs with parameters of:");
            System.err.println(acc.dump());
            System.err.println("\tpm=\t" + pm);
            System.err.println("\tself=\t" + self);
            System.err.println("\tmodelAcc=\t" + acc.dump());
            
            //final List<Parameter> modelsActivities = modelAcc.allExtend(AndroidTypes.ActivityName, getClassHierarchy()); // are in models scope
            //final List<SSAValue> allActivities = new ArrayList<SSAValue>(modelsActivities.size());   // create instances in this scope 
            System.err.println("\tcontextType=\t" + contextType);
            System.err.println("\tandroidContetx=\t" + androidContext);
            System.err.println("\tasMethod=\t" + asMethod);
            System.err.println("\tcaller=\t" + caller);
            System.err.println("\tinfo=\t" + info);
            System.err.println("\tcallerND=\t" + callerNd);
            System.err.println("\tthis=\t" + this.getClass().toString());
            System.err.println("\tthis.name=\t" + this.name);

            throw new IllegalStateException(e);
        }
    }

    /**
     *  Creates an "encapsulated" version of the model.
     *
     *  The generated method will take no parameters. New instances for REUSE-Parameters will
     *  be created.
     *
     *  This variant is useful for the start of an analysis.
     */
    public IMethod getMethodEncap() throws CancelException {
        final MethodReference asMethod;
        {
            final TypeReference clazz = AndroidModelClass.ANDROID_MODEL_CLASS;
            final Atom methodName = Atom.concat(this.getName(), Atom.findOrCreateAsciiAtom("Encap"));
            //final TypeName returnType = this.getReturnType();
            final TypeName returnType = TypeReference.VoidName;
            final Descriptor descr = Descriptor.findOrCreate(new TypeName[]{}, returnType);
            asMethod = MethodReference.findOrCreate(clazz, methodName, descr);
        }

        final AndroidModelClass mClass = AndroidModelClass.getInstance(cha);

        if (mClass.containsMethod(asMethod.getSelector())) {
            // There's already an encap for this method
            return mClass.getMethod(asMethod.getSelector());
        }

        final VolatileMethodSummary encap = new VolatileMethodSummary(new MethodSummary(asMethod));
        encap.setStatic(true);
        final TypeSafeInstructionFactory instructionFactory = new TypeSafeInstructionFactory(getClassHierarchy());
        final ParameterAccessor acc = new ParameterAccessor(asMethod, /* hasImplicitThis: */ false);
        final SSAValueManager pm = new SSAValueManager(acc);
        pm.breadCrumb = "Encap: " + this.getClass().toString();

        final SummarizedMethod model = getMethod();

        final List<SSAValue> params = new ArrayList<>();
        { // Collect Params
            final FlatInstantiator instantiator = new FlatInstantiator(encap, instructionFactory, pm, this.cha, asMethod, this.scope);

            for (int i = 0; i < model.getNumberOfParameters(); ++i) {
                final TypeReference argT = model.getParameterType(i);
                final SSAValue arg;

                if  ( ( AndroidEntryPointManager.MANAGER.doFlatComponents()) &&  
                            (AndroidComponent.isAndroidComponent(argT, cha)) ) { 
                    // Get / Put filed in AndroidModelClass for Android-Components
                    final Atom fdName = argT.getName().getClassName();

                    if (mClass.getField(fdName) != null) {
                        final IField field = mClass.getField(fdName);
                        final int instPC = encap.getNextProgramCounter();
                        arg = pm.getUnallocated(argT, new SSAValue.WeaklyNamedKey(argT.getName(), 
                                    "got" + fdName.toString()));
                        final SSAInstruction getInst = instructionFactory.GetInstruction(instPC, arg, field.getReference());
                        encap.addStatement(getInst);
                        pm.setAllocation(arg, getInst);
                    } else {
                        arg = instantiator.createInstance(argT, false, null, null);

                        mClass.putField(fdName, argT);
                        final int instPC = encap.getNextProgramCounter();
                        final FieldReference fdRef = FieldReference.findOrCreate(mClass.getReference(), fdName, argT);
                        final SSAInstruction putInst = instructionFactory.PutInstruction(instPC, arg, fdRef);
                        encap.addStatement(putInst);
                    }
                } else {
                    final boolean managed = false;
                    final SSAValue.VariableKey key = new SSAValue.TypeKey(argT.getName());
                    arg = instantiator.createInstance (argT, managed, key, null);
                }
                params.add(arg);
            }
        }

        { // Call the model
            final int callPC = encap.getNextProgramCounter();
            final CallSiteReference site = CallSiteReference.make(callPC, model.getReference(), IInvokeInstruction.Dispatch.STATIC);
            final SSAValue exception = pm.getException();
        
            final SSAAbstractInvokeInstruction invokation = instructionFactory.InvokeInstruction(callPC, params, exception, site);
            encap.addStatement(invokation);
        }

        encap.setLocalNames(pm.makeLocalNames());
        final SummarizedMethod method = new SummarizedMethodWithNames(asMethod, encap, mClass);
        mClass.addMethod(method);

        return method;
    }

    @Override
    public IClassHierarchy getClassHierarchy() {
        return this.cha;
    }

    @Override
    public String toString() {
        return "<" + this.getClass() + " name=" + this.name + " />";
    }
}
