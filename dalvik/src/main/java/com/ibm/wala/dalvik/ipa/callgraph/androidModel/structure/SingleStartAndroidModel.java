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
package com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure;

import com.ibm.wala.core.util.ssa.SSAValue;
import com.ibm.wala.core.util.ssa.SSAValue.NamedKey;
import com.ibm.wala.core.util.ssa.SSAValue.TypeKey;
import com.ibm.wala.core.util.ssa.SSAValue.VariableKey;
import com.ibm.wala.core.util.ssa.SSAValueManager;
import com.ibm.wala.core.util.ssa.TypeSafeInstructionFactory;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.ExecutionOrder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;
import com.ibm.wala.shrike.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.types.TypeReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds an Android Model incorporating a single loop.
 *
 * <p>This class models a single run of an Andoird-Component: E.g. The view of an Activity gets
 * shown only once.
 *
 * <p>The incorporated loop is wrapped around user-interaction methods. These are in the section
 * MULTIPLE_TIMES_IN_LOOP.
 *
 * @see com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.LoopAndroidModel
 * @see com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.LoopKillAndroidModel
 * @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public class SingleStartAndroidModel extends AbstractAndroidModel {
  private static final Logger logger = LoggerFactory.getLogger(SingleStartAndroidModel.class);

  // protected VolatileMethodSummary body;
  // protected JavaInstructionFactory insts;
  // protected DexFakeRootMethod.ReuseParameters paramTypes;
  /**
   * @param body The MethodSummary to add instructions to
   * @param insts Will be used to generate the instructions
   */
  public SingleStartAndroidModel(
      VolatileMethodSummary body,
      TypeSafeInstructionFactory insts,
      SSAValueManager paramManager,
      Iterable<? extends Entrypoint> entryPoints) {
    super(body, insts, paramManager, entryPoints);
  }

  private int outerLoopPC = -1;
  Map<TypeReference, SSAValue> outerStartingPhis;

  /**
   * Prepares the PC to get looped to.
   *
   * <p>Thus it tries to assure a new basic block starts here. Additionally it reserves some space
   * for the insertion of Phi-Functions.
   *
   * <p>{@inheritDoc}
   */
  @Override
  protected int enterMULTIPLE_TIMES_IN_LOOP(int PC) {
    logger.info("PC {} is the jump target of START_OF_LOOP", PC);
    this.outerLoopPC = PC;
    PC = body.getNextProgramCounter();
    paramManager.scopeDown(true);

    // Top-Half of Phi-Handling
    outerStartingPhis = new HashMap<>();
    List<TypeReference> outerPhisNeeded =
        returnTypesBetween(ExecutionOrder.START_OF_LOOP, ExecutionOrder.AFTER_LOOP);

    for (TypeReference phiType : outerPhisNeeded) {
      final TypeKey phiKey = new TypeKey(phiType.getName());
      if (paramManager.isSeen(phiKey, false)) {
        final SSAValue newValue = paramManager.getFree(phiType, phiKey);
        outerStartingPhis.put(phiType, newValue);
      }
    }

    body.reserveProgramCounters(outerPhisNeeded.size());
    // Actual Phis will be placed by the bottom-half handler...

    PC = body.getNextProgramCounter(); // Needed if no calls
    return PC;
  }

  /**
   * Loops to MULTIPLE_TIMES_IN_LOOP.
   *
   * <p>It inserts a gotoInstruction and fills the space reserved before with actual PhiInstructions
   *
   * <p>{@inheritDoc}
   */
  @Override
  protected int enterEND_OF_LOOP(int PC) {
    assert (outerLoopPC > 0)
        : "Somehow you managed to get the loop-target negative. This is wierd!";

    // Insert the Phis at the beginning of the Block
    int phiPC = outerLoopPC + 1;
    boolean oldAllowReserved = body.allowReserved(true);
    logger.info("Setting block-inner Phis");
    for (final SSAValue oldPhi : outerStartingPhis.values()) {
      final List<SSAValue> forPhi = new ArrayList<>(2);
      forPhi.add(paramManager.getSuper(oldPhi.key));
      forPhi.add(paramManager.getCurrent(oldPhi.key));

      SSAPhiInstruction phi = insts.PhiInstruction(phiPC, oldPhi, forPhi);
      phiPC++;
      body.addStatement(phi);
      paramManager.setPhi(oldPhi, phi);
    }
    body.allowReserved(oldAllowReserved);

    // Close the Loop
    logger.info("Closing Loop");
    logger.info("PC {}: Goto {}", PC, outerLoopPC);
    if (PC != outerLoopPC) {
      NamedKey trueKey = new SSAValue.NamedKey(TypeReference.BooleanName, "true");
      SSAValue trueVal = paramManager.getFree(TypeReference.Boolean, trueKey);
      paramManager.setPhi(trueVal, null);
      body.addConstant(trueVal.getNumber(), new ConstantValue(true));
      body.addStatement(
          insts.ConditionalBranchInstruction(
              PC,
              IConditionalBranchInstruction.Operator.EQ,
              TypeReference.Boolean,
              trueVal.getNumber(),
              trueVal.getNumber(),
              outerLoopPC));
    }
    paramManager.scopeUp();

    // Add Phi-Statements at the beginning of this block...
    logger.info("Setting outer-block Phis");
    for (Map.Entry<TypeReference, SSAValue> entry : outerStartingPhis.entrySet()) {
      final SSAValue ssaValue = entry.getValue();
      final VariableKey phiKey = ssaValue.key;
      PC = body.getNextProgramCounter();

      List<SSAValue> all = paramManager.getAllForPhi(phiKey);
      final TypeReference phiType = entry.getKey();
      logger.debug("Into phi {} for {}", all, phiType.getName());
      // Narf ... unpacking...

      paramManager.invalidate(phiKey);
      final SSAValue newValue = paramManager.getFree(phiType, phiKey);
      SSAPhiInstruction phi = insts.PhiInstruction(PC, newValue, all);
      body.addStatement(phi);

      paramManager.setPhi(newValue, phi);
    }
    PC = body.getNextProgramCounter();
    return PC;
  }
}
