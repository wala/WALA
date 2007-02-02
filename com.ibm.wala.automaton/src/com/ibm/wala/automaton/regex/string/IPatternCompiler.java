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

public interface IPatternCompiler {
    IAutomaton compile(IPattern pattern);
    IAutomaton onComplement(ComplementPattern pattern);
    IAutomaton onConcatenation(ConcatenationPattern pattern);
    IAutomaton onEmpty(EmptyPattern pattern);
    IAutomaton onIntersection(IntersectionPattern pattern);
    IAutomaton onIteration(IterationPattern pattern);
    IAutomaton onSymbol(SymbolPattern pattern);
    IAutomaton onUnion(UnionPattern pattern);
    IAutomaton onVariableBinding(VariableBindingPattern pattern);
    IAutomaton onVariableReference(VariableReferencePattern pattern);
}
