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
package com.ibm.wala.automaton.test.tree;

import java.util.*;

import com.ibm.wala.automaton.*;
import com.ibm.wala.automaton.tree.*;

public class TestTreeAutomatons extends TreeJunitBase {
    /*
     * t1[t11, t12[121,122], t13]
     * <=> t1[t11[null; t12[t121[null; t122[null;null]]; t13[null; null]]]; null]
     */
    public void testBinarization1() {
        ITree t121 = new Tree("t121");
        ITree t122 = new Tree("t122");
        
        ITree t11 = new Tree("t11");
        ITree t12 = new Tree("t12", new ITree[]{t121, t122});
        ITree t13 = new Tree("t13");
        ITree t1 = new Tree("t1", new ITree[]{t11, t12, t13});
        
        IBinaryTree bt122 = new BinaryTree("t122");
        IBinaryTree bt121 = new BinaryTree("t121", null, bt122);
        
        IBinaryTree bt13 = new BinaryTree("t13");
        IBinaryTree bt12 = new BinaryTree("t12", bt121, bt13);
        IBinaryTree bt11 = new BinaryTree("t11", null, bt12);
        IBinaryTree bt1 = new BinaryTree("t1", bt11, null);

        IBinaryTree result1 = TreeAutomatons.binarize(t1);
        assertEquals(bt1, result1);
        
        ITree result2 = (ITree) TreeAutomatons.unbinarize(bt1).get(0);
        assertEquals(t1, result2);
    }

    /*
     * [t11, t12[121,122], t13]
     * <=> t11[null; t12[t121[null; t122[null;null]]; t13[null; null]]]
     */
    public void testBinarization2() {
        ITree t121 = new Tree("t121");
        ITree t122 = new Tree("t122");
        
        ITree t11 = new Tree("t11");
        ITree t12 = new Tree("t12", new ITree[]{t121, t122});
        ITree t13 = new Tree("t13");
        
        List l = AUtil.list(new ITree[]{t11,t12,t13});

        
        IBinaryTree bt122 = new BinaryTree("t122");
        IBinaryTree bt121 = new BinaryTree("t121", null, bt122);
        
        IBinaryTree bt13 = new BinaryTree("t13");
        IBinaryTree bt12 = new BinaryTree("t12", bt121, bt13);
        IBinaryTree bt11 = new BinaryTree("t11", null, bt12);

        IBinaryTree result1 = TreeAutomatons.binarize(l);
        assertEquals(bt11, result1);
        
        Collection result2 = TreeAutomatons.unbinarize(bt11);
        assertEquals(l, result2);
    }
    
}
