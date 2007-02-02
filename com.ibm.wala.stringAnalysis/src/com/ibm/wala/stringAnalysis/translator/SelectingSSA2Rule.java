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
import com.ibm.wala.ssa.*;

public class SelectingSSA2Rule extends SSA2RuleFilter {
  Set<SSAInstruction> selected;
  
  public SelectingSSA2Rule(ISSA2Rule ssa2rule, Set<SSAInstruction> selected) {
    super(ssa2rule);
    this.selected = selected;
  }

  public Collection<IProductionRule> translate(SSAInstruction instruction, TranslationContext ctx) {
    if (selected.contains(instruction)) {
      return super.translate(instruction, ctx);
    }
    else {
      return new HashSet<IProductionRule>();
    }
  }
}
