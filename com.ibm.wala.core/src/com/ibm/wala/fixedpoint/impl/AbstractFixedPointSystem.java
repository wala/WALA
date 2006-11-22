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
package com.ibm.wala.fixedpoint.impl;

import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.fixpoint.IFixedPointSystem;
import com.ibm.wala.fixpoint.IFixedPointSystemListener;

/**
 * 
 * Basic functionality for managing fixed-point system listeners
 * 
 * @author sfink
 */
public abstract class AbstractFixedPointSystem implements IFixedPointSystem {

  /**
   * List<IFixedPointListener>
   */
  private List<IFixedPointSystemListener> listeners = new LinkedList<IFixedPointSystemListener>();
  
 
  /* (non-Javadoc)
   */
  public void addListener(IFixedPointSystemListener l) {
    listeners.add(l);
  }

  /* (non-Javadoc)
   */
  public void removeListener(IFixedPointSystemListener l) {
    listeners.remove(l);
  }
}