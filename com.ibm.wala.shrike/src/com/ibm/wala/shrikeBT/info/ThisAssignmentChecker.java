/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT.info;

import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.StoreInstruction;

/**
 * This method annotation checks to see whether "this" is assigned to by the method. The result is cached in an annotation.
 */
public class ThisAssignmentChecker implements MethodData.Results {
  private final static String key = ThisAssignmentChecker.class.getName();

  private boolean assignmentToThis;

  ThisAssignmentChecker(MethodData info) {
    recalculateFrom(info);
  }

  private void recalculateFrom(MethodData info) {
    assignmentToThis = false;

    if (!info.getIsStatic()) {
      IInstruction[] instructions = info.getInstructions();

      for (IInstruction instr : instructions) {
        if (instr instanceof StoreInstruction) {
          StoreInstruction st = (StoreInstruction) instr;
          if (st.getVarIndex() == 0) {
            assignmentToThis = true;
          }
        }
      }
    }
  }

  /**
   * This should not be called by any client.
   */
  @Override
  public boolean notifyUpdate(MethodData info, IInstruction[] newInstructions, ExceptionHandler[][] newHandlers,
      int[] newInstructionMap) {
    // just throw this away and we'll recalculate from scratch if necessary
    return true;
  }

  /**
   * @return true iff 'this' is assigned to by the method
   */
  public static boolean isThisAssigned(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    ThisAssignmentChecker c = (ThisAssignmentChecker) info.getInfo(key);
    if (c == null) {
      c = new ThisAssignmentChecker(info);
      info.putInfo(key, c);
    }

    return c.assignmentToThis;
  }
}
