/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

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
package com.ibm.wala.util.ssa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.ssa.SSAValue.NamedKey;
import com.ibm.wala.util.ssa.SSAValue.VariableKey;
import com.ibm.wala.util.strings.Atom;
/**
 *  Manage SSA-Variables in synthetic methods.
 *
 *  @author  Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since   2013-09-19
 */
public class SSAValueManager {
   
    private final static boolean DEBUG = false;
    private final boolean AUTOMAKE_NAMES = true;

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
    /** Description only used for toString() */
    private String description;
    private MethodReference forMethod;

    /** User-Defined debugging info */
    public String breadCrumb = "";

    private static class Managed<T extends SSAValue> {
        public ValueStatus status = ValueStatus.UNUSED;
        public SSAInstruction setBy = null;
        public int setInScope = -1;
        public final VariableKey key;
        public final T value;

        public Managed(final T value, final VariableKey key) {
            this.value = value;
            this.key = key;
        }

        @Override
        public String toString() {
            return "<Managed " + this.value + " key=\"" + this.key + "\" status=\"" + this.status + " setIn=\"" +
                this.setInScope + "\" setBy=\"" + this.setBy + "\" />";
        }
    }

    /** The main data-structure of the management  */
    private Map<VariableKey, List<Managed<? extends SSAValue>>> seenTypes = HashMapFactory.make();
    private List<SSAValue> unmanaged = new ArrayList<>();
   
    public SSAValueManager(ParameterAccessor acc) {
        this.nextLocal = acc.getFirstAfter();
        this.description = " based on ParameterAccessor " + acc;
        this.forMethod = acc.forMethod();

        for (SSAValue val : acc.all()) {
            setAllocation(val, null);
        }
    }

    /*
    public SSAValueManager(final MethodReference forMethod) {
        this (new 
        //this.nextLocal = nextLocal;
        this.description = " stand alone";
        this.forMethod = forMethod;
    }*/

    /**
     *  Register a variable _after_ allocation.
     *
     *  The proper way to add an allocation is to get a Variable using {@link #getUnallocated}. Then 
     *  assign it a value. And at last call this function.
     *
     *  You can however directly call the function if the type has not been seen before.
     *  
     *  @param  value   an unallocated SSA-Variable to assign the allocation to
     *  @param  setBy   The instruction that set the value (optional)
     *  @throws IllegalStateException if you set more than one allocation for that type (TODO better check!)
     *  @throws IllegalArgumentException if type is null or ssaValue is zero or negative
     */
    public void setAllocation(SSAValue value, SSAInstruction setBy) {
        if (value == null) {
            throw new IllegalArgumentException("The SSA-Variable may not be null");
        }

        if (seenTypes.containsKey(value.key)) {
            for (Managed<? extends SSAValue> param : seenTypes.get(value.key)) {
                if (param.status == ValueStatus.UNALLOCATED) {
                    // XXX: Allow more?
                    assert (param.value.getType().equals(value.getType())) : "Inequal types";

                    if ((param.value.getNumber() + 1) > nextLocal) {
                        nextLocal = param.value.getNumber() + 1;
                    }

                    debug("reSetting SSA {} to allocated", value);
                    param.status = ValueStatus.ALLOCATED;
                    param.setInScope = currentScope;
                    param.setBy = setBy;

                    return;
                } else {
                    continue;
                }
            }
            { // DEBUG
                System.out.println("Keys for " + value + ":");
                for (Managed<? extends SSAValue> param : seenTypes.get(value.key)) {
                    System.out.println("\tKey " + param.key + "\t=>" + param.status);
                }
            } // */
            throw new IllegalStateException("The parameter " + value + " using Key " + value.key + " has already been allocated");
        } else {
            info("New variable in management: {}", value);
            final Managed<SSAValue> param = new Managed<>(value, value.key);
            param.status = ValueStatus.ALLOCATED;
            param.setInScope = currentScope;
            param.setBy = setBy;

            final List<Managed<? extends SSAValue>> aParam = new ArrayList<>();
            aParam.add(param);

            seenTypes.put(value.key, aParam);
        }
    }

    /**
     *  Register a Phi-Instruction _after_ added to the model.
     *
     *  @param  value   the number the SSA-Instruction assigns to
     *  @param  setBy   the Phi-Instruction itself - may be null
     *  @throws IllegalArgumentException if you assign to a number requested using
     *      {@link #getFree} but types mismatch.
     *  @throws IllegalStateException if you forgot to close some Phis
     */
    public void setPhi(final SSAValue value, SSAInstruction setBy) {
        if (value == null) {
            throw new IllegalArgumentException("The SSA-Variable may not be null.");
        }

        boolean didPhi = false;

        if (seenTypes.containsKey(value.key)) {
            for (Managed<? extends SSAValue> param : seenTypes.get(value.key)) {
                if ((param.status == ValueStatus.FREE) ||
                    (param.status == ValueStatus.FREE_INVALIDATED) ||
                    (param.status == ValueStatus.FREE_CLOSED)) {
                    // XXX: Allow more?
                    assert (param.value.getType().equals(value.getType())) : "Unequal types";
                    if (param.value.getNumber() != value.getNumber()) {
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
                    param.setBy = setBy;

                    info("Setting SSA {} to phi! now {}", value, param.status);
                    didPhi = true;
                } else if (param.setInScope == currentScope) {
                    if (param.status == ValueStatus.INVALIDATED) {
                        info("Closing SSA Value {} in scope {}", param.value, param.setInScope);
                        param.status = ValueStatus.CLOSED;
                    } else if (param.status == ValueStatus.FREE_INVALIDATED) {       // TODO: FREE CLOSED
                        info("Closing free SSA Value {} in scope {}", param.value, param.setInScope);
                        param.status = ValueStatus.FREE_CLOSED;
                    }
                } else if (param.setInScope < currentScope) {
                    //param.status = ValueStatus.INVALIDATED;
                } else {
                    // TODO: NO! I JUST WANTED TO ADD THEM! *grrr*
                    //error("MISSING PHI for " 
                    //throw new IllegalStateException("You forgot Phis in subordinate blocks");
                }
            }
            assert (didPhi);
            return;
        } else {
            throw new IllegalStateException("This should not be reached!");
        }
    }
 
    /**
     *  Returns and registers a free SSA-Number to a Type.
     *
     *  You have to set the type using a Phi-Instruction. Also you don't have to add
     *  that instruction immediately it is required that it is added before the Model
     *  gets finished.
     *
     *  You can request the List of unmet Phi-Instructions by using XXX
     *
     *  @return an unused SSA-Number
     *  @throws IllegalArgumentException if type is null
     */
    public SSAValue getFree(TypeReference type, VariableKey key) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("The argument key may not be null");
        }

        final SSAValue var = new SSAValue(nextLocal++, type, this.forMethod, key);
        final Managed<SSAValue> param = new Managed<>(var, key);

        param.status = ValueStatus.FREE;
        param.setInScope = currentScope;

        if (seenTypes.containsKey(key)) {
            seenTypes.get(key).add(param);
        } else {
            List<Managed<? extends SSAValue>> aParam = new ArrayList<>();
            aParam.add(param);

            seenTypes.put(key, aParam);
        }

        debug("Returning as Free SSA: {}", param);
        return var;
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
    public SSAValue getUnallocated(TypeReference type, VariableKey key) {
        if (type == null) {
            throw new IllegalArgumentException("The argument type may not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("The argument key may not be null");
        }

        if (seenTypes.containsKey(key)) {
            for (Managed<? extends SSAValue> p : seenTypes.get(key)) {
                if (p.status == ValueStatus.UNALLOCATED) {
                    throw new IllegalStateException("There may be only one unallocated instance to a kay (" + key +
                            ") at a time" );
                }
            }
        }

        final SSAValue var = new SSAValue(nextLocal++, type, this.forMethod, key);
        final Managed<SSAValue> param = new Managed<>(var, key);

        param.status = ValueStatus.UNALLOCATED;
        param.setInScope = currentScope;

        if (seenTypes.containsKey(key)) {
            seenTypes.get(key).add(param);
        } else {
            List<Managed<? extends SSAValue>> aParam = new ArrayList<>();
            aParam.add(param);

            seenTypes.put(key, aParam);
        }

        debug("Returning as Unallocated SSA: {}", param);
        return var;
    }

    /**
     *  Retrieve a SSA-Value that is not under management.
     *
     *  Use instead of 'nextLocal++', else SSA-Values will clash!
     *
     *  @return SSA-Variable
     */
    public SSAValue getUnmanaged(TypeReference type, VariableKey key) {
        final SSAValue var = new SSAValue(nextLocal++, type, this.forMethod, key);
        this.unmanaged.add(var);
        return var;
    }
  
    public SSAValue getUnmanaged(TypeReference type, String name) {
        return getUnmanaged(type, new NamedKey(type.getName(), name));
    }
    /**
     *  Retrieve the SSA-Number that is valid for a type in the current scope.
     *
     *  Either that number origins from an allocation or a PhiInstruction (to be).
     *
     *  @return a ssa number
     *  @throws IllegalStateException if no number is assignable
     *  @throws IllegalArgumentException if type was not seen before or is null
     */
    public SSAValue getCurrent(VariableKey key) {
        if (key == null) {
            throw new IllegalArgumentException("The argument key may not be null");
        }

        Managed<? extends SSAValue> candidate = null;

        if (seenTypes.containsKey(key)) {
            for (Managed<? extends SSAValue> param : seenTypes.get(key)) {
                if ((param.status == ValueStatus.FREE) ||
                    (param.status == ValueStatus.ALLOCATED)) {
                    //assert (param.value.getType().equals(type)) : "Unequal types";
                    if (param.setInScope > currentScope) {
                        debug("SSA Value {} is out of scope {}", param, currentScope);
                        continue;
                    } else if (param.setInScope == currentScope) {
                        debug("Returning SSA Value {} is {}", param.value, param.status);
                        return param.value;
                    } else {
                        if ((candidate == null) || (param.setInScope > candidate.setInScope)) {
                            candidate = param;
                        }
                    }
                } else {
                    debug("SSA Value {} is {}", param, param.status);
                }
            }
        } else {
            throw new IllegalArgumentException("Key " + key + " has never been seen before! Known keys are " + seenTypes.keySet() );
        }

        if (candidate != null ) {
            debug("Returning inherited (from {}) SSA Value {}", candidate.setInScope, candidate);
            return candidate.value;
        } else {
            throw new IllegalStateException("No suitable candidate has been found for Key " + key);
        }
    }

    /**
     *  Retrieve the SSA-Number that is valid for a type in the super-ordinate scope.
     *
     *  Either that number origins from an allocation or a PhiInstruction (to be).
     *
     *  @return a ssa number
     *  @throws IllegalStateException if no number is assignable
     *  @throws IllegalArgumentException if type was not seen before or is null
     */
    public SSAValue getSuper(VariableKey key) {
        if (key == null) {
            throw new IllegalArgumentException("The argument key may not be null");
        }

        final SSAValue cand;
        currentScope--;
        assert(currentScope >= 0 );
        cand = getCurrent(key);
        currentScope++;
        return cand;
    }

    /**
     *  Returns all "free" and "allocated" variables and the invalid ones in a sub-scope.
     *
     *  This is a suggestion which variables to considder as parameter to a Phi-Function.
     *
     *  @throws IllegalArgumentException if type was not seen before or is null
     */
    public List<SSAValue> getAllForPhi(VariableKey key) {
        if (key == null) {
            throw new IllegalArgumentException("The argument key may not be null");
        }

        List<SSAValue> ret = new ArrayList<>();

        if (seenTypes.containsKey(key)) {
            for (Managed<? extends SSAValue> param : seenTypes.get(key)) {
                if ((param.status == ValueStatus.FREE) ||
                    (param.status == ValueStatus.ALLOCATED)) {
                    //assert (param.type.equals(type)) : "Unequal types";

                    ret.add(param.value);
                } else if ((param.status == ValueStatus.INVALIDATED) &&
                            param.setInScope > currentScope) {

                    ret.add(param.value);
               }
            }
        } else {
            throw new IllegalArgumentException("Key " + key + " has never been seen before!");
        }

        return ret;
    }

    /**
     *  Return if the type is managed by this class.
     *
     *  @param  withSuper   when true return true if a managed key may be cast to type,
     *                      when false type has to match exactly
     *  @param  key         the type in question
     *  @throws IllegalArgumentException if key is null
     */
    public boolean isSeen(VariableKey key, boolean withSuper) {
        if (key == null) {
            throw new IllegalArgumentException("The argument key may not be null");
        }

        if (withSuper) {
            return seenTypes.containsKey(key);
        } else {
            if (seenTypes.containsKey(key)) {
/*                if (seenTypes.get(key).get(0).type.equals(type)) {    // TODO: Rethink
                    return true;
                }*/
            }
        return false;

        }
    }

    /**
     *  Return if the type is managed by this class.
     *
     *  This variant respects super-types. Use isSeen(VariableKey, boolean) with a setting
     *  for withSuper of false to enforce exact matches.
     *
     *  @return if the type is managed by this class.
     */
    public boolean isSeen(VariableKey key) {
        return isSeen(key, true);
    }

    /**
     *  Returns if an instance for that type needs to be allocated.
     *
     *  However this function does not respect weather a PhiInstruction is
     *  needed.
     *
     *  @throws IllegalArgumentException if type is null
     */
    public boolean needsAllocation(VariableKey key) {
        if (key == null) {
            throw new IllegalArgumentException("The argument key may not be null");
        }

        if (seenTypes.containsKey(key)) {
            if (seenTypes.get(key).size() > 1) {   // TODO INCORRECT may all be UNALLOCATED
                return false;
            } else {
                return (seenTypes.get(key).get(0).status == ValueStatus.UNALLOCATED);
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
    public boolean needsPhi(VariableKey key) {
        if (key == null) {
            throw new IllegalArgumentException("The argument key may not be null");
        }

        boolean seenLive = false;

        if (seenTypes.containsKey(key)) {
            
            for (Managed<? extends SSAValue> param : seenTypes.get(key)) {   // TODO: Check all these
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
            throw new IllegalArgumentException("Key " + key + " has never been seen before!");
        }

        throw new IllegalStateException("No suitable candidate has been found"); // TODO WRONG text
    }


    /**
     *  Marks all known instances of VariableKey invalid.
     *
     *  A call to this method is useful before a call to setAllocation. This methods sets all
     *  known instances to invalid, setAllocation will assign the new "current" instance to
     *  use.
     *
     *  @param  key     Which variables to invalidate.
     *  @throws IllegalArgumentException if type was not seen before or is null
     */
    public void invalidate(VariableKey key) {
        if (key == null) {
            throw new IllegalArgumentException("The argument key may not be null");
        }

        if (seenTypes.containsKey(key)) {
            for (Managed<? extends SSAValue> param : seenTypes.get(key)) {
                if ((param.status != ValueStatus.CLOSED) &&
                    (param.status != ValueStatus.FREE_CLOSED) &&
                    (param.status != ValueStatus.FREE_INVALIDATED) &&
                    (param.status != ValueStatus.INVALIDATED) &&
                    (param.setInScope==currentScope)  ) {
                    //assert(param.type.equals(type));

                    if (param.status == ValueStatus.FREE) {
                        param.status = ValueStatus.FREE_INVALIDATED;
                    } else {
                        param.status = ValueStatus.INVALIDATED;
                    }
                    info("Invalidated SSA {} for key {}", param, key);
                }
            }
        }
    }


    /**
     *  Enter a subordinate scope.
     *
     *  Call this whenever a new code block starts i.e. when ever you would have to put a
     *  left curly-bracket in the java code.
     *  <p>
     *  This function influences the placement of Phi-Functions. Thus if you don't change
     *  values you don't have to call it.
     *
     *  @param  doesLoop set to true if the scope is introduced for a loop
     *  @return The depth
     */
    public int scopeDown(boolean doesLoop) {    // TODO: Rename scopeInto
        // TODO: Delete Parameters if there already was scopeNo
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
        for (List<Managed<? extends SSAValue>> plist : seenTypes.values()) {
            for (Managed<? extends SSAValue> param : plist) {
                if (param.setInScope == currentScope) {
                    invalidate(param.value.key);
                } else if ((param.setInScope > currentScope) &&
                            ((param.status != ValueStatus.INVALIDATED) &&
                             (param.status != ValueStatus.CLOSED))) {
                    throw new IllegalStateException("A parameter was in wrong status when leaving a sub-subordinate scope: " 
                            + param + " should have been invalidated or closed by an other scope.");
                }
            }
        }
        currentScope--;
        return currentScope;
    }

    @Override
    public String toString() {
        return "<AndroidModelParameterManager " + this.description + ">";
    }

    /**
     *  Create new SSAValue with UniqueKey and Exception-Type.
     *
     *  The generated SSAValue will be unmanaged. It is mainly useful for SSAInvokeInstructions.
     *
     *  @return new unmanaged SSAValue with Exception-Type
     */
    public SSAValue getException() {
        SSAValue exc = new SSAValue(nextLocal++, TypeReference.JavaLangException, this.forMethod, "exception_" + nextLocal); // UniqueKey
        this.unmanaged.add(exc);
        return exc;
    }

    /**
     *  Collect the variable-names of all known variables.
     */
    public Map<Integer, Atom> makeLocalNames() {
        final Map<Integer, Atom> names = new HashMap<>();
        final Map<VariableKey, Integer> suffix = new HashMap<>();
        int currentSuffix = 0;

        for (final List<Managed<? extends SSAValue>> manageds : seenTypes.values()) {
            for (final Managed<? extends SSAValue> managed : manageds) {
                final SSAValue val = managed.value;
                final String name = val.getVariableName();
                if (name != null) {
                    final Atom nameAtom = Atom.findOrCreateAsciiAtom(name);
                    names.put(val.getNumber(), nameAtom);
                } else if (AUTOMAKE_NAMES) {
                    String autoName = val.getType().getName().toString();
                    if (autoName.contains("/")) {
                        autoName = autoName.substring(autoName.lastIndexOf("/") + 1);
                    }
                    if (autoName.contains("$")) {
                        autoName = autoName.substring(autoName.lastIndexOf("$") + 1);
                    }
                    autoName = autoName.replace("[", "Ar");
                    final int mySuffix;
                    if (suffix.containsKey(val.key)) {
                        mySuffix = suffix.get(val.key);
                    } else {
                        mySuffix = currentSuffix++;
                        suffix.put(val.key, mySuffix);
                    }
                    autoName = "m" + autoName + "_" + mySuffix;
                    final Atom nameAtom = Atom.findOrCreateAsciiAtom(autoName);
                    names.put(val.getNumber(), nameAtom);
                }
            }
        }

        for (final SSAValue val : this.unmanaged) {
            final String name = val.getVariableName();
            if (name != null) {
                final Atom nameAtom = Atom.findOrCreateAsciiAtom(name);
                names.put(val.getNumber(), nameAtom);
            } else if (AUTOMAKE_NAMES) {
                String autoName = val.getType().getName().toString();
                if (autoName.contains("/")) {
                    autoName = autoName.substring(autoName.lastIndexOf("/") + 1);
                }
                if (autoName.contains("$")) {
                    autoName = autoName.substring(autoName.lastIndexOf("$") + 1);
                }
                autoName = autoName.replace("[", "Ar");
                final int mySuffix;
                if (suffix.containsKey(val.key)) {
                    mySuffix = suffix.get(val.key);
                } else {
                    mySuffix = currentSuffix++;
                    suffix.put(val.key, mySuffix);
                }
                autoName = "m" + autoName + "_" + mySuffix;
                final Atom nameAtom = Atom.findOrCreateAsciiAtom(autoName);
                names.put(val.getNumber(), nameAtom);
            }
        }

        return names;
    }

    private static void debug(String s, Object ... args) {
      if (DEBUG) { System.err.printf(s, args); }
    }
    
    private static void info(String s, Object ... args) {    
      if (DEBUG) { System.err.printf(s, args); }
    }
    
}
