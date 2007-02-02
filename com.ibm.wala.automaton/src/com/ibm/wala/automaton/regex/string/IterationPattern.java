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

public class IterationPattern extends AbstractPattern implements IPattern {
    private IPattern pattern;
    private boolean empty;
    
    public IterationPattern(IPattern pattern, boolean empty) {
        this.pattern = pattern;
        this.empty = empty;
    }
    
    public IterationPattern(IPattern pattern) {
        this(pattern, true);
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
    
    public IPattern getPattern() {
        return pattern;
    }
    
    public boolean includesEmpty() {
        return empty;
    }
    
    public int hashCode() {
        return toString().hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!getClass().equals(obj.getClass())) return false;
        IterationPattern p = (IterationPattern) obj;
        return pattern.equals(p.getPattern()) && empty==p.includesEmpty();
    }
    
    public String toString() {
        return "(" + pattern.toString() + ")" + (empty ? "*" : "+");
    }

}
