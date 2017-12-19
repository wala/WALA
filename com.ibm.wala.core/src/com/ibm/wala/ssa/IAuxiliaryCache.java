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
package com.ibm.wala.ssa;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;

interface IAuxiliaryCache {

  /**
   * The existence of this is unfortunate.
   */
  void wipe();

  /**
   * @param m a method
   * @param options options governing ssa construction
   * @return the object cached for m, or null if none found
   */
  Object find(IMethod m, Context c, SSAOptions options);

  /**
   * cache new auxiliary information for an &lt;m,options&gt; pair
   * 
   * @param m a method
   * @param options options governing ssa construction
   */
  void cache(IMethod m, Context c, SSAOptions options, Object aux);

  /**
   * invalidate all cached information about a method
   */
  void invalidate(IMethod method, Context c);

}
