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
package com.ibm.wala.stringAnalysis.translator;

import java.util.*;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.stringAnalysis.util.SAUtil;

public class RuleAdder implements ISimplify {
    private Set rules;
    private ISimplify cfg;
    
    public RuleAdder(ISimplify cfg, Set rules) {
        this.cfg = cfg;
        this.rules = new HashSet(rules);
    }
    
    public RuleAdder(ISimplify cfg, IProductionRule rules[]) {
        this(cfg, SAUtil.set(rules));
    }
    
    public SimpleGrammar toSimple() {
        SimpleGrammar cfg = this.cfg.toSimple();
        cfg.getRules().addAll(rules);
        return cfg;
    }
}
