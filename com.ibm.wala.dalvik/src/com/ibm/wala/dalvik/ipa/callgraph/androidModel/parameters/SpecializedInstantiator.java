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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModelClass;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.dalvik.util.AndroidTypes;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.ssa.IInstantiator;
import com.ibm.wala.util.ssa.ParameterAccessor;
import com.ibm.wala.util.ssa.SSAValue;
import com.ibm.wala.util.ssa.SSAValue.UniqueKey;
import com.ibm.wala.util.ssa.SSAValue.VariableKey;
import com.ibm.wala.util.ssa.SSAValueManager;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;

/**
 *  Instantiates certain android-types differently.
 *
 *  For example instantiating an android.content.Context would pull in all Android-components in
 *  scope resulting in a massivly overapproximated model.
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public class SpecializedInstantiator extends FlatInstantiator {

    final IInstantiator parent;

    public SpecializedInstantiator(final VolatileMethodSummary body, final TypeSafeInstructionFactory instructionFactory,
            final SSAValueManager pm, final IClassHierarchy cha, final MethodReference scope, final AnalysisScope analysisScope,
            final IInstantiator parent) {
        super(body, instructionFactory, pm, cha, scope, analysisScope, 100);
        this.parent = parent;
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
    @Override
    public SSAValue createInstance(final TypeReference T, final boolean asManaged, VariableKey key, Set<? extends SSAValue> seen) {
        return createInstance(T, asManaged, key, seen, 0);
    }

    /* package private */ SSAValue createInstance(final TypeReference T, final boolean asManaged, VariableKey key, Set<? extends SSAValue> seen, int currentDepth) {
        if (seen == null) {
            
            seen = new HashSet<>();
        }
       
        if (currentDepth > this.maxDepth) {
            final SSAValue instance = this.pm.getUnmanaged(T, key);
            instance.setAssigned();
            return instance;
        }

//        final IClass klass = this.cha.lookupClass(T);
        { // fetch new value
            if (asManaged) {
                if (key == null) {
                    throw new IllegalArgumentException("A managed variable needs a key - null given.");
                }
            } else {
                if (key == null) {
                    key = new UniqueKey();
                }
            }
        }

        assert(understands(T));

        if (T.equals(AndroidTypes.Context)) {
            return createContext(T, key);
        }

        if (T.equals(AndroidTypes.ContextWrapper)) {
            return createContextWrapper(T, key);
        }

        return null;
    }




    private static final Set<TypeReference> understandTypes = new HashSet<>();
    static {
        understandTypes.add(AndroidTypes.Context);
        understandTypes.add(AndroidTypes.ContextWrapper);
    }

    public static boolean understands(TypeReference T) {
        return understandTypes.contains(T);
    }

    // Now for the specialized types...

    /**
     *  Creates a new instance of android/content/Context.
     */
    public SSAValue createContext(final TypeReference T, VariableKey key) {
        final List<SSAValue> appComponents = new ArrayList<>();
        {
            // TODO: Can we create a tighter conterxt?
            // TODO: Force an Application-Context?

            if (AndroidEntryPointManager.MANAGER.doFlatComponents()) {
                final AndroidModelClass mClass = AndroidModelClass.getInstance(cha);

                // At a given time context is expected to be only of one component already seen.
                // If it's seen there is a field in AndroidModelClass.
                for (final IField f : mClass.getAllFields()) {
                    assert(f.isStatic()) : "All fields of AndroidModelClass are expected to be static! " + f + " is not.";
                    
                    final TypeReference fdType = f.getReference().getFieldType();
                    { // Test assignable
                        if (! ParameterAccessor.isAssignable(fdType, T, cha) ) {
                            assert(false) : "Unexpected but not fatal - remove assertion if this happens";
                            continue;
                        }
                    }
                                                                                                           
                    final VariableKey iKey = new SSAValue.TypeKey(fdType.getName());
                    final SSAValue instance;
                    if (this.pm.isSeen(iKey)) {
                        instance = this.pm.getCurrent(iKey);
                    } else {
                        final int pc = this.body.getNextProgramCounter();
                        final VariableKey subKey = new SSAValue.WeaklyNamedKey(fdType.getName(), "ctx" + fdType.getName().getClassName().toString());
                        instance = this.pm.getUnallocated(fdType, subKey);
                        final SSAInstruction getInst = instructionFactory.GetInstruction(pc, instance, f.getReference());
                        this.body.addStatement(getInst);
                        this.pm.setAllocation(instance, getInst);
                    }

                    appComponents.add(instance);
                }
            } else {
                for (TypeReference component : AndroidEntryPointManager.getComponents()) {
                    final VariableKey iKey = new SSAValue.TypeKey(component.getName());

                    if (this.pm.isSeen(iKey)) {
                        final SSAValue instance;
                        instance = this.pm.getCurrent(iKey);
                        assert (instance.getNumber() > 0);
                        appComponents.add(instance);
                    }
                }
            }
        }

        final SSAValue instance;
        if ( appComponents.size() == 1) {
            instance = appComponents.get(0);
        } else if ( appComponents.size() > 0) {
            { // Phi them together
                final int pc = this.body.getNextProgramCounter();
                instance = this.pm.getFree(T, key);
                assert (pc > 0);
                assert (instance.getNumber() > 0);
                final SSAInstruction phi = instructionFactory.PhiInstruction(pc, instance, appComponents);
                this.body.addStatement(phi);
                this.pm.setPhi(instance, phi);
            }
        } else {
            instance = this.pm.getUnmanaged(T, key);
            this.body.addConstant(instance.getNumber(), new ConstantValue(null));
            instance.setAssigned();
        }

        return instance;
    }


    public SSAValue createContextWrapper(final TypeReference T, VariableKey key) {
        final VariableKey contextKey = new SSAValue.TypeKey(AndroidTypes.ContextName);
        final SSAValue context;
        {
            if (this.pm.isSeen(contextKey)) {
                context = this.pm.getCurrent(contextKey);
            } else {
                context = createContext(AndroidTypes.Context, contextKey);
            }
        }

        final SSAValue instance = this.pm.getUnallocated(T, key);
        {
            // call: ContextWrapper(Context base)
            final MethodReference ctor = MethodReference.findOrCreate(T, MethodReference.initAtom, 
                    Descriptor.findOrCreate(new TypeName[] { AndroidTypes.ContextName }, TypeReference.VoidName));
            final List<SSAValue> params = new ArrayList<>();
            params.add(context);
            addCallCtor(instance, ctor, params);
        }

        return instance;
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
        final int currentDepth;
        {
            if (instantiatorArgs.length == 4) {
                currentDepth = (Integer) instantiatorArgs[3];
            } else {
                currentDepth = 0;
            }
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
                instantiatorArgs[2], currentDepth).getNumber();
    }
}
