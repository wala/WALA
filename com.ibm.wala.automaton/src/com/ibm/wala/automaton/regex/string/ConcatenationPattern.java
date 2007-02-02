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

public class ConcatenationPattern extends AbstractPattern implements IPattern {
    private IPattern head;
    private IPattern tail;
    
    public ConcatenationPattern(IPattern head, IPattern tail) {
        this.head = head;
        this.tail = tail;
    }
    
    public IPattern getHead() {
        return head;
    }
    
    public IPattern getTail() {
        return tail;
    }
    
    public int hashCode() {
        return head.hashCode() + tail.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!getClass().equals(obj.getClass())) return false;
        ConcatenationPattern p = (ConcatenationPattern) obj;
        return head.equals(p.getHead()) && tail.equals(p.getTail());
    }
    
    public String toString() {
        return head.toString() + "." + tail.toString();
    }
    
    public void traverse(IPatternVisitor visitor) {
        visitor.onVisit(this);
        head.traverse(visitor);
        tail.traverse(visitor);
        visitor.onLeave(this);
    }

    public IPattern copy(IPatternCopier copier) {
        List l = new ArrayList();
        l.add(head.copy(copier));
        l.add(tail.copy(copier));
        return copier.copy(this, l);
    }
}
