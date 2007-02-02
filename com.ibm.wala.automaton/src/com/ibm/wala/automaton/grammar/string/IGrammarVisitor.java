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

import com.ibm.wala.automaton.string.ISymbolVisitor;

public interface IGrammarVisitor<T extends IProductionRule> extends ISymbolVisitor, IRuleVisitor {
    public void onVisit(IGrammar<T> g);
}
