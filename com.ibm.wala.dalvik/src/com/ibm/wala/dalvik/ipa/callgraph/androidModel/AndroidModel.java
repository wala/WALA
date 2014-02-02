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

import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.ReuseParameters;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.util.ssa.SSAValue;
import com.ibm.wala.util.ssa.SSAValue.VariableKey;
import com.ibm.wala.util.ssa.SSAValue.WeaklyNamedKey;
import com.ibm.wala.util.ssa.SSAValue.TypeKey;
import com.ibm.wala.util.ssa.ParameterAccessor;
import com.ibm.wala.util.ssa.ParameterAccessor.Parameter;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;
import com.ibm.wala.util.ssa.SSAValueManager;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.Instantiator;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ipa.summaries.SummarizedMethodWithNames;
import com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContext;
import com.ibm.wala.types.Selector;
import com.ibm.wala.ssa.ConstantValue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;

import com.ibm.wala.ipa.summaries.VolatileMethodSummary;

import com.ibm.wala.ipa.cha.IClassHierarchyDweller;
import com.ibm.wala.util.ssa.IInstantiator;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior.InstanceBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.AbstractAndroidModel;


import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.dalvik.util.AndroidTypes;

import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.types.ClassLoaderReference;

import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import com.ibm.wala.util.collections.HashMapFactory;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.CancelException;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Iterable;
import com.ibm.wala.ipa.callgraph.AnalysisScope;

// For debug:
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ssa.IR;
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
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 */
public class AndroidModel /* makes SummarizedMethod */ 
        implements IClassHierarchyDweller {
    private static Logger logger = LoggerFactory.getLogger(AndroidModel.class);
    
    private final Atom name = Atom.findOrCreateAsciiAtom("AndroidModel");
    public MethodReference mRef;

    protected IClassHierarchy cha;
    protected AnalysisOptions options;
    protected AnalysisCache cache;
    private AbstractAndroidModel labelSpecial;
    private IInstantiationBehavior instanceBehavior;
    private SSAValueManager paramManager;
    private ParameterAccessor modelAcc;
    private ReuseParameters reuseParameters;
    protected final AnalysisScope scope;

    protected VolatileMethodSummary body;
    private JavaInstructionFactory instructionFactory;

    private IProgressMonitor monitor;
    private int maxProgress;

    protected IClass klass;
    protected boolean built;
    protected SummarizedMethod model;

    public AndroidModel(final IClassHierarchy cha, final AnalysisOptions options, final AnalysisCache cache) {
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
     *  if the EntryPoint should be included in the model. Then calls {@link #build(Atom, Iterable<? extends Entrypoint>)}
     *  on these.
     *
     *  @param  name    The name the generated method will be known as
     */
    protected void build(Atom name) throws CancelException {
        final List<AndroidEntryPoint> restrictedEntries = new ArrayList<AndroidEntryPoint>();

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
            logger.info("Adding model-class to cha");
            this.klass = AndroidModelClass.getInstance(cha);
            cha.addClass(this.klass);
        }

        this.reuseParameters = new ReuseParameters(this.instanceBehavior, this);
        this.instructionFactory = new JavaInstructionFactory(); // TODO: TSIF
       
        // Complete the signature of the method
        reuseParameters.collectParameters(entrypoints);
        this.mRef = reuseParameters.toMethodReference(null);
        this.modelAcc = new ParameterAccessor(this.mRef, (! isStatic()));
        this.paramManager = new SSAValueManager(modelAcc);

        final Selector selector = this.mRef.getSelector();
        final AndroidModelClass mClass = AndroidModelClass.getInstance(cha);
        if (mClass.containsMethod(selector)) {
            logger.info("Returning existing {}", selector);
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

                    throw new IllegalStateException("Error looking up " + tRef);
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
     *  {@link #build(Iterable<?extends Entrypoint>)} prepares the MethodSummary, then calls populate() to
     *  add the instructions, then finishes the model. Populate is only an extra function to shorten build(),
     *  calling it doesn't make sense in an other context.
     */
    private void populate(Iterable<? extends AndroidEntryPoint> entrypoints) throws CancelException {
        assert (! built) : "You can only build once";
        int currentProgress = 0;

        final TypeSafeInstructionFactory tsif = new TypeSafeInstructionFactory(this.cha);
        final Instantiator instantiator = new Instantiator (this.body, tsif, this.paramManager, this.cha, this.mRef, this.scope);

        
        logger.info("Populating the AndroidModel with {} entryPoints", this.maxProgress);

        for (final AndroidEntryPoint ep : entrypoints) {
            this.monitor.subTask(ep.getMethod().getReference().getSignature() );
            
            if (! selectEntryPoint(ep)) {                       // TODO: Remove
                assert(false): "The ep should not reach here!";
                logger.info("SKIP: " + ep);
                currentProgress++;
                continue;
            }

            //
            //  Is special handling to be inserted?
            //
            if (this.labelSpecial.hadSectionSwitch(ep.order)) {
                logger.info("Adding special handling before: {}.", ep);
                this.labelSpecial.enter(ep.getSection(), body.getNextProgramCounter());
            }

            //
            //  Collect arguments to ep
            //  if their are multiple paramses call the entrypoint multiple times
            //
            List<List<SSAValue>> paramses = new ArrayList<List<SSAValue>>(1);
            {
                final List<Integer> mutliTypePositions = new ArrayList<Integer>();
                { // Add single-type parameters and collect positions for multi-type
                    final List<SSAValue> params = new ArrayList<SSAValue>(ep.getNumberOfParameters());
                    paramses.add(params);

                    //InterfaceConstructor.clearSeen();           // TODO: Remove
                    for (int i = 0; i < ep.getNumberOfParameters(); ++i) {
                        if (ep.getParameterTypes(i).length != 1) {
                            logger.debug("Got multiple types: {}",  Arrays.toString(ep.getParameterTypes(i)));
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
                        final List<List<SSAValue>> new_paramses = new ArrayList<List<SSAValue>>(paramses.size() * typeCountOnPosition);

                        for (int i = 0; i < typeCountOnPosition; ++i) {
                            //new_paramses.addAll(paramses); *grrr* JVM! You could copy at least null - but noooo...
                            for (final List<SSAValue> params : paramses) {
                                final List<SSAValue> new_params = new ArrayList<SSAValue>(params.size());
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
                logger.debug("Adding Call to {}.{}", ep.getMethod().getDeclaringClass().getName(),
                        ep.getMethod().getName());

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
                        logger.debug("Mixing in return type of this EP");

                        final SSAValue oldValue = this.paramManager.getCurrent(returnKey);
                        this.paramManager.invalidate(returnKey);
                        final SSAValue returnValue = paramManager.getUnallocated(returnType, returnKey);

                        invokation = tsif.InvokeInstruction(callPC, returnValue, params, exception, site); // TODO: clallPC?
                        this.body.addStatement(invokation);
                        this.paramManager.setAllocation(returnValue, invokation);

                        // ... and Phi things together ...
                        this.paramManager.invalidate(returnKey);
                        final SSAValue newValue = this.paramManager.getFree(returnType, returnKey);
                        final int phiPC = body.getNextProgramCounter();
                        final List<SSAValue> toPhi = new ArrayList<SSAValue>(2);
                        toPhi.add(oldValue);
                        toPhi.add(returnValue);
                        final SSAPhiInstruction phi = tsif.PhiInstruction(phiPC, newValue, toPhi);
                        this.body.addStatement(phi);
                        this.paramManager.setPhi(newValue, phi);
                    } else {
                        // Just throw away the return value
                        final SSAValue returnValue = paramManager.getUnmanaged(returnType, "trash");   // XXX trash not unique!
                        invokation = tsif.InvokeInstruction(callPC, returnValue, params, exception, site);
                        this.body.addStatement(invokation);
                    }
                }
            }

            this.monitor.worked(++currentProgress);
            MonitorUtil.throwExceptionIfCanceled(this.monitor); 
        }

        logger.debug("All EntryPoints have been added - now closing the model");
        //  Close all sections by "jumping over" the remaining labels
        labelSpecial.finish(body.getNextProgramCounter());

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
    public SummarizedMethod getMethodAs(MethodReference asMethod, TypeReference caller, CGNode callerNd) throws CancelException {
        //System.out.println("\n\nAS: " + asMethod + "\n\n");
        if (!built) {
            getMethod();
        }
        if (asMethod == null) {
            throw new IllegalArgumentException("asMethod may not be null");
        }

        final TypeSafeInstructionFactory instructionFactory = new TypeSafeInstructionFactory(getClassHierarchy());
        final ParameterAccessor acc = new ParameterAccessor(asMethod, /* hasImplicitThis: */ true);
        //final AndroidModelParameterManager pm = new AndroidModelParameterManager(acc);
        final SSAValueManager pm = new SSAValueManager(acc);
        final VolatileMethodSummary redirect = new VolatileMethodSummary(new MethodSummary(asMethod));
        redirect.setStatic(false);
        final Parameter self = acc.getThis();
        final Instantiator instantiator = new Instantiator(redirect, instructionFactory, pm, this.cha, asMethod, this.scope);

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
        final List<SSAValue> allActivities = new ArrayList<SSAValue>(modelsActivities.size());   // create instances in this scope 
        for (Parameter activity: modelsActivities) {
            final TypeReference activityType = activity.getType();
            final Parameter inAsMethod = acc.firstOf(activityType);
            if (inAsMethod != null) {
                allActivities.add(inAsMethod);
            } else {
                final SSAValue newInstance = instantiator.createInstance(activityType, false, null, null);
                allActivities.add(newInstance);
            }
        }
        assert(allActivities.size() == modelsActivities.size());

        // The defaults for connectThrough
        final Set<SSAValue> defaults = new HashSet<SSAValue>();
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

        try { // Add additional Info if Exception occurs...

        // TODO: Check, that caller is an activity where necessary!

        // TODO: Call Activity.setIntent

        // Call the model
        {
            logger.debug("Calling model: {}", this.model.getReference().getName());
            final int callPC = redirect.getNextProgramCounter();
            final CallSiteReference site = CallSiteReference.make(callPC, this.model.getReference(),
                    IInvokeInstruction.Dispatch.STATIC);
            final SSAAbstractInvokeInstruction invokation;
            final SSAValue exception = pm.getException();
            final List<SSAValue> redirectParams = acc.connectThrough(modelAcc, new HashSet<SSAValue>(allActivities), defaults,
                    getClassHierarchy(), /* IInstantiator this.createInstance(type, redirect, pm)  */ instantiator, false, null, null);
         
            if (this.model.getReference().getReturnType().equals(TypeReference.Void)) {
                invokation = instructionFactory.InvokeInstruction(callPC, redirectParams, exception, site);
            } else {
                // it's startExternal...
                final SSAValue trash = pm.getUnmanaged(AndroidTypes.Intent, "trash");
                /*{ // DEBUG
                    System.out.println("\nCalling External with: " + redirectParams);
                    System.out.println("\n---------------------------------------------------------------------------------------");
                    System.out.println("\nThis Acc: " + acc.dump());
                    System.out.println("\nOverrides:");
                    for (SSAValue ovr : allActivities) {
                        System.out.println("\t" + ovr);
                    }
                    System.out.println("\nModel Acc: " + modelAcc.dump());
                    System.out.println("Assign from:");
                    for (SSAValue v : redirectParams) {
                        System.out.println("\t" + v);
                    }
                    System.out.println("---------------------------------------------------------------------------------------");
                } // */
                invokation = instructionFactory.InvokeInstruction(callPC, trash, redirectParams, exception, site);
            }
            redirect.addStatement(invokation);
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
            
            System.err.println("\tasMethod=\t" + asMethod);
            System.err.println("\tcaller=\t" + caller);
            System.err.println("\tcallerND=\t" + callerNd);
            System.err.println("\tthis=\t" + this.getClass().toString());
            System.err.println("\tthis.name=\t" + this.name);

            throw new IllegalStateException(e);
        }
    }

    public IClassHierarchy getClassHierarchy() {
        return this.cha;
    }
}
