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
import java.util.Iterator;

public abstract class AbstractStateCopier implements IStateCopier {

    public IState copy(IState state) {
        if (state == null) {
            return null;
        }
        else {
            return (IState) state.clone();
        }
    }

    public Collection copyStates(Collection states) {
        try {
            Collection c = (Collection) states.getClass().newInstance();
            for (Iterator i = states.iterator(); i.hasNext(); ) {
                IState s = (IState) i.next();
                c.add(s.copy(this));
            }
            return c;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw(new AssertionError("should not reach this code."));
    }

    public abstract String copyStateName(String name);
    
    public abstract IState copyStateReference(IState parent, IState state);

    public Collection copyStateReferences(IState parent, Collection states) {
        try {
            Collection c = (Collection) states.getClass().newInstance();
            for (Iterator i = states.iterator(); i.hasNext(); ) {
                IState s = (IState) i.next();
                c.add(copyStateReference(parent, s));
            }
            return c;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        throw(new AssertionError("should not reach this code."));
    }

}
