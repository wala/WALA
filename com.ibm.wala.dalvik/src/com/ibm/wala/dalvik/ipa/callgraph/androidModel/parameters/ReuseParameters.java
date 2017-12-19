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
import java.util.List;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior.InstanceBehavior;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.ssa.ParameterAccessor.BasedOn;
import com.ibm.wala.util.ssa.ParameterAccessor.ParamerterDisposition;
import com.ibm.wala.util.ssa.ParameterAccessor.Parameter;
import com.ibm.wala.util.strings.Atom;

/**
 *  Helper for building the Descriptor of a model.
 *
 *  Parameters used in a model can be either marked as CREATE or REUSE. This information is derived
 *  from the IInstantiationBehavior.
 *
 *  This class only handles parameters marked as REUSE: These will be parameters to the function 
 *  representing the model itself. On all uses of a variable named REUSE the same Instance (optionally
 *  altered using Phi) will be used.
 *
 *  ReuseParameters collects all those parameters and builds the Descriptor of the later model.
 *  
 *  Also ReuseParameters may be queried how to access these parameters the use of ParameterAccessor
 *  is the better way to get them.
 *
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior
 *  @see    com.ibm.wala.util.ssa.ParameterAccessor 
 *  
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-11-02
 */
public class ReuseParameters {
     public static class ReuseParameter extends Parameter {
        protected ReuseParameter(final int number, final String name, final TypeReference type, final MethodReference mRef, 
                final int descriptorOffset) {
            super(number, name, type, ParamerterDisposition.PARAM, BasedOn.IMETHOD, mRef, descriptorOffset);
        }
    }

    private final IMethod ALL_TARGETS = null;
    private final IInstantiationBehavior instanceBehavior;
    private final AndroidModel forModel;
    private List<TypeName> reuseParameters;

    /**
     *  @param  instanceBehavior   The Behavior to query if the parameter is REUSE
     *  @param  forModel            The AndroidModel in which context the status is to be determined
     */
    public ReuseParameters(final IInstantiationBehavior instanceBehavior, final AndroidModel forModel) {
        this.instanceBehavior = instanceBehavior;
        this.forModel = forModel;
    }

//    private int firstParamSSA() {
//        return 1;   // TODO
//    }

    /**
     *  Searches the given entrypoints for those parameters.
     *
     *  A call to this function resets the internal knowledge of REUSE-Parameters. So in order to
     *  get a union of these parameters a union of the given entrypoints has to be built.
     *
     *  @param  entrypoints The entrypoints to consider in the search.
     */
    public void collectParameters(final Iterable<? extends Entrypoint> entrypoints) {
//        int paramsToModel = firstParamSSA();
        this.reuseParameters = new ArrayList<>();

        for (final Entrypoint ep : entrypoints) {
            final int paramCount = ep.getNumberOfParameters();

            for (int i = 0; i < paramCount; ++i) {
                { // determine paramType
                    final TypeReference[] types = ep.getParameterTypes(i);
                    if (types.length < 1) {
                        throw new IllegalStateException("The Etrypoint " + ep + " did not return any types for its " + i + "th parameter");
                    }
                    
                    // Assert the rest of the types have the same name
                    for (TypeReference type : types) {
                        final TypeName paramType = type.getName();

                        if (isReuse(paramType, ALL_TARGETS)) {
                            if (! reuseParameters.contains(paramType)) {    // XXX: Why not use a Set?
                                reuseParameters.add(paramType);
                                
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *  Get the ssa-number for a parameter to an IMethod.
     *
     *  @see    com.ibm.wala.util.ssa.ParameterAccessor
     */
    private static int ssaFor(IMethod inCallTo, int paramNo) {
        assert (paramNo >= 0);
        assert (paramNo < inCallTo.getNumberOfParameters());

        if (inCallTo.isStatic()) {
            return paramNo + 1;
        } else {
            return paramNo + 1; // TODO 2 or 1?
        }
    }

    /**
     *  Get the first paramNo of a given type.
     *
     *  @see    com.ibm.wala.util.ssa.ParameterAccessor
     */
    private static int firstOf(TypeName type, IMethod inCallTo) {
        for (int i = 0; i < inCallTo.getNumberOfParameters(); ++i) {
            if (inCallTo.getParameterType(i).getName().equals(type)) {
                return i;
            }
        }
        throw new IllegalArgumentException(type.toString() + " is not a parameter to " + inCallTo);
    }

    /**
     *  Is the parameter REUSE in a call from forModel to inCallTo.
     *
     *  The 'forModel' was set in the constructor. Even so a parameter occurs in the descriptor it
     *  does not have to be REUSE for all calls.
     *
     *  The result of this method may vary over time :/
     *
     *  @param  param       The parameter in question of being reuse
     *  @param  inCallTo    The callee to query the REUSEness for
     */
    public boolean isReuse(TypeName param, IMethod inCallTo) {  // TODO: Use IInstantiationBehavior.getBehavior(TypeName param, IMethod inCallTo)
        final TypeName asParameterTo;
        final MethodReference inCall;
        /*final*/ String withName;

        if ((inCallTo != null) && (inCallTo != ALL_TARGETS)) {
            final int bcIndex = 0; // The PC to get the variable name from
            final int localNumber = ssaFor(inCallTo, firstOf(param, inCallTo));
            try {
                withName = inCallTo.getLocalVariableName (bcIndex, localNumber);
            } catch (UnsupportedOperationException e) {
                // DexIMethod doesn't implement this :(
                
                withName = null;
            }
            asParameterTo = inCallTo.getDeclaringClass().getName();
            inCall = inCallTo.getReference();
            /*{ // DEBUG
                System.out.println("isReuse: ");
                System.out.println("\tparam = \t\t" + param);
                System.out.println("\tasParameterTo =\t" + asParameterTo);
                System.out.println("\tinCall =\t" + inCall);
                System.out.println("\twithName =\t" + withName);
            } // */
        } else {
            withName = null;
            asParameterTo = null;
            inCall = null;
        }

        final InstanceBehavior beh = this.instanceBehavior.getBehavior(param, asParameterTo, inCall, withName);

        return (beh == InstanceBehavior.REUSE);
    }

    /**
     *  Generate the descriptor to use for the model.
     *
     *  @param returnType the return type of the later function
     */
    private Descriptor toDescriptor(TypeName returnType) { // Keep private!
        final TypeName[] aTypes = reuseParameters.toArray(new TypeName[reuseParameters.size()]);

        return Descriptor.findOrCreate(aTypes, returnType); 
    }

    public MethodReference toMethodReference(final AndroidModelParameterManager pm) {
        final TypeReference clazz = this.forModel.getDeclaringClass().getReference();
        final Atom name = this.forModel.getName();
        final TypeName returnType = this.forModel.getReturnType();

        final Descriptor descr = toDescriptor(returnType);
        final MethodReference mRef = MethodReference.findOrCreate(clazz, name, descr);

        // TODO: Build Parameters and register them
        if (pm != null) {
            int paramSSA = 1;
            if (! this.forModel.isStatic()) {
                paramSSA = 2;
            }
            final int descriptorOffset;
            if (this.forModel.isStatic()) {
                descriptorOffset = 0;  // TODO Verify!
            } else {
                descriptorOffset = -1;  // TODO Verify!
            }

            for (final TypeName param : reuseParameters) {
                final String tName = null;   // TODO
                final TypeReference tRef = TypeReference.find (ClassLoaderReference.Primordial, param); // TODO: Loaders!
                final ReuseParameter rp = new ReuseParameter(paramSSA, tName, tRef, mRef, descriptorOffset );
                pm.setAllocation(rp);
                //pm.setAllocation(tRef, paramSSA); // TODO: Old-school call
                

                paramSSA++;
            }
        }

        return mRef;
    }
}
