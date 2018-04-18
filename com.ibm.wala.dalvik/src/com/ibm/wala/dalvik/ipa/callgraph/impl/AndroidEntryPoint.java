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
package com.ibm.wala.dalvik.ipa.callgraph.impl;

import java.util.Comparator;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator.AndroidPossibleEntryPoint;
import com.ibm.wala.dalvik.util.AndroidTypes;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;
/**
 *  An AdnroidEntryPoint is basically the same as a DexEntryPoint. The Difference is, that further
 *  actions are taken to give a list of EntryPoints a specific order an to add information on how
 *  the entrypoints are to be called in a model which represents the usage of an android application.
 *  <p>
 *  You might want to use AndroidEntryPointLocator to generate a list of all EntryPoints in an 
 *  android application 
 *
 *  @see    com.ibm.wala.dalvik.util.AndroidEntryPointLocator
 *
 *  @author  Tobias Blaschke &lt;code@toiasblaschke.de&gt;
 *  @since   2013-09-01
 */
public class AndroidEntryPoint extends DexEntryPoint {
        //implements AndroidEntryPoint.IExecutionOrder { 
    public ExecutionOrder order; // XXX protect?

    protected AndroidComponent superType;

    public AndroidEntryPoint(AndroidPossibleEntryPoint p, IMethod method, IClassHierarchy cha, AndroidComponent inComponent) {
        super(method, cha);
        this.order = p.order;
        this.superType = inComponent;
    }

    public AndroidEntryPoint(AndroidPossibleEntryPoint p, IMethod method, IClassHierarchy cha) {
        super(method, cha);
        this.order = p.order;
        this.superType = AndroidComponent.from(method, cha);
    }

    public AndroidEntryPoint(ExecutionOrder o, IMethod method, IClassHierarchy cha, AndroidComponent inComponent) {
        this(o, method, cha);
        this.superType = inComponent;
    }

    public AndroidEntryPoint(ExecutionOrder o, IMethod method, IClassHierarchy cha) {
        super(method, cha);
        if (o == null) {
            throw new IllegalArgumentException("The execution order may not be null.");
        }
        this.order = o;
        this.superType = AndroidComponent.from(method, cha);
    }

    public AndroidComponent getComponent() {
        return this.superType;
    }
    
    /**
     *  If the function is defined in a class that extends an Activity.
     */
    public boolean isActivity() {
        IClassHierarchy cha = getClassHierarchy();
        final TypeReference activity = AndroidTypes.Activity;
        return cha.isSubclassOf(method.getDeclaringClass(), cha.lookupClass(activity));
    }

    public boolean belongsTo(AndroidComponent compo) {
        if ((compo == AndroidComponent.SERVICE) && (this.superType.equals(AndroidComponent.INTENT_SERVICE))) {
            return true;
        }
        return (this.superType.equals(compo));
    }

    public boolean isMemberOf(Atom klass) {
        return method.getDeclaringClass().getName().toString().startsWith(klass.toString());
        
        //IClassHierarchy cha = getClassHierarchy();
        //final TypeReference type = TypeReference.find(ClassLoaderReference.Primordial, klass.toString());
        //if (type == null) {
        //    throw new IllegalArgumentException("Unable to look up " + klass.toString());
        //}
        //return cha.isSubclassOf(method.getDeclaringClass(), cha.lookupClass(type));
    }
    /**
     *  Implement this interface to put entitys into the AndroidModel. 
     *
     *  Currently only AndroidEntryPoints are supportet directly. If you want to add other stuff you might want
     *  to subclass AbstractAndroidModel.
     */
    public interface IExecutionOrder extends Comparable<IExecutionOrder>{
        /**
         *  Returns an integer-representation of the ExecutionOrder.
         */
        public int getOrderValue();
        /**
         * AbstractAndroidModel inserts code at section switches. 
         *
         * There are eight hardcoded sections. Sections are derived by rounding the integer-representation.
         *
         * @return the section of this entity
         */
        public ExecutionOrder getSection();
    }

    /**
     *  AndroidEntryPoints have to be sorted before building the model.
     */
    public static class ExecutionOrderComperator implements Comparator<AndroidEntryPoint> {
        @Override public int compare(AndroidEntryPoint a, AndroidEntryPoint b) {
            return a.order.compareTo(b.order);
        }
    }


    /**
     * The section is used to build classes of EntryPoints on how they are to be called.
     *
     * The section is represented by the last label passed before the entity is reached.
     *
     * @return the section of this entrypoint
     */
    public ExecutionOrder getSection() {
        if (this.order.compareTo(ExecutionOrder.BEFORE_LOOP) == -1) return ExecutionOrder.AT_FIRST;
        if (this.order.compareTo(ExecutionOrder.START_OF_LOOP) == -1) return ExecutionOrder.BEFORE_LOOP;
        if (this.order.compareTo(ExecutionOrder.MIDDLE_OF_LOOP) == -1) return ExecutionOrder.START_OF_LOOP;
        if (this.order.compareTo(ExecutionOrder.MULTIPLE_TIMES_IN_LOOP) == -1) return ExecutionOrder.MIDDLE_OF_LOOP;
        if (this.order.compareTo(ExecutionOrder.END_OF_LOOP) == -1) return ExecutionOrder.MULTIPLE_TIMES_IN_LOOP;
        if (this.order.compareTo(ExecutionOrder.AFTER_LOOP) == -1) return ExecutionOrder.END_OF_LOOP;
        if (this.order.compareTo(ExecutionOrder.AT_LAST) == -1) return ExecutionOrder.AFTER_LOOP;
        return ExecutionOrder.AT_LAST;
    }

    public int getOrderValue() { return order.getOrderValue(); }
    public int compareTo(AndroidEntryPoint.IExecutionOrder o) {
        return this.order.compareTo(o);
    }


    /**
     * The ExecutionOrder is used to partially order EntryPoints. 
     *
     * The order has to be understood inclusive! E.g. "after(END_OF_LOOP)" means that the position is __BEFORE__ the
     * loop is actually closed!
     *
     * Before building the model a list of AdroidEntryPoints is to be sorted by that criterion. 
     * You can use AndroidEntryPoint.ExecutionOrderComperator for that task.
     */
    public static class ExecutionOrder implements IExecutionOrder {
        // This is an Enum-Style class
        /** Visit the EntryPoint once at the beginning of the model use that for initialization stuff  */
        public final static ExecutionOrder AT_FIRST = new ExecutionOrder(0);
        /** Basicly the same as AT_FIRST but visited after AT_FIRST */
        public final static ExecutionOrder BEFORE_LOOP = new ExecutionOrder (Integer.MAX_VALUE / 8);
        /** Visit multiple times (endless) in the loop */
        public final static ExecutionOrder START_OF_LOOP = new ExecutionOrder (Integer.MAX_VALUE / 8 * 2);
        /** Basicly the same as START_OF_LOOP */
        public final static ExecutionOrder MIDDLE_OF_LOOP = new ExecutionOrder (Integer.MAX_VALUE / 8 * 3);
        /** Do multiple calls in the loop. Visited after MIDDLE_OF_LOOP, before EEN_OF_LOOP */
        public final static ExecutionOrder MULTIPLE_TIMES_IN_LOOP = new ExecutionOrder (Integer.MAX_VALUE / 8 * 4);
        /** Things in END_OF_LOOP are acutually part of the loop. Use AFTER_LOOP if you want them executed only once */
        public final static ExecutionOrder END_OF_LOOP = new ExecutionOrder (Integer.MAX_VALUE / 8 * 5);
        /** Basicly the same as AT_LAST but visited before */
        public final static ExecutionOrder AFTER_LOOP = new ExecutionOrder (Integer.MAX_VALUE / 8 * 6);
        /** Last calls in the model */
        public final static ExecutionOrder AT_LAST = new ExecutionOrder (Integer.MAX_VALUE / 8 * 7);
        /** This value getts used by the detection heuristic - It is not recommended for manual use. */
		public final static ExecutionOrder DEFAULT = MIDDLE_OF_LOOP;

        private int value;
        /**
         *  Unrecommended way to generate the Order based on an Integer.
         *
         *  This method is handy when reading back files. In your code you should prefer the methods
         *  {@link #after(com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.IExecutionOrder)} and {@link #between(com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.IExecutionOrder, com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.IExecutionOrder)}.
         */
        public ExecutionOrder(int val) { 
            this.value=val; 
        }
        /**
         *  Unrecommended way to generate the Order based on a Label-String.
         *
         *  This method is handy when reading back files. If you want to refer to a label you
         *  should prefer the static members.
         */
        public ExecutionOrder(String label) {
            if (label.equals("AT_FIRST")) { this.value = ExecutionOrder.AT_FIRST.getOrderValue(); return; }
            if (label.equals("BEFORE_LOOP")) { this.value =  ExecutionOrder.BEFORE_LOOP.getOrderValue(); return; }
            if (label.equals("START_OF_LOOP")) { this.value = ExecutionOrder.START_OF_LOOP.getOrderValue(); return; }
            if (label.equals("MIDDLE_OF_LOOP")) { this.value = ExecutionOrder.MIDDLE_OF_LOOP.getOrderValue(); return; }
            if (label.equals("MULTIPLE_TIMES_IN_LOOP")) { this.value = ExecutionOrder.MULTIPLE_TIMES_IN_LOOP.getOrderValue(); return; }
            if (label.equals("END_OF_LOOP")) { this.value = ExecutionOrder.END_OF_LOOP.getOrderValue(); return; }
            if (label.equals("AFTER_LOOP")) { this.value = ExecutionOrder.AFTER_LOOP.getOrderValue(); return; }
            if (label.equals("AT_LAST")) { this.value = ExecutionOrder.AT_LAST.getOrderValue(); return; }
            throw new IllegalArgumentException("ExecutionOrder was constructed from an illegal label: " + label);
        }
        /** {@inheritDoc} */
        @Override
        public int getOrderValue() { return this.value; }

        /**
         *  Use {@link #between(IExecutionOrder, IExecutionOrder)} instead.
         *
         *  Does the internal calculations for the placement.
         */
        private static ExecutionOrder between(int after, int before) {
            if ((before - after) == 1) {
                // It could be ok to warn here instead of throwing: Functions would be located at the same
                // 'slot' in this case.
                // An alternative solution could be to choose other values for before or after.
                throw new ArithmeticException("Precision to low when cascading the ExecutionOrder. You can prevent this error by "
                        + "assigning the ExecutionOrders (e.g. using before() and after()) in a different order.");
            }
            if (after > before) {
                throw new IllegalArgumentException("The requested ordering could not be established due to the after-parameter "
                        + "being greater than the before parameter! after=" + after + " before=" + before);
            }
            return new ExecutionOrder(after + (before - after) / 2);
        }

        /** 
         *  Use this to place a call to an EntryPoint between two other EntryPoint calls or ExecutionOrder "labels".
         *  between() does not care about section-boundaries by itself! 
         *
         *  Use {@link #between(com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.IExecutionOrder[], com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.IExecutionOrder[])} and use labels as additional placement-information 
         *  to prevent unexpected misplacement.
         *
         *  @param  after   the call or "label" to be executed before this one
         *  @param  before  the call or "label" to be executed after this one (inclusive)
         *  @return A sortable object to represent the execution order
         *  @throws ArithmeticException when the precision is no more suitable for further cascading
         *  @throws IllegalArgumentException if parameter after is larger than before.
         *  @throws NullPointerException if either parameter is null
         */
        public static ExecutionOrder between(IExecutionOrder after, IExecutionOrder before) { 
            if (after == null) {
                throw new NullPointerException("after may not be null");
            }
            if (before == null) {
                throw new NullPointerException("after may not be null");
            }
            return between(after.getOrderValue(), before.getOrderValue()); 
        }

        /**
         *  Helper for internal use.
         */
        private static ExecutionOrder between(IExecutionOrder after, int before) { 
            return between(after.getOrderValue(), before); 
        }

        /**
         *  Use this variant to refer to multiple locations.
         *
         *  The minimum / maximum is computed before the placement of the ExecutionOrder.
         *
         *  This method is intended to be more robust when changing the position-information of referred-to
         *  ExecutionOrders.
         *
         *  In any other means it behaves exactly like {@link #between(com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.IExecutionOrder, com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.IExecutionOrder)}.
         *
         *  @param  after   the calls or "labels"  to be executed before this one
         *  @param  before  the calls or "labels" to be executed after this one (inclusive)
         *  @return A sortable object to represent the execution order
         *  @throws ArithmeticException when the precision is no more suitable for further cascading
         *  @throws IllegalArgumentException if parameter after is larger than before.
         *  @throws NullPointerException if either parameter is null
         */
        public static ExecutionOrder between(IExecutionOrder[] after, IExecutionOrder[] before) {
            if ((after == null) || (after.length == 0)) {
                throw new NullPointerException("after may not be null or empty array");
            }
            if ((before == null) || (before.length == 0)) {
                throw new NullPointerException("before may not be null or empty array");
            }
            int max = Integer.MIN_VALUE;
            int min = Integer.MAX_VALUE;
            for (IExecutionOrder i : after) if (max < i.getOrderValue()) max=i.getOrderValue();
            for (IExecutionOrder i : before) if (min > i.getOrderValue()) min=i.getOrderValue();
            return between(max, min);
        }

        public static ExecutionOrder between(IExecutionOrder after, IExecutionOrder[] before) {
            if ((before == null) || (before.length == 0)) {
                throw new NullPointerException("after may not be null or empty array");
            }
            int min = Integer.MAX_VALUE;
            for (IExecutionOrder i : before) if (min > i.getOrderValue()) min=i.getOrderValue();
            return between(after, min);
        }

        public static ExecutionOrder between(IExecutionOrder[] after, IExecutionOrder before) {
            if ((after == null) || (after.length == 0)) {
                throw new NullPointerException("after may not be null or empty array");
            }
            if (before == null) {
                throw new NullPointerException("before may not be null");
            }
            int max = Integer.MIN_VALUE;
            for (IExecutionOrder i : after) if (max < i.getOrderValue()) max=i.getOrderValue();
            return between(max, before.getOrderValue());
        }

        /**
         *  Place the call in the same section after the given call or "label".
         *
         *  @param  after   the call to be executed before this one or label the call belongs to
         *  @return A sortable object to represent the execution order
         *  @throws ArithmeticException when the precision is no more suitable for further cascading
         *  @throws NullPointerException if the parameter is null
         */
        public static ExecutionOrder after (IExecutionOrder after) { 
            if (after == null) {
                throw new NullPointerException("after may not be null");
            }
            return after(after.getOrderValue()); 
        }

        /**
         *  Prefer {@link #after(com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.IExecutionOrder)} whenever possible.
         */
        public static ExecutionOrder after (int after) {
            return between(after, ((after / (Integer.MAX_VALUE / 8)) + 1) * (Integer.MAX_VALUE / 8));
        }

        /**
         *  Use this variant to refer to multiple locations.
         *
         *  The maximum is computed before the placement of the ExecutionOrder.
         *
         *  @param  after   the call to be executed before this one or label the call belongs to
         *  @return A sortable object to represent the execution order
         *  @throws ArithmeticException when the precision is no more suitable for further cascading
         *  @throws NullPointerException if the parameter is null
         */
        public static ExecutionOrder after (IExecutionOrder[] after) {
            if (after == null) {
                throw new NullPointerException("after may not be null");
            }

            int max = Integer.MIN_VALUE;
            for (IExecutionOrder i : after) if (max < i.getOrderValue()) max=i.getOrderValue();
            return after(max);
        }

        public static ExecutionOrder directlyBefore(IExecutionOrder before) {
            return new ExecutionOrder(before.getOrderValue() - 1);
        }

        public static ExecutionOrder directlyAfter(IExecutionOrder before) {
            return new ExecutionOrder(before.getOrderValue() + 1);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExecutionOrder getSection() {
            if (this.compareTo(ExecutionOrder.BEFORE_LOOP) == -1) return ExecutionOrder.AT_FIRST;
            if (this.compareTo(ExecutionOrder.START_OF_LOOP) == -1) return ExecutionOrder.BEFORE_LOOP;
            if (this.compareTo(ExecutionOrder.MIDDLE_OF_LOOP) == -1) return ExecutionOrder.START_OF_LOOP;
            if (this.compareTo(ExecutionOrder.MULTIPLE_TIMES_IN_LOOP) == -1) return ExecutionOrder.MIDDLE_OF_LOOP;
            if (this.compareTo(ExecutionOrder.END_OF_LOOP) == -1) return ExecutionOrder.MULTIPLE_TIMES_IN_LOOP;
            if (this.compareTo(ExecutionOrder.AFTER_LOOP) == -1) return ExecutionOrder.END_OF_LOOP;
            if (this.compareTo(ExecutionOrder.AT_LAST) == -1) return ExecutionOrder.AFTER_LOOP;
            return ExecutionOrder.AT_LAST;
        }


        @Override
        public int compareTo(IExecutionOrder o) {
            if (this.value == o.getOrderValue()) return 0;
            if (this.value > o.getOrderValue()) return 1;
            return -1;
        }

        @Override
        public String toString() {
            if (this.compareTo(ExecutionOrder.AT_FIRST) == 0) return "ExecutionOrder.AT_FIRST";
            if (this.compareTo(ExecutionOrder.BEFORE_LOOP) == 0) return "ExecutionOrder.BEFORE_LOOP";
            if (this.compareTo(ExecutionOrder.START_OF_LOOP) == 0) return "ExecutionOrder.START_OF_LOOP";
            if (this.compareTo(ExecutionOrder.MIDDLE_OF_LOOP) == 0) return "ExecutionOrder.MIDDLE_OF_LOOP";
            if (this.compareTo(ExecutionOrder.MULTIPLE_TIMES_IN_LOOP) == 0) return "ExecutionOrder.MULTIPLE_TIMES_IN_LOOP";
            if (this.compareTo(ExecutionOrder.END_OF_LOOP) == 0) return "ExecutionOrder.END_OF_LOOP";
            if (this.compareTo(ExecutionOrder.AFTER_LOOP) == 0) return "ExecutionOrder.AFTER_LOOP";
            if (this.compareTo(ExecutionOrder.AT_LAST) == 0) return "ExecutionOrder.AT_LAST";

            if (this.compareTo(ExecutionOrder.BEFORE_LOOP) == -1) return "in section ExecutionOrder.AT_FIRST";
            if (this.compareTo(ExecutionOrder.START_OF_LOOP) == -1) return "in section ExecutionOrder.BEFORE_LOOP";
            if (this.compareTo(ExecutionOrder.MIDDLE_OF_LOOP) == -1) return "in section ExecutionOrder.START_OF_LOOP";
            if (this.compareTo(ExecutionOrder.MULTIPLE_TIMES_IN_LOOP) == -1) return "in section ExecutionOrder.MIDDLE_OF_LOOP";
            if (this.compareTo(ExecutionOrder.END_OF_LOOP) == -1) return "in section ExecutionOrder.MULTIPLE_TIMES_IN_LOOP";
            if (this.compareTo(ExecutionOrder.AFTER_LOOP) == -1) return "in section ExecutionOrder.END_OF_LOOP";
            if (this.compareTo(ExecutionOrder.AT_LAST) == -1) return "in section ExecutionOrder.AFTER_LOOP";
            return "in section ExecutionOrder.AT_LAST";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AndroidEntryPoint) {
            AndroidEntryPoint other = (AndroidEntryPoint) o;
            return this.getMethod().equals(other.getMethod());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 3 * this.getMethod().hashCode();
    }
}
