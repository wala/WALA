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

package com.ibm.wala.ipa.callgraph;

import java.util.Iterator;

/**
 *
 * An object that implements this interface provides an enumeration
 * of the methods that should be considered entrypoints during call graph
 * construction
 * 
 * @author sfink
 */
public interface Entrypoints {
  /**
   * Return an iterator of Entrypoint which represents the methods that should
   * be considered entrypoints during call graph construction
   * @return an Iterator of Entrypoint
   */
  public Iterator<Entrypoint> iterator();

}
