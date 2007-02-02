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

import com.ibm.wala.automaton.string.*;

public interface IProductionRule extends Cloneable {
    public IVariable getLeft();
    public void setLeft(IVariable left);
    
    // public List<ISymbol> getRight();
    public List getRight();
    public ISymbol getRight(int index);
    
    public boolean isEpsilonRule(); // = getRight().isEmpty()
    
    public void traverseSymbols(ISymbolVisitor visitor);
    
    public void traverse(IRuleVisitor visitor);
    
    public IProductionRule copy(IRuleCopier copier);
    
    public Object clone();
}
