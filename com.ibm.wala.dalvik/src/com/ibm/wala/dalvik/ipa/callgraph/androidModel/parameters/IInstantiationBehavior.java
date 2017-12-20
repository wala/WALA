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

import java.io.Serializable;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;

/**
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public abstract class IInstantiationBehavior implements Serializable {
	private static final long serialVersionUID = -3698760758700891479L;

    /**
     *  The handling for a variable occurring in the AndroidModel.
     */
    public static enum InstanceBehavior { 
        /**
         *  Create a new instance on each occurrence.
         */
        CREATE, 
        /**
         *  Use a single instance throughout the model (uses Phi-in).
         */
        REUSE,
        //CREUSE; 
    } 
    
    /**
     *  Information on how the IInstanciationBehavior made its decision for {@link InstanceBehavior}
     */
    public static enum Exactness { 
        /**
         *  The decision was made based on a exact known mapping from the given data.
         */
        EXACT, 
        /**
         *  No direct mapping was found for the type, the one returned is from a superclass.
         */
        INHERITED,
        /**
         *  The value is based on the package of the variable.
         */
        PACKAGE,
        PREFIX,
        /**
         *  No mapping was found, the default-value was used as a fall-back.
         */
        DEFAULT; 
    }

    /**
     *  Returns how the model should behave on the type.
     *
     *  See the documentation of {@link InstanceBehavior} for the description of the possible behaviours.
     *
     *  Although this function takes a parameter withName one should not rely on its value.
     *
     *  @param  type            The type of the variable in question
     *  @param  asParameterTo   The component whose function the variable shall be used as parameter to.
     *  @param  inCall          The call in question
     *  @param  withName        The name of the parameter in inCall (this might not work)
     *  @return The behaviour to use
     */
    public abstract InstanceBehavior getBehavior(TypeName type, TypeName asParameterTo, MethodReference inCall, String withName);

    /**
     *  Returns how the model should behave on the type.
     *
     *  @param  param       The parameter in question of being reuse
     *  @param  inCallTo    The callee to query the REUSEness for
     */
    public InstanceBehavior getBehavior(final TypeName param, final IMethod inCallTo, final String withName) {
        final TypeName asParameterTo;
        final MethodReference inCall;

        if ((inCallTo != null)) { // XXX: && (inCallTo != ALL_TARGETS)) {
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
            asParameterTo = null;
            inCall = null;
        }

        return getBehavior(param, asParameterTo, inCall, withName);
    }

    /**
     *  The Exactness depends on how the behavior to a type was determined. 
     *
     *  Currently it has no effect on the model but it may come in handy if you want to cascade classes 
     *  for determining the IInstanciationBehavior.
     */
    public abstract Exactness getExactness(TypeName type, TypeName asParameterTo, MethodReference inCall, String withName);

    public abstract InstanceBehavior getDafultBehavior();


}
