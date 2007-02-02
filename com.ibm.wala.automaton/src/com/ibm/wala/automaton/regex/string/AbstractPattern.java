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

import java.util.List;

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.ISymbol;

public abstract class AbstractPattern implements IPattern {
    private IAutomaton compiledPattern = null;
    
    public IAutomaton compile(IPatternCompiler compiler) {
        if (compiledPattern == null) {
            compiledPattern = compiler.compile(this);
        }
        return compiledPattern;
    }
    
    public boolean matches(List sequence) {
        return compiledPattern.accept(sequence);
    }
    
    public boolean matches(ISymbol symbol) {
        return matches(AUtil.list(new ISymbol[]{symbol}));
    }
    
    abstract public void traverse(IPatternVisitor visitor);
    
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw(new RuntimeException(e));
        }
    }
}
