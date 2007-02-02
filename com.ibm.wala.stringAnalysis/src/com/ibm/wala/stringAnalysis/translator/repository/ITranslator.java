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
/**
 * 
 */
package com.ibm.wala.stringAnalysis.translator.repository;

import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.SimpleGrammar;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.IVariableFactory;
import com.ibm.wala.stringAnalysis.translator.CallEnv;
import com.ibm.wala.stringAnalysis.translator.IConstraintSolver;

public interface ITranslator {
    ITranslator copy();
    SimpleGrammar solve(IConstraintSolver solver,
                        String funcName, ISymbol recv,
                        List params, IProductionRule invokeRule,
                        SimpleGrammar targetGrammar,
                        SimpleGrammar otherRules,
                        Stack<CallEnv> callStack,
                        IVariableFactory varFactory);
    boolean acceptCyclic();
    SimpleGrammar translateCyclic(SimpleGrammar g, Set terminals);
    Set possibleOutputs(Set terminals);
    SimpleGrammar toComparable(SimpleGrammar target);
    boolean isFixpoint(SimpleGrammar target, SimpleGrammar translatedGrammar);
}