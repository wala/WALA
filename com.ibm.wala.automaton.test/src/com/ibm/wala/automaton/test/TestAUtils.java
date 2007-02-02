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
package com.ibm.wala.automaton.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.automaton.AUtil;

import junit.framework.TestCase;

public class TestAUtils extends TestCase {
    public void testList() {
        List l1 = AUtil.list(new Object[]{"a", "b", "c"});
        List l2 = new ArrayList();
        l2.add("a");
        l2.add("b");
        l2.add("c");
        assertEquals(l2, l1);
    }
    
    public void testSet() {
        Set s1 = AUtil.set(new Object[]{"a", "b", "c"});
        Set s2 = new HashSet();
        s2.add("a");
        s2.add("b");
        s2.add("c");
        assertEquals(s2, s1);
    }
    
    public void testAllList() {
        Set s1 = AUtil.set(new Object[]{"a", "b", "c"});
        Set s2 = AUtil.set(new Object[]{
                AUtil.list(new Object[]{"a", "b", "c"}),
                AUtil.list(new Object[]{"a", "c", "b"}),
                AUtil.list(new Object[]{"b", "a", "c"}),
                AUtil.list(new Object[]{"b", "c", "a"}),
                AUtil.list(new Object[]{"c", "a", "b"}),
                AUtil.list(new Object[]{"c", "b", "a"}),
        });
        Set comb = AUtil.allOrder(s1);
        assertEquals(s2, comb);
    }
    
    public void testMap() {
        Map m = AUtil.map(
                new String[]{"a", "b", "c"},
                new String[]{"A", "B", "C"});
        Map expected = new HashMap();
        expected.put("a", "A");
        expected.put("b", "B");
        expected.put("c", "C");
        assertEquals(expected, m);
    }

    public void testMap2() {
        Map m = AUtil.map(
                new String[]{"a", "b", "c"},
                new String[]{"A", "B"});
        Map expected = new HashMap();
        expected.put("a", "A");
        expected.put("b", "B");
        expected.put("c", null);
        assertEquals(expected, m);
    }

    public void testMap3() {
        Map m = AUtil.map(
                new String[]{"a", "b"},
                new String[]{"A", "B", "C"});
        Map expected = new HashMap();
        expected.put("a", "A");
        expected.put("b", "B");
        assertEquals(expected, m);
    }
    
    public void testUniqueName() {
        Set names = AUtil.set(new String[]{"a1", "a2", "b1", "c"});
        assertEquals("a3", AUtil.createUniqueName("a", names));
        assertEquals("a4", AUtil.createUniqueName("a", names));
        assertEquals("b2", AUtil.createUniqueName("b", names));
        assertEquals("b3", AUtil.createUniqueName("b", names));
        assertEquals("c1", AUtil.createUniqueName("c", names));
        assertEquals("c2", AUtil.createUniqueName("c", names));
        assertEquals("d1", AUtil.createUniqueName("d", names));
        assertEquals("d2", AUtil.createUniqueName("d", names));
        assertEquals("a10", AUtil.createUniqueName("a", names, 10));
        assertEquals("a11", AUtil.createUniqueName("a", names, 10));
    }
    
    public void testMapper() {
        List l1 = AUtil.list(new String[]{"a", "b", "c"});
        List l2 = AUtil.list(new String[]{"a1", "b1", "c1"});
        assertEquals(l2, AUtil.collect(l1, new AUtil.IElementMapper(){
            public Object map(Object obj) {
                String s = (String) obj;
                return s + "1";
            }
        }));
    }
    
    public void testSelector() {
        List l1 = AUtil.list(new String[]{"ab", "b", "cd", "e"});
        List l2 = AUtil.list(new String[]{"ab", "cd"});
        assertEquals(l2, AUtil.select(l1, new AUtil.IElementSelector(){
            public boolean selected(Object obj) {
                String s = (String) obj;
                return s.length() > 1;
            }
        }));
    }
    
    /*
    public void testPrettyFormat() {
        String str1 = "{a:1, b:2, c:3}";
        String str2 =
            "{" + AUtils.lineSeparator +
            "    a:1," + AUtils.lineSeparator +
            "    b:2," + AUtils.lineSeparator +
            "    c:3" + AUtils.lineSeparator +
            "}";
        String formatted = AUtils.prettyFormat(str1);
        assertEquals(str2, formatted);
    }
    
    public void testPrettyFormat2() {
        String str1 = "{a:1, b:[0,1,2], c:3}";
        String str2 =
            "{" + AUtils.lineSeparator +
            "    a:1," + AUtils.lineSeparator +
            "    b:[" + AUtils.lineSeparator +
            "        0," + AUtils.lineSeparator +
            "        1," + AUtils.lineSeparator +
            "        2" + AUtils.lineSeparator +
            "    ]," + AUtils.lineSeparator +
            "    c:3" + AUtils.lineSeparator +
            "}";
        String formatted = AUtils.prettyFormat(str1);
        assertEquals(str2, formatted);
    }
    */
    
}
