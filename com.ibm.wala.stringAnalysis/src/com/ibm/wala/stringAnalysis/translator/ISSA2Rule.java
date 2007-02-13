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

import java.util.Collection;

import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.stringAnalysis.grammar.GR;

public interface ISSA2Rule {
    Collection<IProductionRule> translate(SSAInstruction instruction, TranslationContext ctx);
    GR postTranslate(GR gr);
 
    ISymbol getDefaultParameterValueSymbol();
    ISymbol getValueSymbol(int v, SSAInstruction instruction, TranslationContext ctx);
    IProductionRule createRule(IR ir, SSAInstruction instruction, IVariable left, ISymbol right[]);    
}
