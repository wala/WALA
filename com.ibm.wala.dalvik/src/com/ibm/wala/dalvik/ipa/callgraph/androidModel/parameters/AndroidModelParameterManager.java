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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.ssa.ParameterAccessor;
import com.ibm.wala.util.ssa.SSAValue;

/**
 *  Manages SSA-Numbers for the arguments to Entrypoints.
 *
 *  This class comes in handy if you want to use loops or mix a return value of a function into the
 *  parameter of a later function. It supports multiple levels of cascading code blocks and delivers 
 *  information which SSA-Value is the latest to use or which aught to be combined using a Phi-Statement.
 *  <p>
 *  However it does no allocations or Phi-Statements on its own. It just juggles with the numbers.
 *
 *  @see com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.AbstractAndroidModel
 *
 *  @author  Tobias Blaschke &lt;code@toiasblaschke.de&gt;
 *  @since   2013-09-19
 *
 *  TODO:
 *  <ul>
 *    <li>Track if a variable has been refered to to be able to prune unused Phi-Instructions later</li>
 *    <li>Trim Memory consumption? The whole class should not be in *  memory for long time so this might be not neccessary.</li>
 *  </ul>
 */
public class AndroidModelParameterManager {
 
    private enum ValueStatus {
        UNUSED,             /** Value has never been mentioned before */
        UNALLOCATED,        /** Awaiting to be set using setAllocation */ 
        ALLOCATED,          /** Set and ready to use */
        FREE,               /** Has to be assigned using a Phi-Instruction */
        INVALIDATED,        /** Should only be used as argument to a Phi Instruction */
        CLOSED,             /** Should not be referenced any more  */
        FREE_INVALIDATED,   /** Well FREE and INVALIDATED */ 
        FREE_CLOSED         /** Well FREE and CLOSED */ 
    }

    // TODO: nextLocal may be 0 on getUnamanged!
    /** The next variable not under management yet */
    private int nextLocal;
    /** for managing cascaded code blocks */
    private int currentScope = 0;
    /** For checking if type is CREATE or REUSE (optional) */
    private IInstantiationBehavior behaviour = null;
    /** Description only used for toString() */
    private String description;
//    private MethodReference forMethod;

    /**
     *  Representing a ssa-number - thus a version of an instance to a type.
     */
    private static class ManagedParameter {
        public ValueStatus status = ValueStatus.UNUSED;
        public TypeReference type = null;
        public int ssa = -1;
//        public SSAInstruction setBy = null;
        public int setInScope = -1;
    }

    /** The main data-structure of the management  */
    private Map<TypeReference, List<ManagedParameter>> seenTypes = new HashMap<>();

    /**
     *  Setting the behaviour may be handy in the later model.
     *
     *  However it brings no benefit to the AndroidModelParameterManager.
     */
    public AndroidModelParameterManager(IInstantiationBehavior behaviour) {
        this.behaviour = behaviour;
        this.description = " based on behaviours of " + behaviour;
//        this.forMethod = null; // XXX
    }

    public AndroidModelParameterManager(MethodReference mRef, boolean isStatic) {
        this(new ParameterAccessor(mRef, isStatic));
        this.description = " based on MethodReference " + mRef;
//        this.forMethod = mRef;
    }

    public AndroidModelParameterManager(ParameterAccessor acc) {
        this.behaviour = null;
        nextLocal = acc.getFirstAfter();

        /*
        for (Parameter param: acc.all()) {
            setAllocation(param.getType(), param.getNumber()); 
        }*/

        this.description = " based on ParameterAccessor " + acc;
//        this.forMethod = acc.forMethod();
    }

    //public AndroidModelParameterManager() {
    //    this.behaviour = null;
    //}

    /*
    public void readDescriptior(Descriptor forMethod) {
        for (int i=0; i < forMethod.getParameters().length; ++i) {
            setAllocation(forMethod.getParameters()[i], i + 1);
        }
    }*/

    /**
     *  Register a variable _after_ allocation.
     *
     *  The proper way to add an allocation is to get a Variable using {@link #getUnallocated}. Then 
     *  assign it a value. And at last call this function.
     *
     *  You can however directly call the function if the type has not been seen before.
     *  
     *  @param  type    The type allocated
     *  @param  ssaValue an unallocated SSA-Variable to assign the allocation to
     *  @param  setBy   The instruction that set the value
     *  @throws IllegalStateException if you set more than one allocation for that type (TODO better check!)
     *  @throws IllegalArgumentException if type is null or ssaValue is zero or negative
     */
    public void setAllocation(TypeReference type, int ssaValue, SSAInstruction setBy) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }
        if (ssaValue <= 0) {
            throw new IllegalArgumentException("The SSA-Variable may not be zero or negative.");
        }

        if (seenTypes.containsKey(type)) {
            for (ManagedParameter param : seenTypes.get(type)) {
                if (param.status == ValueStatus.UNALLOCATED) {
                    // XXX: Allow more?
                    assert (param.type.equals(type)) : "Inequal types";
                
                    if ((ssaValue + 1) > nextLocal) {
                        nextLocal = ssaValue + 1;
                    }

                    
                    param.status = ValueStatus.ALLOCATED;
                    param.ssa = ssaValue;
                    param.setInScope = currentScope;
//                    param.setBy = setBy;
                    
                    return;
                } else {
                    continue;
                }
            }
            throw new IllegalStateException("The parameter " + type.getName() + " has already been allocated!");
        } else {
            ManagedParameter param = new ManagedParameter();
            param.status = ValueStatus.ALLOCATED;
            param.type = type;
            param.ssa = ssaValue;
            if ((ssaValue + 1) > nextLocal) {
                nextLocal = ssaValue + 1;
            }
            param.setInScope = currentScope;

            List<ManagedParameter> aParam = new ArrayList<>();
            aParam.add(param);

            
            seenTypes.put(type, aParam);
            return;
        }
    }

    public void setAllocation(TypeReference type, int ssaValue) {
        setAllocation(type, ssaValue, null);
    }

    public void setAllocation(SSAValue val) {
        setAllocation(val.getType(), val.getNumber(), null);
    }

    /**
     *  Register a Phi-Instruction _after_ added to the model.
     *
     *  @param  type    the type the Phi-Instruction sets
     *  @param  ssaValue the number the SSA-Instruction assignes to
     *  @param  setBy   the Phi-Instruction itself - may be null
     *  @throws IllegalArgumentException if you assign to a number requested using
     *      {@link #getFree(TypeReference)} but types mismach.
     *  @throws IllegalStateException if you forgot to close some Phis
     */
    public void setPhi(TypeReference type, int ssaValue, SSAInstruction setBy) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }
        if (ssaValue <= 0) {
            throw new IllegalArgumentException("The SSA-Variable may not be zero or negative.");
        }

        boolean didPhi = false;

        if (seenTypes.containsKey(type)) {
            for (ManagedParameter param : seenTypes.get(type)) {
                if ((param.status == ValueStatus.FREE) ||
                    (param.status == ValueStatus.FREE_INVALIDATED) ||
                    (param.status == ValueStatus.FREE_CLOSED)) {
                    // XXX: Allow more?
                    assert (param.type.equals(type)) : "Inequal types";
                    if (param.ssa != ssaValue) {
                        if ((param.status == ValueStatus.FREE) &&
                            (param.setInScope == currentScope)) {
                                param.status = ValueStatus.FREE_CLOSED;
                        }
                        continue;
                    }
                    
                    if (param.status == ValueStatus.FREE) {
                        param.status = ValueStatus.ALLOCATED;
                    } else if (param.status == ValueStatus.FREE_INVALIDATED) {
                        param.status = ValueStatus.INVALIDATED;
                    } else if (param.status == ValueStatus.FREE_CLOSED) {
                        param.status = ValueStatus.CLOSED;
                    }
                    param.setInScope = currentScope;
//                    param.setBy = setBy;

                    didPhi = true;
                } else if (param.setInScope == currentScope) {
                    if (param.status == ValueStatus.INVALIDATED) {
                        
                        param.status = ValueStatus.CLOSED;
                    } else if (param.status == ValueStatus.FREE_INVALIDATED) {       // TODO: FREE CLOSED
                        
                        param.status = ValueStatus.FREE_CLOSED;
                    }
                } else if (param.setInScope < currentScope) {
                    //param.status = ValueStatus.INVALIDATED;
                } else {
                    // TODO: NO! I JUST WANTED TO ADD THEM! *grrr*
                    //logger.error("MISSING PHI for " 
                    //throw new IllegalStateException("You forgot Phis in subordinate blocks");
                }
            }
            assert (didPhi);
            return;
        } else {
            ManagedParameter param = new ManagedParameter();
            param.status = ValueStatus.ALLOCATED;
            param.type = type;
            param.setInScope = currentScope;
            param.ssa = ssaValue;
            if ((ssaValue + 1) > nextLocal) {
                nextLocal = ssaValue + 1;
            }

            
            List<ManagedParameter> aParam = new ArrayList<>();
            aParam.add(param);

            seenTypes.put(type, aParam);
            return;
        }
    }

    /**
     *  Returns and registers a free SSA-Number to a Type.
     *
     *  You have to set the type using a Phi-Instruction. Also you don't have to add
     *  that instruction immediatly it is required that it is added before the Model
     *  gets finished.
     *
     *  You can request the List of unmet Phi-Instructions by using XXX
     *
     *  @return an unused SSA-Number
     *  @throws IllegalArgumentException if type is null
     */
    public int getFree(TypeReference type) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }

        ManagedParameter param = new ManagedParameter();
        param.status = ValueStatus.FREE;
        param.type = type;
        param.ssa = nextLocal++;
        param.setInScope = currentScope;

        if (seenTypes.containsKey(type)) {
            seenTypes.get(type).add(param);
        } else {
            List<ManagedParameter> aParam = new ArrayList<>();
            aParam.add(param);

            seenTypes.put(type, aParam);
        }

        
        return param.ssa;
    }

    /**
     *  Get an unused number to assign to.
     *
     *  There may only be one unallocated value for each type at a time.    XXX: Really?
     *
     *  @return SSA-Variable
     *  @throws IllegalStateException if there is already an unallocated variable of that type
     *  @throws IllegalArgumentException if type is null
     */
    public int getUnallocated(TypeReference type) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }

        if (seenTypes.containsKey(type)) {
            for (ManagedParameter p : seenTypes.get(type)) {
                if (p.status == ValueStatus.UNALLOCATED) {
                    throw new IllegalStateException("There may be only one unallocated instance to a type (" + type + 
                            ") at a time" );
                }
            }
        }

        ManagedParameter param = new ManagedParameter();
        param.status = ValueStatus.UNALLOCATED;
        param.type = type;
        param.ssa = nextLocal++;
        param.setInScope = currentScope;

        if (seenTypes.containsKey(type)) {
            seenTypes.get(type).add(param);
        } else {
            List<ManagedParameter> aParam = new ArrayList<>();
            aParam.add(param);

            seenTypes.put(type, aParam);
        }

        
        return param.ssa;
    }

    /**
     *  Retreive a SSA-Value that is not under management.
     *
     *  Use instead of 'nextLocal++', else SSA-Values will clash!
     *
     *  @return SSA-Variable
     */
    public int getUnmanaged() {
        int ret = nextLocal++;
        return ret;
    }

    /**
     *  Retreive the SSA-Number that is valid for a type in the current scope.
     *
     *  Either that number origins from an allocation or a PhiInstruction (to be).
     *
     *  @return a ssa number
     *  @throws IllegalStateException if no number is assignable
     *  @throws IllegalArgumentException if type was not seen before or is null
     */
    public int getCurrent(TypeReference type) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }

        int candidateSSA = -1;
        int candidateScope = -1;

        if (seenTypes.containsKey(type)) {
            for (ManagedParameter param : seenTypes.get(type)) {
                if ((param.status == ValueStatus.FREE) ||
                    (param.status == ValueStatus.ALLOCATED)) {
                    assert (param.type.equals(type)) : "Inequal types";
                    if (param.setInScope > currentScope) {
                        
                        continue;
                    } else if (param.setInScope == currentScope) {
                        
                        return param.ssa;
                    } else {
                        if (param.setInScope > candidateScope) {
                            candidateScope = param.setInScope;
                            candidateSSA = param.ssa;
                        }
                    }
                } else {
                    
                }
            }
        } else {
            throw new IllegalArgumentException("Type " + type + " has never been seen before!");
        }

        if (candidateSSA < 0 ) {
            
            return candidateSSA;
        } else {
            throw new IllegalStateException("No suitable candidate has been found for " + type.getName());
        }
    }

    /**
     *  Retreive the SSA-Number that is valid for a type in the super-ordinate scope.
     *
     *  Either that number origins from an allocation or a PhiInstruction (to be).
     *
     *  @return a ssa number
     *  @throws IllegalStateException if no number is assignable
     *  @throws IllegalArgumentException if type was not seen before or is null
     */
    public int getSuper(TypeReference type) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }

        int ssa;
        currentScope--;
        assert(currentScope >= 0 );
        ssa = getCurrent(type);
        currentScope++;
        return ssa;
    }

    /**
     * 
     *  @throws IllegalArgumentException if type was not seen before or is null
     */
    public List<Integer> getAllForPhi(TypeReference type) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }

        List<Integer> ret = new ArrayList<>();

        if (seenTypes.containsKey(type)) {
            for (ManagedParameter param : seenTypes.get(type)) {
                if ((param.status == ValueStatus.FREE) ||
                    (param.status == ValueStatus.ALLOCATED)) {
                    assert (param.type.equals(type)) : "Inequal types";
                  
                    ret.add(param.ssa);
                } else if ((param.status == ValueStatus.INVALIDATED) &&
                            param.setInScope > currentScope) {

                    ret.add(param.ssa);
               }
            }
        } else {
            throw new IllegalArgumentException("Type " + type + " has never been seen before!");
        }

        return ret;
    }

    /**
     *  Return if the type is managed by this class.
     *
     *  @param  withSuper   when true return true if a managed key may be cast to type,
     *                      when false type has to match exactly
     *  @param  type        the type in question
     *  @throws IllegalArgumentException if type is null
     */
    public boolean isSeen(TypeReference type, boolean withSuper) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }

        if (withSuper) {
            return seenTypes.containsKey(type);
        } else {
            if (seenTypes.containsKey(type)) {
                if (seenTypes.get(type).get(0).type.equals(type)) {
                    return true;
                }
            }
        return false;

        }
    }

    public boolean isSeen(TypeReference type) {
        return isSeen(type, true);
    }

    /**
     *  Returns if an instance for that type needs to be allocated.
     *
     *  However this function does not respect weather a PhiInstruction is
     *  needed.
     *
     *  @throws IllegalArgumentException if type is null
     */
    public boolean needsAllocation(TypeReference type) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }

        if (seenTypes.containsKey(type)) {
            if (seenTypes.get(type).size() > 1) {   // TODO INCORRECT may all be UNALLOCATED
                return false;
            } else {
                return (seenTypes.get(type).get(0).status == ValueStatus.UNALLOCATED);
            }
        } else {
            return true;
        }
    }

    /**
     *  Returns if a PhiInstruction (still) has to be added.
     *
     *  This is true if the Value has changed in a deeper scope, has been invalidated
     *  or requested using getFree
     *
     *  @throws IllegalArgumentException if type is null or has not been seen before
     */
    public boolean needsPhi(TypeReference type) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }

        boolean seenLive = false;

        if (seenTypes.containsKey(type)) {
 
            for (ManagedParameter param : seenTypes.get(type)) {   // TODO: Check all these
                if ((param.status == ValueStatus.FREE)) {   // TODO: What about scopes
                    return true;
                }

                if (param.status == ValueStatus.ALLOCATED) {
                    if (seenLive) {
                        return true;
                    } else {
                        seenLive = true;
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Type " + type + " has never been seen before!");
        }

        throw new IllegalStateException("No suitable candidate has been found"); // TODO WRONG text
    }

    /**
     *  @throws IllegalArgumentException if type was not seen before or is null
     */
    public void invalidate(TypeReference type) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }

        if (seenTypes.containsKey(type)) {
            for (ManagedParameter param : seenTypes.get(type)) {
                if ((param.status != ValueStatus.CLOSED) &&
                    (param.status != ValueStatus.FREE_CLOSED) &&
                    (param.status != ValueStatus.FREE_INVALIDATED) &&
                    (param.status != ValueStatus.INVALIDATED) &&
                    (param.setInScope==currentScope)  ) {
                    assert(param.type.equals(type));

                    if (param.status == ValueStatus.FREE) {
                        param.status = ValueStatus.FREE_INVALIDATED;
                    } else {
                        param.status = ValueStatus.INVALIDATED;
                    }
                    
                }
            }
        }
    }

    
    /**
     *  Enter a subordinate scope.
     *
     *  Call this whenever a new code block starts i.e. when ever you would have to put a
     *  left curly-bracket in yout java code.
     *  <p>
     *  This function influences the placement of Phi-Functions. Thus if you don't change
     *  values you don't have to call it.
     *
     *  @param  doesLoop set to true if the scope is introduced for a loop
     *  @return The depth
     */
    public int scopeDown(boolean doesLoop) {    // TODO: Rename scopeInto
        // TODO: Delete Parameters if therw already was scopeNo
        currentScope++;
        return currentScope;
    }

    /**
     *  Leave a subordinate scope.
     *
     *  All changes are marked invalid thus to be expected to be collected by a PhiInstruction.
     *  @throws IllegalStateException if already at top level
     */
    public int scopeUp() {                      // TODO: Rename scopeOut
        // First: Invalidate changed values
        for (List<ManagedParameter> plist : seenTypes.values()) {
            for (ManagedParameter param : plist) {
                if (param.setInScope == currentScope) {
                    invalidate(param.type);
                } else if ((param.setInScope > currentScope) &&
                            ((param.status != ValueStatus.INVALIDATED) ||
                             (param.status != ValueStatus.CLOSED))) {
                    throw new IllegalStateException("Something went wrong in leaving a sub-subordinate scope");
                }
            }
        }
        currentScope--;
        return currentScope;
    }

    /**
     *  Handed through to an IInstantiationBehavior if set in the constructor.
     *
     *  @return true if Type is a REUSE type
     *  @throws IllegalStateException if AndroidModelParameterManager was constructed without an IInstanciationBehavior
     */
    public boolean isReuse(TypeReference type) {
        if (this.behaviour == null) {
            throw new IllegalStateException("AndroidModelParameterManager was constructed without an IInstanciationBehavior");
        }
        if (type.isPrimitiveType()) return false;
        final IInstantiationBehavior.InstanceBehavior beh = this.behaviour.getBehavior(type.getName(), null, null, null);    // TODO: More info here!
        return (beh == IInstantiationBehavior.InstanceBehavior.REUSE);
    }

    /**
     *  Shorthand for not({@link #isReuse(TypeReference)}.
     *
     *  @return true if type is a CREATE-Type
     *  @throws IllegalStateException if AndroidModelParameterManager was constructed without an IInstanciationBehavior
     */
    public boolean isCreate(TypeReference type) {
        return (! isReuse(type));
    }

    @Override
    public String toString() {
        return "<AndroidModelParameterManager " + this.description + ">";
    }
}
