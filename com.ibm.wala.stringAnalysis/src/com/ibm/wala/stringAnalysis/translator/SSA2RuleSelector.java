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
import java.util.HashSet;

import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.stringAnalysis.grammar.GR;

public abstract class SSA2RuleSelector extends SSA2RuleFilter {
  public SSA2RuleSelector(ISSA2Rule ssa2rule) {
    super(ssa2rule);
  }
  
  abstract public boolean accept(SSAInstruction instruction, TranslationContext ctx);

  public Collection<IProductionRule> translate(SSAInstruction instruction, TranslationContext ctx) {
    if (accept(instruction, ctx)) {
      return super.translate(instruction, ctx);
    }
    else {
      return new HashSet<IProductionRule>();
    }
  }
}
