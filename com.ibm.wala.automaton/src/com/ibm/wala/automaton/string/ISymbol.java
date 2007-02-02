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
package com.ibm.wala.automaton.string;


public interface ISymbol extends Cloneable {
    String getName();

    // this method is defined in IFlexibleNamingSymbol
    //void setName(String name);
    
    boolean matches(ISymbol symbol, IMatchContext context);
    
    boolean possiblyMatches(ISymbol symbol, IMatchContext context);
    
    void traverse(ISymbolVisitor visitor);
    
    ISymbol copy(ISymbolCopier copier);
    
    Object clone();
    
    int size();
}
