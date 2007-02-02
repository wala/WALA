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

public class DeepStateCopier extends AbstractStateCopier {
    public IState copyStateReference(IState parent, IState state) {
        if (state == null) {
            return null;
        }
        else {
            return state.copy(this);
        }
    }
    
    public String copyStateName(String name) {
        return name;
    }
    
    static final public DeepStateCopier defaultCopier = new DeepStateCopier();
}
