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
package com.ibm.wala.ipa.cfg.exceptionpruning.filter;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.FilteredException;

public class DummyFilter<Instruction> implements ExceptionFilter<Instruction>{
  @Override
  public boolean alwaysThrowsException(Instruction instruction) {
    return false;
  }

  @Override
  public Collection<FilteredException> filteredExceptions(Instruction instruction) {
    return Collections.emptyList();
  }
}
