/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.automaton.regex.string;


import com.ibm.wala.automaton.string.*;

public class PatternCompiler extends AbstractPatternCompiler {
    public IAutomaton onComplement(ComplementPattern pattern) {
        return null;
    }

    public IAutomaton onConcatenation(ConcatenationPattern pattern) {
        return null;
    }

    public IAutomaton onEmpty(EmptyPattern pattern) {
        return null;
    }

    public IAutomaton onIntersection(IntersectionPattern pattern) {
        return null;
    }

    public IAutomaton onIteration(IterationPattern pattern) {
        return null;
    }

    public IAutomaton onSymbol(SymbolPattern pattern) {
        return null;
    }

    public IAutomaton onUnion(UnionPattern pattern) {
        return null;
    }

    public IAutomaton onVariableBinding(VariableBindingPattern pattern) {
        return null;
    }

    public IAutomaton onVariableReference(VariableReferencePattern pattern) {
        return null;
    }
}

