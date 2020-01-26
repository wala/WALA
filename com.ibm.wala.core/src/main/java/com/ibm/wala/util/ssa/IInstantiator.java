/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

/*
*  Copyright (c) 2013,
*      Tobias Blaschke <code@tobiasblaschke.de>
*  All rights reserved.

*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  1. Redistributions of source code must retain the above copyright notice,
*     this list of conditions and the following disclaimer.
*
*  2. Redistributions in binary form must reproduce the above copyright notice,
*     this list of conditions and the following disclaimer in the documentation
*     and/or other materials provided with the distribution.
*
*  3. The names of the contributors may not be used to endorse or promote
*     products derived from this software without specific prior written
*     permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package com.ibm.wala.util.ssa;

import com.ibm.wala.types.TypeReference;

/**
 * Used for CallBacks to create an Instance.
 *
 * <p>When methods in this package detect, that they have to generate a new instance of a type this
 * CallBack will be used.
 *
 * <p>This mainly applies to the connectThrough-Function.
 *
 * @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public interface IInstantiator {
  /**
   * Create an instance of type.
   *
   * <p>The varArgs argument gets passed through from functions like connectThrough (which have
   * varArgs as well) to the instantiatior.
   *
   * @param type Type to generate an instance from
   * @param instantiatorArgs passed through utility functions
   * @return SSA-Number of the instance
   */
  public int createInstance(TypeReference type, Object... instantiatorArgs);
}
