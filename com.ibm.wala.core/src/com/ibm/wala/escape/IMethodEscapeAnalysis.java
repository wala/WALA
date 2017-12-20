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
package com.ibm.wala.escape;

import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.WalaException;

/**
 * Basic interface from which to execute and get the results of escape analysis.
 */
public interface IMethodEscapeAnalysis {

  /**
   * @param allocMethod a method which holds an allocation site
   * @param allocPC bytecode index of allocation site
   * @param m method in question
   * @return true if an object allocated at the allocation site &lt;allocMethod,allocPC&gt; may escape from an activation of method m,
   *          false otherwise
   */
  public boolean mayEscape(MethodReference allocMethod, int allocPC, MethodReference m) throws WalaException;

}
