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
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
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

package com.ibm.wala.dalvik.dex.instructions;

import com.ibm.wala.dalvik.classLoader.DexIMethod;
import org.jf.dexlib2.Opcode;

public class Switch extends Instruction {

  public final int regA;
  public final int tableAddressOffset;
  public SwitchPad pad;
  private int[] casesAndLabels;
  private int defaultLabel;

  public Switch(int instLoc, int regA, int tableAddressOffset, Opcode opcode, DexIMethod method) {
    super(instLoc, opcode, method);
    this.regA = regA;
    this.tableAddressOffset = tableAddressOffset;
  }

  public void setSwitchPad(SwitchPad pad) {
    this.pad = pad;
    computeCasesAndLabels();
  }

  private void computeCasesAndLabels() {
    casesAndLabels = pad.getLabelsAndOffsets();

    for (int i = 1; i < casesAndLabels.length; i += 2)
      //            casesAndLabels[i] = method.getInstructionIndex(pc+casesAndLabels[i]);
      casesAndLabels[i] = pc + casesAndLabels[i];

    // defaultLabel = method.getInstructionIndex(pc + pad.getDefaultOffset());
    defaultLabel = pc + pad.getDefaultOffset();
  }

  public int[] getOffsets() {
    return pad.getOffsets();
  }

  public int getDefaultLabel() {
    return defaultLabel;
  }

  public int[] getCasesAndLabels() {
    return casesAndLabels;
  }

  @Override
  public int[] getBranchTargets() {
    int[] r = new int[casesAndLabels.length / 2 + 1];
    r[0] = method.getInstructionIndex(defaultLabel);
    for (int i = 1; i < r.length; i++) {
      r[i] = method.getInstructionIndex(casesAndLabels[(i - 1) * 2 + 1]);
    }
    return r;
  }

  @Override
  public void visit(Visitor visitor) {
    visitor.visitSwitch(this);
  }
}
