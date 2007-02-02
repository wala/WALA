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

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.stringAnalysis.translator.repository.ITranslatorRepository;

public class GR2CFG implements ICFGSolver {
    private IConstraintSolver solver;
    
    static public class ConstraintSolver extends SimpleConstraintSolver {
      public ConstraintSolver(ITranslatorRepository translators) {
        super(translators);
      }
    }
    
    public GR2CFG(ITranslatorRepository translators) {
        solver = new ConstraintSolver(translators);
    }

    public IContextFreeGrammar solve(ISimplify grammar, IVariable startSymbol) {
        return new ContextFreeGrammar(solver.solve(grammar, startSymbol));
    }
}
