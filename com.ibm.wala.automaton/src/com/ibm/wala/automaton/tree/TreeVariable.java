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
package com.ibm.wala.automaton.tree;

import java.util.*;

import com.ibm.wala.automaton.string.*;

public class TreeVariable extends VariableWrapper implements ITreeVariable  {
    public TreeVariable(String name) {
        this(new Variable(name));
    }
    
    public TreeVariable(IVariable v) {
        super(v);
    }

    public ISymbol getLabel() {
        return getVariable();
    }
}
