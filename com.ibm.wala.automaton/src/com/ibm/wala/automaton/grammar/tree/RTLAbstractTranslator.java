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
package com.ibm.wala.automaton.grammar.tree;

import java.util.*;

import com.ibm.wala.automaton.*;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.automaton.tree.*;

public abstract class RTLAbstractTranslator implements IRTLTranslator {
    private IStateTransitionSystem system;
    
    static protected class StateVariable2Name extends DMap {
        private Set names;
        public StateVariable2Name(Set names) {
            super();
            this.names = new HashSet(names);
        }
        
        public StateVariable2Name(IGrammar g) {
            this(Grammars.collectVariableNames(TreeGrammars.collectUsedVariables(g)));
        }
        
        public String get(StateBinaryTree t) {
            return (String) super.get(t);
        }
        
        public String get(IState state, IBinaryTreeVariable v) {
            return get(new StateBinaryTree(state, v));
        }
        
        public Object create(Object key) {
            String vname = AUtil.createUniqueName("N", names);
            return vname;
        }
    }
    
    protected RTLAbstractTranslator(IStateTransitionSystem system) {
        this.system = system;
    }
    
    public IStateTransitionSystem getSystem() {
        return system;
    }
    
    protected Set translate(IBinaryTreeVariable v, IBinaryTree bt, final StateVariable2Name sv2name) {
        Set transitions = system.getAcceptTransitions(TreeTransition.createCompositeState(bt), bt);
        Set rules = new HashSet();
        for (Iterator i = transitions.iterator(); i.hasNext(); ) {
            ITreeTransition tr = (ITreeTransition) i.next();
            IBinaryTree bt2 = tr.transit(bt);
            IProductionRule rule = new ProductionRule(v, bt2);
            rules.add(rule);
        }
        return rules;
    }
    
    protected Set getPrimitiveStates() {
        Set states = new HashSet();
        for (Iterator i = system.getStates().iterator(); i.hasNext(); ) {
            IState s = (IState) i.next();
            if (s instanceof CompositeState) {
                CompositeState cs = (CompositeState) s;
                states.addAll(AUtil.set(cs.getStates()));
            }
            else {
                states.add(s);
            }
        }
        return states;
    }
}
