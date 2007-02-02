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

public class IntersectionPattern extends AbstractPattern implements IPattern {
    private IPattern left;
    private IPattern right;
    
    public IntersectionPattern(IPattern left, IPattern right) {
        this.left = left;
        this.right = right;
    }
    
    public IPattern getLeft() {
        return left;
    }
    
    public IPattern getRight() {
        return right;
    }
    
    public int hashCode() {
        return left.hashCode() + right.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!getClass().equals(obj.getClass())) return false;
        IntersectionPattern p = (IntersectionPattern) obj;
        return left.equals(p.getLeft()) && right.equals(p.getRight());
    }
    
    public String toString() {
        return "(" + left.toString() + "&" + right.toString() + ")";
    }
    
    public void traverse(IPatternVisitor visitor) {
        visitor.onVisit(this);
        left.traverse(visitor);
        right.traverse(visitor);
        visitor.onLeave(this);
    }

    public IPattern copy(IPatternCopier copier) {
        List l = new ArrayList();
        l.add(left.copy(copier));
        l.add(right.copy(copier));
        return copier.copy(this, l);
    }
}
