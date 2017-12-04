/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural;

import java.util.Collection;
import java.util.LinkedList;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.filter.CombinedExceptionFilter;

public class CombinedInterproceduralExceptionFilter<Instruction> implements InterproceduralExceptionFilter<Instruction> {
  private final Collection<InterproceduralExceptionFilter<Instruction>> filter;

  public CombinedInterproceduralExceptionFilter() {
    this.filter = new LinkedList<>();
  }

  public CombinedInterproceduralExceptionFilter(
      Collection<InterproceduralExceptionFilter<Instruction>> filter) {
    this.filter = filter;
  }

  public boolean add(InterproceduralExceptionFilter<Instruction> e) {
    return this.filter.add(e);
  }

  public boolean addAll(
      Collection<? extends InterproceduralExceptionFilter<Instruction>> c) {
    return this.filter.addAll(c);
  }  
  
  @Override
  public ExceptionFilter<Instruction> getFilter(CGNode node) {
    CombinedExceptionFilter<Instruction> result = new CombinedExceptionFilter<>();
    for (InterproceduralExceptionFilter<Instruction> exceptionFilter:filter) {
      result.add(exceptionFilter.getFilter(node));
    }    
    
    return result;
  }

}
