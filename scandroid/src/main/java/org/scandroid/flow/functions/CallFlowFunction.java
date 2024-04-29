/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * This file is a derivative of code released under the terms listed below.
 *
 */
/*
 * Copyright (c) 2009-2012,
 *
 * <p>Galois, Inc. (Aaron Tomb <atomb@galois.com>, Rogan Creswick <creswick@galois.com>, Adam
 * Foltzer <acfoltzer@galois.com>) Steve Suh <suhsteve@gmail.com>
 *
 * <p>All rights reserved.
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>3. The names of the contributors may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.scandroid.flow.functions;

import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.scandroid.domain.CodeElement;
import org.scandroid.domain.DomainElement;
import org.scandroid.domain.IFDSTaintDomain;
import org.scandroid.domain.LocalElement;

/**
 * @author creswick
 * @author acfoltzer
 */
public class CallFlowFunction<E extends ISSABasicBlock> implements IUnaryFlowFunction {

  /**
   * A map from the code elements of actual parameters, to the set of code elements for formal
   * parameters
   */
  private final Map<CodeElement, Set<CodeElement>> paramArgsMap;

  private final IFDSTaintDomain<E> domain;

  public CallFlowFunction(IFDSTaintDomain<E> domain, List<CodeElement> actualParams) {
    this.domain = domain;

    final int numParams = actualParams.size();
    this.paramArgsMap = HashMapFactory.make(numParams);
    for (int i = 0; i < numParams; i++) {
      // add a mapping for each parameter
      final CodeElement actual = actualParams.get(i);
      if (!(actual instanceof LocalElement)) {}

      final CodeElement formal = new LocalElement(i + 1); // +1 for SSA
      Set<CodeElement> existingFormals = paramArgsMap.get(actual);
      if (null == existingFormals) {
        existingFormals = HashSetFactory.make(numParams);
      }
      existingFormals.add(formal);
      paramArgsMap.put(actual, existingFormals);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction#getTargets(int)
   */
  @Override
  public IntSet getTargets(int d1) {
    if (0 == d1) {
      return TaintTransferFunctions.ZERO_SET;
    }
    DomainElement de = domain.getMappedObject(d1);
    MutableSparseIntSet set = MutableSparseIntSet.makeEmpty();

    /*
     * We're in the situation of calling a function:
     *
     * f(x, y, z)
     *
     * And determining how taints flow from that call site to the entry of
     * the function
     *
     * ... f(X x, Y y, Z z) { ... }
     *
     * Our goals are twofold: 1. Propagate taints from the actual parameter
     * x to the formal parameter x 2. Exclude any other non-local
     * information from propagating to callee
     *
     * Since we're unioning the result of this with the
     * GlobalIdentityFunction, we don't have to worry about 2 for this
     * IntSet.
     */

    final Set<CodeElement> formals = paramArgsMap.get(de.codeElement);
    if (null != formals) {
      for (CodeElement formal : formals) {
        set.add(domain.getMappedIndex(new DomainElement(formal, de.taintSource)));
      }
    }
    return set;
  }
}
