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
import com.ibm.wala.shrikeBT.ILoadInstruction;
import com.ibm.wala.shrikeBT.IStoreInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.Util;

/**
 * This method annotation parcels out fresh local variables for use as temporaries by instrumentation code. It assumes that local
 * variables are not allocated by any other mechanism.
 */
public class LocalAllocator implements MethodData.Results {
  private final static String key = LocalAllocator.class.getName();

  private int nextLocal;

  LocalAllocator(MethodData info) {
    recalculateFrom(info);
  }

  private void recalculateFrom(MethodData info) {
    IInstruction[] instructions = info.getInstructions();
    final int[] max = { Util.getParamsWordSize(info.getSignature()) + (info.getIsStatic() ? 0 : 1) };

    IInstruction.Visitor visitor = new IInstruction.Visitor() {
      @Override
      public void visitLocalLoad(ILoadInstruction instruction) {
        int v = instruction.getVarIndex() + Util.getWordSize(instruction.getType());
        if (v > max[0]) {
          max[0] = v;
        }
      }

      @Override
      public void visitLocalStore(IStoreInstruction instruction) {
        int v = instruction.getVarIndex() + Util.getWordSize(instruction.getType());
        if (v > max[0]) {
          max[0] = v;
        }
      }
    };

    for (IInstruction instruction : instructions) {
      instruction.visit(visitor);
    }

    nextLocal = max[0];
  }

  private int allocateLocals(int count) {
    int r = nextLocal;
    nextLocal += count;
    return r;
  }

  /**
   * This should not be called by clients.
   */
  @Override
  public boolean notifyUpdate(MethodData info, IInstruction[] newInstructions, ExceptionHandler[][] newHandlers,
      int[] newInstructionMap) {
    return false;
  }

  /**
   * Allocates a new local variable of the specified type.
   */
  public static int allocate(MethodData info, int count) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    LocalAllocator l = (LocalAllocator) info.getInfo(key);
    if (l == null) {
      l = new LocalAllocator(info);
      info.putInfo(key, l);
    }

    return l.allocateLocals(count);
  }

  public static int allocate(MethodData info, String type) throws IllegalArgumentException {
    return allocate(info, type == null ? 2 : Util.getWordSize(type));
  }

  /**
   * Allocates a new local that will fit any type.
   */
  public static int allocate(MethodData info) throws IllegalArgumentException {
    return allocate(info, null);
  }
}
