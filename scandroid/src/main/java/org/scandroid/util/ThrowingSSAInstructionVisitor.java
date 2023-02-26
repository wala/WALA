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
package org.scandroid.util;

import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction.IVisitor;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;

public class ThrowingSSAInstructionVisitor implements IVisitor {
  private final RuntimeException e;

  public ThrowingSSAInstructionVisitor(RuntimeException e) {
    this.e = e;
  }

  @Override
  public void visitGoto(SSAGotoInstruction instruction) {
    throw e;
  }

  @Override
  public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
    throw e;
  }

  @Override
  public void visitArrayStore(SSAArrayStoreInstruction instruction) {
    throw e;
  }

  @Override
  public void visitBinaryOp(SSABinaryOpInstruction instruction) {
    throw e;
  }

  @Override
  public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
    throw e;
  }

  @Override
  public void visitConversion(SSAConversionInstruction instruction) {
    throw e;
  }

  @Override
  public void visitComparison(SSAComparisonInstruction instruction) {
    throw e;
  }

  @Override
  public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
    throw e;
  }

  @Override
  public void visitSwitch(SSASwitchInstruction instruction) {
    throw e;
  }

  @Override
  public void visitReturn(SSAReturnInstruction instruction) {
    throw e;
  }

  @Override
  public void visitGet(SSAGetInstruction instruction) {
    throw e;
  }

  @Override
  public void visitPut(SSAPutInstruction instruction) {
    throw e;
  }

  @Override
  public void visitInvoke(SSAInvokeInstruction instruction) {
    throw e;
  }

  @Override
  public void visitNew(SSANewInstruction instruction) {
    throw e;
  }

  @Override
  public void visitArrayLength(SSAArrayLengthInstruction instruction) {
    throw e;
  }

  @Override
  public void visitThrow(SSAThrowInstruction instruction) {
    throw e;
  }

  @Override
  public void visitMonitor(SSAMonitorInstruction instruction) {
    throw e;
  }

  @Override
  public void visitCheckCast(SSACheckCastInstruction instruction) {
    throw e;
  }

  @Override
  public void visitInstanceof(SSAInstanceofInstruction instruction) {
    throw e;
  }

  @Override
  public void visitPhi(SSAPhiInstruction instruction) {
    throw e;
  }

  @Override
  public void visitPi(SSAPiInstruction instruction) {
    throw e;
  }

  @Override
  public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
    throw e;
  }

  @Override
  public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
    throw e;
  }
}
