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

import java.util.ArrayList;
import java.util.List;

import com.ibm.wala.automaton.string.IVariable;

public class VariableBindingPattern extends AbstractPattern implements IPattern {
    private IVariable variable;
    private IPattern pattern;
    
    public VariableBindingPattern(IPattern pattern, IVariable variable) {
        this.pattern = pattern;
        this.variable = variable;
    }
    
    public VariableBindingPattern(IVariable variable, IPattern pattern) {
        this(pattern, variable);
    }
    
    public IPattern getPattern() {
        return pattern;
    }
    
    public IVariable getVariable() {
        return variable;
    }
    
    public int hashCode() {
        return pattern.hashCode() + variable.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!getClass().equals(obj.getClass())) return false;
        VariableBindingPattern p = (VariableBindingPattern) obj;
        return pattern.equals(p.getPattern()) && variable.equals(p.getVariable());
    }
    
    public String toString() {
        return "(" + pattern.toString() + " as " + variable.toString() + ")";
    }
    
    public void traverse(IPatternVisitor visitor) {
        visitor.onVisit(this);
        pattern.traverse(visitor);
        visitor.onLeave(this);
    }

    public IPattern copy(IPatternCopier copier) {
        List l = new ArrayList();
        l.add(pattern.copy(copier));
        return copier.copy(this, l);
    }
}
