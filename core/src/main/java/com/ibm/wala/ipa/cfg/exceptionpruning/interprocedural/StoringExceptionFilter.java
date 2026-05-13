/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.cfg.exceptionpruning.interprocedural;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NonNull;

public abstract class StoringExceptionFilter<Instruction>
    implements InterproceduralExceptionFilter<Instruction> {
  private final Map<CGNode, @NonNull ExceptionFilter<Instruction>> store;

  public StoringExceptionFilter() {
    this.store = new LinkedHashMap<>();
  }

  protected abstract @NonNull ExceptionFilter<Instruction> computeFilter(CGNode node);

  @Override
  public ExceptionFilter<Instruction> getFilter(CGNode node) {
    return store.computeIfAbsent(node, this::computeFilter);
  }
}
