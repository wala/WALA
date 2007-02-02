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

import com.ibm.wala.automaton.string.*;

public class VariableReferencePattern extends AbstractPattern implements IPattern {
    private IVariable variable;
    
    public VariableReferencePattern(IVariable variable) {
        this.variable = variable;
    }
    
    public IVariable getVariable() {
        return variable;
    }
    
    public int hashCode() {
        return variable.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!getClass().equals(obj.getClass())) return false;
        VariableReferencePattern p = (VariableReferencePattern) obj;
        return variable.equals(p.getVariable());
    }
    
    public String toString() {
        return "ref" + variable.toString();
    }
    
    public void traverse(IPatternVisitor visitor) {
        visitor.onVisit(this);
        visitor.onLeave(this);
    }

    public IPattern copy(IPatternCopier copier) {
        return copier.copy(this, null);
    }
}
