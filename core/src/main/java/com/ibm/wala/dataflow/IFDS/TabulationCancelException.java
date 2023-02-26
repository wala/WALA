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
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.CancelException;

/**
 * A {@link CancelException} thrown during tabulation; holds a pointer to a partial {@link
 * com.ibm.wala.dataflow.IFDS.TabulationSolver.Result}. Use with care, this can hold on to a lot of
 * memory.
 */
public class TabulationCancelException extends CancelException {

  private static final long serialVersionUID = 4073189707860241945L;
  private final TabulationSolver<?, ?, ?>.Result result;

  protected TabulationCancelException(Exception cause, TabulationSolver<?, ?, ?>.Result r) {
    super(cause);
    this.result = r;
  }

  public TabulationSolver<?, ?, ?>.Result getResult() {
    return result;
  }
}
