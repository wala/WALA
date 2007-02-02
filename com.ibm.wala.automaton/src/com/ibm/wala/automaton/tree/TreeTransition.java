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

public class TreeTransition extends Transition implements ITreeTransition {
    final static private String compositeStateName = "cs";
    
    static public CompositeState createCompositeState(IBinaryTree tree) {
        final Set states = new HashSet();
        tree.traverse(new ISymbolVisitor(){
            public void onVisit(ISymbol symbol) {
                if (symbol instanceof StateBinaryTree) {
                    StateBinaryTree v = (StateBinaryTree) symbol;
                    states.add(v.getState());
                }
            }
            public void onLeave(ISymbol symbol) {
            }
        });
        CompositeState s = new CompositeState(compositeStateName, states);
        return s;
    }

    public TreeTransition(IBinaryTree input, IBinaryTree output) {
        super(createCompositeState(input), createCompositeState(output), input, new ISymbol[]{output});
    }
    
    public IBinaryTree transit(IBinaryTree tree) {
        return (IBinaryTree) transit((ISymbol)tree).get(0);
    }
}
