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
package com.ibm.wala.automaton.grammar.string;

import java.util.*;

import com.ibm.wala.automaton.string.IVariable;

public interface IContextFreeGrammar<T extends IProductionRule> extends IGrammar<T>, ISimplify {
    public void setStartSymbol(IVariable symbol);

    public void addRule(T rule);
    public void addRules(Collection<T> rules);
    public void addRules(Iterator<T> rules);
}
