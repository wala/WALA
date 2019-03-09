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
 *
 * Copyright (c) 2009-2012,
 *
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.scandroid.flow.types;

import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;

/**
 * A flow to or from the parameter of a method. This can represent formal parameters of methods
 * being analyzed, or actual parameters of methods being called. In the former case, the associated
 * block is the entry block of the method. In the latter case, the block is the block containing the
 * invoke instruction.
 *
 * @author atomb
 */
public class ParameterFlow<E extends ISSABasicBlock> extends FlowType<E> {

  private final int argNum;

  public ParameterFlow(BasicBlockInContext<E> block, int argNum, boolean source) {
    super(block, source);
    this.argNum = argNum;
  }

  public int getArgNum() {
    return argNum;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + argNum;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    @SuppressWarnings("unchecked")
    ParameterFlow<E> other = (ParameterFlow<E>) obj;
    if (argNum != other.argNum) return false;
    return true;
  }

  @Override
  public String toString() {
    return "ParameterFlow( argNum=" + argNum + ' ' + super.toString() + ')';
  }

  @Override
  public String descString() {
    String s = "arg(" + argNum + ')';
    if (!getBlock().isEntryBlock()) {
      SSAInvokeInstruction inv =
          (SSAInvokeInstruction) ((IExplodedBasicBlock) getBlock().getDelegate()).getInstruction();
      s = s + ':' + inv.getDeclaredTarget().getSignature();
    }
    return s;
  }

  @Override
  public <R> R visit(FlowTypeVisitor<E, R> v) {
    return v.visitParameterFlow(this);
  }
}
