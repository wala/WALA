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

import java.util.Collection;

import com.ibm.wala.automaton.AUtil;

public abstract class AbstractTransitionCopier implements ITransitionCopier {

    public abstract ITransition copy(ITransition transition);

    public Collection copyTransitions(Collection transitions) {
        return AUtil.collect(transitions, new AUtil.IElementMapper(){
            public Object map(Object obj) {
                ITransition t = (ITransition) obj;
                return t.copy(AbstractTransitionCopier.this);
            }
        });
    }
}
