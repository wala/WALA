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

public class SimpleSymbolCopier extends AbstractSymbolCopier {
    public ISymbol copySymbolReference(ISymbol parent, ISymbol symbol) {
        return symbol;
    }
    
    public String copyName(String name) {
        return name;
    }

    static final public SimpleSymbolCopier defaultCopier = new SimpleSymbolCopier();
}
