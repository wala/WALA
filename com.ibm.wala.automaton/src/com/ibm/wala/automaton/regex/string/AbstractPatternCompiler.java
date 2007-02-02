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

import com.ibm.wala.automaton.string.IAutomaton;

public abstract class AbstractPatternCompiler implements IPatternCompiler {
    public IAutomaton compile(IPattern pattern) {
        if (pattern instanceof IterationPattern) {
            return onIteration((IterationPattern) pattern);
        }
        else if (pattern instanceof ComplementPattern) {
            return onComplement((ComplementPattern) pattern);
        }
        else if (pattern instanceof ConcatenationPattern) {
            return onConcatenation((ConcatenationPattern) pattern);
        }
        else if (pattern instanceof EmptyPattern) {
            return onEmpty((EmptyPattern) pattern);
        }
        else if (pattern instanceof IntersectionPattern) {
            return onIntersection((IntersectionPattern) pattern);
        }
        else if (pattern instanceof SymbolPattern) {
            return onSymbol((SymbolPattern) pattern);
        }
        else if (pattern instanceof UnionPattern) {
            return onUnion((UnionPattern) pattern);
        }
        else if (pattern instanceof VariableBindingPattern) {
            return onVariableBinding((VariableBindingPattern) pattern);
        }
        else if (pattern instanceof VariableReferencePattern) {
            return onVariableReference((VariableReferencePattern) pattern);
        }
        throw(new AssertionError("unknown type: " + pattern.getClass()));
    }

    public abstract IAutomaton onComplement(ComplementPattern pattern);
    public abstract IAutomaton onConcatenation(ConcatenationPattern pattern);
    public abstract IAutomaton onEmpty(EmptyPattern pattern);
    public abstract IAutomaton onIntersection(IntersectionPattern pattern);
    public abstract IAutomaton onIteration(IterationPattern pattern);
    public abstract IAutomaton onSymbol(SymbolPattern pattern);
    public abstract IAutomaton onUnion(UnionPattern pattern);
    public abstract IAutomaton onVariableBinding(VariableBindingPattern pattern);
    public abstract IAutomaton onVariableReference(VariableReferencePattern pattern);
}
