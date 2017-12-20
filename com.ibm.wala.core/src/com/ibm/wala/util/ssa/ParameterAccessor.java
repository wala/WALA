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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.PrimitiveAssignability;
import com.ibm.wala.util.ssa.SSAValue.WeaklyNamedKey;

/**
 *  Access parameters without confusion on their numbers.
 *
 *  Depending on the representation of a method (IMethod, MethodReference) parameters are placed at a
 *  different position. Functions furthermore may have an implicit this-pointer which alters the 
 *  positions again.
 *
 *  Accessing parameters of these functions by their numbers only is error prone and leads to confusion.
 *  This class tries to leverage parameter-access.
 *
 *  You can use this class using now numbers at all. However if you choose to use numbers this class has yet 
 *  another numbering convention (jupeee): 1 is the first parameter appearing in the Selector, no matter if 
 *  the Method has an implicit this. It is not zero as Java initializes new integer-arrays with zero.
 *
 *  If you want to alter the values of the incoming parameters you may also want to use the ParameterManager
 *  which tracks the changes.
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-10-19
 */
public class ParameterAccessor {

  private final static boolean DEBUG = false;
  
    /**
     *  The Constructor used to create ParameterAccessor influences the parameter-offset.
     *
     *  If this enum is extended many functions will throw if not also extended.
     */
    public enum BasedOn {
            /** ParameterAccessor was constructed using an IMethod */
        IMETHOD,            
            /** ParameterAccessor was constructed using a MethodReference */
        METHOD_REFERENCE
    }

    /**
     *  The kind of parameter.
     *
     *  Extending this enum should not introduce any problems in ParameterAccessor.
     */
    public enum ParamerterDisposition {
            /** Parameter is an implicit this-pointer */
        THIS,               
            /** Parameter is a regular parameter occurring in the Descriptor */
        PARAM,
            /** The return-value of a method (has to be crafted manually) */
        RETURN,
        NEW
    }

    /**
     *  This key is identified by type and parameter number.
     */
    public static class ParameterKey extends WeaklyNamedKey {
        final int paramNo;
        
        public ParameterKey(final TypeName type, final int no, final String name) {
            super(type, ((name==null)?"param_" + no:name));
            this.paramNo = no;
        }
        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }
        @Override
        public int hashCode() {
            return this.type.hashCode();
        }
        @Override
        public String toString() {
            return "<ParameterKey no=" + this.paramNo + " type=" + this.type + " name=\"" + this.name + "\" />";
        }
    }

    /**
     *  The representation of a Parameter handled using a ParameterAccessor.
     *
     *  It basically consists of a SSA-Value and an associated TypeReference.
     *
     *  Use .getNumber() to access the associated SSA-Value.
     *
     *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
     *  @since  2013-10-19
     */
    public static class Parameter extends SSAValue {
        /** Implicit this or regular parameter? */
        private final ParamerterDisposition disp;   
        /** Add to number to get position in descriptor */
        private final int descriptorOffset; 

        /**
         *  Create Parameters using ParameterAccessor.
         *
         *  @param  number  SSA-Value to access this parameter
         *  @param  name    Optional variable-name - may be null
         *  @param  type    Variable Type to this parameter
         *  @param  disp    Implicit this, regular parameter or return value?
         *  @param  basedOn Is Accessor constructed with IMethod or MethodReference
         *  @param  mRef    Method this parameter belongs to
         *  @param  descriptorOffset add to number to get position in descriptor
         */
        protected Parameter(final int number, final String name, final TypeReference type, final ParamerterDisposition disp, 
                final BasedOn basedOn, final MethodReference mRef, final int descriptorOffset) {
            super(number, type, mRef, new ParameterKey(type.getName(), number + descriptorOffset, name));

            if (mRef == null) {
                throw new IllegalArgumentException("MethodReference (mRef) of a Parameter may not be null");
            }
            if (basedOn == null) {
                throw new IllegalArgumentException("Argument basedOn of a Parameter may not be null");
            }
            if (disp == null) {
                throw new IllegalArgumentException("ParamerterDisposition (disp) of a Parameter may not be null");
            }
            // If type was null the call to super failed
            if ((number < 1) && (basedOn == BasedOn.METHOD_REFERENCE)) {
                throw new IllegalArgumentException("The first accessible SSA-Value of a MethodReference is 1 but the " +
                        "Value-Number given is " + number);
            }
            if ((number < 0) && (basedOn == BasedOn.IMETHOD)) {
                throw new IllegalArgumentException("The first accessible SSA-Value of an IMethod is 0 but the " +
                        "Value-Number given is " + number);
            }
            if ((disp == ParamerterDisposition.PARAM) && (number + descriptorOffset > mRef.getNumberOfParameters())) {
                throw new IllegalArgumentException("The SSA-Value " + number + " (with added offset " + descriptorOffset + 
                        ") is beyond the number of Arguments (" + mRef.getNumberOfParameters() + ") of the Method " +
                        mRef.getName() + "\n" + mRef.getSignature());
            }
            if ((disp == ParamerterDisposition.THIS) && (basedOn == BasedOn.METHOD_REFERENCE) && (number != 1)) {
                throw new IllegalArgumentException("The implicit this-pointer of a MethodReference is located at SSA-Value 1. " +
                        "The SSA-Value given is " + number);
            }
            if ((disp == ParamerterDisposition.THIS) && (basedOn == BasedOn.IMETHOD) && (number != 0)) {
                throw new IllegalArgumentException("The implicit this-pointer of an IMethod is located at SSA-Value 0. " +
                        "The SSA-Value given is " + number);
            }
            if ((descriptorOffset < -2) || (descriptorOffset > 1)) {
                throw new IllegalArgumentException("The descriptor-offset given is not within its expected bounds: " +
                        "-1 (for a method without implicit this-pointer) to 1. The given offset is " + descriptorOffset);
            }

            this.disp = disp;
            this.descriptorOffset = descriptorOffset;
            super.isAssigned();
        }

        /**
         *  The position of the parameter in the methods Desciptor starting with 1.
         */
        public int getNumberInDescriptor() {    // TODO: Verify all descriptorOffset stuff!
            //if (this.descriptorOffset < 0) {
            //    return this.number;
            //} else {
                return this.number + this.descriptorOffset;
            //}
        }

        public ParamerterDisposition getDisposition() {
            return this.disp;   
        }

        /**
         *  @throws IllegalArgumentException if you compare this to an object totally different.
         */
        @Override
        public boolean equals(final Object o) {
            if (o instanceof Parameter) {
                final Parameter other = (Parameter) o;
                return ((this.type.equals(other.type)) && (this.number == other.number) && (this.mRef.equals(other.mRef)));
            } 
            if (o instanceof SSAValue) {
                return super.equals(o);
            }
            throw new IllegalArgumentException("Can't compare Parameter to " + o.getClass());
        }

        /**
         *  Clashes deliberately with SSAValue as it's basically the same thing.
         */
        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        @Override
        public String toString() {
            switch (this.disp) {
                case THIS:
                    return "Implicit this-parameter of " + this.mRef.getName() + " as " + this.type + " accessible using " +
                        "SSA-Value " + this.number;
                case PARAM:
                    if (this.key instanceof NamedKey) {
                        return "Parameter " + getNumberInDescriptor() + " \"" + getVariableName() + "\" of " + this.mRef.getName() +
                            " is " + this.type + " accessible using SSA-Value " + this.number;
                    } else {
                         return "Parameter " + getNumberInDescriptor() + " of " + this.mRef.getName() + " is " + this.type + 
                             " accessible using SSA-Value " + this.number;
                    }
                case RETURN:
                    return "Return Value of " + this.mRef.getName() + " as " + this.type + " accessible using SSA-Value " + 
                        this.number;
                case NEW:
                    return "New instance of " + this.type + " accessible in " + this.mRef.getName() + " using number " + this.number;
                default:
                    return "Parameter " + getNumberInDescriptor() + " - " + this.disp + " of " + this.mRef.getName() + " as " +
                        this.type + " accessible using SSA-Value " + this.number;
            }
        }
    }

    /** The Constructor used to create this ParameterAceesor */
    private final BasedOn base;
    /** The Method associated to this ParameterAceesor if constructed using a mRef */
    private final MethodReference mRef;
    /** The Method associated to this ParameterAceesor if constructed using an IMethod */
    private final IMethod method;           
    /** The Value-Number for the implicit this-pointer or -1 if there is none */
    private final int implicitThis;         
     /** SSA-Number + descriptorOffset yield the parameters position in the Descriptor starting with 1 */
    private final int descriptorOffset;
    /** Number of parameters _excluding_ implicit this */
    private final int numberOfParameters;

    /**
     *  Reads the parameters of a MethodReference CAUTION:.
     *
     *  Do _not_ use ParameterAceesor(IMethod.getReference()), but ParameterAceesor(IMehod)!
     *
     *  Using this Constructor influences the SSA-Values returned later. The cha is needed to
     *  determine whether mRef is static. If this is already known one should prefer the faster
     *  {@link #ParameterAccessor(MethodReference, boolean)}.
     *
     *  @param  mRef    The method to read the parameters from.
     */
    public ParameterAccessor(final MethodReference mRef, final IClassHierarchy cha) {
        if (mRef == null) {
            throw new IllegalArgumentException("Can't read the arguments from null.");
        }

        this.mRef = mRef;
        this.method = null;
        this.base = BasedOn.METHOD_REFERENCE;
        this.numberOfParameters = mRef.getNumberOfParameters();

        final boolean hasImplicitThis;
        Set<IMethod> targets = cha.getPossibleTargets(mRef);
        if (targets.size() < 1) {
            warn("Unable to look up the method {} starting extensive search...", mRef);

            targets = new HashSet<>();
            final TypeReference mClass = mRef.getDeclaringClass();
            final Selector mSel = mRef.getSelector();
            final Set<IClass> testClasses = new HashSet<>();

            // Look up all classes matching exactly
            for (IClassLoader loader : cha.getLoaders()) {
                final IClass cand = loader.lookupClass(mClass.getName());
                
                if (cand != null) {
                    testClasses.add(cand);
                }
            }

            // Try lookupClass..
            final IClass lookedUp;
            lookedUp = cha.lookupClass(mClass);
            if (lookedUp != null) {
                debug("Found using cha.lookupClass()");
                testClasses.add(lookedUp);
            }

            info("Searching the classes {} for the method", testClasses);

            for (IClass testClass : testClasses) {
                final IMethod cand = testClass.getMethod(mSel);

                if (cand != null) {
                    targets.add(cand);
                }
            }

            if (targets.size() < 1) {
                warn("Still no candidates for the method - continuing with super-classes (TODO)");

                // TODO
               
                { // DEBUG
                    for (IClass testClass : testClasses) {
                        info("Known Methods in " + testClass);
                        for (IMethod contained : testClass.getAllMethods()) {
                            System.out.println(contained);
                            info("\t" + contained);
                        }
                    }
                } // */
                
                throw new IllegalStateException("Unable to look up the method " + mRef);
            }
        }

        { // Iterate all candidates
            final Iterator<IMethod> it = targets.iterator();
            final boolean testStatic = it.next().isStatic();
            while (it.hasNext()) {
                final boolean tmpStatic = it.next().isStatic();
                if (testStatic != tmpStatic) {
                    throw new IllegalStateException("The ClassHierarchy knows multiple (" + targets.size() + ") targets for " + mRef +
                            ". The targets contradict themselves if they have an implicit this!");
                }
            }
            hasImplicitThis = (! testStatic);
        }

        if (hasImplicitThis) {
            info("The method {} has an implicit this pointer", mRef);
            this.implicitThis = 1;
            this.descriptorOffset = -1;  
        } else {
            info("The method {} has no implicit this pointer", mRef);
            this.implicitThis = -1;
            this.descriptorOffset = 0;
        }
    }

    /**
     *  Reads the parameters of a MethodReference CAUTION:.
     *
     *  Do _not_ use ParameterAceesor(IMethod.getReference()), but ParameterAceesor(IMehod)!
     *
     *  This constructor is faster than {@link #ParameterAccessor(MethodReference, IClassHierarchy)}.
     *
     *  @param  mRef    The method to read the parameters from.
     */
    public ParameterAccessor(final MethodReference mRef, final boolean hasImplicitThis) {
        if (mRef == null) {
            throw new IllegalArgumentException("Can't read the arguments from null.");
        }

        this.mRef = mRef;
        this.method = null;
        this.base = BasedOn.METHOD_REFERENCE;
        this.numberOfParameters = mRef.getNumberOfParameters();

        if (hasImplicitThis) {
            info("The method {} has an implicit this pointer", mRef);
            this.implicitThis = 1;
            this.descriptorOffset = -1;  
        } else {
            info("The method {} has no implicit this pointer", mRef);
            this.implicitThis = -1;
            this.descriptorOffset = 0;
        }
    }

    /**
     *  Read the parameters from an IMethod.
     *
     *  Using this Constructor influences the SSA-Values returned later.
     *
     *  @param  method  The method to read the parameters from.
     */
    public ParameterAccessor(final IMethod method) {   
        if (method == null) {
            throw new IllegalArgumentException("Can't read the arguments from null.");
        }

        // Don't make a mRef but keep the IMethod!
        this.mRef = null;           
        this.method = method;
        this.base = BasedOn.IMETHOD;
        this.numberOfParameters = method.getReference().getNumberOfParameters(); 

        if (method.isStatic() && (! method.isInit())) {
            assert(method.getNumberOfParameters() == method.getReference().getNumberOfParameters()) : "WTF!" + method;
            this.implicitThis = -1;
            this.descriptorOffset = 0;
        } else {
            assert(method.getNumberOfParameters() == 1 + method.getReference().getNumberOfParameters()) : "WTF!" + method;
            this.implicitThis = 1;
            this.descriptorOffset = -1;
        }
    }

    /**
     *  Make an Parameter Object using a Descriptor-based numbering (starting with 1).
     *
     *  Number 1 is the first parameter in the methods Selector. No matter if the function has an
     *  implicit this pointer.
     * 
     *  If the Function has an implicit this-pointer you can access it using getThis().
     *
     *  @param  no  the number in the Selector
     *  @return new Parameter-Object for no
     *  @throws IllegalArgumentException if the parameter is zero
     *  @throws ArrayIndexOutOfBoundsException if no is not within bounds [1 to numberOfParameters]
     */
    public Parameter getParameter(final int no) {
        // no is checked by getParameterNo(int)
        final int newNo = getParameterNo(no);
    
        switch (this.base) {
            case IMETHOD:   // TODO: Try reading parameter name
                return new Parameter(newNo, null, this.method.getParameterType(no), ParamerterDisposition.PARAM, 
                        this.base, this.method.getReference(), this.descriptorOffset);
            case METHOD_REFERENCE:
                return new Parameter(newNo, null, this.mRef.getParameterType(no - 1), ParamerterDisposition.PARAM, 
                        this.base, this.mRef, this.descriptorOffset);
            default:
                throw new UnsupportedOperationException("No implementation of getParameter() for base " + this.base);
        }
    }

    /**
     *  Return the SSA-Value to access a parameter using a Descriptor-based numbering (starting with 1).
     *
     *  Number 1 is the first parameter in the methods Selector. No matter if the function has an
     *  implicit this pointer.
     * 
     *  If the Function has an implicit this-pointer you can acess it using getThisNo().
     *
     *  @param  no  the number in the Selector
     *  @return the offseted number for accessing the parameter
     *  @throws IllegalArgumentException if the parameter is zero
     *  @throws ArrayIndexOutOfBoundsException if no is not within bounds [1 to numberOfParameters]
     */
    public int getParameterNo(final int no) {
        if (no == 0) {
            throw new IllegalArgumentException("Parameter numbers start with 1. Use getThis() to access a potential implicit this.");
        }
        if ((no < 0) || (no > this.numberOfParameters)) {
            throw new ArrayIndexOutOfBoundsException("The given number (" + no + ") was not within bounds (1 to " + this.numberOfParameters +
                    ") when acessing a parameter of " + this);
        }

        switch (this.base) {
            case IMETHOD:
                return no + this.implicitThis; // + this.implicitThis; // TODO: Verify
            case METHOD_REFERENCE:
                if (this.implicitThis > 0) {
                    return no + this.implicitThis; // 
                } else {
                    return no;
                }
            default:
                throw new UnsupportedOperationException("No implementation of getParameter() for base " + this.base);
        }
    }

    /**
     *  Same as Parameter.getNumber().
     *
     *  @return SSA-Value to access the parameters contents.
     */
    public int getParameterNo(final Parameter param) {
        if (param == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }

        return param.getNumber();
    }

    /**
     *  This list _excludes_ the implicit this-pointer (if any).
     *
     *  If you want the implicit this-pointer use getThis().
     *
     *  @return All parameters appearing in the Selector.
     */
    public List<Parameter> all() {
        // TODO: Cache!
        List<Parameter> all = new ArrayList<>(this.getNumberOfParameters());

        if (this.getNumberOfParameters() == 0) {
            return all;
        } else {
            switch (this.base) {
                case IMETHOD:
                    {
                        // final int firstInSelector = firstInSelector();
                        for (int i = ((hasImplicitThis())?1:0); i < this.method.getNumberOfParameters(); ++i) {   
                            debug("all() adding: Parameter({}, {}, {}, {}, {})", (i + 1), this.method.getParameterType(i),
                                    this.base,  this.method, this.descriptorOffset);
                            all.add(new Parameter(i + 1, null, this.method.getParameterType(i), ParamerterDisposition.PARAM, 
                                    this.base, this.method.getReference(), this.descriptorOffset));
                        }
                    }
                    break;
                case METHOD_REFERENCE:
                    {
                        final int firstInSelector = firstInSelector();
                        for (int i = 0 /*firstInSelector()*/; i < this.numberOfParameters; ++i) {       // TODO:
                            all.add(new Parameter(i + firstInSelector, null, this.mRef.getParameterType(i), ParamerterDisposition.PARAM, 
                                    this.base, this.mRef, this.descriptorOffset));
                        }
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("No implementation of all() for base " + this.base);
            }
        }

        return all;
    }

    /**
     *  Return the implicit this-pointer (or throw).
     *
     *  This obviously only works on non-static methods. You probably want to check if the method has
     *  such an implicit this using hasImplicitThis() as this method will throw if there is none.
     *
     *  If you only want the number use the more lightweight getThisNo().
     *
     *  @return Object containing all Information on the parameter.
     *  @throws IllegalStateException if the function has no implicit this
     */
    public Parameter getThis() {
        final int self = getThisNo();
        final TypeReference selfType;
        switch (this.base) {
            case IMETHOD:
                selfType = this.method.getParameterType(self);
                break;
            case METHOD_REFERENCE:
                selfType = this.mRef.getDeclaringClass();
                break;
            default:
                throw new UnsupportedOperationException("No implementation of getThis() for base " + this.base);
        }
        return getThisAs(selfType);
    }

    /**
     *  Return the implicit this-pointer as a supertype.
     *
     *  @param  asType  A type of a super-class of this
     */
    public Parameter getThisAs(final TypeReference asType) {
        final int self = getThisNo();

        switch (this.base) {
            case IMETHOD:
                final IClassHierarchy cha = this.method.getClassHierarchy();
                try {
                    if (! isSubclassOf(this.method.getParameterType(self), asType, cha) ) {
                        throw new IllegalArgumentException("Class " + asType + " is not a super-class of " +
                                this.method.getParameterType(self));
                    }
                } catch (ClassLookupException e) {
                    // Cant't test assume all fitts
                }

                return new Parameter(self, "self", asType, ParamerterDisposition.THIS, 
                        this.base, this.method.getReference(), this.descriptorOffset);
            case METHOD_REFERENCE:
                // TODO assert asType is a subtype of self.type - we need cha to do that :(
                return new Parameter(self, "self", asType, ParamerterDisposition.THIS,
                        this.base, this.mRef, this.descriptorOffset);
            default:
                throw new UnsupportedOperationException("No implementation of getThis() for base " + this.base);
        }
    }

    /**
     *  Return the SSA-Value of the implicit this-pointer (or throw).
     *
     *  This obviously only works on non-static methods. You probably want to check if the method has
     *  such an implicit this using hasImplicitThis() as this method will throw if there is none.
     *
     *  @return Number of the this.
     *  @throws IllegalStateException if the function has no implicit this.
     */
    public int getThisNo() {
        if (this.implicitThis >= 0) {
            return this.implicitThis;
        } else {
            throw new IllegalStateException("getThisNo called for a method that has no implicit this");
        }
    }

    /**
     *  If the method has an implicit this parameter.
     */
    public boolean hasImplicitThis() {
        return (this.implicitThis >= 0);
    }

    /**
     *  Create a "Parameter" containing the Return-Type w/o Type-checking.
     *
     *  This should be of rather theoretical use.
     *
     *  @throws IllegalStateException if used on a void-Function
     */
    public Parameter makeReturn(final int ssa) {
        if (! hasReturn()) {
            throw new IllegalStateException("Can't generate a return-value for a void-function.");
        }

        switch (this.base) {
            case IMETHOD:
                return new Parameter(ssa, "retVal", getReturnType(), ParamerterDisposition.RETURN, this.base,
                        this.method.getReference(), this.descriptorOffset);
            case METHOD_REFERENCE:
                return new Parameter(ssa, "retVal", getReturnType(), ParamerterDisposition.RETURN, this.base,
                        this.mRef, this.descriptorOffset);
            default:
                throw new UnsupportedOperationException("No implementation of getReturn() for base " + this.base);
        }
    }

    /**
     *  Create a "Parameter" containing the Return-Type with Type-checking.
     *  
     *  @param  ssa     The value to return
     *  @param  type    The type of ssa
     *  @param  cha     The ClassHierarchy to use for the assignability test
     *  @throws IllegalStateException if used on a void-Function
     */
    public Parameter makeReturn(final int ssa, final TypeReference type, final IClassHierarchy cha) {
        if (! hasReturn()) {
            throw new IllegalStateException("Can't generate a return-value for a void-function.");
        }

        final TypeReference returnType = getReturnType();

        if (returnType.equals(type)) {
            return makeReturn(ssa);
        } else if (cha == null) {
            throw new IllegalArgumentException("Needed to test assignability but no cha given.");  // TODO: Throw always or never
        } else if (isAssignable(type, returnType, cha)) {
            return makeReturn(ssa);
        } else {
            throw new IllegalStateException("Return type " + returnType + " is not assignable from " + type);
        }
    }

    /**
     *  The SSA-Value to acces the parameter appearing first in the Descriptor with.
     *
     *  @throws IllegalArgumentException if the method has no parameters in its Descriptor.
     */
    public int firstInSelector() {
        if (this.numberOfParameters == 0) {
            if (this.method != null) {
                throw new IllegalArgumentException("The method " + this.method.toString() + " has no explicit parameters.");
            } else {
                throw new IllegalArgumentException("The method " + this.mRef.toString() + " has no explicit parameters.");
            }
        }
        if (this.implicitThis > 1) {
            throw new IllegalStateException("An internal error in ParameterAccessor locating the implicit this pointer occurred! Invalid: " + this.implicitThis);
        }

        switch (this.base) {
            case IMETHOD:
                if (this.hasImplicitThis()) {   // XXX TODO BUG!
                    debug("This IMethod {} has an implicit this pointer at {}, so firstInSelector is accessible using SSA-Value {}", this.method, this.implicitThis, (this.implicitThis  + 1));
                    return this.implicitThis + 1;
                } else {
                    debug("This IMethod {} has no implicit this pointer, so firstInSelector is accessible using SSA-Value 1" , this.method);
                    return 1;
                }
            case METHOD_REFERENCE:
                if (this.hasImplicitThis()) {
                    debug("This IMethod {} has an implicit this pointer at {}, so firstInSelector is accessible using SSA-Value {}", this.mRef, this.implicitThis, (this.implicitThis  + 1));
                    return this.implicitThis + 1;
                } else {
                    debug("This mRef {} has no implicit this pointer, so firstInSelector is accessible using SSA-Value 1", this.mRef);
                    return 1;
                }
            default:
                throw new UnsupportedOperationException("No implementation of firstInSelector() for base " + this.base);
        }
    }

    /**
     *  Prefer: getParameter(int no) or all().
     *
     *  Get the type of the parameter (not this) using a fixed numbering.
     *
     *  Number 1 is the first parameter in the methods Selector. No matter if the function has an
     *  implicit this pointer.
     *
     *  Use all() if you want to get all parameter-types.
     *
     *  @param  no  the number in the Selector
     *  @return the type of the parameter
     */
    public TypeReference getParameterType(final int no) {   // XXX Remove?
        switch (this.base) {
            case IMETHOD:
                return this.method.getParameterType(getParameterNo(no));
            case METHOD_REFERENCE:
                return this.method.getParameterType(getParameterNo(no));
            default:
                throw new UnsupportedOperationException("No implementation of getParameterType() for base " + this.base);
        }
    }

    /**
     *  First parameter in the selector that matches _exactly_.
     *
     *  @return first parameter found or null if there is none
     *  @throws IllegalArgumentException if searching for void or null
     */
    public Parameter firstOf(final TypeName tName) {
        if (tName == null) {
            throw new IllegalArgumentException("Search-name may not be null");
        }
        if (tName.equals(TypeReference.VoidName)) {
            throw new IllegalArgumentException("You are searching for 'void' as a parameter.");
        }

        final List<Parameter> all = all();

        // ****
        // Implementation starts here

        for (final Parameter cand : all) {
            if (cand.getType().getName().equals(tName)) {
                return cand;
            }
        }

        return null;
    }

    /**
     *  First parameter in the selector that matches _exactly_.
     *
     *  @return first parameter found or null if there is none
     *  @throws IllegalArgumentException if searching for void or null
     */
    public Parameter firstOf(final TypeReference tRef) {
        if (tRef == null) {
            throw new IllegalArgumentException("Search-name may not be null");
        }
        if (tRef.equals(TypeReference.Void)) {
            throw new IllegalArgumentException("You are searching for 'void' as a parameter.");
        }

        final List<Parameter> all = all();

        // ****
        // Implementation starts here

        for (final Parameter cand : all) {
            if (cand.getType().equals(tRef)) {
                return cand;
            }
        }

        return null;
    }

    /**
     *  All parameters in the selector that are a subclass of tName (slow).  
     *
     *  TypeNames have to be looked up first, do prefer the variant with the TypeReference if
     *  one is available.
     *
     *  @throws IllegalArgumentException if searching for void or null
     */
    public List<Parameter> allExtend(final TypeName tName, final IClassHierarchy cha) {
        if (tName == null) {
            throw new IllegalArgumentException("Search-name may not be null");
        }
        if (tName.equals(TypeReference.VoidName)) {
            throw new IllegalArgumentException("You are searching for 'void' as a parameter.");
        }
        if (cha == null) {
            throw new IllegalArgumentException("Can't search ClassHierarchy without having a ClassHierarchy (is null)");
        }

        final List<Parameter> all = all();
        final List<Parameter> allExctends = new ArrayList<>();
        IClass searchType = null;
        final IClassLoader[] allLoaders = cha.getLoaders();

        // ****
        // Implementation starts here

        { // Retrieve a reference of the type
            for (final IClassLoader loader : allLoaders) {
                searchType = loader.lookupClass(tName);
                if (searchType != null) {
                    break;
                }
            }
        }

        if (searchType == null) {
            throw new IllegalStateException("Could not find " + tName + " in any loader!");
        } else {
            debug("Retrieved {} as {}", tName, searchType);
        }

        for (final Parameter cand : all) {
            final IClass candClass = cha.lookupClass(cand.getType());

            if (candClass != null) {                        // TODO: Extra function
                if (cha.isSubclassOf(candClass, searchType)) {
                    allExctends.add(cand);
                }
            } else {
                for (final IClassLoader loader: cha.getLoaders()) {
                    final IClass c = loader.lookupClass(cand.getType().getName());
                    if (c != null) {
                        info("Using alternative for from: {}", cand);
                        if (cha.isSubclassOf(c, searchType)) {
                            allExctends.add(cand);
                        }    
                    }
                }

                // TODO: That's true for base-type too
                warn("Unable to look up IClass of {}", cand);
            }
        }

        return allExctends;
    }

    /**
     *  All parameters in the selector that are a subclass of tRef (slow).  
     *
     *  @throws IllegalArgumentException if searching for void or null
     */
    public List<Parameter> allExtend(final TypeReference tRef, final IClassHierarchy cha) {
        if (tRef == null) {
            throw new IllegalArgumentException("Search TypeReference may not be null");
        }
        if (tRef.equals(TypeReference.Void)) {
            throw new IllegalArgumentException("You are searching for 'void' as a parameter.");
        }
        if (cha == null) {
            throw new IllegalArgumentException("Can't search ClassHierarchy without having a ClassHierarchy (is null)");
        }

        // ****
        // Implementation starts here

        final IClass searchType = cha.lookupClass(tRef);
        final List<Parameter> all = all();
        final List<Parameter> allExctends = new ArrayList<>();
            
        if (searchType == null) {
            throw new IllegalStateException("Could not find the IClass of " + tRef);
        } else {
            debug("Reteived {} as {}", tRef, searchType);
        }

        for (final Parameter cand : all) {
            final IClass candClass = cha.lookupClass(cand.getType());

            if (candClass != null) {
                if (cha.isSubclassOf(candClass, searchType)) {
                    allExctends.add(cand);
                }
            } else {
                // TODO: That's true for base-type too
                warn("Unable to look up IClass of {}", cand);
            }
        }

        return allExctends;
    }


    /**
     *  First parameter in the selector that is a subclass of tName (slow).  
     *
     *  TypeNames have to be lloked up first, do prefer the variant with the TypeReference if
     *  one is available.
     *
     *  @return first parameter found or null if there is none
     *  @throws IllegalArgumentException if searching for void or null
     */
    public Parameter firstExtends(final TypeName tName, final IClassHierarchy cha) {
        if (tName == null) {
            throw new IllegalArgumentException("Search-name may not be null");
        }
        if (tName.equals(TypeReference.VoidName)) {
            throw new IllegalArgumentException("You are searching for 'void' as a parameter.");
        }
        if (cha == null) {
            throw new IllegalArgumentException("Can't search ClassHierarchy without having a ClassHierarchy (is null)");
        }

        final List<Parameter> all = all();
        IClass searchType = null;
        final IClassLoader[] allLoaders = cha.getLoaders();

        // ****
        // Implementation starts here

        { // Reteive a reference of the type
            for (final IClassLoader loader : allLoaders) {
                searchType = loader.lookupClass(tName);
                if (searchType != null) {
                    break;
                }
            }
        }

        if (searchType == null) {
            throw new IllegalStateException("Could not find " + tName + " in any loader!");
        } else {
            debug("Reteived {} as {}", tName, searchType);
        }

        for (final Parameter cand : all) {
            final IClass candClass = cha.lookupClass(cand.getType());

            if (candClass != null) {
                if (cha.isSubclassOf(candClass, searchType)) {
                    return cand;
                }
            } else {
                for (final IClassLoader loader: cha.getLoaders()) {
                    final IClass c = loader.lookupClass(cand.getType().getName());
                    if (c != null) {
                        info("Using alternative for from: {}", cand);
                        if (cha.isSubclassOf(c, searchType)) {
                            return cand;
                        }    
                    }
                }

                // TODO: That's true for primitive-type too
                warn("Unable to look up IClass of {}", cand);
            }
        }

        return null;
    }

    /**
     *  First parameter in the selector that is a subclass of tRef (slow).  
     *
     *  @return first parameter found or null if there is none
     *  @throws IllegalArgumentException if searching for void or null
     */
    public Parameter firstExtends(final TypeReference tRef, final IClassHierarchy cha) {
        if (tRef == null) {
            throw new IllegalArgumentException("Search TypeReference may not be null");
        }
        if (tRef.equals(TypeReference.Void)) {
            throw new IllegalArgumentException("You are searching for 'void' as a parameter.");
        }
        if (cha == null) {
            throw new IllegalArgumentException("Can't search ClassHierarchy without having a ClassHierarchy (is null)");
        }

        // ****
        // Implementation starts here

        final IClass searchType = cha.lookupClass(tRef);
        final List<Parameter> all = all();

        if (searchType == null) {
            throw new IllegalStateException("Could not find the IClass of " + tRef);
        } else {
            debug("Reteived {} as {}", tRef, searchType);
        }

        for (final Parameter cand : all) {
            final IClass candClass = cha.lookupClass(cand.getType());

            if (candClass != null) {
                if (cha.isSubclassOf(candClass, searchType)) {
                    return cand;
                }
            } else {
                // TODO: That's true for base-type too
                warn("Unable to look up IClass of {}", cand);
            }
        }

        return null;
    }

    /**
     *  The first SSA-Number after the parameters.
     *
     *  This is useful for making synthetic methods.
     */
    public int getFirstAfter() {
        return this.numberOfParameters + 2; // Should be +1 ?
    }

    /**
     *  Generate the params-param for an InvokeIstruction w/o type checking.
     *
     *  @param  args    list to build the arguments from - without implicit this
     */
    public int[] forInvokeStatic(final List<? extends SSAValue> args) {
        if (args == null) { // XXX Allow?
            throw new IllegalArgumentException("args is null");
        }
        
        int[] params =  new int[args.size()];
        if (params.length == 0) {
            return params;
        }

        if ((args.get(1) instanceof Parameter) && (((Parameter)args.get(1)).getDisposition() == ParamerterDisposition.THIS)) {
            warn("The first argument is an implicit this: {} this may be ok however.", args.get(1));
        }
 
        // ****
        // Implementation starts here
        
        for (int i = 0; i < params.length; ++i) {
            params[i] = args.get(i).getNumber();
        }

        return params;
    }

    /**
     *  Generate the params-param for an InvokeIstruction with type checking.
     *
     *  @param  args    list to build the arguments from - without implicit this
     *  @param  target  the method to be called - for type checking only
     *  @param  cha     if types don't match exactly needed for the assignability check (may be null if that check is not wanted)
     *  @throws IllegalArgumentException if you call this method on a target that needs an implicit this
     *  @throws IllegalArgumentException if args length does not match the targets param-length
     *  @throws IllegalArgumentException if a parameter is unassignable
     */
    public int[] forInvokeStatic(final List<? extends SSAValue> args, final ParameterAccessor target, final IClassHierarchy cha) {
        if (args == null) {
            throw new IllegalArgumentException("args is null");
        }
        if (target == null) {
            throw new IllegalArgumentException("ParameterAccessor for the target is null");
        }
        if (target.hasImplicitThis()) {
            throw new IllegalArgumentException("You used forInvokeStatic on a method that has an implicit this pointer");
        }
        if (target.getNumberOfParameters() != args.size()) {
            throw new IllegalArgumentException("Number of arguments mismatch: " + args.size() + " given on a method that " +
                    "needs " + target.getNumberOfParameters() + " arguments. Arguments given were " + args + " for a static " +
                    "call to " + target);
        }

        int[] params =  new int[args.size()];
        if (params.length == 0) {
            return params;
        }

        if ((args.get(1) instanceof Parameter) && (((Parameter)args.get(1)).getDisposition() == ParamerterDisposition.THIS)) {
            warn("The first argument is an implicit this: {} this may be ok however.", args.get(1));
        }

        // ****
        // Implementation starts here

        for (int i = 0; i < params.length; ++i) {
            final SSAValue param = args.get(i);
            if (param.getType().equals(target.getParameter(i).getType())) {
                params[i] = param.getNumber();
            } else { 
                if (cha == null) {
                    throw new IllegalArgumentException("Parameter " + i + " (" + param + ") of the Arguments list " +
                            "is not equal to param " +  i + " ( " + target.getParameter(i) + ") of " + target + 
                            "and no ClassHierarchy was given to test assignability");
                } else if (isAssignable(param, target.getParameter(i), cha)) {
                    params[i] = param.getNumber();
                } else {
                    throw new IllegalArgumentException("Parameter " + i + " (" + param + ") of the Arguments list " +
                            "is not assignable to param " + i + " ( " + target.getParameter(i) + ") of " +
                            target);
                }
            }
        }

        return params;
    }

    /**
     *  Generate the params-param for an InvokeIstruction w/o type checking.
     *
     *  @param  self    the this-pointer to use
     *  @param  args    the rest of the arguments. Be shure it does not start with a 
     *      this pointer. This is _not_ checked so you can use a this-pointer as 
     *      an argument. However a warning is issued.
     *  @throws IllegalArgumentException if the value of self is to small in the current
     *      method
     */
    public int[] forInvokeVirtual(final int self, final List<? extends SSAValue> args) {
        if (args == null) {
            throw new IllegalArgumentException("args is null");
        }
        if ((this.base == BasedOn.METHOD_REFERENCE) && (self < 1)) {
            throw new IllegalArgumentException("The first SSA-Value of a MethodReference is 1. The given this (self) is " +
                    self);
        } else if (self < 0) {
            throw new IllegalArgumentException("self = " + self + " < 0");
        }
       
        int[] params =  new int[args.size() + 1];
        if ((params.length > 1) && (args.get(1) instanceof Parameter) && (((Parameter)args.get(1)).getDisposition() == 
                ParamerterDisposition.THIS)) {
            warn("The first argument is an implicit this: {} this may be ok however.", args.get(1));
        }
 
        // ****
        // Implementation starts here
        //

        params[0] = self;
        for (int i = 1; i < params.length; ++i) {
            params[i] = args.get(i - 1).getNumber();
        }

        return params;
    }

    /**
     *  Generate the params-param for an InvokeIstruction with type checking.
     *
     *  @param  self    the this-pointer to use
     *  @param  args    list to build the arguments from - without implicit this
     *  @param  target  the method to be called - for type checking only
     *  @param  cha     if types don't match exactly needed for the assignability check (may be null if that check is not wanted)
     *  @throws IllegalArgumentException if you call this method on a target that needs an implicit this
     *  @throws IllegalArgumentException if args length does not match the targets param-length
     *  @throws IllegalArgumentException if a parameter is unassignable
     */
    public int[] forInvokeVirtual(final int self, final List<? extends SSAValue> args, final ParameterAccessor target, final IClassHierarchy cha) {
        if (args == null) {
            throw new IllegalArgumentException("args is null");
        }
        if ((this.base == BasedOn.METHOD_REFERENCE) && (self < 1)) {
            throw new IllegalArgumentException("The first SSA-Value of a MethodReference is 1. The given this (self) is " +
                    self);
        } else if (self < 0) {
            throw new IllegalArgumentException("self = " + self + " < 0");
        }
        if (target == null) {
            throw new IllegalArgumentException("ParameterAccessor for the target is null");
        }
        if (! target.hasImplicitThis()) {
            throw new IllegalArgumentException("You used forInvokeVirtual on a method that has no implicit this pointer");
        }
        if (target.getNumberOfParameters() != args.size() + 1) {    // TODO: Verify
            throw new IllegalArgumentException("Number of arguments mismatch: " + args.size() + " given on a method that " +
                    "needs " + target.getNumberOfParameters() + " arguments. Arguments given were " + args + " for a static " +
                    "call to " + target);
        }

        int[] params =  new int[args.size() + 1];
        if ((params.length > 1) && (args.get(1) instanceof Parameter) && (((Parameter)args.get(1)).getDisposition() == 
                ParamerterDisposition.THIS)) {
            warn("The first argument is an implicit this: {} this may be ok however.",  args.get(1));
        }

        // ****
        // Implementation starts here

        params[0] = self;   // TODO: Can't typecheck this!
        for (int i = 1; i < params.length; ++i) {
            final SSAValue param = args.get(i - 1);
            if (param.getType().equals(target.getParameter(i).getType())) {
                params[i] = param.getNumber();
            } else { 
                if (cha == null) {
                    throw new IllegalArgumentException("Parameter " + i + " (" + param + ") of the Arguments list " +
                            "is not equal to param " +  i + " ( " + target.getParameter(i) + ") of " + target + 
                            "and no ClassHierarchy was given to test assignability");
                } else if (isAssignable(param, target.getParameter(i), cha)) {
                    params[i] = param.getNumber();
                } else {
                    throw new IllegalArgumentException("Parameter " + i + " (" + param + ") of the Arguments list " +
                            "is not assignable to param " + i + " ( " + target.getParameter(i) + ") of " +
                            target);
                }
            }
        }

        return params;
    }

    /**
     *  Connects though parameters from the calling function (overridable) - CAUTION:.
     *
     *  This functions makes is decisions based on Type-Referes only so if a TypeReference occurs multiple
     *  times in the caller or callee it may make surprising connections.
     *
     *  The List of Parameters is generated based on the overrides, than parameters in 'this' are searched, finally we'll
     *  fall back to defaults. A "perfect match" is searched.
     *
     *  If a parameter was not assigned yet these three sources are considdered again but cha.isAssignableFrom is used.
     *
     *  If the parameter was still not found a value of 'null' is used.
     *
     *  This funktion is useful when generating wrapper-functions.
     *
     *  @param  callee      The function to generate the parameter-list for
     *  @param  overrides   If a parameter occurs here, it is preferred over the ones present in this
     *  @param  defaults    If a parameter is not present in this or the overrides, defaults are searched. If the parameter is not present there null is assigned.
     *  @param  cha         Optional class hierarchy for testing assignability
     *  @return the parameter-list for the call of toMethod
     */
    @SuppressWarnings("unchecked")  // TODO: Can we do this for overrides and defaults only?
    public List<SSAValue> connectThrough(final ParameterAccessor callee, Set<? extends SSAValue> overrides, Set<? extends SSAValue> defaults, final IClassHierarchy cha, IInstantiator instantiator, Object... instantiatorArgs) {
        if (callee == null) {
            throw new IllegalArgumentException("Cannot connect through to null-callee");
        }
        if (overrides == null) {
            overrides = Collections.EMPTY_SET;
        }
        if (defaults == null) {
            defaults = Collections.EMPTY_SET;
        }
        if (callee.getNumberOfParameters() == 0) {
            return new ArrayList<>(0);
        }
        
        final List<SSAValue> assigned = new ArrayList<>(); // TODO: Set initial size
        final List<Parameter> calleeParams = callee.all();
        final List<Parameter> thisParams = all();

        // ****
        // Implementation starts here
        debug("Collecting parameters for callee {}", ((callee.mRef!=null)?callee.mRef:callee.method));
        debug("\tThe calling function is {}", ((this.mRef!=null)?this.mRef:this.method));
  forEachParameter: 
        for (final Parameter param : calleeParams) {
            debug("\tSearching candidate for {}", param);
            final TypeReference paramType = param.getType();

            { // Exact match in overrides
                for (final SSAValue cand : overrides) {
                    if (cand.getType().getName().equals(paramType.getName())) { // XXX: What about the loader?
                        assigned.add(cand);
                        debug("\t\tAsigning: {} from the overrides (eq)", cand);
                        continue forEachParameter;
                    } else {
                        debug("\t\tSkipping: {} of the overrides (eq)", cand);
                    }
                }
            }

            { // Exact match in this params
                for (final Parameter cand : thisParams) {
                    if (cand.getType().getName().equals(paramType.getName())) {
                        assigned.add(cand);
                        debug("\t\tAsigning: {} from callers params (eq)", cand);
                        continue forEachParameter;
                    } else {
                        debug("\t\tSkipping: {} of the callers params (eq)", cand);
                    }
                }
            }

            { // Exact match in defaults
                for (final SSAValue cand : defaults) {
                    if (cand.getType().getName().equals(paramType.getName())) {
                        assigned.add(cand);
                        debug("\t\tAsigning: {} from the defaults (eq)", cand);
                        continue forEachParameter;
                    }
                }
            }
            
            debug("\tThe parameter is still not found - try again using an assignability check...");

            // If we got here we need cha
            if (cha != null) {
                { // Assignable from overrides
                    try {
                        for (final SSAValue cand : overrides) {
                            if (isAssignable(cand, param, cha)) {
                                assigned.add(cand);
                                debug("\t\tAsigning: {} from the overrides (ass)", cand);
                                continue forEachParameter;
                            }
                        }
                    } catch (ClassLookupException e) {
                    }
                }

                { // Assignable from this params
                    for (final Parameter cand : thisParams) {
                        try {
                            if (isAssignable(cand, param, cha)) {
                                assigned.add(cand);
                                debug("\t\tAsigning: {} from the callrs params (ass)", cand);
                                continue forEachParameter;
                            }
                        } catch (ClassLookupException e) {
                        }
                    }
                }

                { // Assignable from defaults
                    for (final SSAValue cand : defaults) {
                        if (isAssignable(cand, param, cha)) {
                            assigned.add(cand);
                            debug("\t\tAsigning: {} from the defaults (ass)", cand);
                            continue forEachParameter;
                        }
                    }
                }
       
                if (instantiator != null) {
                    info("Creating new instance of: {} in call to {}", param, callee);
                    /*{ // DEBUG
                        System.out.println("Creating new instance of: " + param);
                        System.out.println("in connectThrough");
                        System.out.println("\tCaller:\t\t" + this.forMethod());
                        System.out.println("\tCallee:\t\t" + callee.forMethod());
                        System.out.println("\tOverrides:\t" + overrides);
                        System.out.println("\tDefaults:\t" + defaults);
                    } // */
                    final int inst = instantiator.createInstance(param.getType(), instantiatorArgs);
                    if (inst < 0) {
                        warn("No type was assignable and the instantiator returned an invalidone! Using null for {}", param);
                        assigned.add(null);
                        continue forEachParameter;
                    } else {
                        final Parameter newParam;
                        if (this.base == BasedOn.IMETHOD) {
                            newParam = new Parameter(inst, "craftedForCall", param.getType(), ParamerterDisposition.NEW, 
                                this.base, this.method.getReference(), this.descriptorOffset);
                        } else if (this.base == BasedOn.METHOD_REFERENCE) {
                            newParam = new Parameter(inst, "craftedForCall", param.getType(), ParamerterDisposition.NEW, 
                                this.base, this.mRef, this.descriptorOffset);
                        } else {
                            throw new UnsupportedOperationException("Can't handle base " + this.base);
                        }

                        assigned.add(newParam);
                        continue forEachParameter;
                    }
                } else {
                    warn("No IInstantiator given and no known parameter assignable - using null");
                    assigned.add(null);
                    continue forEachParameter;
                }
            } else {
                // TODO: CreateInstance Call-Back
                
                warn("No type was equal. We can't ask isAssignable since we have no cha!");
                assigned.add(null);
                continue forEachParameter;
            } // of (cha != null)

            //Assertions.UNREACHABLE(); // Well it's unreachable
        } // of final Parameter param : calleeParams


        if(assigned.size() != calleeParams.size()) {
            System.err.println("Assigned " + assigned.size() + " params to a method taking " + calleeParams.size() + " params!"); 
            System.err.println("The call takes the parameters");
            for (Parameter param : calleeParams) {
                System.err.println("\t" + param);
            }
            System.err.println("The following were assigned:");
            for (int i = 0; i < assigned.size(); ++i) {
                System.err.println("\tAssigned parameter " + (i + 1) + " is " + assigned.get(i));
            }
            throw new IllegalStateException("Parameter mismatch!");
        }
        return assigned;
    }



    // *****************************************************************************
    //
    //  Private helper functions follow...

    /**
     *  Does "to x := from" hold?.
     */
    public static boolean isAssignable(final TypeReference from, final TypeReference to, final IClassHierarchy cha) {
        if (cha == null) {
            throw new IllegalArgumentException("ClassHierarchy may not be null");
        }

        if (from.getName().equals(to.getName())) return true;

        if (from.isPrimitiveType() && to.isPrimitiveType()) {
            //return PrimitiveAssignability.isAssignableFrom(from.getName(), to.getName());
            return PrimitiveAssignability.isAssignableFrom(to.getName(), from.getName()); // TODO: Which way
        }
        
        if (from.isPrimitiveType() || to.isPrimitiveType()) {
            return false;
        }

        IClass fromClass = cha.lookupClass(from);
        IClass toClass = cha.lookupClass(to);

        if (fromClass == null) {
            debug("Unable to look up the type of from=" + from + " in the ClassHierarchy - tying other loaders...");
            for (final IClassLoader loader: cha.getLoaders()) {
                final IClass cand = loader.lookupClass(from.getName());
                if (cand != null) {
                    debug("Using alternative for from: {}", cand);
                    fromClass = cand;
                    break;
                }
            }

            if (fromClass == null) {
                throw new ClassLookupException("Unable to look up the type of from=" + from + 
                        " in the ClassHierarchy");
                //return false; // TODO
            }
        }

        if (toClass == null) {
            debug("Unable to look up the type of to=" + to + " in the ClassHierarchy - tying other loaders...");
            for (final IClassLoader loader: cha.getLoaders()) {
                final IClass cand = loader.lookupClass(to.getName());
                if (cand != null) {
                    debug("Using alternative for to: {}", cand);
                    toClass = cand;
                    break;
                }
            }

            if (toClass == null) {
                error("Unable to look up the type of to={} in the ClassHierarchy", to);
                return false;
                //throw new ClassLookupException("Unable to look up the type of to=" + to + 
                //        " in the ClassHierarchy");
            }
        }
        
        // cha.isAssignableFrom (IClass c1, IClass c2)
        //  Does an expression c1 x := c2 y typecheck? 
         
        trace("isAssignableFrom({}, {}) = {}", toClass, fromClass, cha.isAssignableFrom(toClass, fromClass));
        return cha.isAssignableFrom(toClass, fromClass);
    }

     /**
     *  Is sub a subclass of superC (or the same).
     */
    public static boolean isSubclassOf(final TypeReference sub, final TypeReference superC, final IClassHierarchy cha)
                throws ClassLookupException {
        if (cha == null) {
            throw new IllegalArgumentException("ClassHierarchy may not be null");
        }

        if (sub.getName().equals(superC.getName())) return true;

        if (sub.isPrimitiveType() || superC.isPrimitiveType()) {
            return false;
        }

        IClass subClass = cha.lookupClass(sub);
        IClass superClass = cha.lookupClass(superC);

        if (subClass == null) {
            debug("Unable to look up the type of from=" + sub + " in the ClassHierarchy - tying other loaders...");
            for (final IClassLoader loader: cha.getLoaders()) {
                final IClass cand = loader.lookupClass(sub.getName());
                if (cand != null) {
                    debug("Using alternative for from: {}", cand);
                    subClass = cand;
                    break;
                }
            }

            if (subClass == null) {
                throw new ClassLookupException("Unable to look up the type of from=" + sub + 
                        " in the ClassHierarchy");
            }
        }

        if (superClass == null) {
            debug("Unable to look up the type of to=" + superC + " in the ClassHierarchy - tying other loaders...");
            for (final IClassLoader loader: cha.getLoaders()) {
                final IClass cand = loader.lookupClass(superC.getName());
                if (cand != null) {
                    debug("Using alternative for to: {}", cand);
                    superClass = cand;
                    break;
                }
            }

            if (superClass == null) {
                error("Unable to look up the type of to={} in the ClassHierarchy", superC);
                throw new ClassLookupException("Unable to look up the type of to=" + superC + 
                        " in the ClassHierarchy");
            }
        }
        
        return cha.isSubclassOf(subClass, superClass);
    }
   
    /**
     *  The method this accessor reads the parameters from.
     */
    public MethodReference forMethod() {
        if (this.mRef != null) {
            return this.mRef;
        } else {
            return this.method.getReference();
        }
    }


    // *****************************************************************************
    //
    //  Shorthand functions follow...

    /**
     *  Does "to x := from" hold?.
     */
    protected boolean isAssignable(final SSAValue from, final SSAValue to, final IClassHierarchy cha) {
        return isAssignable(from.getType(), to.getType(), cha);
    }

    /**
     *  Shorthand for forInvokeStatic(final List&lt;? extends Parameter&gt; args, final ParameterAccessor target, final IClassHierarchy cha).
     *
     *  Generates a new ParameterAccessor for target and hands the call through.
     */
    public int[] forInvokeStatic(final List<?extends Parameter> args, final MethodReference target, final IClassHierarchy cha) {
        return forInvokeStatic(args, new ParameterAccessor(target, cha), cha);
    }

    /**
     *  Shorthand for forInvokeVirtual(final int self, final List&lt;? extends Parameter&gt; args, final ParameterAccessor target, final IClassHierarchy cha).
     *
     *  Generates a new ParameterAccessor for target and hands the call through.
     */
    public int[] forInvokeVirtual (final int self, final List<? extends Parameter> args, final MethodReference target, final IClassHierarchy cha) {
        return forInvokeVirtual (self, args, new ParameterAccessor(target, cha), cha);
    }

    /**
     *  If the method returns a value eg is non-void.
     */
    public boolean hasReturn() {
        return (getReturnType() != TypeReference.Void); 
    }

    /**
     *  Assign parameters to a call based on their type.
     *
     *  this variant of connectThrough cannot create new instances if needed.
     *
     *  @param  callee      The function to generate the parameter-list for
     *  @param  overrides   If a parameter occurs here, it is preferred over the ones present in this
     *  @param  defaults    If a parameter is not present in this or the overrides, defaults are searched. If the parameter is not present there null is assigned.
     *  @param cha          Optional class hierarchy for testing assignability 
     *  @return the parameter-list for the call of toMethod 
     */
    public List<SSAValue> connectThrough(final ParameterAccessor callee, Set<? extends SSAValue> overrides, Set<? extends SSAValue> defaults, 
            final IClassHierarchy cha) {
        return connectThrough(callee, overrides, defaults, cha, null);
    }
    // *****************************************************************************
    //
    //  Hand-through functions follow...

    /**
     *  Handed through to the IMethod / MethodReference
     */
    public TypeReference getReturnType() {
        switch (this.base) {
            case IMETHOD:
                return this.method.getReturnType();
            case METHOD_REFERENCE:
                return this.mRef.getReturnType();
            default:
                throw new UnsupportedOperationException("No implementation of getReturnType() for base " + this.base);
        }
    }

    /**
     * Number of parameters _excluding_ implicit this
     */
    public int getNumberOfParameters() {
        return this.numberOfParameters;
    }

    /**
     *  Extensive output for debugging purposes.
     */
    public String dump() {
        String ret = "Parameter Accessor for " + ((this.mRef!=null)?"mRef:" + this.mRef.toString():"IMethod: " + this.method.toString()) +
            "\nContains " + this.numberOfParameters + " Parameters " + this.base + "\n";
        /*for (int i = 1; i <= this.numberOfParameters; ++i) {
            try {
                ret += "\t" + getParameter(i).toString() + "\n";
            } catch (Exception e) {
                ret += "\tNone at " + i + "\n";
            }
        }*/
        ret += "\nAnd all is:\n";
        for (Parameter p : all()) {
            ret += "\t" + p + "\n";
        }
        if (hasImplicitThis ()) {
            ret +="This: " + getThis();
        } else {
            ret +="Is static";
        }
        return ret;
    }

    @Override
    public String toString() {
        return "<ParamAccessor forMethod=" + this.forMethod() + " />";
    }
    
    private static void debug(String s, Object ... args) {
      if (DEBUG) { System.err.printf(s, args); }
    }
    
    private static void info(String s, Object ... args) {    
      if (DEBUG) { System.err.printf(s, args); }
    }
    
    private static void warn(String s, Object ... args) {    
      if (DEBUG) { System.err.printf(s, args); }
    }
    
    private static void trace(String s, Object ... args) {    
      if (DEBUG) { System.err.printf(s, args); }
    }
    
    private static void error(String s, Object ... args) {    
      if (DEBUG) { System.err.printf(s, args); }
    }
}
