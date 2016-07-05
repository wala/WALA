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

import java.util.LinkedHashMap;
import java.util.Map;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;

public abstract class StoringExceptionFilter<Instruction> implements InterproceduralExceptionFilter<Instruction>{
  private Map<CGNode, ExceptionFilter<Instruction>> store;
  
  public StoringExceptionFilter(){
    this.store = new LinkedHashMap<>();
  }
  
  abstract protected ExceptionFilter<Instruction> computeFilter(CGNode node);
  
  @Override
  public ExceptionFilter<Instruction> getFilter(CGNode node) {
    if (store.containsKey(node)) {
      return store.get(node);
    } else {
      ExceptionFilter<Instruction> filter = computeFilter(node);
      store.put(node, filter);
      return filter;
    }
  }
}
