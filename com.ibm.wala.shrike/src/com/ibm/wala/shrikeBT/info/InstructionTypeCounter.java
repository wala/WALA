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

import com.ibm.wala.shrikeBT.ArrayLengthInstruction;
import com.ibm.wala.shrikeBT.ArrayLoadInstruction;
import com.ibm.wala.shrikeBT.ArrayStoreInstruction;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrikeBT.CheckCastInstruction;
import com.ibm.wala.shrikeBT.ComparisonInstruction;
import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.ConversionInstruction;
import com.ibm.wala.shrikeBT.DupInstruction;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.GetInstruction;
import com.ibm.wala.shrikeBT.GotoInstruction;
import com.ibm.wala.shrikeBT.InstanceofInstruction;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrikeBT.LoadInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MonitorInstruction;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeBT.PopInstruction;
import com.ibm.wala.shrikeBT.PutInstruction;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.ShiftInstruction;
import com.ibm.wala.shrikeBT.StoreInstruction;
import com.ibm.wala.shrikeBT.SwapInstruction;
import com.ibm.wala.shrikeBT.SwitchInstruction;
import com.ibm.wala.shrikeBT.ThrowInstruction;
import com.ibm.wala.shrikeBT.UnaryOpInstruction;

/**
 * This method annotation counts the number of instructions of each type
 * (according to each Instruction subclass).
 * 
 * The get...Count methods are the only methods needed by clients. These methods
 * check to see if the MethodData object already has an InstructionTypeCounter
 * annotation before recomputing the counts and returning the desired count.
 */
public class InstructionTypeCounter implements MethodData.Results {
  private final static String key = InstructionTypeCounter.class.getName();

  private int countMonitors;

  private int countGets;

  private int countPuts;

  private int countArrayLoads;

  private int countArrayStores;

  private int countInvokes;

  private int countArrayLengths;

  private int countBinaryOps;

  private int countCheckCasts;

  private int countComparisons;

  private int countConditionalBranches;

  private int countConstants;

  private int countConversions;

  private int countDups;

  private int countGotos;

  private int countInstanceOfs;

  private int countLocalLoads;

  private int countLocalStores;

  private int countNews;

  private int countPops;

  private int countReturns;

  private int countShifts;

  private int countSwaps;

  private int countSwitches;

  private int countThrows;

  private int countUnaryOps;

  InstructionTypeCounter(MethodData info) {
    recalculateFrom(info.getInstructions());
  }

  private void recalculateFrom(Instruction[] instructions) {
    countMonitors = 0;
    countGets = 0;
    countPuts = 0;
    countArrayLoads = 0;
    countArrayStores = 0;
    countInvokes = 0;
    countArrayLengths = 0;
    countBinaryOps = 0;
    countCheckCasts = 0;
    countComparisons = 0;
    countConditionalBranches = 0;
    countConstants = 0;
    countConversions = 0;
    countDups = 0;
    countGotos = 0;
    countInstanceOfs = 0;
    countLocalLoads = 0;
    countLocalStores = 0;
    countNews = 0;
    countPops = 0;
    countReturns = 0;
    countShifts = 0;
    countSwaps = 0;
    countSwitches = 0;
    countThrows = 0;
    countUnaryOps = 0;

    Instruction.Visitor visitor = new Instruction.Visitor() {
      public void visitArrayLength(ArrayLengthInstruction instruction) {
        countArrayLengths++;
      }

      public void visitBinaryOp(BinaryOpInstruction instruction) {
        countBinaryOps++;
      }

      public void visitCheckCast(CheckCastInstruction instruction) {
        countCheckCasts++;
      }

      public void visitComparison(ComparisonInstruction instruction) {
        countComparisons++;
      }

      public void visitConditionalBranch(ConditionalBranchInstruction instruction) {
        countConditionalBranches++;
      }

      public void visitConstant(ConstantInstruction instruction) {
        countConstants++;
      }

      public void visitConversion(ConversionInstruction instruction) {
        countConversions++;
      }

      public void visitDup(DupInstruction instruction) {
        countDups++;
      }

      public void visitGoto(GotoInstruction instruction) {
        countGotos++;
      }

      public void visitInstanceof(InstanceofInstruction instruction) {
        countInstanceOfs++;
      }

      public void visitLocalLoad(LoadInstruction instruction) {
        countLocalLoads++;
      }

      public void visitLocalStore(StoreInstruction instruction) {
        countLocalStores++;
      }

      public void visitNew(NewInstruction instruction) {
        countNews++;
      }

      public void visitPop(PopInstruction instruction) {
        countPops++;
      }

      public void visitReturn(ReturnInstruction instruction) {
        countReturns++;
      }

      public void visitShift(ShiftInstruction instruction) {
        countShifts++;
      }

      public void visitSwap(SwapInstruction instruction) {
        countSwaps++;
      }

      public void visitSwitch(SwitchInstruction instruction) {
        countSwitches++;
      }

      public void visitThrow(ThrowInstruction instruction) {
        countThrows++;
      }

      public void visitUnaryOp(UnaryOpInstruction instruction) {
        countUnaryOps++;
      }

      public void visitArrayLoad(ArrayLoadInstruction instruction) {
        countArrayLoads++;
      }

      public void visitArrayStore(ArrayStoreInstruction instruction) {
        countArrayStores++;
      }

      public void visitGet(GetInstruction instruction) {
        countGets++;
      }

      public void visitPut(PutInstruction instruction) {
        countPuts++;
      }

      public void visitMonitor(MonitorInstruction instruction) {
        countMonitors++;
      }

      public void visitInvoke(InvokeInstruction instruction) {
        countInvokes++;
      }
    };

    for (int i = 0; i < instructions.length; i++) {
      instructions[i].visit(visitor);
    }
  }

  /**
   * Whenever the underlying method is updated, we'll throw away our counts so
   * they can be reconstructed from scratch next time.
   * 
   * This is not to be called by clients.
   */
  public boolean notifyUpdate(MethodData info, Instruction[] newInstructions, ExceptionHandler[][] newHandlers,
      int[] newInstructionMap) {
    // just throw this away and we'll recalculate from scratch if necessary
    return true;
  }

  public static int getArrayLoadCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countArrayLoads;
  }

  public static int getArrayStoreCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countArrayStores;
  }

  public static int getGetCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countGets;
  }

  public static int getPutCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countPuts;
  }

  public static int getMonitorCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countMonitors;
  }

  public static int getInvokeCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countInvokes;
  }

  public static int getComparisonCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countComparisons;
  }

  public static int getArrayLengthCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countArrayLengths;
  }

  public static int getConstantCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countConstants;
  }

  public static int getShiftCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countShifts;
  }

  public static int getSwitchesCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countSwitches;
  }

  public static int getSwapCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countSwaps;
  }

  public static int getBinaryOpCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countBinaryOps;
  }

  public static int getCheckCastCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countCheckCasts;
  }

  public static int getThrowCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countThrows;
  }

  public static int getConditionalBranchCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countConditionalBranches;
  }

  public static int getConversionCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countConversions;
  }

  public static int getDupCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countDups;
  }

  public static int getGotoCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countGotos;
  }

  public static int getReturnCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countReturns;
  }

  public static int getInstanceOfCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countInstanceOfs;
  }

  public static int getLocalLoadCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countLocalLoads;
  }

  public static int getLocalStoreCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countLocalStores;
  }

  public static int getNewCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countNews;
  }

  public static int getPopCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countPops;
  }

  public static int getUnaryOpCount(MethodData info) throws IllegalArgumentException {
    if (info == null) {
      throw new IllegalArgumentException();
    }
    return getCounter(info).countUnaryOps;
  }

  private static InstructionTypeCounter getCounter(MethodData info) {
    InstructionTypeCounter c = (InstructionTypeCounter) info.getInfo(key);
    if (c == null) {
      c = new InstructionTypeCounter(info);
      info.putInfo(key, c);
    }

    return c;
  }
}