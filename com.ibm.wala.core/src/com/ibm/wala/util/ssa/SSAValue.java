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

import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

/**
 *  A number representating an SSA-Value and its type.
 *
 *  WALA does not use this on a regular basis but it may come in handy for creating 
 *  SyntheticMethods.
 *
 *  Use ParameterAccessor to get the parameters of a function as SSAValues.
 *
 *  @see    com.ibm.wala.util.ssa.TypeSafeInstructionFactory
 *  @see    com.ibm.wala.util.ssa.ParameterAccessor
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-10-20
 */
public class SSAValue {
    /** The SSA Value itself */
    protected final int number;
    /** The type of this variable */
    protected final TypeReference type;
    /** All variables with the same name in the source code share a key. */ 
    public final VariableKey key; // TODO: Protect again?
    /** Method the variable is valid in */
    protected final MethodReference mRef;
    /** If an instruction wrote to this value (set manually) */
    private boolean isAssigned;

    /**
     *  All variables with the same name in the source code.
     */
    public interface VariableKey {}
    /**
     *  A key that cannot be recreated.
     */
    public static class UniqueKey implements VariableKey {
        public UniqueKey() { }
    }
    /**
     *  A key that matches variables by their type - does not compare to NamedKey.
     */
    public static class TypeKey implements VariableKey {
        public final TypeName type;
        public TypeKey(final TypeName type) {
            this.type = type;
        }
        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (o instanceof TypeKey) {
                TypeKey other = (TypeKey) o;
                return this.type.equals(other.type);
            } else if (o instanceof WeaklyNamedKey) {
                WeaklyNamedKey other = (WeaklyNamedKey) o;
                return this.type.equals(other.type);
            } else {
                return false;
            }
        }
        @Override
        public int hashCode() {
            return this.type.hashCode();
        }
        @Override
        public String toString() {
            return "<TypeKey type=\"" + this.type  + "\" />";
        }
    }
    /**
     *  This NamedKey also equals to TypeKeys.
     */
    public static class WeaklyNamedKey extends NamedKey {
        public WeaklyNamedKey(final TypeName type, final String name) {
            super(type, name);
        }
        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (o instanceof NamedKey) {
                NamedKey other = (NamedKey) o;
                return (this.type.equals(other.type) && this.name.equals(other.name));
            } else if (o instanceof TypeKey) {
                TypeKey other = (TypeKey) o;
                return (this.type.equals(other.type));
            } else {
                return false;
            }
        }
        @Override
        public int hashCode() {
            return this.type.hashCode() * ((this.name==null)?1:this.name.hashCode());
        }
        @Override
        public String toString() {
            return "<WaklyNamedKey type=\"" + this.type  + "\" name=\"" + this.name + "\" />";
        }
    }
    /**
     *  Identify variables by a string and type.
     */
    public static class NamedKey implements VariableKey {
        public final String name;
        public final TypeName type;
        public NamedKey(final TypeName type, final String name) {
            this.name = name;
            this.type = type;
        }
        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (o instanceof NamedKey) {
                NamedKey other = (NamedKey) o;
                return (this.type.equals(other.type) && this.name.equals(other.name));
            } else {
                return false;
            }
        }
        @Override
        public int hashCode() {
            return this.type.hashCode() * ((this.name==null)?1:this.name.hashCode());
        }
        @Override
        public String toString() {
            return "<NamedKey type=\"" + this.type  + "\" name=\"" + this.name + "\" />";
        }
    }



    /**
     *  Makes a SSAValue with number and type valid in the specified Method.
     *
     *  The number is the one to use with SSAInstructions. 
     *
     *  The MethodReference (validIn) is an optional value. However the TypeSafeInstructionFactory relies on it
     *  to verify its ReturnInstruction so setting it does not hurt.
     *
     *  The variableName is optional and not really used yet. It might be handy for debugging.
     *
     *  @param  number  access the value using this number
     *  @param  validIn optionally assign this value to a method
     *  @throws IllegalArgumentException on negative parameter number
     */
    public SSAValue(final int number, final TypeReference type, final MethodReference validIn, final VariableKey key) {
        if (number < 0) {
            throw new IllegalArgumentException("A SSA-Value can't have a negative number, " + number + "given");
        }
        if (type == null) {
            throw new IllegalArgumentException("The type for the SSA-Variable may not be null");
        }
        if (type.equals(TypeReference.Void)) {
            throw new IllegalArgumentException("You can't create a SSA-Variable of type void");
        }

        this.type = type;
        this.number = number;
        this.key = key;
        this.mRef = validIn;
        this.isAssigned = false;
    }

    /**
     *  Generates a SSAValue with a NamedKey (or TypeKey if name==null).
     */
    public SSAValue(final int number, final TypeReference type, final MethodReference validIn, final String variableName) {
        this(number, type, validIn, ((variableName==null)?new TypeKey(type.getName()):new NamedKey(type.getName(), variableName)));
    }

    /**
     *  Generates a SSAValue with a UniqueKey.
     */
    public SSAValue(final int number, final TypeReference type, final MethodReference validIn) {
        this(number, type, validIn, new UniqueKey());
    }


    /**
     *  Create a new instance of the same type, validity and name.
     *
     *  Of course you still have to assign something to this value.
     *
     *  @param  number      the new number to use
     *  @param  copyFrom    where to get the rest of the attributes
     */
    public SSAValue(final int number, SSAValue copyFrom) {
        this(number, copyFrom.type, copyFrom.mRef, copyFrom.key);
    }

    /**
     *  The SSA-Value to use with SSAInstructions.
     *
     *  As an alternative one can generate Instructions using the TypeSafeInstructionFactory which takes
     *  SSAValues as parameters.
     */
    public int getNumber() {
        return this.number;
    }

    /**
     *  The type this SSA-Value represents.
     */
    public TypeReference getType() {
        return this.type;
    }

    /**
     *  If setAssigned() was called on this variable.
     */
    public boolean isAssigned() {
        return this.isAssigned;
    }

    /**
     *  Mark this variable as assigned.
     *
     *  Sets the value returned by isAssigned() to true. As a safety measure one can only call this method
     *  once on a SSAValue, the second time raises an exception.
     *
     *  The TypeSafeInstructionFactory calls this method when writing to an SSAValue. It does however not check 
     *  the setting when reading from an SSAValue.
     *
     *  This does obviously not prevent from generating a new SSAValue with the same number and double-assign
     *  anyhow.
     *
     *  @throws IllegalStateException   if the variable was already assigned to
     */
    public void setAssigned() {
        if (this.isAssigned) {
            throw new IllegalStateException("The SSA-Variable " + this + " was assigned to twice.");
        }

        this.isAssigned = true;
    }

    /**
     *  Return the MethodReference this Variable was set valid in.
     *
     *  The value returned by this method is the one set in the constructor. As this parameter is optional to
     *  it this function may return null if it was not set.
     *
     *  @return the argument validIn to the constructor
     */
    public MethodReference getValidIn() {
        return this.mRef;
    }
    /**
     *  Return the optional variable name.
     *
     *  @return the argument variableName to the constructor
     */
    public String getVariableName() {
        if (this.key instanceof NamedKey) {
            return ((NamedKey)this.key).name;
        } else {
            return null;    // TODO: build a name?
        }
    }

    @Override 
    public String toString() {
        return "<SSAValue " + this.number + " type=" + this.type + " validIn=" + this.mRef + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SSAValue) {
            final SSAValue other = (SSAValue) o;
            return ((this.number == other.number) && (this.mRef.equals(other.mRef)) && this.type.equals(other.type));
        }
        throw new IllegalArgumentException("Can't compare SSAValue to " + o.getClass());
    }

    @Override
    public int hashCode() {
        return 157 * this.number * this.mRef.hashCode() * this.type.hashCode();
    }

}
