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
package com.ibm.wala.stringAnalysis.test;

import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.parser.*;
import com.ibm.wala.automaton.regex.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.core.tests.util.*;

abstract public class SAJunitBase extends WalaTestCase {
    static private AmtParser parser = new AmtParser();
    static private IPatternCompiler patternCompiler = new StringPatternCompiler();
    
    int MAX_FILENAME = 128;
    
    protected void setUp() throws Exception {
        super.setUp();
        String name = Trace.getTraceFile();
        if (name != null && name.length()>=MAX_FILENAME) {
            Trace.setTraceFile(name.substring(0,MAX_FILENAME));
        }
    }
    
    protected IAutomaton pattern(String patStr) {
        IPattern pat = (IPattern) parser.parse("/" + patStr + "/").get(new Variable("_"));
        if (pat == null) {
            return new Automaton(                
                new State("s1"),
                new State[]{},
                new Transition[]{}
            );
        }
        IAutomaton a = patternCompiler.compile(pat);
        return a;
    }
    
    protected void verifyCFG(IAutomaton pattern, IContextFreeGrammar cfg) {
        assertTrue(CFLReachability.containsAll(pattern, cfg));
    }
    
    protected void verifyCFG(String patStr, IContextFreeGrammar cfg) {
        verifyCFG(pattern(patStr), cfg);
    }
}
