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
package com.ibm.wala.util;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

/**
 *  Offers checks like ClassHierarchy.isAssignable but for primitives.
 *
 *  This Class does not consider Boxing / Unboxing 
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-11-21
 */
public class PrimitiveAssignability {
    public static enum AssignabilityKind {
        IDENTITY,
        WIDENING,
        NARROWING,
        WIDENING_NARROWING,
        UNASSIGNABLE
    }

    private static enum Primitive {
        BOOLEAN,
        CHAR,
        BYTE,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE
    }

    private static final Map<Primitive, Map<Primitive, AssignabilityKind>> assignability =
        new EnumMap<>(Primitive.class);

    static {
        // fill assignability

        for (final Primitive t : Primitive.values()) {
            final Map<Primitive, AssignabilityKind> addendum = new EnumMap<>(Primitive.class);
            addendum.put(t, AssignabilityKind.IDENTITY);
            assignability.put(t, addendum);
        }

        putWidening(Primitive.BYTE, Primitive.SHORT);
        putWidening(Primitive.BYTE, Primitive.INT);
        putWidening(Primitive.BYTE, Primitive.LONG);
        putWidening(Primitive.BYTE, Primitive.FLOAT);
        putWidening(Primitive.BYTE, Primitive.DOUBLE);
        putWidening(Primitive.SHORT, Primitive.INT);
        putWidening(Primitive.SHORT, Primitive.LONG);
        putWidening(Primitive.SHORT, Primitive.FLOAT);
        putWidening(Primitive.SHORT, Primitive.DOUBLE);
        putWidening(Primitive.CHAR, Primitive.INT);
        putWidening(Primitive.CHAR, Primitive.LONG);
        putWidening(Primitive.CHAR, Primitive.FLOAT);
        putWidening(Primitive.CHAR, Primitive.DOUBLE);
        putWidening(Primitive.INT, Primitive.LONG);
        putWidening(Primitive.INT, Primitive.FLOAT);
        putWidening(Primitive.INT, Primitive.DOUBLE);
        putWidening(Primitive.LONG, Primitive.FLOAT); 
        putWidening(Primitive.LONG, Primitive.DOUBLE); 
        putWidening(Primitive.FLOAT, Primitive.DOUBLE); 

        putNarrowing(Primitive.SHORT, Primitive.BYTE);
        putNarrowing(Primitive.SHORT, Primitive.CHAR);
        putNarrowing(Primitive.CHAR, Primitive.BYTE);
        putNarrowing(Primitive.CHAR, Primitive.SHORT);
        putNarrowing(Primitive.INT, Primitive.BYTE);
        putNarrowing(Primitive.INT, Primitive.SHORT);
        putNarrowing(Primitive.INT, Primitive.CHAR);
        putNarrowing(Primitive.LONG, Primitive.BYTE);
        putNarrowing(Primitive.LONG, Primitive.SHORT);
        putNarrowing(Primitive.LONG, Primitive.CHAR);
        putNarrowing(Primitive.LONG, Primitive.INT);
        putNarrowing(Primitive.FLOAT, Primitive.BYTE);
        putNarrowing(Primitive.FLOAT, Primitive.SHORT);
        putNarrowing(Primitive.FLOAT, Primitive.CHAR);
        putNarrowing(Primitive.FLOAT, Primitive.INT);
        putNarrowing(Primitive.FLOAT, Primitive.LONG);
        putNarrowing(Primitive.DOUBLE, Primitive.BYTE);
        putNarrowing(Primitive.DOUBLE, Primitive.SHORT);
        putNarrowing(Primitive.DOUBLE, Primitive.CHAR);
        putNarrowing(Primitive.DOUBLE, Primitive.INT);
        putNarrowing(Primitive.DOUBLE, Primitive.LONG);
        putNarrowing(Primitive.DOUBLE, Primitive.FLOAT);

        assignability.get(Primitive.BYTE).put(Primitive.CHAR, AssignabilityKind.WIDENING_NARROWING);
    }

    private static void putNarrowing(final Primitive from, final Primitive to) {
        assert (! assignability.get(from).containsKey(to));
        assignability.get(from).put(to, AssignabilityKind.NARROWING);
    }

    private static void putWidening(final Primitive from, final Primitive to) {
        assert (! assignability.get(from).containsKey(to));
        assignability.get(from).put(to, AssignabilityKind.WIDENING);
    }

    private static final Map<TypeName, Primitive> namePrimitiveMap = new HashMap<>();

    static {
        // fill namePrimitiveMap
        namePrimitiveMap.put(TypeReference.BooleanName, Primitive.BOOLEAN);
        namePrimitiveMap.put(TypeReference.ByteName, Primitive.BYTE);
        namePrimitiveMap.put(TypeReference.CharName, Primitive.CHAR);
        namePrimitiveMap.put(TypeReference.DoubleName, Primitive.DOUBLE);
        namePrimitiveMap.put(TypeReference.FloatName, Primitive.FLOAT);
        namePrimitiveMap.put(TypeReference.IntName, Primitive.INT);
        namePrimitiveMap.put(TypeReference.LongName, Primitive.LONG);
        namePrimitiveMap.put(TypeReference.ShortName, Primitive.SHORT);
    }

    /**
     *  Is information lost on c1 x := c2 y? 
     */
    public static AssignabilityKind getAssignableFrom(TypeName from, TypeName to) {
        final Primitive f = namePrimitiveMap.get(from);
        final Primitive t = namePrimitiveMap.get(to);

        if (assignability.get(f).containsKey(t)) {
            return assignability.get(f).get(t);
        } else {
            return AssignabilityKind.UNASSIGNABLE;
        }
    }

    /**
     *  Does an expression c1 x := c2 y typecheck? 
     */
    public static boolean isAssignableFrom(TypeName from, TypeName to) {
        return getAssignableFrom(from, to) != AssignabilityKind.UNASSIGNABLE;
    }


}
