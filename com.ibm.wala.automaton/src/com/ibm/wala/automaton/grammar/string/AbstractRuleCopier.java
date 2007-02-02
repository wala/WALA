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
package com.ibm.wala.automaton.grammar.string;

import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.automaton.string.*;

public abstract class AbstractRuleCopier implements IRuleCopier {
    public abstract IProductionRule copy(IProductionRule rule);

    public Collection copyRules(Collection rules) {
        try {
            Collection c = (Collection) rules.getClass().newInstance();
            for (Iterator i = rules.iterator(); i.hasNext(); ) {
                IProductionRule r = (IProductionRule) i.next();
                c.add(r.copy(this));
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
