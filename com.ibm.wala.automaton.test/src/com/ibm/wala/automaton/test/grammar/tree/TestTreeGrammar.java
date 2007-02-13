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
package com.ibm.wala.automaton.test.grammar.tree;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.grammar.tree.*;
import com.ibm.wala.automaton.string.Symbol;
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.automaton.test.tree.TreeJunitBase;

public class TestTreeGrammar extends TreeJunitBase {
    public void testTreeGrammarNew() {
        TG(
                BV(1),
                new Object[]{
                    BV(1), BT("a", BV(2), BT("c")),
                    BV(2), BT("b", BV(2), null),
                }
        );
    }
    
    public void testTreeGrammarNew2() {
        Variable v1 = new Variable("v1");
        Variable v2 = new Variable("v2");
        Variable v3 = new Variable("v3");
        SimpleGrammar g = new SimpleGrammar(
                v1,
                new IProductionRule[]{
                    new ProductionRule(v1, BT("a", BV("v2"), BV("v3"))),
                    new ProductionRule(v2, BT("b", BV("v3"), null)),
                    new ProductionRule(v3, new Symbol("c")),
                });
        ITreeGrammar tg = new TreeGrammar(g);
        IContextFreeGrammar cfg = new ContextFreeGrammar(
                v3,
                new IProductionRule[]{
                    new ProductionRule(v3, new Symbol("c")),
                });
        ITreeGrammar expected = TG(
                BV("v1"),
                new Object[]{
                    BV("v1"), BT("a", BV("v2"), BV("v3")),
                    BV("v2"), BT("b", BV("v3"), null),
                    BV("v3"), BT(new CFGSymbol(cfg), null, null),
                }
        );
        assertEquals(expected, tg);
    }
}
