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
package com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.analysis.typeInference.ConeType;
import com.ibm.wala.analysis.typeInference.PrimitiveType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModelClass;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.ssa.IInstantiator;
import com.ibm.wala.util.ssa.ParameterAccessor;
import com.ibm.wala.util.ssa.SSAValue;
import com.ibm.wala.util.ssa.SSAValue.UniqueKey;
import com.ibm.wala.util.ssa.SSAValue.VariableKey;
import com.ibm.wala.util.ssa.SSAValueManager;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;
import com.ibm.wala.util.strings.Atom;

/**
 *  Add code to create an instance of a type in a synthetic method.
 *
 *  Creates an instance of (hopefully) anything.
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public class Instantiator implements IInstantiator {
    private static final Logger logger = LoggerFactory.getLogger(Instantiator.class);

    final IClassHierarchy cha;
    final VolatileMethodSummary body;
    final TypeSafeInstructionFactory instructionFactory;
    final SSAValueManager pm;
    final MethodReference scope;
    final AnalysisScope analysisScope; 

    public Instantiator(final VolatileMethodSummary body, final TypeSafeInstructionFactory instructionFactory,
            final SSAValueManager pm, final IClassHierarchy cha, final MethodReference scope, final AnalysisScope analysisScope) {
        this.body = body;
        this.instructionFactory = instructionFactory;
        this.pm = pm;
        this.cha = cha;
        this.scope = scope;
        this.analysisScope = analysisScope;
    }

    private boolean isExcluded(IClass cls) {
        if (this.analysisScope.getExclusions() != null && this.analysisScope.getExclusions().contains(cls.getName().toString())) {   // XXX FUUUUU
            logger.info("Hit exclusions with {}", cls);
            return true;
        } else {
            return false;
        }
    }

    /**
     *  Creates a new instance of type calling all that's necessary.
     *
     *  If T is a class-type all its constructors are searched for the one found best suited (takes the least arguments, ...).
     *  New instances are created for all parameters, then the constructor is called.
     *
     *  If T represents multiple types (is an interface, abstract class, ...) _all_ implementors of that type are instantiated
     *  After that they get Phi-ed together.
     *
     *  If T is an array-type a new array of length 1 is generated.
     *
     *  TODO: Do we want to mix in REUSE-Parameters?
     */
    public SSAValue createInstance(final TypeReference T, final boolean asManaged, VariableKey key, Set<? extends SSAValue> seen) {
        if (T == null) {
            throw new IllegalArgumentException("Can't create an instance of null");
        }
        if (seen == null) {
            logger.debug("Empty seen");
            seen = new HashSet<>();
        }

        { // Special type?
            if (SpecializedInstantiator.understands(T)) {
                final SpecializedInstantiator sInst = new SpecializedInstantiator(body, instructionFactory, pm,
                        cha, scope, analysisScope, this);
                return sInst.createInstance(T, asManaged, key, seen);
            }
        }

        final IClass klass = this.cha.lookupClass(T);
        final SSAValue instance;
        { // fetch new value
            if (asManaged) {
                if (key == null) {
                    throw new IllegalArgumentException("A managed variable needs a key - null given.");
                }
                if ((klass != null) && (klass.isAbstract() || klass.isInterface())) {
                    // We'll need a phi
                    instance = this.pm.getFree(T, key);
                } else {
                    instance = this.pm.getUnallocated(T, key);
                }
            } else {
                if (key == null) {
                    key = new UniqueKey();
                }
                instance = this.pm.getUnmanaged(T, key); 
            }
        }
     
        { // Try fetch Android-Components from AndroidModelClass
            if (com.ibm.wala.dalvik.util.AndroidComponent.isAndroidComponent(T, cha)) {
                if ( AndroidEntryPointManager.MANAGER.doFlatComponents()) {
                    final AndroidModelClass mClass = AndroidModelClass.getInstance(cha);
                    final Atom fdName = T.getName().getClassName();

                    if (mClass.getField(fdName) != null) {
                        final IField field = mClass.getField(fdName);
                        final int instPC = this.body.getNextProgramCounter();
                        final SSAInstruction getInst = instructionFactory.GetInstruction(instPC, instance, field.getReference());
                        this.body.addStatement(getInst);
                        pm.setAllocation(instance, getInst);
                        return instance;
                    } else {
                        logger.info("NEW Component {} \n\tbreadCrumb: {}", instance, pm.breadCrumb);
                    }
                } else {
                    logger.info("NEW Component {} \n\tbreadCrumb: {}", instance, pm.breadCrumb);
                }
            }
        } // */

        if (T.isPrimitiveType()) {
            createPrimitive(instance);
            return instance;
        } else if (klass == null) {
            if (! T.getName().toString().startsWith("Landroid/")) {
                logger.error("The Type {} is not in the ClassHierarchy! Returning null as instance", T);
            } else {
                logger.debug("The Type {} is not in the ClassHierarchy! Returning null as instance", T);
            }
            this.body.addConstant(instance.getNumber(), new ConstantValue(null));
            instance.setAssigned();
            return instance;
        } else if (isExcluded(klass)) {
            this.body.addConstant(instance.getNumber(), new ConstantValue(null));   // TODO: null or nothing?
            instance.setAssigned();
            return instance;
        } 
        
        final Set<TypeReference> types = getTypes(T);  

        logger.info("Creating instance of {} is  {}", T, types);
        if (types.isEmpty()) {
            throw new IllegalStateException("Types of " + T + " are empty");
        }
       
        
        if ((types.size() == 1) && (! klass.isAbstract()) && (! klass.isArrayClass()) && (! klass.isInterface() )) {
            // It's a "regular" class
            final SSANewInstruction newInst = addNew(instance);
            selectAndCallCtor(instance, seen);
            if (asManaged) {
                this.pm.setAllocation(instance, newInst);
            }
            assert(newInst.getDef() == instance.getNumber());
            return instance;
        } else if (klass.isArrayClass()) {      
            logger.info("Creating Array-Class {}", klass.toString());

            final TypeReference payloadType = T.getArrayElementType();
            SSAValue payload = null;
            {
                for (final SSAValue see : seen) {
                    if (ParameterAccessor.isAssignable(see.getType(), payloadType, this.cha)) {
                        // Happens on Array of interfaces
                        logger.trace("Reusing {} for array payload {}", see, payload);
                        payload = see;
                    }
                }
                if (payload == null) {
                    payload = createInstance(payloadType, false, new UniqueKey(), seen); 
                }
            }
            //assert (types.size() == 1);   // TODO
            
            // Generate an array of length 1
            final SSANewInstruction newInst;
            {
                final int pc = this.body.getNextProgramCounter();
                final NewSiteReference nRef = NewSiteReference.make(pc, instance.getType());

                final SSAValue arrayLength = this.pm.getUnmanaged(TypeReference.Int, new UniqueKey());
                this.body.addConstant(arrayLength.getNumber(), new ConstantValue(1));
                arrayLength.setAssigned();

                final ArrayList<SSAValue> params = new ArrayList<>(1);
                params.add(arrayLength);

                newInst = this.instructionFactory.NewInstruction(pc, instance, nRef, params);
                this.body.addStatement(newInst);
                assert(instance.getNumber() == newInst.getDef());
            }

            // Put a payload into the array
            {
                final int pc = this.body.getNextProgramCounter();
                final SSAInstruction write = this.instructionFactory.ArrayStoreInstruction(pc, instance, 0, payload);
                body.addStatement(write);
            }

            assert(newInst.getDef() == instance.getNumber());
            return instance;
        } else {
            // Abstract, Interface or array
            logger.debug("Not a regular class {}", T);
            final Set<SSAValue> subInstances = new HashSet<>();
            for (final TypeReference type : types) {
                final IClass subKlass = this.cha.lookupClass(type);

                if (subKlass.isAbstract() || subKlass.isInterface()) { 
                    // All "regular" classes in consideration should already be in types
                    continue;
                }
               
                { // Create instance of subInstance
                    final SSAValue subInstance = pm.getUnmanaged(type, new UniqueKey());
                    final SSANewInstruction newInst = addNew(subInstance);
                    selectAndCallCtor(subInstance, seen);
                    assert (subInstance.getNumber() == newInst.getDef()) : "Unexpected: number and def differ: " + subInstance.getNumber() + ", " +
                                    newInst.getDef();
                    final Set<SSAValue> newSeen = new HashSet<>();  // Narf
                    newSeen.addAll(seen);
                    newSeen.add(subInstance);
                    seen = newSeen;
                    subInstances.add(subInstance);
                }
            }

            TypeAbstraction abstraction = null;
            { // Build the abstraction
                for (final TypeReference type : types) {
                    final IClass cls;
                    if (type.isPrimitiveType()) {
                        cls = null;
                    } else {
                        cls = this.cha.lookupClass(type);
                        assert(cls != null);
                    }

                    if (abstraction == null) {
                        // TODO: assert primitive stays primitive
                        if (type.isPrimitiveType()) {   // XXX: May this happen here?
                            abstraction = PrimitiveType.getPrimitive(type);
                        } else {
                            abstraction = new ConeType(cls); 
                        }
                    } else {
                        if (type.isPrimitiveType()) {
                            abstraction = abstraction.meet(PrimitiveType.getPrimitive(type));
                        } else {
                            abstraction = abstraction.meet(new ConeType(cls));
                        }
                    }
                }

                // XXX: What to do with the abstraction now?
            }

            { // Phi together everything
                if (subInstances.size() > 0) {
                    final int pc = this.body.getNextProgramCounter();
                    final SSAInstruction phi = this.instructionFactory.PhiInstruction(pc, instance, subInstances);
                    body.addStatement(phi);
                    if (asManaged) {
                        this.pm.setPhi(instance, phi);
                    }
                } else {
                    logger.warn("No sub-instances for: {} - setting to null", instance);
                    this.body.addConstant(instance.getNumber(), new ConstantValue(null));
                    instance.setAssigned();
                }
            }
        }

        return instance; 
    }

    private static void createPrimitive(SSAValue instance) {
        // XXX; something else?
        instance.setAssigned();
    }

    /**
     *  Add a NewInstruction to the body.
     */
    private SSANewInstruction addNew(SSAValue val) {
        final int pc = this.body.getNextProgramCounter();
        final NewSiteReference nRef = NewSiteReference.make(pc, val.getType());
        final SSANewInstruction newInstr = this.instructionFactory.NewInstruction(pc, val, nRef);
        this.body.addStatement(newInstr);
        assert(val.getNumber() == newInstr.getDef());
        return newInstr;
    }

    /**
     *  Add a call to a single clinit to the body.
     *
     *  @param  val the "this" to call clinit on
     *  @param  inClass the class to call its clinit of
     */
    /*private void addCallCLinit(SSAValue val, TypeReference inClass) {
        final int pc = this.body.getNextProgramCounter();
        final MethodReference mRef = MethodReference.findOrCreate(inClass, MethodReference.clinitSelector);
        final SSAValue exception = pm.getException();
        final CallSiteReference site = CallSiteReference.make(pc, mRef, IInvokeInstruction.Dispatch.STATIC);
        final List<SSAValue> params = new ArrayList<SSAValue>(1);
        params.add(val);
        final SSAInstruction ctorCall = instructionFactory.InvokeInstruction(pc, params, exception, site);
        body.addStatement(ctorCall);
    } */

    /**
     *  Add a call to the given constructor to the body.
     *
     *  @param  self the "this" to call the constructor on
     *  @param  ctor the constructor to call
     *  @param  ctorParams parameters to the ctor _without_ implicit this
     */
    private void addCallCtor(SSAValue self, MethodReference ctor, List<SSAValue> ctorParams) {
        final int pc = this.body.getNextProgramCounter();
        final SSAValue exception = pm.getException();
        final CallSiteReference site = CallSiteReference.make(pc, ctor, IInvokeInstruction.Dispatch.SPECIAL);
        final List<SSAValue> params = new ArrayList<>(1 + ctorParams.size());
        params.add(self);
        params.addAll(ctorParams);
        final SSAInstruction ctorCall = instructionFactory.InvokeInstruction(pc, params, exception, site);
        body.addStatement(ctorCall);
    }

    private MethodReference selectAndCallCtor(SSAValue val, final Set<? extends SSAValue> overrides) {
        final IMethod cTor = lookupConstructor(val.getType());
        final ParameterAccessor ctorAcc = new ParameterAccessor(cTor);
        assert (ctorAcc.hasImplicitThis()) : "CTor detected as not having implicit this pointer";
        logger.debug("Acc for: %", this.scope);
        final ParameterAccessor acc = new ParameterAccessor(this.scope, false); // TODO pm needs a connectThrough too!
                                                                // TODO false is false
        // TODO: The overrides may lead to use before definition
        //if (acc.firstOf(val.getType().getName()) != null) {
            final SSAValue nullSelf = pm.getUnmanaged(val.getType(), new UniqueKey());
            this.body.addConstant(nullSelf.getNumber(), new ConstantValue(null));
            nullSelf.setAssigned();
        //}
        final Set<SSAValue> seen = new HashSet<>(1 + overrides.size());
        seen.add(nullSelf);
        seen.addAll(overrides);
        
        logger.debug("Recursing for: {}", cTor);
        logger.debug("With seen: {}", seen);
        final List<SSAValue> ctorParams = acc.connectThrough(ctorAcc, overrides, /* defaults */ null, this.cha, 
                this, /* managed */ false, /* key */ null, seen); // XXX This starts the recursion!
        addCallCtor(val, cTor.getReference(), ctorParams);
        return cTor.getReference();
    }

    /**
     *  Get all sub-types a type represents until concrete ones are reached.
     *
     *  A concrete type only represents itself.
     *
     *  @throws IllegalArgumentException if T is a primitive.
     */
    private Set<TypeReference> getTypes(final TypeReference T) {
        final IClass cls = this.cha.lookupClass(T);
        if (isExcluded(cls)) {
            return new HashSet<>();
        }
        return getTypes(T, Collections.<TypeReference>emptySet());
    }

    /**
     *  Used internally to avoid endless recursion on getTypes().
     */
    private Set<TypeReference> getTypes(final TypeReference T, final Set<TypeReference> seen) {
        logger.debug("getTypes({}, {})", T, seen);
        final Set<TypeReference> ret = new HashSet<>();
        ret.add(T);
       
        if (T.isPrimitiveType()) {
            logger.warn("getTypes called on a primitive");
            return ret;
            //throw new IllegalArgumentException("Not you that call primitive type on :P");
        }

        final IClass cls = this.cha.lookupClass(T);
        if (cls == null) {
            logger.error("The type {} is not in the ClassHierarchy - try continuing anyway", T);
            return ret;
            //throw new IllegalArgumentException("The type " + T + " is not in the ClassHierarchy");
        } else if (isExcluded(cls)) {
            return ret;
        } else if (seen.contains(T)) {
            return ret;
        }

        if (cls.isInterface()) {
            final Set<IClass> impls = cha.getImplementors(T);
            if (impls.isEmpty()) {
                //throw new IllegalStateException("The interface " + T + " has no known implementors");
                if (! T.getName().toString().startsWith("Landroid/")) {
                    logger.error("The interface {} has no known implementors - skipping over it", T);
                } else {
                    logger.debug("The interface {} has no known implementors - skipping over it", T);
                }
                return ret; // XXX: This is a bad idea?
            } else {
                // ADD all
                for (IClass impl: impls) {
                    if (impl.isAbstract()) {
                        ret.addAll(getTypes(impl.getReference(), ret));  // impl added through recursion
                    } else {
                        ret.add(impl.getReference());
                    }
                }
            }
        } else if (cls.isAbstract()) {
            final Collection<IClass> subs = cha.computeSubClasses(T);
            if (subs.isEmpty()) {
                throw new IllegalStateException("The class " + T + " is abstract but has no subclasses known to the ClassHierarchy");
            } else {
                for (final IClass sub: subs) {
                    if (seen.contains(sub.getReference())) {
                        logger.debug("Seen: {}", sub);
                        continue;
                    }
                    if (sub.isAbstract()) {
                        // Recurse on abstract classes
                        ret.addAll(getTypes(sub.getReference(), ret));  // sub added through recursion
                    } else {
                        ret.add(sub.getReference());
                    }
                }
            }
        } else if (cls.isArrayClass()) {
            final ArrayClass aCls = (ArrayClass) cls;
            final int dim = aCls.getDimensionality();

            if (aCls.isOfPrimitives()) {
                ret.add(aCls.getReference());
            } else {
                final IClass inner = aCls.getInnermostElementClass();
                
                if (inner == null) {
                    throw new IllegalStateException("The array " + T + " has no inner class");
                }

                if ((inner.isInterface()) || (inner.isAbstract())) {
                    final Set<TypeReference> innerTypes = getTypes(inner.getReference(), Collections.<TypeReference>emptySet());
                    for (TypeReference iT : innerTypes) {
                        TypeReference aT = TypeReference.findOrCreateArrayOf(iT);
                        for (int i = 1; i < dim; ++i) {
                            aT = TypeReference.findOrCreateArrayOf(aT);
                        }
                        ret.add(aT);
                    }
                } else {
                    ret.add(TypeReference.findOrCreateArrayOf(inner.getReference()));
                }
            }
        }

        return ret;
    }

    /**
     *  Path back to Object (including T itself).
     */
    @SuppressWarnings("unused")
    private List<TypeReference> getAllSuper(final TypeReference T) {
        if (T.isPrimitiveType()) {
            throw new IllegalArgumentException("Not you that call primitive type on :P");
        }
        final List<TypeReference> ret = new ArrayList<>();

        IClass cls = this.cha.lookupClass(T);
        if (cls == null) {
            throw new IllegalArgumentException("The type " + T + " is not in the ClassHierarchy");
        }

        while (cls != null) {
            ret.add(cls.getReference());
            cls = cls.getSuperclass();
        }

        return ret;
    }

    /**
     *  The Constructor starts with 'this()' or 'super()'.
     */
    /*private boolean callsCtor(MethodReference ctor) {
        if (ctor == null) {
            throw new IllegalArgumentException("Null ctor");
        }
        
        final Set<IMethod> methods = cha.getPossibleTargets(ctor);

        if (methods == null) {
            throw new IllegalArgumentException("Unable to look up IMethod for ctor " + ctor);
        }

        if (methods.size() != 1) {
            throw new UnsupportedOperationException("Unexpected multiple candidates for ctor " + ctor + " are " + methods);
        }

        final IMethod method = methods.iterator().next();
        assert (method.isInit());
        final SSAInstruction firstInstruction = this.cache.getIR(method).iterateAllInstructions().next();
        logger.debug("First instruction of ctor is: " + firstInstruction);
        if (firstInstruction instanceof SSAAbstractInvokeInstruction) {
            final SSAAbstractInvokeInstruction invokation = (SSAAbstractInvokeInstruction) firstInstruction;
            return invokation.isSpecial(); // Always?
        }

        return false;
    }*/

    /**
     *  Selects the constructor of T found to be bes suited.
     */
    private IMethod lookupConstructor(TypeReference T) {
        IMethod ctor = null;
        int score = -10000;
        final IClass klass = cha.lookupClass(T);
        
        if (klass == null) {
            throw new IllegalArgumentException("Unable to look up the class for " + T);
        }

        if (klass.isInterface() || klass.isAbstract()) {
            throw new IllegalArgumentException("Class is interface or abstract");
        }

        for (final IMethod im: klass.getDeclaredMethods()) {
            if (! im.isInit()) continue;

            int candidScore = 0;
            final int paramCount = im.getNumberOfParameters();

            if (im.isPrivate()) {
                score -= 10;
            } else if (im.isProtected()) {
                score -= 1;
            }

            for (int i = 1; i < paramCount; ++i) {
                final TypeReference paramType = im.getParameterType(i);

                if (paramType.isPrimitiveType()) {
                    candidScore -= 1;
                } else if (paramType.isArrayType()) {       // TODO: Reevaluate scores
                     candidScore-=30;

                     if (paramType.getInnermostElementType().equals(T)) {
                         // Array of itself
                         candidScore -= 1000;
                     }
                } else if (paramType.isClassType()) {
                     candidScore-=101;
                } else if (paramType.isReferenceType()) {    // TODO: Avoid interfaces
                    if (paramType.equals(T)) {
                        candidScore -= 1000;
                    } else {
                        candidScore -= 7;
                    }

                    if (paramType.equals(TypeReference.JavaLangObject)) {
                        candidScore -= 1500;
                    }
                } else {
                    // ?!
                    candidScore -= 800;
                }
            }

            if (candidScore > score) {
                ctor = im;
                score = candidScore;
            }

            logger.debug("CTor {} got score {}", im, candidScore);

        }

        if (ctor == null) {
            logger.warn("Still found no CTor for {}", T);
            return cha.resolveMethod(klass, MethodReference.initSelector);
        } else {
            return ctor;
        }
    }

    /**
     *  Satisfy the interface.
     */
    @Override
    @SuppressWarnings("unchecked")
    public int createInstance(TypeReference type, Object... instantiatorArgs) {
        // public SSAValue createInstance(final TypeReference T, final boolean asManaged, VariableKey key, Set<SSAValue> seen) {
        if (! (instantiatorArgs[0] instanceof Boolean)) {
            throw new IllegalArgumentException("Argument 0 to createInstance has to be boolean.");
        }
        if (! ((instantiatorArgs[1] == null) || (instantiatorArgs[1] instanceof VariableKey))) {
            throw new IllegalArgumentException("Argument 1 to createInstance has to be null or an instance of VariableKey"); 
        }
        if (! ((instantiatorArgs[2] == null) || (instantiatorArgs[2] instanceof Set))) {
            throw new IllegalArgumentException("Argument 2 to createInstance has to be null or an instance of Set<? extends SSAValue>, " +
                    "got: " + instantiatorArgs[2].getClass()); 
        }
        if (instantiatorArgs[2] != null) {
            final Set<?> seen = (Set<?>) instantiatorArgs[2];
            if (! seen.isEmpty()) {
                final Object o = seen.iterator().next();
                if (! (o instanceof SSAValue)) {
                    throw new IllegalArgumentException("Argument 2 to createInstance has to be null or an instance of Set<? extends SSAValue>, " +
                            "got Set<" + o.getClass() + ">");
                }
            }
        }

        return createInstance(type, (Boolean) instantiatorArgs[0], (VariableKey) instantiatorArgs[1], (Set<? extends SSAValue>) 
                instantiatorArgs[2]).getNumber();
    }
}
