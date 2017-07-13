/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.dataflow.graph;

import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.fixpoint.IVariable;

/**
 * Abstract superclass for meet operators
 */
public abstract class AbstractMeetOperator<T extends IVariable<T>> extends AbstractOperator<T> {

  /**
   * subclasses can override if needed
   * @return true iff this meet is a noop when applied to one argument
   */
  public boolean isUnaryNoOp() {
    return true;
  }
}
